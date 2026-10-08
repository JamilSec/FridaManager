package com.j41k.fridamanager.repack

import com.android.apksig.ApkSigner
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.random.Random

/**
 * Motor de repaquetado (Etapa 1).
 *
 * Toma un APK de entrada, reescribe su AndroidManifest.xml binario con un nombre de
 * paquete (y, opcionalmente, componentes/label) nuevos, y lo vuelve a firmar con una
 * identidad efímera, para que la app corra bajo otro nombre de paquete.
 *
 * IMPORTANTE: parchear solo el manifiesto es seguro para un APK **stub** (sin
 * dependencias en resources.arsc del nombre de paquete). Aplicarlo sobre la app
 * completa rompería la resolución de recursos. En esta app es seguro porque sus
 * componentes son solo referencias a clases (no hay ContentProvider ni authorities
 * propias), así que basta reescribir el manifiesto manteniendo las clases absolutas.
 */
object ApkRepacker {

    private val ALPHA = ('a'..'z').toList()
    private val ALPHADOTS = ALPHA + '.'

    /** Nombre de paquete aleatorio: 5–19 chars, minúsculas con puntos, sin puntos dobles. */
    fun genPackageName(): String {
        val len = 5 + Random.nextInt(15)
        val sb = StringBuilder(len)
        var dotted = false
        for (i in 0 until len) {
            val c = when {
                i == 0 || i == len - 1 -> ALPHA.random()        // ni empieza ni acaba en punto
                sb.last() == '.' -> ALPHA.random()              // sin puntos consecutivos
                else -> ALPHADOTS.random()
            }
            if (c == '.') dotted = true
            sb.append(c)
        }
        if (!dotted) {
            val idx = 1 + Random.nextInt(len - 2)
            sb.setCharAt(idx, '.')
        }
        return sb.toString()
    }

    /**
     * Repaqueta [input] a [output] aplicando [stringMap] a cada cadena del manifiesto y
     * firmando con [keys]. Devuelve true si tuvo éxito.
     */
    fun repack(
        input: File,
        output: File,
        keys: Keygen.Keys,
        stringMap: (String) -> String
    ) {
        val unsigned = File.createTempFile("repack", ".apk", output.parentFile)
        try {
            rewriteManifest(input, unsigned, stringMap)
                ?: error("No se pudo parchear el manifiesto (formato inesperado: ¿estilos o cabecera no estándar?)")
            sign(unsigned, output, keys)
        } finally {
            unsigned.delete()
        }
    }

    /** Copia el zip reemplazando el manifiesto parcheado y eliminando firmas previas. */
    private fun rewriteManifest(input: File, output: File, map: (String) -> String): Unit? {
        ZipFile(input).use { zip ->
            val manifestEntry = zip.getEntry("AndroidManifest.xml") ?: return null
            val patched = AXML.patchStrings(
                zip.getInputStream(manifestEntry).readBytes(), map
            ) ?: return null

            ZipOutputStream(output.outputStream().buffered()).use { out ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val e = entries.nextElement()
                    val name = e.name
                    // Las firmas v1 se regeneran; no copiamos las viejas.
                    if (name.startsWith("META-INF/") &&
                        (name.endsWith(".SF") || name.endsWith(".RSA") ||
                            name.endsWith(".DSA") || name.endsWith(".EC") ||
                            name.equals("META-INF/MANIFEST.MF", true))
                    ) continue

                    if (name == "AndroidManifest.xml") {
                        // El manifiesto cambia de tamaño: siempre DEFLATED (no requiere alineación).
                        val ne = ZipEntry(name).apply { method = ZipEntry.DEFLATED }
                        out.putNextEntry(ne)
                        out.write(patched)
                        out.closeEntry()
                        continue
                    }

                    // Preservar el método original. resources.arsc y los .so deben
                    // seguir STORED (sin comprimir) o Android 11+ rechaza la instalación.
                    val ne = ZipEntry(name).apply {
                        method = e.method
                        if (e.method == ZipEntry.STORED) {
                            size = e.size
                            compressedSize = e.compressedSize
                            crc = e.crc
                        }
                    }
                    out.putNextEntry(ne)
                    zip.getInputStream(e).use { it.copyTo(out) }
                    out.closeEntry()
                }
            }
        }
        return Unit
    }

    /** Firma v1+v2+v3 con apksig (v2/v3 son obligatorias para instalar en Android 11+). */
    private fun sign(input: File, output: File, keys: Keygen.Keys) {
        val signerConfig = ApkSigner.SignerConfig.Builder(
            "FRIDA", keys.key, listOf(keys.cert)
        ).build()
        ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(input)
            .setOutputApk(output)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .build()
            .sign()
    }
}
