package dev.prootcode.app.ui.screens

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import dev.prootcode.app.data.environment.InstallRequest
import dev.prootcode.app.data.environment.InstallStage
import dev.prootcode.app.data.environment.Presets
import dev.prootcode.app.runtime.network.DnsResolver
import dev.prootcode.app.ui.terminal.TerminalActivity
import dev.prootcode.app.ui.terminal.TerminalSessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.Locale
import java.util.UUID

@Composable
fun HomeScreen(
    onOpenEnvironment: (Environment) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val environments = remember { mutableStateListOf<Environment>() }
    var catalog by remember { mutableStateOf<Catalog?>(null) }
    var showWizard by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var stage by remember { mutableStateOf<InstallStage?>(null) }
    var progress by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var messageRes by remember { mutableStateOf<Int?>(null) }
    var messageArg by remember { mutableStateOf<String?>(null) }
    var installJob by remember { mutableStateOf<Job?>(null) }
    var settingsEnvironment by remember { mutableStateOf<Environment?>(null) }

    val arch =
        remember {
            Build.SUPPORTED_ABIS.firstOrNull { it in SUPPORTED_ARCHES } ?: SUPPORTED_ARCHES.first()
        }
    val registry = remember { EnvironmentRegistry(File(context.filesDir, "environments.json")) }
    val installer = remember { EnvironmentInstaller(File(context.filesDir, "environments"), registry) }
    val artifacts = remember { Artifacts(context) }
    val repository = remember { CatalogDefaults.create(context.assets, context.cacheDir) }
    val projectsDir =
        remember { File(context.getExternalFilesDir(null) ?: context.filesDir, "projects") }
    val bridgeVerifier =
        remember {
            BridgeVerifier(
                nativeLibDir = File(context.applicationInfo.nativeLibraryDir),
                tmpDir = File(context.filesDir, "runtime/tmp"),
                projectsDir = projectsDir,
            )
        }

    fun refreshEnvironments() {
        environments.clear()
        environments.addAll(registry.load())
    }

    fun loadCatalog(refresh: Boolean) {
        scope.launch(Dispatchers.IO) {
            runCatching { repository.load(refresh) }
                .onSuccess { catalog = it }
                .onFailure { error = it.message }
        }
    }

    suspend fun openRootfs(distro: DistroArtifact): InputStream {
        if (BuildConfig.BUNDLED_ARTIFACTS) {
            artifacts.openBundled(ROOTFS_ASSET)?.let { return it }
        }
        val file =
            artifacts.download(
                name = "rootfs-${distro.arch}.bin",
                url = distro.url,
                sha256 = distro.sha256,
                size = distro.size,
            ) { received, total -> progress = received to total }
        return file.inputStream()
    }

    suspend fun openCodeServer(
        artifact: CodeServerArtifact,
        forceDownload: Boolean,
    ): InputStream {
        val bundled =
            !forceDownload &&
                BuildConfig.BUNDLED_ARTIFACTS &&
                artifact.version == BuildConfig.BUNDLED_CODE_SERVER_VERSION
        if (bundled) {
            artifacts.openBundled(CODE_SERVER_ASSET)?.let { return it }
        }
        val file =
            artifacts.download(
                name = "code-server-${artifact.version}-${artifact.arch}.tar.gz",
                url = artifact.url,
                sha256 = artifact.sha256,
                size = artifact.size,
            ) { received, total -> progress = received to total }
        return file.inputStream()
    }

    fun install(
        name: String,
        distro: DistroArtifact,
        codeServer: CodeServerArtifact?,
        presets: List<String>,
    ) {
        busy = true
        error = null
        messageRes = null
        messageArg = null
        progress = null
        installJob =
            scope.launch(Dispatchers.IO) {
                try {
                    val request =
                        InstallRequest(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            distroId = distro.id,
                            distroVersion = distro.version,
                            arch = distro.arch,
                            codeServerVersion = codeServer?.version,
                            dnsServers = DnsResolver.servers(context),
                            presets = presets,
                        )
                    openRootfs(distro).use { rootfsStream ->
                        val codeServerStream =
                            codeServer?.let { openCodeServer(it, forceDownload = false) }
                        codeServerStream.use { stream ->
                            installer.install(request, rootfsStream, stream) { stage = it }
                        }
                    }
                    val bridgeOk = bridgeVerifier.verify(installer.rootfsDir(request.id))
                    withContext(Dispatchers.Main) {
                        refreshEnvironments()
                        showWizard = false
                        messageRes = if (bridgeOk) R.string.bridge_ok else R.string.bridge_failed
                    }
                } catch (cancelled: CancellationException) {
                    withContext(Dispatchers.Main) { messageRes = R.string.install_cancelled }
                    throw cancelled
                } catch (failure: Throwable) {
                    error = failure.message ?: failure.javaClass.simpleName
                } finally {
                    busy = false
                    progress = null
                    stage = null
                }
            }
    }

    fun changeCodeServer(
        environment: Environment,
        artifact: CodeServerArtifact,
    ) {
        busy = true
        error = null
        messageRes = null
        messageArg = null
        progress = null
        installJob =
            scope.launch(Dispatchers.IO) {
                try {
                    val forceDownload =
                        !(
                            BuildConfig.BUNDLED_ARTIFACTS &&
                                artifact.version == BuildConfig.BUNDLED_CODE_SERVER_VERSION
                        )
                    openCodeServer(artifact, forceDownload).use { stream ->
                        installer.installCodeServer(environment, artifact.version, stream)
                    }
                    withContext(Dispatchers.Main) {
                        refreshEnvironments()
                        messageRes = R.string.code_server_changed
                        messageArg = artifact.version
                    }
                } catch (failure: Throwable) {
                    error = failure.message ?: failure.javaClass.simpleName
                } finally {
                    busy = false
                    progress = null
                }
            }
    }

    fun testBridge(environment: Environment) {
        error = null
        messageRes = null
        messageArg = null
        scope.launch(Dispatchers.IO) {
            val ok = bridgeVerifier.verify(installer.rootfsDir(environment.id))
            withContext(Dispatchers.Main) {
                messageRes = if (ok) R.string.bridge_ok else R.string.bridge_failed
            }
        }
    }

    fun delete(environment: Environment) {
        scope.launch(Dispatchers.IO) {
            TerminalSessionManager.closeAll(context, environment.id)
            installer.delete(environment.id)
            withContext(Dispatchers.Main) { refreshEnvironments() }
        }
    }

    fun openWindow(environment: Environment) {
        val intent =
            Intent(context, TerminalActivity::class.java)
                .putExtra(TerminalActivity.EXTRA_ENVIRONMENT_ID, environment.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        context.startActivity(intent)
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { refreshEnvironments() }
        loadCatalog(refresh = false)
    }

    LaunchedEffect(showWizard) {
        if (showWizard) loadCatalog(refresh = true)
    }

    val messageText =
        messageRes?.let { resource ->
            messageArg?.let { argument -> stringResource(resource, argument) }
                ?: stringResource(resource)
        }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            SmallFloatingActionButton(onClick = { showWizard = true }) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.environments_new),
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(onClick = onSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.settings_title),
                        )
                    }
                }
            }

            error?.let { text ->
                item {
                    Text(
                        text = text,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            messageText?.let { text ->
                item {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            when {
                busy ->
                    item {
                        BusyCard(
                            stage = stage,
                            progress = progress,
                            onCancel = { installJob?.cancel() },
                        )
                    }
                showWizard ->
                    item {
                        Wizard(
                            catalog = catalog,
                            arch = arch,
                            existingNames = environments.map { it.name },
                            enabled = catalog != null,
                            onRefresh = { loadCatalog(refresh = true) },
                            onInstall = { name, distro, codeServer, presets ->
                                install(name, distro, codeServer, presets)
                            },
                            onCancel = { showWizard = false },
                        )
                    }
                environments.isEmpty() ->
                    item {
                        HomeEmptyState(onCreate = { showWizard = true })
                    }
                else ->
                    items(environments, key = { it.id }) { environment ->
                        EnvironmentHeroCard(
                            environment = environment,
                            onOpenTerminal = { onOpenEnvironment(environment) },
                            onOpenWindow = { openWindow(environment) },
                            onSettings = { settingsEnvironment = environment },
                        )
                    }
            }

            if (!busy && !showWizard && environments.isNotEmpty()) {
                item {
                    Text(
                        text =
                            stringResource(
                                R.string.environments_storage,
                                formatBytes(registry.totalBytes()),
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    settingsEnvironment?.let { environment ->
        EnvironmentSettingsSheet(
            environment = environment,
            codeServers = catalog?.codeServerFor(arch).orEmpty(),
            onDismiss = { settingsEnvironment = null },
            onTestBridge = {
                testBridge(environment)
                settingsEnvironment = null
            },
            onChangeCodeServer = { artifact ->
                changeCodeServer(environment, artifact)
                settingsEnvironment = null
            },
            onDelete = {
                delete(environment)
                settingsEnvironment = null
            },
        )
    }
}

@Composable
private fun HomeEmptyState(onCreate: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Outlined.Terminal,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.home_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onCreate) {
                Text(stringResource(R.string.environments_new))
            }
        }
    }
}

@Composable
private fun EnvironmentHeroCard(
    environment: Environment,
    onOpenTerminal: () -> Unit,
    onOpenWindow: () -> Unit,
    onSettings: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = environment.name,
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text =
                    "${environment.distroVersion} · ${environment.arch} · " +
                        formatBytes(environment.sizeBytes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onOpenTerminal) {
                    Icon(
                        imageVector = Icons.Outlined.Terminal,
                        contentDescription = stringResource(R.string.environments_terminal),
                    )
                }
                IconButton(onClick = onOpenWindow) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = stringResource(R.string.terminal_window),
                    )
                }
                IconButton(onClick = onSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.environments_settings),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnvironmentSettingsSheet(
    environment: Environment,
    codeServers: List<CodeServerArtifact>,
    onDismiss: () -> Unit,
    onTestBridge: () -> Unit,
    onChangeCodeServer: (CodeServerArtifact) -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = environment.name, style = MaterialTheme.typography.headlineSmall)
            Text(
                text =
                    "${environment.distroVersion} · ${environment.arch} · " +
                        formatBytes(environment.sizeBytes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ListItem(
                leadingContent = { Icon(Icons.Outlined.Sync, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.bridge_test)) },
                modifier = Modifier.clickable { onTestBridge() },
            )
            codeServers
                .filter { it.version != environment.codeServerVersion }
                .forEach { artifact ->
                    ListItem(
                        leadingContent = { Icon(Icons.Outlined.SwapHoriz, contentDescription = null) },
                        headlineContent = {
                            Text(stringResource(R.string.code_server_change, artifact.version))
                        },
                        modifier = Modifier.clickable { onChangeCodeServer(artifact) },
                    )
                }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            ListItem(
                leadingContent = {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                headlineContent = {
                    Text(
                        text = stringResource(R.string.environments_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                modifier = Modifier.clickable { onDelete() },
            )
        }
    }
}

@Composable
private fun BusyCard(
    stage: InstallStage?,
    progress: Pair<Long, Long>?,
    onCancel: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val label =
                when (stage) {
                    InstallStage.EXTRACT_ROOTFS -> stringResource(R.string.install_stage_rootfs)
                    InstallStage.INSTALL_CODE_SERVER ->
                        stringResource(R.string.install_stage_code_server)
                    InstallStage.READY -> stringResource(R.string.install_stage_ready)
                    null -> stringResource(R.string.install_downloading)
                }
            Text(text = label, style = MaterialTheme.typography.titleSmall)
            val current = progress
            if (current != null && current.second > 0) {
                LinearProgressIndicator(
                    progress = { current.first.toFloat() / current.second.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "${current.first * 100 / current.second}%",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.install_cancel))
            }
        }
    }
}

@Composable
private fun Wizard(
    catalog: Catalog?,
    arch: String,
    existingNames: List<String>,
    enabled: Boolean,
    onRefresh: () -> Unit,
    onInstall: (String, DistroArtifact, CodeServerArtifact?, List<String>) -> Unit,
    onCancel: (() -> Unit)?,
) {
    val distros = remember(catalog, arch) { catalog?.distrosFor(arch).orEmpty() }
    val codeServers = remember(catalog, arch) { catalog?.codeServerFor(arch).orEmpty() }
    var name by remember { mutableStateOf("") }
    var distroIndex by remember(distros) { mutableStateOf(0) }
    var codeServerIndex by remember(codeServers) {
        mutableStateOf(if (codeServers.isEmpty()) -1 else 0)
    }
    val selectedPresets = remember { mutableStateListOf<String>() }
    val duplicate = existingNames.any { it.equals(name.trim(), ignoreCase = true) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.wizard_title),
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.wizard_name)) },
                singleLine = true,
                isError = duplicate,
                modifier = Modifier.fillMaxWidth(),
            )
            if (duplicate) {
                Text(
                    text = stringResource(R.string.wizard_name_taken),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Text(
                text = stringResource(R.string.wizard_distro),
                style = MaterialTheme.typography.titleSmall,
            )
            if (distros.isEmpty()) {
                Text(
                    text = stringResource(R.string.wizard_no_distros),
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                distros.forEachIndexed { index, distro ->
                    ChoiceRow(
                        selected = index == distroIndex,
                        label = "${distro.name} ${distro.version} (${distro.arch})",
                        onSelect = { distroIndex = index },
                    )
                }
            }

            Text(
                text = stringResource(R.string.wizard_code_server),
                style = MaterialTheme.typography.titleSmall,
            )
            ChoiceRow(
                selected = codeServerIndex == -1,
                label = stringResource(R.string.wizard_none),
                onSelect = { codeServerIndex = -1 },
            )
            codeServers.forEachIndexed { index, codeServer ->
                ChoiceRow(
                    selected = index == codeServerIndex,
                    label = "${codeServer.version} (${codeServer.arch})",
                    onSelect = { codeServerIndex = index },
                )
            }

            Text(
                text = stringResource(R.string.wizard_presets),
                style = MaterialTheme.typography.titleSmall,
            )
            Presets.ALL.forEach { preset ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = preset in selectedPresets,
                        onCheckedChange = { checked ->
                            if (checked) {
                                selectedPresets.add(preset)
                            } else {
                                selectedPresets.remove(preset)
                            }
                        },
                    )
                    Text(text = stringResource(presetLabel(preset)))
                }
            }
            Text(
                text = stringResource(R.string.wizard_presets_note),
                style = MaterialTheme.typography.bodySmall,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        val distro = distros.getOrNull(distroIndex) ?: return@Button
                        onInstall(
                            name.trim(),
                            distro,
                            codeServers.getOrNull(codeServerIndex),
                            selectedPresets.toList(),
                        )
                    },
                    enabled = enabled && name.isNotBlank() && !duplicate && distros.isNotEmpty(),
                ) {
                    Text(stringResource(R.string.wizard_install))
                }
                TextButton(onClick = onRefresh) {
                    Text(stringResource(R.string.catalog_refresh))
                }
                onCancel?.let { cancel ->
                    TextButton(onClick = cancel) {
                        Text(stringResource(R.string.wizard_cancel))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceRow(
    selected: Boolean,
    label: String,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(text = label)
    }
}

private fun presetLabel(preset: String): Int =
    when (preset) {
        Presets.ESSENTIALS -> R.string.preset_essentials
        Presets.EDITORS -> R.string.preset_editors
        Presets.LANGUAGES -> R.string.preset_languages
        else -> R.string.preset_utilities
    }

private fun formatBytes(bytes: Long): String {
    val megabytes = bytes / (1024.0 * 1024.0)
    return if (megabytes >= 1024) {
        String.format(Locale.getDefault(), "%.1f GB", megabytes / 1024.0)
    } else {
        String.format(Locale.getDefault(), "%.0f MB", megabytes)
    }
}

private val SUPPORTED_ARCHES = listOf("arm64-v8a", "x86_64")
private const val ROOTFS_ASSET = "bundled/rootfs.bin"
private const val CODE_SERVER_ASSET = "bundled/code-server.bin"
