package dev.prootcode.app.runtime.proot

import java.io.File

class ProotCommand(
    private val nativeLibDir: File,
    private val rootfsDir: File,
    private val tmpDir: File,
    private val projectsDir: File,
) {
    private val prootBinary = File(nativeLibDir, "libproot.so")
    private val prootLoader = File(nativeLibDir, "libproot_loader.so")

    val prootBinaryPath: String get() = prootBinary.absolutePath

    fun argv(script: String): List<String> = baseArgv() + listOf("/bin/sh", "-c", script)

    fun interactiveArgv(shell: String = "/bin/bash"): List<String> = baseArgv() + listOf(shell, "-l")

    fun environment(noSeccomp: Boolean = false): Map<String, String> =
        buildMap {
            put("PROOT_LOADER", prootLoader.absolutePath)
            put("PROOT_TMP_DIR", tmpDir.absolutePath)
            if (noSeccomp) put("PROOT_NO_SECCOMP", "1")
            put("LD_LIBRARY_PATH", nativeLibDir.absolutePath)
            put("HOME", "/root")
            put("TERM", "xterm-256color")
            put("LANG", "C.UTF-8")
            put("PATH", GUEST_PATH)
            put("DEBIAN_FRONTEND", "noninteractive")
            put("APT_LISTCHANGES_FRONTEND", "none")
        }

    private fun baseArgv(): List<String> =
        listOf(
            prootBinary.absolutePath,
            "-r",
            rootfsDir.absolutePath,
            "-0",
            "-w",
            "/root",
            "--link2symlink",
            "-b",
            "/dev",
            "-b",
            "/proc",
            "-b",
            "/sys",
            "-b",
            "${projectsDir.absolutePath}:/root/projects_bridge",
        )

    companion object {
        private const val GUEST_PATH = "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
    }
}
