package dev.prootcode.app.spike

import java.util.concurrent.TimeUnit

data class ExecResult(
    val exitCode: Int,
    val output: String,
    val durationMs: Long,
    val timedOut: Boolean,
)

class CommandRunner {
    fun run(
        command: List<String>,
        environment: Map<String, String>,
        timeoutSeconds: Long,
    ): ExecResult {
        val startedAt = System.currentTimeMillis()
        val process =
            ProcessBuilder(command)
                .redirectErrorStream(true)
                .apply { environment().putAll(environment) }
                .start()
        val output = StringBuilder()
        val reader =
            Thread {
                process.inputStream.bufferedReader().forEachLine { line ->
                    synchronized(output) { output.appendLine(line) }
                }
            }
        reader.isDaemon = true
        reader.start()
        val finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            process.waitFor(5, TimeUnit.SECONDS)
        }
        reader.join(2_000)
        val exitCode = if (finished) process.exitValue() else TIMEOUT_EXIT_CODE
        val text = synchronized(output) { output.toString() }
        return ExecResult(
            exitCode = exitCode,
            output = text,
            durationMs = System.currentTimeMillis() - startedAt,
            timedOut = !finished,
        )
    }

    companion object {
        const val TIMEOUT_EXIT_CODE = 124
    }
}
