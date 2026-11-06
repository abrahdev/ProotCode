package dev.prootcode.app.ui.terminal

import android.content.Context
import com.termux.terminal.TerminalSession
import dev.prootcode.app.data.environment.Environment
import dev.prootcode.app.runtime.proot.ProotCommand
import java.io.File

class TerminalSessionFactory(
    private val context: Context,
) {
    fun create(
        environment: Environment,
        onScreenUpdated: () -> Unit,
    ): TerminalSession {
        val rootfsDir = File(context.filesDir, "environments/${environment.id}/rootfs")
        val projectsDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "projects")
        val command =
            ProotCommand(
                nativeLibDir = File(context.applicationInfo.nativeLibraryDir),
                rootfsDir = rootfsDir,
                tmpDir = File(context.filesDir, "runtime/tmp"),
                projectsDir = projectsDir,
            )
        val environmentVariables =
            command.environment().map { (key, value) -> "$key=$value" }.toTypedArray()
        return TerminalSession(
            command.prootBinaryPath,
            rootfsDir.absolutePath,
            command.interactiveArgv().toTypedArray(),
            environmentVariables,
            TRANSCRIPT_ROWS,
            ProotSessionClient(onScreenUpdated = onScreenUpdated),
        )
    }

    private companion object {
        private const val TRANSCRIPT_ROWS = 5_000
    }
}
