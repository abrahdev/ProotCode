package dev.prootcode.app.ui.terminal

import android.content.Context
import com.termux.terminal.TerminalSession
import dev.prootcode.app.runtime.proot.EnvironmentMounts
import dev.prootcode.app.runtime.proot.GuestEnvironment
import dev.prootcode.app.runtime.proot.ProotCommand
import java.io.File
import dev.prootcode.app.data.environment.Environment as ProotEnvironment

class TerminalSessionFactory(
    private val context: Context,
) {
    fun create(
        environment: ProotEnvironment,
        promptColor: Int,
        onScreenUpdated: () -> Unit,
    ): TerminalSession {
        val rootfsDir = File(context.filesDir, "environments/${environment.id}/rootfs")
        val projectsDir = hostProjectsDir()
        val settings = environment.settings
        val command =
            ProotCommand(
                nativeLibDir = File(context.applicationInfo.nativeLibraryDir),
                rootfsDir = rootfsDir,
                tmpDir = File(context.filesDir, "runtime/tmp"),
                projectsDir = projectsDir,
                mounts = EnvironmentMounts.of(context, settings, rootfsDir),
            )
        val coloredPrompt = prompt(promptColor)
        val environmentVariables =
            command
                .environment(extraEnvironment = GuestEnvironment.of(settings))
                .toMutableMap()
                .apply {
                    put("PS1", coloredPrompt)
                    put("PROMPT_COMMAND", "PS1='$coloredPrompt'")
                }.map { (key, value) -> "$key=$value" }
                .toTypedArray()
        return TerminalSession(
            command.prootBinaryPath,
            rootfsDir.absolutePath,
            command.interactiveArgv().toTypedArray(),
            environmentVariables,
            TRANSCRIPT_ROWS,
            ProotSessionClient(onScreenUpdated = onScreenUpdated),
        )
    }

    fun createNative(
        promptColor: Int,
        onScreenUpdated: () -> Unit,
    ): TerminalSession =
        run {
            val coloredPrompt = prompt(promptColor)
            TerminalSession(
                "/system/bin/sh",
                context.filesDir.absolutePath,
                emptyArray(),
                arrayOf(
                    "PATH=/system/bin:/system/xbin",
                    "HOME=${context.filesDir.absolutePath}",
                    "TERM=xterm-256color",
                    "PS1=$coloredPrompt",
                    "PROMPT_COMMAND=PS1='$coloredPrompt'",
                ),
                TRANSCRIPT_ROWS,
                ProotSessionClient(onScreenUpdated = onScreenUpdated),
            )
        }

    private fun prompt(color: Int): String {
        val red = (color shr 16) and 0xFF
        val green = (color shr 8) and 0xFF
        val blue = color and 0xFF
        return "\\[\\e[38;2;$red;$green;${blue}m\\]\\u@\\h\\[\\e[0m\\]:\\w\\# "
    }

    fun hostProjectsDir(): File = File(context.getExternalFilesDir(null) ?: context.filesDir, "projects")

    private companion object {
        private const val TRANSCRIPT_ROWS = 5_000
    }
}
