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
        // Un solo comando, independiente del nombre: mata por ruta del ejecutable.
        // El server se lanza desde /data/local/tmp, así que su cmdline la contiene.
        Shell.cmd("pkill -9 -f '/data/local/tmp/' 2>/dev/null").exec()
    }

    data class FridaStatus(
        val isRunning: Boolean,
        val pid: Int? = null,
        val port: String? = null,
        val listenAddress: String? = null
    )

    /**
     * Estado del server de forma INDEPENDIENTE DEL NOMBRE del binario (que puede estar
     * renombrado por la función de ocultado). El PID se obtiene del proceso cuyo
     * ejecutable vive en /data/local/tmp, y "corre" se decide por el socket en escucha
     * en [expectedPort] (o el rango por defecto si no se indica).
     */
    fun getDetailedStatus(expectedPort: String? = null): FridaStatus {
        val wanted = expectedPort?.trim()?.toIntOrNull()
        fun portMatches(p: Int) = if (wanted != null) p == wanted else p in 27042..27052

        // Vía rápida: ss en una sola llamada da estado + puerto + dirección + PID,
        // sin importar cómo se llame el binario.
        val ss = Shell.cmd("ss -H -ltnp 2>/dev/null").exec()
        if (ss.isSuccess) {
            for (line in ss.out) {
                // LISTEN 0 50 0.0.0.0:27042 0.0.0.0:* users:(("srv-x",pid=1234,fd=7))
                val cols = line.trim().split(Regex("\\s+"))
                val local = cols.getOrNull(3) ?: continue
                val p = local.substringAfterLast(':').toIntOrNull() ?: continue
                if (!portMatches(p)) continue
                val pid = Regex("pid=(\\d+)").find(line)?.groupValues?.get(1)?.toIntOrNull()
                return FridaStatus(
                    isRunning = true,
                    pid = pid,
                    port = p.toString(),
                    listenAddress = normalizeAddr(local.substringBeforeLast(':'))
                )
            }
            return FridaStatus(false)
        }

        // Fallback (sin ss): /proc/net/tcp — da estado y puerto, sin PID.
        val tcpResult = Shell.cmd("cat /proc/net/tcp /proc/net/tcp6 2>/dev/null").exec()
        if (tcpResult.isSuccess) {
            for (line in tcpResult.out.drop(1)) {
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size >= 4 && parts[3] == "0A") {
                    val addrPort = parts[1].split(":")
                    if (addrPort.size == 2) {
                        val decPort = addrPort[1].toIntOrNull(16) ?: continue
                        if (portMatches(decPort)) {
                            return FridaStatus(
                                isRunning = true,
                                pid = null,
                                port = decPort.toString(),
                                listenAddress = parseHexIp(addrPort[0])
                            )
                        }
                    }
                }
            }
        }
        return FridaStatus(false)
    }

    private fun normalizeAddr(raw: String): String {
        val a = raw.trim('[', ']')
        return if (a == "*" || a == "::" || a.isEmpty()) "0.0.0.0" else a
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
        // Detectamos frida-server por FIRMA DE CONTENIDO, no por nombre: así un
        // binario renombrado a un nombre neutro (auto-renombrado al descargar)
        // sigue siendo reconocido por la app. El nombre del fichero ya no delata
        // a Frida ante una app objetivo, pero nosotros lo seguimos identificando.
        val result = Shell.cmd("""
            find '$directory' -maxdepth 1 -type f 2>/dev/null | while IFS= read -r f; do
                # Solo ejecutables ELF (descarta scripts .js, .json, .txt, etc.)
                head -c 4 "${'$'}f" 2>/dev/null | grep -qa 'ELF' || continue
                # ...que contengan el marcador de frida en su contenido. Así un
                # binario renombrado a un nombre neutro se sigue reconociendo.
                grep -qa 'frida' "${'$'}f" 2>/dev/null || continue
                if [ -x "${'$'}f" ]; then
                    echo "1:${'$'}f"
                else
                    echo "0:${'$'}f"
                fi
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