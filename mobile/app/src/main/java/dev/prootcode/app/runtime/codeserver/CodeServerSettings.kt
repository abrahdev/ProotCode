package dev.prootcode.app.runtime.codeserver

import org.json.JSONObject
import java.io.File

object CodeServerSettings {
    private const val SETTINGS_FILE = "User/settings.json"
    private const val KEYBOARD_DISPATCH = "keyboard.dispatch"

    fun seed(
        rootfs: File,
        userDataDir: String,
    ) {
        val relative = userDataDir.trimStart('/')
        val file = File(File(rootfs, relative), SETTINGS_FILE)
        val settings =
            file
                .takeIf { it.isFile }
                ?.let { existing -> runCatching { JSONObject(existing.readText()) }.getOrNull() }
                ?: JSONObject()
        if (settings.optString(KEYBOARD_DISPATCH).isBlank()) {
            settings.put(KEYBOARD_DISPATCH, "keyCode")
        }
        file.parentFile?.mkdirs()
        runCatching { file.writeText(settings.toString(2)) }
    }
}
