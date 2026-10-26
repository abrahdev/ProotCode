package dev.prootcode.app.runtime.network

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files

class ResolvConfTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `writes one nameserver per line`() {
        ResolvConf.write(tempDir, listOf("10.0.2.3", "1.1.1.1"))

        val content = File(tempDir, "etc/resolv.conf").readText()
        assertEquals("nameserver 10.0.2.3\nnameserver 1.1.1.1\n", content)
    }

    @Test
    fun `replaces an existing symlink with a regular file`() {
        val etc = File(tempDir, "etc").apply { mkdirs() }
        val target = File(tempDir, "stub-resolv.conf").apply { writeText("nameserver 127.0.0.1\n") }
        Files.createSymbolicLink(File(etc, "resolv.conf").toPath(), target.toPath())

        ResolvConf.write(tempDir, listOf("8.8.8.8"))

        val resolvConf = File(etc, "resolv.conf")
        assertFalse(Files.isSymbolicLink(resolvConf.toPath()))
        assertEquals("nameserver 8.8.8.8\n", resolvConf.readText())
    }
}
