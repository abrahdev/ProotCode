package dev.prootcode.app.runtime.proot

import android.content.Context
import android.os.Environment
import dev.prootcode.app.data.environment.EnvironmentSettings
import java.io.File

object EnvironmentMounts {
    fun of(
        context: Context,
        settings: EnvironmentSettings,
        rootfsDir: File,
    ): List<Pair<String, String>> {
        val mounts = mutableListOf<Pair<String, String>>()
        if (settings.mountDownloads) {
            val selected = settings.downloadsPath?.let(::File)?.takeIf { it.isDirectory }
            val downloads =
                selected
                    ?: File(
                        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir,
                        "Downloads",
                    ).also { it.mkdirs() }
            val guest = "/root/mounts/Downloads"
            ensureGuestDir(rootfsDir, guest)
            mounts += downloads.absolutePath to guest
        }
        val usedNames = mutableSetOf<String>()
        settings.extraMounts.forEach { path ->
            val host = File(path)
            if (host.isDirectory) {
                val base = host.name.ifBlank { "folder" }
                var name = base
                var suffix = 2
                while (!usedNames.add(name)) {
                    name = "$base-$suffix"
                    suffix++
                }
                val guest = "/root/mounts/Shared/$name"
                ensureGuestDir(rootfsDir, guest)
                mounts += host.absolutePath to guest
            }
        }
        return mounts
    }

    private fun ensureGuestDir(
        rootfsDir: File,
        guest: String,
    ) {
        File(rootfsDir, guest.removePrefix("/")).mkdirs()
    }
}
