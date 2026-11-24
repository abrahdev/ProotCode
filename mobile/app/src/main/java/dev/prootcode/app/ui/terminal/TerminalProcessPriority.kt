package dev.prootcode.app.ui.terminal

import dev.prootcode.app.data.environment.EnvironmentSettings

object TerminalProcessPriority {
    fun apply(
        environmentId: String,
        priority: String,
    ) {
        val nice = niceValue(priority)
        val pids =
            TerminalSessionManager
                .sessions(environmentId)
                .flatMap { ResourceMonitor.processTree(it.pid) }
                .distinct()
        pids.forEach { pid ->
            if (pid <= 0) return@forEach
            runCatching {
                ProcessBuilder(
                    "/system/bin/renice",
                    "-n",
                    nice.toString(),
                    "-p",
                    pid.toString(),
                ).start()
            }
        }
    }

    private fun niceValue(priority: String): Int =
        when (priority) {
            EnvironmentSettings.PRIORITY_LOW -> 10
            else -> 0
        }
}
