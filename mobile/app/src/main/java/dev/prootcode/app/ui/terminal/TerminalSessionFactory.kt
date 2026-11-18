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
                mounts = EnvironmentMounts.of(context, settings),
            )
        val environmentVariables =
            command
                .environment(extraEnvironment = GuestEnvironment.of(settings))
                .map { (key, value) -> "$key=$value" }
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

    fun createNative(onScreenUpdated: () -> Unit): TerminalSession =
        TerminalSession(
            "/system/bin/sh",
            context.filesDir.absolutePath,
            emptyArray(),
            arrayOf(
                "PATH=/system/bin:/system/xbin",
                "HOME=${context.filesDir.absolutePath}",
                "TERM=xterm-256color",
            ),
            TRANSCRIPT_ROWS,
            ProotSessionClient(onScreenUpdated = onScreenUpdated),
        )

    fun hostProjectsDir(): File = File(context.getExternalFilesDir(null) ?: context.filesDir, "projects")

    private companion object {
        private const val TRANSCRIPT_ROWS = 5_000
    }
}
