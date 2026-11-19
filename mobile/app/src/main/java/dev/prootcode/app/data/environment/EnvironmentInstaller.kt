package dev.prootcode.app.data.environment

import dev.prootcode.app.runtime.network.ResolvConf
import dev.prootcode.app.runtime.proot.GuestEnvironment
import dev.prootcode.app.runtime.proot.ProotCommand
import dev.prootcode.app.runtime.rootfs.RootfsExtractor
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.CancellationException as KCancellationException

enum class InstallStage {
    EXTRACT_ROOTFS,
    INSTALL_PACKAGES,
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
    val presets: List<String> = emptyList(),
    val packages: List<String> = emptyList(),
    val settings: EnvironmentSettings = EnvironmentSettings(),
)

class EnvironmentInstaller(
    private val environmentsDir: File,
    private val registry: EnvironmentRegistry,
    private val nativeLibDir: File? = null,
    private val tmpDir: File? = null,
    private val projectsDir: File? = null,
    private val mounts: List<Pair<String, String>> = emptyList(),
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
        try {
            onStage(InstallStage.EXTRACT_ROOTFS)
            RootfsExtractor.extract(rootfs, rootfsDir, request.distroVersion)
            ResolvConf.write(rootfsDir, request.dnsServers)
            val packages =
                (request.packages + PresetPackages.packagesFor(request.presets))
                    .distinct()
                    .sorted()
            if (packages.isNotEmpty() && nativeLibDir != null && tmpDir != null) {
                onStage(InstallStage.INSTALL_PACKAGES)
                installPackages(request.id, rootfsDir, packages, request.settings)
            }
            if (codeServer != null) {
                onStage(InstallStage.INSTALL_CODE_SERVER)
                RootfsExtractor.extract(
                    codeServer,
                    File(rootfsDir, CODE_SERVER_DIR),
                    request.codeServerVersion.orEmpty(),
                )
            }
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
                    presets = request.presets,
                    settings = request.settings,
                )
            registry.add(environment)
            onStage(InstallStage.READY)
            return environment
        } catch (failure: Throwable) {
            directory.deleteRecursively()
            throw failure
        }
    }

    fun installCodeServer(
        environment: Environment,
        version: String,
        archive: InputStream,
    ): Environment {
        val target = rootfsDir(environment.id).resolve(CODE_SERVER_DIR)
        target.deleteRecursively()
        RootfsExtractor.extract(archive, target, version)
        val updated =
            environment.copy(
                codeServerVersion = version,
                sizeBytes = sizeOf(environmentDir(environment.id)),
            )
        registry.add(updated)
        return updated
    }

    fun delete(id: String) {
        environmentDir(id).deleteRecursively()
        registry.remove(id)
    }

    fun sizeOf(directory: File): Long = directory.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    private fun installPackages(
        environmentId: String,
        rootfsDir: File,
        packages: List<String>,
        settings: EnvironmentSettings,
    ) {
        tmpDir!!.mkdirs()
        val log = File(environmentDir(environmentId), "install.log")
        log.parentFile?.mkdirs()
        log.writeText("")
        val command =
            ProotCommand(
                nativeLibDir = nativeLibDir!!,
                rootfsDir = rootfsDir,
                tmpDir = tmpDir,
                projectsDir = projectsDir ?: File(rootfsDir, "root"),
                mounts = emptyList(),
            )
        val script =
            "export DEBIAN_FRONTEND=noninteractive; " +
                "apt-get update && " +
                "apt-get install -y --no-install-recommends ${packages.joinToString(" ")}"
        val processBuilder =
            ProcessBuilder(command.execArgv(listOf("/bin/bash", "-c", script)))
                .directory(tmpDir)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(log))
        val processEnvironment = processBuilder.environment()
        processEnvironment.putAll(command.environment())
        processEnvironment.putAll(GuestEnvironment.of(settings))
        processEnvironment["DEBIAN_FRONTEND"] = "noninteractive"
        processEnvironment["TERM"] = "dumb"
        val process = processBuilder.start()
        try {
            val exit = process.waitFor()
            if (exit != 0) {
                val tail = log.readLines().takeLast(20).joinToString("\n")
                throw IOException("Package installation failed (exit $exit): $tail")
            }
        } catch (interrupted: InterruptedException) {
            process.destroyForcibly()
            throw KCancellationException(interrupted.message, interrupted)
        } finally {
            if (process.isAlive) process.destroyForcibly()
        }
    }

    companion object {
        const val CODE_SERVER_DIR = "opt/code-server"

        object PresetPackages {
            private val packagesByPreset =
                mapOf(
                    Presets.ESSENTIALS to listOf("git", "build-essential", "curl"),
                    Presets.EDITORS to listOf("neovim"),
                    Presets.LANGUAGES to listOf("python3", "nodejs", "golang", "rustc"),
                    Presets.UTILITIES to listOf("htop", "ripgrep", "tmux"),
                )

            fun packagesFor(presets: List<String>): List<String> =
                presets.flatMap { packagesByPreset[it].orEmpty() }.distinct().sorted()
        }
    }
}
