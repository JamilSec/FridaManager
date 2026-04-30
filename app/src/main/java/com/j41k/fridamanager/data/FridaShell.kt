package com.j41k.fridamanager.data

import com.topjohnwu.superuser.Shell

class FridaShell {

    fun checkRoot(onResult: (Boolean) -> Unit) {
        Shell.getShell { shell ->
            onResult(shell.isRoot)
        }
    }

    fun runCommand(command: String): Boolean {
        val result = Shell.cmd(command).exec()
        return result.isSuccess
    }

    fun stopFrida() {
        Shell.cmd("pkill -9 frida-server 2>/dev/null; pkill -9 frida-server 2>/dev/null").exec()
    }

    data class FridaStatus(
        val isRunning: Boolean,
        val pid: Int? = null,
        val port: String? = null,
        val listenAddress: String? = null
    )

    fun getDetailedStatus(): FridaStatus {
        // OPTIMIZADO: Una sola llamada shell en lugar de cientos.
        // ps -A devuelve todos los procesos; '[f]' evita que grep se auto-detecte.
        val psResult = Shell.cmd(
            "ps -A 2>/dev/null | grep '[f]rida-server' | awk '{print \$2}' | head -1"
        ).exec()

        val pid = psResult.out.firstOrNull()?.trim()?.toIntOrNull()
            ?: return FridaStatus(false)

        // Verificar socket en escucha — revisamos tanto tcp como tcp6
        val tcpResult = Shell.cmd("cat /proc/net/tcp /proc/net/tcp6 2>/dev/null").exec()
        var port: String? = null
        var address: String? = null
        var isListening = false

        if (tcpResult.isSuccess) {
            for (line in tcpResult.out.drop(1)) {
                val parts = line.trim().split(Regex("\\s+"))
                // parts[3] == "0A" → LISTEN, parts[7] → inode owner UID (opcional)
                if (parts.size >= 4 && parts[3] == "0A") {
                    val addrPort = parts[1].split(":")
                    if (addrPort.size == 2) {
                        val decPort = addrPort[1].toIntOrNull(16) ?: continue
                        // Puerto de Frida por defecto es exactamente 27042.
                        // Aceptamos ±10 para cubrir configuraciones personalizadas comunes.
                        if (decPort in 27042..27052) {
                            isListening = true
                            port = decPort.toString()
                            address = parseHexIp(addrPort[0])
                            break
                        }
                    }
                }
            }
        }

        // Si el proceso existe PERO aún no hay socket (arrancando), reportamos "iniciando"
        // usando isRunning=true con port=null para que la UI lo refleje
        return FridaStatus(
            isRunning = isListening,
            pid = pid, // Siempre mostramos el PID si el proceso existe
            port = if (isListening) port else null,
            listenAddress = if (isListening) address else null
        )
    }

    private fun parseHexIp(hexIp: String): String {
        return try {
            val i = hexIp.toLong(16)
            val a = (i and 0xFF)
            val b = (i shr 8 and 0xFF)
            val c = (i shr 16 and 0xFF)
            val d = (i shr 24 and 0xFF)
            if (a == 0L && b == 0L && c == 0L && d == 0L) "0.0.0.0"
            else "$a.$b.$c.$d"
        } catch (e: Exception) {
            "0.0.0.0"
        }
    }

    fun ensureExecutable(path: String): Boolean {
        return Shell.cmd("chmod 755 $path").exec().isSuccess
    }

    fun moveBinary(sourcePath: String, destPath: String): Boolean {
        // Intentamos cp + rm como fallback por si mv falla entre sistemas de archivos
        val mvResult = Shell.cmd("mv '$sourcePath' '$destPath'").exec()
        if (mvResult.isSuccess) return true
        val cpResult = Shell.cmd("cp '$sourcePath' '$destPath' && rm -f '$sourcePath'").exec()
        return cpResult.isSuccess
    }

    fun deleteBinary(path: String): Boolean {
        return Shell.cmd("rm -f '$path'").exec().isSuccess
    }

    fun isSelinuxPermissive(): Boolean {
        val result = Shell.cmd("getenforce").exec()
        return result.out.firstOrNull()?.trim()?.equals("Permissive", ignoreCase = true) == true
    }

    fun setSelinuxPermissive(permissive: Boolean): Boolean {
        val mode = if (permissive) "0" else "1"
        return Shell.cmd("setenforce $mode").exec().isSuccess
    }

    fun listFiles(directory: String): List<FridaFile> {
        // Usamos find + test en un único script shell para evitar problemas con
        // ls -F en distintas versiones de toybox (columnas, flags, etc.)
        val result = Shell.cmd("""
            find '$directory' -maxdepth 1 -type f 2>/dev/null | while IFS= read -r f; do
                n=${'$'}(basename "${'$'}f")
                case "${'$'}n" in
                    *frida*server*|*frida-server*)
                        case "${'$'}n" in
                            *.dex|*.so) ;;
                            *)
                                if [ -x "${'$'}f" ]; then
                                    echo "1:${'$'}f"
                                else
                                    echo "0:${'$'}f"
                                fi
                                ;;
                        esac
                        ;;
                esac
            done
        """.trimIndent()).exec()

        if (result.out.isEmpty()) return emptyList()

        return result.out.mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.length < 3 || trimmed[1] != ':') return@mapNotNull null
            val isExecutable = trimmed[0] == '1'
            val path = trimmed.substring(2)
            val name = path.substringAfterLast('/')
            if (name.isEmpty()) return@mapNotNull null
            FridaFile(name = name, path = path, isExecutable = isExecutable)
        }
    }
}