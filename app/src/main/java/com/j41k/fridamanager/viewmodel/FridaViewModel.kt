package com.j41k.fridamanager.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.j41k.fridamanager.data.FridaFile
import com.j41k.fridamanager.data.FridaShell
import com.j41k.fridamanager.net.FridaDownloader
import com.j41k.fridamanager.service.FridaService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.OutputStream

/** Nivel semántico de un mensaje de log (detectado por símbolo prefijo). */
enum class LogLevel(val symbol: String) {
    INFO("▶"), OK("✓"), WARN("⚠"), ERROR("✗"), TRACE("·");

    companion object {
        fun detect(message: String): LogLevel {
            // El message puede venir con timestamp [HH:mm:ss] al inicio.
            val tail = message.substringAfter(']', message).trimStart()
            return when {
                tail.startsWith("✓") -> OK
                tail.startsWith("✗") -> ERROR
                tail.startsWith("⚠") -> WARN
                tail.startsWith("▶") || tail.startsWith("■") || tail.startsWith("↻") -> INFO
                else -> TRACE
            }
        }
    }
}

data class LogEntry(
    val id: Long,
    val timestamp: String,
    val message: String,
    val level: LogLevel
) {
    fun toExportLine(): String = "[$timestamp] ${level.symbol} $message"
}

/** Mensaje efímero para el usuario (Snackbar), con acción opcional. */
data class UiMessage(
    val text: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

class FridaViewModel(application: Application) : AndroidViewModel(application) {

    private val fridaShell = FridaShell()
    private val fridaDownloader = FridaDownloader(application)

    var isRooted by mutableStateOf(false)
    var fridaStatus by mutableStateOf(FridaShell.FridaStatus(false))
    val isFridaRunning get() = fridaStatus.isRunning

    // ── Requisitos de entorno ─────────────────────────────────
    var isDeveloperModeEnabled by mutableStateOf(false)
    var isUsbDebuggingEnabled  by mutableStateOf(false)

    var isTransitioning by mutableStateOf(false)

    var availableBinaries by mutableStateOf<List<FridaFile>>(emptyList())
    var selectedBinary by mutableStateOf<FridaFile?>(null)

    var currentTab by mutableStateOf(0)
    var uptimeMillis by mutableStateOf(0L)
    private var uptimeJob: Job? = null
    private var uptimeStartMs: Long = 0L

    /** Buffer estructurado de logs (sustituye a la lista de strings plana). */
    val logEntries = mutableStateListOf<LogEntry>()

    private var nextLogId = 0L

    /** Último id de log visto en la pestaña Registros (para el badge de no leídos). */
    var logsSeenUpTo by mutableLongStateOf(-1L)

    val unreadLogCount: Int
        get() = logEntries.count { it.id > logsSeenUpTo }

    fun markLogsSeen() {
        logEntries.lastOrNull()?.let { logsSeenUpTo = it.id }
    }

    private var logcatJob: Job? = null

    private val _messages = MutableSharedFlow<UiMessage>(extraBufferCapacity = 8)
    val messages: SharedFlow<UiMessage> = _messages.asSharedFlow()

    fun notify(text: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
        _messages.tryEmit(UiMessage(text, actionLabel, onAction))
    }

    var isDownloading by mutableStateOf(false)
    var downloadProgress by mutableStateOf(0f)
    var downloadStatus by mutableStateOf("")

    var isDeleting by mutableStateOf(false)

    var remoteRelease by mutableStateOf<FridaDownloader.FridaRelease?>(null)
    var isCheckingRemote by mutableStateOf(false)

    // Configuración persistente (en memoria; persistencia real → fuera de scope)
    var fridaPort by mutableStateOf("27042")
    var fridaAddress by mutableStateOf("0.0.0.0")
    var isSelinuxPermissive by mutableStateOf(false)
    var autoPermissive by mutableStateOf(true) // Activado por defecto para Samsung/otros

    // Filtros / UI estado de Logs
    var logSearchQuery by mutableStateOf("")
    var logLevelFilter by mutableStateOf<LogLevel?>(null) // null = TODO
    var logAutoScroll by mutableStateOf(true)

    // ── Validación de configuración de red ────────────────────
    val portError: String?
        get() {
            val p = fridaPort.toIntOrNull()
            return when {
                fridaPort.isBlank() -> "Introduce un puerto"
                p == null || p !in 1..65535 -> "Puerto entre 1 y 65535"
                else -> null
            }
        }

    val addressError: String?
        get() {
            val parts = fridaAddress.split(".")
            val valid = parts.size == 4 && parts.all { part ->
                part.isNotEmpty() && part.length <= 3 && part.toIntOrNull()?.let { it in 0..255 } == true
            }
            return if (valid) null else "Dirección IPv4 no válida (ej. 0.0.0.0)"
        }

    val isNetworkConfigValid: Boolean get() = portError == null && addressError == null

    private var pollingJob: Job? = null

    init {
        checkDevSettings()
        fridaShell.checkRoot { rooted ->
            isRooted = rooted
            if (rooted) {
                isSelinuxPermissive = fridaShell.isSelinuxPermissive()
                refreshBinaries()
                startStatusPolling()
                checkRemoteRelease()
                startLogcatCapture()
            } else {
                addRawLog("✗ Root denegado. La app necesita permisos root para operar.")
            }
        }
    }

    /** Lee las settings de desarrollador del sistema (no requiere root). */
    fun checkDevSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            val cr = getApplication<Application>().contentResolver
            val devMode = Settings.Global.getInt(
                cr, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
            ) == 1
            val adb = Settings.Global.getInt(
                cr, Settings.Global.ADB_ENABLED, 0
            ) == 1
            withContext(Dispatchers.Main) {
                isDeveloperModeEnabled = devMode
                isUsbDebuggingEnabled  = adb
            }
        }
    }

    /**
     * Verifica si el binario seleccionado es compatible con la arquitectura del dispositivo.
     * Retorna true si no hay binario seleccionado (el problema es "no hay binario", no incompatibilidad).
     */
    fun isBinaryCompatibleWithDevice(): Boolean {
        val binary = selectedBinary ?: return true
        val name = binary.name.lowercase()
        val arch = deviceArchitecture().lowercase()
        return when {
            arch.contains("arm64")   -> name.contains("arm64")
            arch.contains("arm")     -> name.contains("-arm") && !name.contains("arm64")
            arch.contains("x86_64")  -> name.contains("x86_64")
            arch.contains("x86")     -> name.contains("x86") && !name.contains("x86_64")
            else                     -> true
        }
    }

    fun checkRemoteRelease() {
        viewModelScope.launch(Dispatchers.IO) {
            isCheckingRemote = true
            remoteRelease = fridaDownloader.getLatestRelease()
            isCheckingRemote = false
        }
    }

    fun deviceArchitecture(): String = fridaDownloader.getDeviceArchitecture()

    /**
     * Nombre de fichero neutro para el binario instalado: sin la cadena "frida"
     * (para que no delate al server por nombre de fichero ni de proceso) pero
     * conservando la arquitectura, de la que depende el chequeo de compatibilidad.
     */
    private fun neutralBinaryName(arch: String): String {
        val token = buildString {
            val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
            repeat(6) { append(chars.random()) }
        }
        return "srv-$token-$arch"
    }

    /** Inserta un log con detección automática de nivel (uso interno + UI). */
    fun addRawLog(message: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val timestamp = java.text.SimpleDateFormat(
                "HH:mm:ss",
                java.util.Locale.getDefault()
            ).format(java.util.Date())
            val level = LogLevel.detect(message)
            logEntries.add(LogEntry(nextLogId++, timestamp, message.trimStart(), level))
            while (logEntries.size > 500) logEntries.removeAt(0)
        }
    }

    private fun startLogcatCapture() {
        logcatJob?.cancel()
        logcatJob = viewModelScope.launch(Dispatchers.IO) {
            var process: Process? = null
            try {
                process = Runtime.getRuntime().exec(
                    arrayOf("su", "-c", "logcat -v time -T 1 *:S frida-server:V 2>&1")
                )
                val reader = process.inputStream.bufferedReader()
                addRawLog("Captura de logs iniciada.")

                while (isActive) {
                    val line = withContext(Dispatchers.IO) { reader.readLine() } ?: break
                    addRawLog(line)
                }
            } catch (e: Exception) {
                if (isActive) addRawLog("Error en logcat: ${e.message}")
            } finally {
                process?.destroy()
            }
        }
    }

    fun clearLogs() {
        if (logEntries.isEmpty()) return
        val snapshot = logEntries.toList()
        logEntries.clear()
        notify("Registros borrados", actionLabel = "Deshacer") {
            logEntries.addAll(0, snapshot)
            while (logEntries.size > 500) logEntries.removeAt(0)
        }
    }

    /**
     * Vuelca los logs al OutputStream proporcionado por SAF.
     * Devuelve el número de líneas escritas.
     */
    suspend fun exportLogsTo(stream: OutputStream): Int = withContext(Dispatchers.IO) {
        var written = 0
        val snapshot = logEntries.toList()
        stream.bufferedWriter().use { writer ->
            writer.appendLine("# Frida Manager — log export")
            writer.appendLine("# ${java.util.Date()}")
            writer.appendLine()
            snapshot.forEach { entry ->
                writer.appendLine(entry.toExportLine())
                written++
            }
        }
        written
    }

    /** Lista filtrada que la UI consume — respeta búsqueda y filtro de nivel. */
    fun filteredLogs(): List<LogEntry> {
        val q = logSearchQuery.trim()
        val level = logLevelFilter
        val snapshot = logEntries.toList()
        if (q.isEmpty() && level == null) return snapshot
        return snapshot.filter { entry ->
            (level == null || entry.level == level) &&
            (q.isEmpty() || entry.message.contains(q, ignoreCase = true))
        }
    }

    private fun startStatusPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val status = try {
                    fridaShell.getDetailedStatus()
                } catch (e: Exception) {
                    FridaShell.FridaStatus(false)
                }

                if (!isTransitioning) {
                    val wasRunning = fridaStatus.isRunning
                    fridaStatus = status

                    // Agresivo: Si debe ser Permisivo y no lo es, forzarlo (Fix para Samsung que revierte)
                    if (status.isRunning && autoPermissive && !fridaShell.isSelinuxPermissive()) {
                        if (fridaShell.setSelinuxPermissive(true)) {
                            isSelinuxPermissive = true
                            addRawLog("↻ SELinux reforzado a Permisivo (Mantenimiento).")
                        }
                    }

                    when {
                        status.isRunning && !wasRunning -> {
                            uptimeStartMs = System.currentTimeMillis()
                            startUptimeCounter()
                            addRawLog("Frida Server activo — PID ${status.pid}, puerto ${status.port}")
                        }
                        !status.isRunning && wasRunning -> {
                            stopUptimeCounter()
                            addRawLog("Frida Server detenido.")
                        }
                        status.isRunning -> {
                            if (uptimeJob?.isActive != true) startUptimeCounter()
                        }
                    }
                } else {
                    if (status.isRunning == fridaStatus.isRunning) {
                        isTransitioning = false
                    }
                }

                delay(800)
            }
        }
    }

    private fun startUptimeCounter() {
        uptimeJob?.cancel()
        val capturedStart = if (uptimeStartMs > 0) uptimeStartMs else System.currentTimeMillis()
        uptimeJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                uptimeMillis = System.currentTimeMillis() - capturedStart
                delay(1000)
            }
        }
    }

    private fun stopUptimeCounter() {
        uptimeJob?.cancel()
        uptimeJob = null
        uptimeMillis = 0L
        uptimeStartMs = 0L
    }

    fun getUptimeString(): String {
        val totalSeconds = uptimeMillis / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun refreshBinaries(userInitiated: Boolean = false) {
        checkDevSettings() // Refrescar estado del entorno al mismo tiempo
        addRawLog("Escaneando /data/local/tmp...")
        viewModelScope.launch(Dispatchers.IO) {
            val binaries = fridaShell.listFiles("/data/local/tmp")
            availableBinaries = binaries

            val stillExists = binaries.any { it.path == selectedBinary?.path }
            if (!stillExists) {
                selectedBinary = binaries.firstOrNull()
            }

            if (binaries.isEmpty()) {
                addRawLog("No se encontraron binarios de Frida en /data/local/tmp.")
            } else {
                addRawLog("${binaries.size} binario(s) encontrado(s).")
            }
            if (userInitiated) {
                notify(
                    when (binaries.size) {
                        0 -> "No hay binarios en /data/local/tmp"
                        1 -> "1 binario encontrado"
                        else -> "${binaries.size} binarios encontrados"
                    }
                )
            }
        }
    }

    fun downloadLatestFrida() {
        if (isDownloading) return
        viewModelScope.launch(Dispatchers.IO) {
            isDownloading = true
            downloadStatus = "Consultando GitHub..."
            addRawLog("Iniciando proceso de descarga...")

            val release = remoteRelease ?: fridaDownloader.getLatestRelease()
            if (release == null) {
                addRawLog("No se pudo contactar con la API de GitHub.")
                isDownloading = false
                downloadStatus = ""
                notify("Sin conexión con GitHub", actionLabel = "Reintentar") { downloadLatestFrida() }
                return@launch
            }

            downloadStatus = "Descargando ${release.version}..."
            addRawLog("Descargando frida-server ${release.version}...")

            var result: String
            var failed = false
            val downloadedFile = fridaDownloader.downloadAndDecompress(release) { progress ->
                downloadProgress = progress
            }

            if (downloadedFile != null && downloadedFile.exists()) {
                downloadStatus = "Instalando..."
                // Se instala con un nombre de fichero neutro (sin "frida") para que
                // ni el binario en /data/local/tmp ni el nombre del proceso al lanzarlo
                // delaten a frida-server. La app lo reconoce por firma de contenido.
                val installName = neutralBinaryName(release.arch.ifBlank { deviceArchitecture() })
                addRawLog("Instalando binario como $installName...")
                val targetPath = "/data/local/tmp/$installName"
                val success = fridaShell.moveBinary(downloadedFile.absolutePath, targetPath)
                if (success) {
                    fridaShell.ensureExecutable(targetPath)
                    addRawLog("frida-server ${release.version} instalado y listo.")
                    result = "frida-server ${release.version} instalado"
                    refreshBinaries()
                } else {
                    addRawLog("No se pudo mover el binario. ¿Permisos root?")
                    result = "No se pudo instalar el binario"
                    failed = true
                }
            } else {
                addRawLog("La descarga o descompresión falló.")
                result = "La descarga falló"
                failed = true
            }

            isDownloading = false
            downloadStatus = ""
            downloadProgress = 0f
            if (failed) {
                notify(result, actionLabel = "Reintentar") { downloadLatestFrida() }
            } else {
                notify(result)
            }
        }
    }

    fun toggleFrida() {
        val binary = selectedBinary ?: run {
            addRawLog("Selecciona un binario antes de iniciar.")
            return
        }
        if (isTransitioning) return
        if (!isFridaRunning && !isNetworkConfigValid) {
            notify("Revisa la configuración de red en Ajustes")
            return
        }

        val isStarting = !isFridaRunning
        isTransitioning = true
        fridaStatus = fridaStatus.copy(isRunning = isStarting)

        addRawLog(
            if (isStarting) "▶ Iniciando ${binary.name} en $fridaAddress:$fridaPort..."
            else "■ Deteniendo Frida Server..."
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (isStarting) {
                    // Fix para Samsung/modernos: SELinux Permissive
                    if (autoPermissive && !isSelinuxPermissive) {
                        if (fridaShell.setSelinuxPermissive(true)) {
                            isSelinuxPermissive = true
                            addRawLog("✓ SELinux auto-ajustado a Permisivo.")
                        }
                    }
                    fridaShell.ensureExecutable(binary.path)
                }

                withContext(Dispatchers.Main) {
                    val intent = Intent(getApplication(), FridaService::class.java).apply {
                        action = if (isStarting) "START" else "STOP"
                        if (isStarting) {
                            putExtra("SERVER_PATH", binary.path)
                            putExtra("PORT", fridaPort)
                            putExtra("ADDRESS", fridaAddress)
                        }
                    }
                    getApplication<Application>().startService(intent)
                }

                if (isStarting) uptimeStartMs = System.currentTimeMillis()
                delay(1500)
                isTransitioning = false
            } catch (e: Exception) {
                addRawLog("Error en el servicio: ${e.message}")
                notify(if (isStarting) "No se pudo iniciar el servicio" else "No se pudo detener el servicio")
                fridaStatus = fridaStatus.copy(isRunning = !isStarting)
                isTransitioning = false
            }
        }
    }

    fun deleteSelectedBinary() {
        val binary = selectedBinary ?: return
        viewModelScope.launch(Dispatchers.IO) {
            isDeleting = true
            addRawLog("Eliminando ${binary.name}...")
            delay(800)
            if (fridaShell.deleteBinary(binary.path)) {
                addRawLog("Binario eliminado correctamente.")
                notify("${binary.name} eliminado")
                selectedBinary = null
                refreshBinaries()
            } else {
                addRawLog("No se pudo eliminar '${binary.name}'.")
                notify("No se pudo eliminar el binario")
            }
            isDeleting = false
        }
    }

    fun toggleSelinux() {
        viewModelScope.launch(Dispatchers.IO) {
            val target = !isSelinuxPermissive
            if (fridaShell.setSelinuxPermissive(target)) {
                isSelinuxPermissive = target
                addRawLog(if (target) "✓ SELinux configurado en modo PERMISIVO." else "▶ SELinux configurado en modo ENFORCING.")
                notify(if (target) "SELinux en modo permisivo" else "SELinux en modo enforcing")
            } else {
                addRawLog("✗ No se pudo cambiar el estado de SELinux.")
                notify("No se pudo cambiar SELinux")
            }
        }
    }

    /**
     * "Turbo Fix" para Samsung/Zygote:
     * 1. SELinux 0
     * 2. Desactivar USAP Pool (Evita crashes de JNI/Zygote en Samsung)
     * 3. Mantener pantalla encendida (Opcional pero útil para debugging)
     */
    fun applyTurboFix() {
        viewModelScope.launch(Dispatchers.IO) {
            addRawLog("▶ Aplicando Turbo Fix (Zygote/Samsung)...")
            
            // 1. SELinux
            if (fridaShell.setSelinuxPermissive(true)) isSelinuxPermissive = true
            
            // 2. USAP Pool Fix (Clave para JNI FatalError en Samsung)
            val usapSuccess = fridaShell.runCommand("setprop persist.device_config.runtime_native.usap_pool_enabled false")
            
            // 3. Keep Screen On (Settings)
            fridaShell.runCommand("settings put system screen_off_timeout 600000") // 10 min
            
            if (usapSuccess) {
                addRawLog("✓ Turbo Fix aplicado: USAP desactivado y SELinux Permisivo.")
                notify("Ajuste para Samsung aplicado")
            } else {
                addRawLog("⚠ Turbo Fix parcial: Se aplicó SELinux pero falló el setprop.")
                notify("Ajuste aplicado parcialmente: revisa Registros")
            }
        }
    }

    /** Abre el repositorio Frida en el navegador del sistema. */
    fun openGitHubRepo(): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/frida/frida"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
