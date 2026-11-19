package dev.prootcode.app.runtime.codeserver

import android.content.Context
import dev.prootcode.app.data.environment.Environment
import dev.prootcode.app.runtime.proot.EnvironmentMounts
import dev.prootcode.app.runtime.proot.GuestEnvironment
import dev.prootcode.app.runtime.proot.ProotCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

sealed interface CodeServerStatus {
    data object Stopped : CodeServerStatus

    data object Starting : CodeServerStatus

    data class Running(
        val port: Int,
    ) : CodeServerStatus

    data class Failed(
        val message: String,
    ) : CodeServerStatus
}

fun findCodeServerBinary(rootfs: File): File? {
    val base = File(rootfs, "opt/code-server")
    val candidates =
        buildList {
            add(File(base, "bin/code-server"))
            base.listFiles()?.filter { it.isDirectory }?.forEach { directory ->
                add(File(directory, "bin/code-server"))
                directory.listFiles()?.filter { it.isDirectory }?.forEach { nested ->
                    add(File(nested, "bin/code-server"))
                }
            }
        }
    return candidates.firstOrNull { it.isFile }
}

fun codeServerGuestPath(
    rootfs: File,
    binary: File,
): String = binary.absolutePath.removePrefix(rootfs.absolutePath)

fun codeServerArgs(
    port: Int,
    workspace: String = "/root/projects_bridge",
): List<String> =
    listOf(
        "--bind-addr",
        "127.0.0.1:$port",
        "--auth",
        "none",
        "--disable-telemetry",
        "--disable-update-check",
        workspace,
    )

fun isCodeServerPortOpen(port: Int): Boolean =
    runCatching {
        Socket().use { socket ->
            socket.connect(InetSocketAddress("127.0.0.1", port), PORT_PROBE_TIMEOUT_MS)
        }
    }.isSuccess

private const val PORT_PROBE_TIMEOUT_MS = 300

private const val START_TIMEOUT_MS = 90_000L

private const val POLL_INTERVAL_MS = 400L

private const val MIN_PORT = 1024

private const val MAX_PORT = 65535

object CodeServerManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val statuses = mutableMapOf<String, MutableStateFlow<CodeServerStatus>>()
    private val processes = mutableMapOf<String, Process>()
    private val jobs = mutableMapOf<String, Job>()

    fun status(environmentId: String): StateFlow<CodeServerStatus> = statusFlow(environmentId).asStateFlow()

    fun isRunning(environmentId: String): Boolean = statusFlow(environmentId).value is CodeServerStatus.Running

    fun start(
        context: Context,
        environment: Environment,
    ): Job {
        val environmentId = environment.id
        jobs.remove(environmentId)?.cancel()
        statusFlow(environmentId).value = CodeServerStatus.Starting
        val job = scope.launch { launchProcess(context.applicationContext, environment) }
        jobs[environmentId] = job
        return job
    }

    fun stop(environmentId: String) {
        jobs.remove(environmentId)?.cancel()
        processes.remove(environmentId)?.let { process ->
            runCatching { process.destroy() }
            runCatching { process.destroyForcibly() }
        }
        statusFlow(environmentId).value = CodeServerStatus.Stopped
        scope.launch { killLeftoverProcesses(environmentId) }
    }

    fun logFile(
        context: Context,
        environmentId: String,
    ): File = File(File(context.filesDir, "code-server"), "$environmentId.log")

    private fun statusFlow(environmentId: String): MutableStateFlow<CodeServerStatus> =
        statuses.getOrPut(environmentId) { MutableStateFlow(CodeServerStatus.Stopped) }

    private suspend fun launchProcess(
        context: Context,
        environment: Environment,
    ) {
        val environmentId = environment.id
        val port = environment.settings.codeServerPort.coerceIn(MIN_PORT, MAX_PORT)
        if (isCodeServerPortOpen(port)) {
            statusFlow(environmentId).value = CodeServerStatus.Running(port)
            return
        }
        val rootfs = File(context.filesDir, "environments/$environmentId/rootfs")
        val binary = findCodeServerBinary(rootfs)
        if (binary == null) {
            statusFlow(environmentId).value =
                CodeServerStatus.Failed("code-server is not installed in this environment")
            return
        }
        val log = logFile(context, environmentId)
        log.parentFile?.mkdirs()
        runCatching { log.writeText("") }
        runCatching {
            val command =
                ProotCommand(
                    nativeLibDir = File(context.applicationInfo.nativeLibraryDir),
                    rootfsDir = rootfs,
                    tmpDir = File(context.filesDir, "runtime/tmp"),
                    projectsDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "projects"),
                    mounts = EnvironmentMounts.of(context, environment.settings, rootfs),
                )
            val argv =
                command.execArgv(
                    listOf(codeServerGuestPath(rootfs, binary)) + codeServerArgs(port),
                )
            val builder =
                ProcessBuilder(argv)
                    .directory(context.filesDir)
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(log))
            File(rootfs, "tmp/runtime-root").mkdirs()
            val processEnvironment = builder.environment()
            processEnvironment.putAll(command.environment())
            processEnvironment.putAll(GuestEnvironment.of(environment.settings))
            processEnvironment["TMPDIR"] = "/tmp"
            processEnvironment["XDG_RUNTIME_DIR"] = "/tmp/runtime-root"
            processEnvironment["XDG_CACHE_HOME"] = "/root/.cache"
            val process = builder.start()
            processes[environmentId] = process
            val result = awaitReady(port, process, log)
            statusFlow(environmentId).value = result
            if (result is CodeServerStatus.Failed) {
                stop(environmentId)
                statusFlow(environmentId).value = result
            }
        }.onFailure { failure ->
            statusFlow(environmentId).value =
                CodeServerStatus.Failed(
                    failure.message ?: failure.javaClass.simpleName,
                )
            stop(environmentId)
            statusFlow(environmentId).value =
                CodeServerStatus.Failed(
                    failure.message ?: failure.javaClass.simpleName,
                )
        }
    }

    private suspend fun awaitReady(
        port: Int,
        process: Process,
        log: File,
    ): CodeServerStatus {
        val deadline = System.currentTimeMillis() + START_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            currentCoroutineContext().ensureActive()
            if (isCodeServerPortOpen(port)) return CodeServerStatus.Running(port)
            if (!process.isAlive) {
                return CodeServerStatus.Failed(
                    logTail(log).ifBlank { "code-server exited unexpectedly" },
                )
            }
            delay(POLL_INTERVAL_MS)
        }
        return CodeServerStatus.Failed("Timed out waiting for code-server on port $port")
    }

    private fun logTail(
        file: File,
        lines: Int = 15,
    ): String = runCatching { file.readLines().takeLast(lines).joinToString("\n") }.getOrDefault("")

    private fun killLeftoverProcesses(environmentId: String) {
        val marker = "environments/$environmentId/rootfs"
        runCatching {
            File("/proc").listFiles()?.forEach { directory ->
                val pid = directory.name.toIntOrNull() ?: return@forEach
                if (pid == android.os.Process.myPid()) return@forEach
                val cmdline =
                    runCatching { File(directory, "cmdline").readText() }.getOrNull()
                        ?: return@forEach
                if (cmdline.contains(marker) && cmdline.contains("code-server")) {
                    runCatching { android.os.Process.killProcess(pid) }
                }
            }
        }
    }
}
