package dev.prootcode.app.data.environment

import dev.prootcode.app.runtime.exec.CommandRunner
import dev.prootcode.app.runtime.proot.ProotCommand
import java.io.File

object PresetScripts {
    fun packageScript(packages: List<String>): String? =
        if (packages.isEmpty()) {
            null
        } else {
            "apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y ${packages.joinToString(" ")}"
        }

    fun openCodeScript(): String =
        """
        apt-get update
        apt-get install -y --no-install-recommends curl ca-certificates nodejs npm
        curl -fsSL https://opencode.ai/install | bash
        export PATH="${'$'}HOME/.opencode/bin:${'$'}PATH"
        opencode --version
        """.trimIndent()
}

class PresetInstaller(
    private val nativeLibDir: File,
    private val projectsDir: File,
    private val tmpDir: File,
    private val runner: CommandRunner = CommandRunner(),
) {
    fun install(
        rootfsDir: File,
        presets: Collection<String>,
        onLine: (String) -> Unit = {},
    ): Boolean {
        val command = ProotCommand(nativeLibDir, rootfsDir, tmpDir, projectsDir)
        val packages = PresetPackages.packagesFor(presets)
        PresetScripts.packageScript(packages)?.let { script ->
            val result =
                runner.run(command.argv(script), command.environment(), TIMEOUT_SECONDS, onLine)
            if (result.exitCode != 0) return false
        }
        if (Presets.EDITORS in presets) {
            val result =
                runner.run(
                    command.argv(PresetScripts.openCodeScript()),
                    command.environment(),
                    TIMEOUT_SECONDS,
                    onLine,
                )
            if (result.exitCode != 0) return false
        }
        return true
    }

    private companion object {
        private const val TIMEOUT_SECONDS = 900L
    }
}
