package dev.prootcode.app.spike

import android.content.Context
import android.net.ConnectivityManager
import java.io.File

class ProotEnvironment(
    context: Context,
) {
    private val appContext = context.applicationContext

    val nativeLibDir = File(appContext.applicationInfo.nativeLibraryDir)
    val baseDir = File(appContext.filesDir, "spike")
    val rootfsDir = File(baseDir, "ubuntu")
    val tmpDir = File(baseDir, "tmp")
    val controlDir = File(baseDir, "control")
    val storeDir = appContext.getExternalFilesDir(null) ?: appContext.filesDir
    val projectsDir = File(storeDir, "projects")
    val reportsDir = File(storeDir, "diagnostics")

    val prootBinary = File(nativeLibDir, "libproot.so")
    val prootLoader = File(nativeLibDir, "libproot_loader.so")

    fun prepare() {
        listOf(tmpDir, controlDir, projectsDir, reportsDir).forEach { it.mkdirs() }
    }

    fun prootEnvironment(noSeccomp: Boolean): Map<String, String> =
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

    fun guestCommand(script: String): List<String> =
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
            "/bin/sh",
            "-c",
            script,
        )

    fun writeResolvConf(servers: List<String>) {
        val file = File(rootfsDir, "etc/resolv.conf")
        file.delete()
        file.parentFile?.mkdirs()
        file.writeText(servers.joinToString(separator = "\n", postfix = "\n") { "nameserver $it" })
    }

    fun dnsServers(): List<String> {
        val manager = appContext.getSystemService(ConnectivityManager::class.java)
        val network = manager?.activeNetwork
        val properties = network?.let { manager.getLinkProperties(it) }
        val servers = properties?.dnsServers?.mapNotNull { it.hostAddress }.orEmpty()
        return servers.ifEmpty { listOf("8.8.8.8", "1.1.1.1") }
    }

    companion object {
        private const val GUEST_PATH =
            "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
    }
}
