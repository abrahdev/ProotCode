package dev.prootcode.app.runtime.network

import java.io.File

object ResolvConf {
    fun write(
        rootfsDir: File,
        servers: List<String>,
    ) {
        val file = File(rootfsDir, "etc/resolv.conf")
        file.parentFile?.mkdirs()
        file.delete()
        file.writeText(servers.joinToString(separator = "\n", postfix = "\n") { "nameserver $it" })
    }
}
