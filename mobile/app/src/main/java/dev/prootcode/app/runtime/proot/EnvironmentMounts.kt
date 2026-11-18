package dev.prootcode.app.runtime.proot

import android.content.Context
import android.os.Environment
import dev.prootcode.app.data.environment.EnvironmentSettings
import java.io.File

object EnvironmentMounts {
    fun of(
        context: Context,
        settings: EnvironmentSettings,
    ): List<Pair<String, String>> {
        val mounts = mutableListOf<Pair<String, String>>()
        if (settings.mountDownloads) {
            val downloads =
                File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir,
                    "Downloads",
                )
            downloads.mkdirs()
            mounts += downloads.absolutePath to "/root/mounts/Downloads"
        }
        settings.extraMounts.forEach { path ->
            val host = File(path)
            if (host.isDirectory) {
                mounts += host.absolutePath to "/root/mounts/${host.name}"
            }
        }
        return mounts
    }
}
