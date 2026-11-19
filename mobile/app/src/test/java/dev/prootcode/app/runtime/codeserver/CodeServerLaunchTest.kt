package dev.prootcode.app.runtime.codeserver

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.ServerSocket

class CodeServerLaunchTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `finds the versioned code-server binary`() {
        val rootfs = File(tempDir, "rootfs")
        val binary = File(rootfs, "opt/code-server/code-server-4.143.1-linux-arm64/bin/code-server")
        binary.parentFile?.mkdirs()
        binary.writeText("#!/bin/sh")

        assertEquals(binary, findCodeServerBinary(rootfs))
    }

    @Test
    fun `finds a binary directly under opt`() {
        val rootfs = File(tempDir, "rootfs")
        val binary = File(rootfs, "opt/code-server/bin/code-server")
        binary.parentFile?.mkdirs()
        binary.writeText("#!/bin/sh")

        assertEquals(binary, findCodeServerBinary(rootfs))
    }

    @Test
    fun `returns null when code-server is missing`() {
        assertNull(findCodeServerBinary(File(tempDir, "rootfs")))
    }

    @Test
    fun `maps the binary to its guest path`() {
        val rootfs = File(tempDir, "rootfs")
        val binary = File(rootfs, "opt/code-server/code-server-4.143.1-linux-arm64/bin/code-server")

        assertEquals(
            "/opt/code-server/code-server-4.143.1-linux-arm64/bin/code-server",
            codeServerGuestPath(rootfs, binary),
        )
    }

    @Test
    fun `builds localhost-only arguments`() {
        val args = codeServerArgs(8080)

        assertEquals("127.0.0.1:8080", args[args.indexOf("--bind-addr") + 1])
        assertEquals("none", args[args.indexOf("--auth") + 1])
        assertTrue(args.contains("--disable-telemetry"))
        assertEquals("/root/projects_bridge", args.last())
    }

    @Test
    fun `detects an open and closed port`() {
        val server = ServerSocket(0)
        val port = server.localPort

        assertTrue(isCodeServerPortOpen(port))
        server.close()

        assertFalse(isCodeServerPortOpen(port))
    }
}
