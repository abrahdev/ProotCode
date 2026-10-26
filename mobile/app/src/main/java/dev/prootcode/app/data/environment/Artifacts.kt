package dev.prootcode.app.data.environment

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

class Artifacts(
    private val context: Context,
) {
    fun openBundled(asset: String): InputStream? = runCatching { context.assets.open(asset) }.getOrNull()

    suspend fun download(
        name: String,
        url: String,
        sha256: String,
        size: Long,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
    ): File {
        val manager =
            context.getSystemService(DownloadManager::class.java)
                ?: throw IOException("download manager unavailable")
        val directory = File(context.getExternalFilesDir(null) ?: context.filesDir, ARTIFACTS_DIR)
        directory.mkdirs()
        val target = File(directory, name)
        target.delete()
        val uri = Uri.parse(url)
        val request = DownloadManager.Request(uri)
        request.setTitle(name)
        request.setDestinationInExternalFilesDir(context, null, "$ARTIFACTS_DIR/$name")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        val id = manager.enqueue(request)
        try {
            while (true) {
                coroutineContext.ensureActive()
                delay(POLL_MILLIS)
                val status = queryStatus(manager, id, onProgress)
                if (status == DownloadManager.STATUS_SUCCESSFUL) break
                if (status == DownloadManager.STATUS_FAILED) throw IOException("download failed")
            }
        } catch (failure: Throwable) {
            manager.remove(id)
            target.delete()
            throw failure
        }
        if (size > 0 && target.length() != size) throw IOException("download is incomplete")
        val actual = checksum(target)
        if (actual != sha256) {
            target.delete()
            throw IOException("checksum mismatch: $actual")
        }
        return target
    }

    private fun queryStatus(
        manager: DownloadManager,
        id: Long,
        onProgress: (Long, Long) -> Unit,
    ): Int =
        manager.query(DownloadManager.Query().setFilterById(id)).use { cursor ->
            if (!cursor.moveToFirst()) throw IOException("download not found")
            val downloaded =
                cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            onProgress(downloaded, total)
            cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
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
        private const val ARTIFACTS_DIR = "artifacts"
        private const val BUFFER_SIZE = 64 * 1024
        private const val POLL_MILLIS = 500L
    }
}
