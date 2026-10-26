package dev.prootcode.app.data.environment

import dev.prootcode.app.runtime.exec.CommandRunner
import dev.prootcode.app.runtime.proot.ProotCommand
import java.io.File

class BridgeVerifier(
    private val nativeLibDir: File,
    private val tmpDir: File,
    private val projectsDir: File,
    private val runner: CommandRunner = CommandRunner(),
) {
    fun verify(rootfsDir: File): Boolean {
        projectsDir.mkdirs()
        tmpDir.mkdirs()
        val probe = File(projectsDir, PROBE)
        probe.delete()
        val command = ProotCommand(nativeLibDir, rootfsDir, tmpDir, projectsDir)
        val result =
            runner.run(
                command.argv("echo prootcode > /root/projects_bridge/$PROBE"),
                command.environment(noSeccomp = true),
                TIMEOUT_SECONDS,
            )
        val visible = probe.isFile && probe.readText().trim() == "prootcode"
        probe.delete()
        return result.exitCode == 0 && visible
    }

    companion object {
        private const val PROBE = ".prootcode-probe"
        private const val TIMEOUT_SECONDS = 60L
    }
}
