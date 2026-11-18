package dev.prootcode.app.ui.viewmodels

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import dev.prootcode.app.BuildConfig
import dev.prootcode.app.R
import dev.prootcode.app.data.catalog.Catalog
import dev.prootcode.app.data.catalog.CatalogDefaults
import dev.prootcode.app.data.catalog.CodeServerArtifact
import dev.prootcode.app.data.catalog.DistroArtifact
import dev.prootcode.app.data.environment.Artifacts
import dev.prootcode.app.data.environment.BridgeVerifier
import dev.prootcode.app.data.environment.Environment
import dev.prootcode.app.data.environment.EnvironmentInstaller
import dev.prootcode.app.data.environment.EnvironmentRegistry
import dev.prootcode.app.data.environment.EnvironmentSettings
import dev.prootcode.app.data.environment.InstallRequest
import dev.prootcode.app.data.environment.InstallStage
import dev.prootcode.app.runtime.codeserver.CodeServerManager
import dev.prootcode.app.runtime.network.DnsResolver
import dev.prootcode.app.ui.terminal.TerminalSessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID

data class HomeUiState(
    val environments: List<Environment> = emptyList(),
    val totalBytes: Long = 0,
    val catalog: Catalog? = null,
    val busy: Boolean = false,
    val stage: InstallStage? = null,
    val progress: Pair<Long, Long>? = null,
    val error: String? = null,
    val messageRes: Int? = null,
    val messageArg: String? = null,
    val messageText: String? = null,
    val adbConnected: Boolean = false,
    val reorderMode: Boolean = false,
)

data class EnvironmentInstallInput(
    val name: String,
    val distro: DistroArtifact,
    val codeServer: CodeServerArtifact?,
    val presets: List<String>,
    val settings: EnvironmentSettings,
)

class HomeViewModel(
    private val context: Context,
) : ViewModel() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val registry = EnvironmentRegistry(File(context.filesDir, "environments.json"))
    val arch: String =
        Build.SUPPORTED_ABIS.firstOrNull { it in SUPPORTED_ARCHES } ?: SUPPORTED_ARCHES.first()
    val projectsDir: File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, "projects")

    private val installer = EnvironmentInstaller(File(context.filesDir, ENVIRONMENTS_DIR), registry)
    private val artifacts = Artifacts(context)
    private val repository = CatalogDefaults.create(context.assets, context.cacheDir)
    private val bridgeVerifier =
        BridgeVerifier(
            nativeLibDir = File(context.applicationInfo.nativeLibraryDir),
            tmpDir = File(context.filesDir, "runtime/tmp"),
            projectsDir = projectsDir,
        )

    private val state = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = state.asStateFlow()

    private var installJob: Job? = null

    init {
        refreshEnvironments()
        loadCatalog(refresh = false)
    }

    fun refreshEnvironments() {
        scope.launch {
            val environments = withContext(Dispatchers.IO) { registry.load() }
            state.update {
                it.copy(
                    environments = environments,
                    totalBytes = environments.sumOf { env ->
                        env.sizeBytes
                    },
                )
            }
        }
    }

    fun loadCatalog(refresh: Boolean) {
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { repository.load(refresh) } }
                .onSuccess { catalog -> state.update { it.copy(catalog = catalog) } }
                .onFailure { failure ->
                    state.update { it.copy(error = failure.message ?: failure.javaClass.simpleName) }
                }
        }
    }

    fun install(
        input: EnvironmentInstallInput,
        onInstalled: () -> Unit,
    ) {
        if (state.value.busy) return
        state.update {
            it.copy(
                busy = true,
                stage = null,
                progress = null,
                error = null,
                messageRes = null,
                messageText = null,
                messageArg = null,
            )
        }
        installJob =
            scope.launch {
                try {
                    val request =
                        InstallRequest(
                            id = UUID.randomUUID().toString(),
                            name = input.name,
                            distroId = input.distro.id,
                            distroVersion = input.distro.version,
                            arch = input.distro.arch,
                            codeServerVersion = input.codeServer?.version,
                            dnsServers = DnsResolver.servers(context),
                            presets = input.presets,
                            settings = input.settings,
                        )
                    withContext(Dispatchers.IO) {
                        openRootfs(input.distro).use { rootfsStream ->
                            openCodeServer(input.codeServer, forceDownload = false).use { codeServerStream ->
                                installer.install(request, rootfsStream, codeServerStream) { stage ->
                                    state.update { it.copy(stage = stage) }
                                }
                            }
                        }
                        val bridgeOk = bridgeVerifier.verify(installer.rootfsDir(request.id))
                        state.update {
                            it.copy(
                                messageRes =
                                    if (bridgeOk) {
                                        R.string.bridge_ok
                                    } else {
                                        R.string.bridge_failed
                                    },
                            )
                        }
                        refreshEnvironments()
                    }
                    onInstalled()
                } catch (cancelled: CancellationException) {
                    state.update { it.copy(messageRes = R.string.install_cancelled) }
                    throw cancelled
                } catch (failure: Throwable) {
                    state.update {
                        it.copy(error = failure.message ?: failure.javaClass.simpleName)
                    }
                } finally {
                    state.update { it.copy(busy = false, stage = null, progress = null) }
                }
            }
    }

    fun cancelInstall() {
        installJob?.cancel()
    }

    fun changeCodeServer(
        environment: Environment,
        artifact: CodeServerArtifact,
    ) {
        if (state.value.busy) return
        state.update { it.copy(busy = true, error = null, messageRes = null, messageText = null) }
        installJob =
            scope.launch {
                try {
                    val forceDownload =
                        !(
                            BuildConfig.BUNDLED_ARTIFACTS &&
                                artifact.version == BuildConfig.BUNDLED_CODE_SERVER_VERSION
                        )
                    withContext(Dispatchers.IO) {
                        val stream = openCodeServer(artifact, forceDownload)
                        if (stream != null) {
                            stream.use { input ->
                                installer.installCodeServer(environment, artifact.version, input)
                            }
                        }
                    }
                    state.update {
                        it.copy(
                            messageRes = R.string.code_server_changed,
                            messageArg = artifact.version,
                        )
                    }
                    refreshEnvironments()
                } catch (failure: Throwable) {
                    state.update {
                        it.copy(error = failure.message ?: failure.javaClass.simpleName)
                    }
                } finally {
                    state.update { it.copy(busy = false, progress = null) }
                }
            }
    }

    fun testBridge(environment: Environment) {
        state.update { it.copy(error = null, messageRes = null, messageText = null) }
        scope.launch {
            val ok =
                withContext(Dispatchers.IO) {
                    bridgeVerifier.verify(installer.rootfsDir(environment.id))
                }
            state.update {
                it.copy(messageRes = if (ok) R.string.bridge_ok else R.string.bridge_failed)
            }
        }
    }

    fun delete(environment: Environment) {
        scope.launch {
            withContext(Dispatchers.IO) {
                CodeServerManager.stop(environment.id)
                TerminalSessionManager.closeAll(context, environment.id)
                installer.delete(environment.id)
            }
            refreshEnvironments()
        }
    }

    fun updateEnvironment(environment: Environment) {
        scope.launch {
            withContext(Dispatchers.IO) { registry.update(environment) }
            refreshEnvironments()
        }
    }

    fun reorder(ids: List<String>) {
        scope.launch {
            withContext(Dispatchers.IO) { registry.reorder(ids) }
            refreshEnvironments()
        }
    }

    fun setReorderMode(enabled: Boolean) {
        state.update { it.copy(reorderMode = enabled) }
    }

    fun setAdbConnected(connected: Boolean) {
        state.update { it.copy(adbConnected = connected) }
    }

    fun showMessage(
        resId: Int,
        arg: String? = null,
    ) {
        state.update {
            it.copy(messageRes = resId, messageArg = arg, messageText = null)
        }
    }

    fun showText(text: String) {
        state.update { it.copy(messageText = text, messageRes = null) }
    }

    fun clearMessage() {
        state.update { it.copy(messageRes = null, messageArg = null, messageText = null) }
    }

    fun clearError() {
        state.update { it.copy(error = null) }
    }

    fun rootfsDir(environmentId: String): File = installer.rootfsDir(environmentId)

    fun environmentDir(environmentId: String): File = installer.environmentDir(environmentId)

    private suspend fun openRootfs(distro: DistroArtifact): InputStream {
        if (BuildConfig.BUNDLED_ARTIFACTS) {
            artifacts.openBundled(ROOTFS_ASSET)?.let { return it }
        }
        return artifacts
            .download(
                name = "rootfs-${distro.arch}.bin",
                url = distro.url,
                sha256 = distro.sha256,
                size = distro.size,
            ) { received, total -> state.update { it.copy(progress = received to total) } }
            .inputStream()
    }

    private suspend fun openCodeServer(
        artifact: CodeServerArtifact?,
        forceDownload: Boolean,
    ): InputStream? {
        if (artifact == null) return null
        val bundled =
            !forceDownload &&
                BuildConfig.BUNDLED_ARTIFACTS &&
                artifact.version == BuildConfig.BUNDLED_CODE_SERVER_VERSION
        if (bundled) {
            artifacts.openBundled(CODE_SERVER_ASSET)?.let { return it }
        }
        return artifacts
            .download(
                name = "code-server-${artifact.version}-${artifact.arch}.tar.gz",
                url = artifact.url,
                sha256 = artifact.sha256,
                size = artifact.size,
            ) { received, total -> state.update { it.copy(progress = received to total) } }
            .inputStream()
    }

    override fun onCleared() {
        scope.cancel()
        super.onCleared()
    }

    companion object {
        private const val ENVIRONMENTS_DIR = "environments"
        private const val ROOTFS_ASSET = "bundled/rootfs.bin"
        private const val CODE_SERVER_ASSET = "bundled/code-server.bin"
        private val SUPPORTED_ARCHES = listOf("arm64-v8a", "x86_64")
    }
}
