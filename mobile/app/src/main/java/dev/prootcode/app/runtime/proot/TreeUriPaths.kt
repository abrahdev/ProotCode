package dev.prootcode.app.runtime.proot

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import java.io.File

object TreeUriPaths {
    fun resolve(uri: Uri): String? =
        runCatching {
            val id = DocumentsContract.getTreeDocumentId(uri)
            if (id.startsWith("raw:")) {
                return id.removePrefix("raw:").takeIf(String::isNotBlank)
            }
            val parts = id.split(":", limit = 2)
            val base =
                when (parts.firstOrNull()) {
                    "primary" -> Environment.getExternalStorageDirectory().absolutePath
                    else -> return null
                }
            val child = parts.getOrNull(1).orEmpty()
            val resolved = if (child.isEmpty()) File(base) else File(base, child)
            resolved.canonicalPath.takeIf { it == base || it.startsWith("$base/") }
        }.getOrNull()

    fun persist(
        context: Context,
        uri: Uri,
    ) {
        val flags =
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
    }
}
