package dev.prootcode.app.data.environment

import android.content.res.AssetManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest

class ArtifactStore(
    private val cacheDir: File,
    private val assets: AssetManager?,
) {
    fun open(
        name: String,
        url: String,
        sha256: String,
        size: Long,
        bundledAsset: String?,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
    ): InputStream {
        val assetManager = assets
        if (bundledAsset != null && assetManager != null) {
            val bundled = runCatching { assetManager.open(bundledAsset) }.getOrNull()
            if (bundled != null) {
                onProgress(size, size)
                return bundled
            }
        }
        val file = download(name, url, sha256, size, onProgress)
        return FileInputStream(file)
    }

    fun download(
        name: String,
        url: String,
        sha256: String,
        size: Long,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
    ): File {
        cacheDir.mkdirs()
        val target = File(cacheDir, name)
        if (target.isFile && checksum(target) == sha256) return target
        val part = File(cacheDir, "$name.part")
        var offset = if (part.isFile) part.length() else 0L
        if (size > 0 && offset > size) {
            part.delete()
            offset = 0
        }
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = 20_000
        connection.readTimeout = 60_000
        if (offset > 0) connection.setRequestProperty("Range", "bytes=$offset-")
        try {
            val code = connection.responseCode
            val resume = code == HttpURLConnection.HTTP_PARTIAL && offset > 0
            if (code != HttpURLConnection.HTTP_OK && !resume) {
                throw IOException("download failed with $code")
            }
            if (!resume) {
                part.delete()
                offset = 0
            }
            connection.inputStream.use { input ->
                FileOutputStream(part, resume).use { output ->
                    var received = offset
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        received += read
                        onProgress(received, size)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
        if (size > 0 && part.length() != size) {
            part.delete()
            throw IOException("download is incomplete")
        }
        val actual = checksum(part)
        if (actual != sha256) {
            part.delete()
            throw IOException("checksum mismatch: $actual")
        }
        if (!part.renameTo(target)) throw IOException("could not move the downloaded artifact")
        return target
    }

    private fun checksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        private const val BUFFER_SIZE = 64 * 1024
    }
}
