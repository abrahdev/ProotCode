package dev.prootcode.app.runtime.proot

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ProotCommandTest {
    @TempDir
    lateinit var tempDir: File

    private val nativeLibDir: File get() = dir("lib")
    private val rootfsDir: File get() = dir("rootfs")
    private val tmpDir: File get() = dir("tmp")
    private val projectsDir: File get() = dir("projects")

    private fun dir(name: String): File = File(tempDir, name).apply { mkdirs() }

    private fun command() = ProotCommand(nativeLibDir, rootfsDir, tmpDir, projectsDir)

    @Test
    fun `argv runs proot with the rootfs and the bind mounts`() {
        val argv = command().argv("/bin/true")

        assertEquals(File(nativeLibDir, "libproot.so").absolutePath, argv.first())
        assertEquals(rootfsDir.absolutePath, argv[argv.indexOf("-r") + 1])
        assertEquals("/root", argv[argv.indexOf("-w") + 1])
        assertTrue(argv.contains("--link2symlink"))
        assertTrue(argv.contains("${projectsDir.absolutePath}:/root/projects_bridge"))
        assertEquals(listOf("/bin/sh", "-c", "/bin/true"), argv.takeLast(3))
    }

    @Test
    fun `environment points to the loader and the native libraries`() {
        val environment = command().environment()

        assertEquals(File(nativeLibDir, "libproot_loader.so").absolutePath, environment["PROOT_LOADER"])
        assertEquals(tmpDir.absolutePath, environment["PROOT_TMP_DIR"])
        assertEquals(nativeLibDir.absolutePath, environment["LD_LIBRARY_PATH"])
        assertEquals("/root", environment["HOME"])
        assertEquals("xterm-256color", environment["TERM"])
        assertTrue(environment["PATH"].orEmpty().contains("/usr/bin"))
        assertFalse(environment.containsKey("PROOT_NO_SECCOMP"))
    }

    @Test
    fun `disabling seccomp adds the variable`() {
        assertEquals("1", command().environment(noSeccomp = true)["PROOT_NO_SECCOMP"])
    }
}
