package dev.prootcode.app.data.environment

import dev.prootcode.app.runtime.network.ResolvConf
import dev.prootcode.app.runtime.rootfs.RootfsExtractor
import java.io.File
import java.io.InputStream

enum class InstallStage {
    EXTRACT_ROOTFS,
    INSTALL_CODE_SERVER,
    READY,
}

data class InstallRequest(
    val id: String,
    val name: String,
    val distroId: String,
    val distroVersion: String,
    val arch: String,
    val codeServerVersion: String?,
    val dnsServers: List<String>,
)

class EnvironmentInstaller(
    private val environmentsDir: File,
    private val registry: EnvironmentRegistry,
) {
    fun environmentDir(id: String): File = File(environmentsDir, id)

    fun rootfsDir(id: String): File = File(environmentDir(id), "rootfs")

    fun install(
        request: InstallRequest,
        rootfs: InputStream,
        codeServer: InputStream?,
        onStage: (InstallStage) -> Unit = {},
    ): Environment {
        val directory = environmentDir(request.id)
        directory.deleteRecursively()
        val rootfsDir = File(directory, "rootfs")
        onStage(InstallStage.EXTRACT_ROOTFS)
        RootfsExtractor.extract(rootfs, rootfsDir, request.distroVersion)
        if (codeServer != null) {
            onStage(InstallStage.INSTALL_CODE_SERVER)
            RootfsExtractor.extract(
                codeServer,
                File(rootfsDir, CODE_SERVER_DIR),
                request.codeServerVersion.orEmpty(),
            )
        }
        ResolvConf.write(rootfsDir, request.dnsServers)
        val environment =
            Environment(
                id = request.id,
                name = request.name,
                distroId = request.distroId,
                distroVersion = request.distroVersion,
                arch = request.arch,
                codeServerVersion = request.codeServerVersion,
                createdAtMillis = System.currentTimeMillis(),
                sizeBytes = sizeOf(directory),
            )
        registry.add(environment)
        onStage(InstallStage.READY)
        return environment
    }

    fun delete(id: String) {
        environmentDir(id).deleteRecursively()
        registry.remove(id)
    }

    fun sizeOf(directory: File): Long = directory.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    companion object {
        const val CODE_SERVER_DIR = "opt/code-server"
    }
}
