package com.j41k.fridamanager.repack

import com.topjohnwu.superuser.Shell
import java.io.File

/**
 * Instala/desinstala/lanza paquetes con root.
 *
 * El APK se copia primero a /data/local/tmp porque el instalador del sistema
 * (installd) no puede leer el cacheDir privado de la app por SELinux.
 */
object RootInstaller {

    data class InstallResult(val success: Boolean, val output: String)

    fun install(apk: File): InstallResult {
        val staged = "/data/local/tmp/.pkg_${System.currentTimeMillis()}.apk"
        return try {
            val prep = Shell.cmd(
                "cp '${apk.absolutePath}' '$staged'",
                "chmod 644 '$staged'"
            ).exec()
            if (!prep.isSuccess) {
                return InstallResult(false, (prep.out + prep.err).joinToString("\n").ifBlank { "no se pudo copiar a /data/local/tmp" })
            }
            // -t permite APKs marcados testOnly (los builds debug lo traen); -g concede permisos.
            val res = Shell.cmd("pm install -r -t -g '$staged'").exec()
            val output = (res.out + res.err).joinToString("\n").trim()
            InstallResult(res.isSuccess && output.contains("Success", ignoreCase = true), output)
        } finally {
            Shell.cmd("rm -f '$staged'").exec()
        }
    }

    fun uninstall(pkg: String): Boolean =
        Shell.cmd("pm uninstall '$pkg'").exec().isSuccess

    /** Lanza la actividad principal del paquete indicado. */
    fun launch(pkg: String): Boolean =
        Shell.cmd("monkey -p '$pkg' -c android.intent.category.LAUNCHER 1 2>/dev/null").exec().isSuccess

    /**
     * Lanza la copia [launchPkg] y desinstala la app actual [uninstallPkg] (la app
     * "se muda" a la nueva identidad). El uninstall va en un subshell desacoplado con una
     * pequeña espera, para que se complete aunque nuestro proceso muera al
     * desinstalarse a sí mismo.
     */
    fun launchThenUninstall(launchPkg: String, uninstallPkg: String) {
        Shell.cmd(
            "monkey -p '$launchPkg' -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1",
            "(sleep 2; pm uninstall '$uninstallPkg') >/dev/null 2>&1 &"
        ).exec()
    }

    fun isInstalled(pkg: String): Boolean =
        Shell.cmd("pm list packages '$pkg'").exec().out.any { it.trim() == "package:$pkg" }
}
