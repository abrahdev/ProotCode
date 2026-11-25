package dev.prootcode.app.runtime.codeserver

import android.content.Context
import java.io.File
import java.security.SecureRandom

object CodeServerCredentials {
    private const val PASSWORD_LENGTH = 16
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"

    fun file(
        context: Context,
        environmentId: String,
    ): File = File(File(context.filesDir, "code-server"), "$environmentId.password")

    fun password(
        context: Context,
        environmentId: String,
    ): String {
        val file = file(context, environmentId)
        if (file.isFile) {
            val existing = runCatching { file.readText().trim() }.getOrDefault("")
            if (existing.isNotEmpty()) return existing
        }
        return regenerate(context, environmentId)
    }

    fun regenerate(
        context: Context,
        environmentId: String,
    ): String {
        val password = generate()
        val file = file(context, environmentId)
        file.parentFile?.mkdirs()
        runCatching { file.writeText(password) }
        return password
    }

    fun generate(): String {
        val random = SecureRandom()
        return buildString(PASSWORD_LENGTH) {
            repeat(PASSWORD_LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
        }
    }
}
