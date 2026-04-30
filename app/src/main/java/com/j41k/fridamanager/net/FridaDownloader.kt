package com.j41k.fridamanager.net

import android.content.Context
import android.os.Build
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import org.tukaani.xz.XZInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class FridaDownloader(private val context: Context) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Información completa de una release de Frida.
     * Todos los campos provienen literalmente de la API de GitHub.
     */
    data class FridaRelease(
        val version: String,
        val downloadUrl: String,
        val fileName: String,
        val sizeBytes: Long = 0L,
        val publishedAt: String? = null,
        val changelog: String? = null,
        val arch: String = "",
        val htmlUrl: String? = null
    )

    fun getLatestRelease(): FridaRelease? {
        return try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/frida/frida/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JsonParser.parseString(body).asJsonObject
                val version = json.get("tag_name").asString
                val publishedAt = json.get("published_at")?.asString
                val htmlUrl = json.get("html_url")?.asString
                val changelog = json.get("body")?.asString
                val assets = json.getAsJsonArray("assets")

                val arch = getDeviceArchitecture()
                val targetNamePart = "frida-server-$version-android-$arch.xz"

                for (asset in assets) {
                    val assetObj = asset.asJsonObject
                    val name = assetObj.get("name").asString
                    if (name == targetNamePart) {
                        return FridaRelease(
                            version = version,
                            downloadUrl = assetObj.get("browser_download_url").asString,
                            fileName = name,
                            sizeBytes = assetObj.get("size")?.asLong ?: 0L,
                            publishedAt = publishedAt,
                            changelog = changelog,
                            arch = arch,
                            htmlUrl = htmlUrl
                        )
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    fun getDeviceArchitecture(): String {
        val abis = Build.SUPPORTED_ABIS
        return when {
            abis.any { it.contains("arm64") } -> "arm64"
            abis.any { it.contains("armeabi-v7a") || it.contains("armv7") } -> "arm"
            abis.any { it.contains("x86_64") } -> "x86_64"
            abis.any { it.contains("x86") } -> "x86"
            else -> "arm64"
        }
    }

    fun downloadAndDecompress(release: FridaRelease, onProgress: (Float) -> Unit): File? {
        return try {
            val request = Request.Builder().url(release.downloadUrl).build()
            val tempXzFile = File(context.cacheDir, release.fileName)
            val outFile = File(context.cacheDir, release.fileName.removeSuffix(".xz"))

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null

                val totalBytes = response.body?.contentLength() ?: -1L
                var bytesRead = 0L

                response.body?.byteStream()?.use { input ->
                    FileOutputStream(tempXzFile).use { output ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            bytesRead += read
                            if (totalBytes > 0) {
                                onProgress(bytesRead.toFloat() / totalBytes)
                            }
                        }
                    }
                }
            }

            // Decompress
            onProgress(0.99f)
            FileInputStream(tempXzFile).use { fileIn ->
                XZInputStream(fileIn).use { xzIn ->
                    FileOutputStream(outFile).use { fileOut ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (xzIn.read(buffer).also { read = it } != -1) {
                            fileOut.write(buffer, 0, read)
                        }
                    }
                }
            }
            tempXzFile.delete()
            outFile
        } catch (e: Exception) {
            null
        }
    }
}
