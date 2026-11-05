package dev.prootcode.app.runtime.exec

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File

class CommandRunnerTest {
    private val runner = CommandRunner()

    @Test
    fun `returns the exit code and output`() {
        assumeShell()
        val result = runner.run(listOf("/bin/sh", "-c", "echo hello"), emptyMap(), TIMEOUT)
        assertEquals(0, result.exitCode)
        assertFalse(result.timedOut)
        assertTrue(result.output.contains("hello"))
    }

    @Test
    fun `reports a non zero exit code`() {
        assumeShell()
        val result = runner.run(listOf("/bin/sh", "-c", "exit 3"), emptyMap(), TIMEOUT)
        assertEquals(3, result.exitCode)
        assertFalse(result.timedOut)
    }

    @Test
    fun `kills the process when it times out`() {
        assumeShell()
        val result = runner.run(listOf("/bin/sh", "-c", "sleep 10"), emptyMap(), 1)
        assertTrue(result.timedOut)
        assertEquals(CommandRunner.TIMEOUT_EXIT_CODE, result.exitCode)
    }

    @Test
    fun `passes the environment to the process`() {
        assumeShell()
        val result = runner.run(listOf("/bin/sh", "-c", "printf %s \"\$GREETING\""), mapOf("GREETING" to "hi"), TIMEOUT)
        assertEquals(0, result.exitCode)
        assertEquals("hi", result.output.trim())
    }

    private fun assumeShell() {
        assumeTrue(File("/bin/sh").exists(), "requires a POSIX shell")
    }

    private companion object {
        private const val TIMEOUT = 30L
    }
}
