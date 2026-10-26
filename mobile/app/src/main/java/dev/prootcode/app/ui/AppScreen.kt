package dev.prootcode.app.ui

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import dev.prootcode.app.BuildConfig
import dev.prootcode.app.R
import dev.prootcode.app.data.catalog.Catalog
import dev.prootcode.app.data.catalog.CatalogDefaults
import dev.prootcode.app.data.catalog.CodeServerArtifact
import dev.prootcode.app.data.catalog.DistroArtifact
import dev.prootcode.app.data.environment.ArtifactStore
import dev.prootcode.app.data.environment.Environment
import dev.prootcode.app.data.environment.EnvironmentInstaller
import dev.prootcode.app.data.environment.EnvironmentRegistry
import dev.prootcode.app.data.environment.InstallRequest
import dev.prootcode.app.data.environment.InstallStage
import dev.prootcode.app.runtime.network.DnsResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID

@Composable
fun AppScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val environments = remember { mutableStateListOf<Environment>() }
    var catalog by remember { mutableStateOf<Catalog?>(null) }
    var showWizard by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var stage by remember { mutableStateOf<InstallStage?>(null) }
    var progress by remember { mutableStateOf<Pair<Long, Long>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val arch =
        remember {
            Build.SUPPORTED_ABIS.firstOrNull { it in SUPPORTED_ARCHES } ?: SUPPORTED_ARCHES.first()
        }
    val registry = remember { EnvironmentRegistry(File(context.filesDir, "environments.json")) }
    val installer = remember {
        EnvironmentInstaller(File(context.filesDir, "environments"), registry)
    }
    val store = remember { ArtifactStore(File(context.cacheDir, "artifacts"), context.assets) }
    val repository = remember { CatalogDefaults.create(context.assets, context.cacheDir) }

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

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { refreshEnvironments() }
        loadCatalog(refresh = false)
        showWizard = environments.isEmpty()
    }

    fun install(
        name: String,
        distro: DistroArtifact,
        codeServer: CodeServerArtifact?,
    ) {
        busy = true
        error = null
        progress = null
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
                    )
                val rootfs =
                    store.open(
                        name = "rootfs-${distro.arch}.bin",
                        url = distro.url,
                        sha256 = distro.sha256,
                        size = distro.size,
                        bundledAsset = bundledAsset(BuildConfig.BUNDLED_ARTIFACTS, ROOTFS_ASSET),
                    ) { received, total -> progress = received to total }
                rootfs.use { rootfsStream ->
                    val codeServerStream =
                        codeServer?.let { artifact ->
                            store.open(
                                name = "code-server-${artifact.version}-${artifact.arch}.tar.gz",
                                url = artifact.url,
                                sha256 = artifact.sha256,
                                size = artifact.size,
                                bundledAsset =
                                    bundledAsset(BuildConfig.BUNDLED_ARTIFACTS, CODE_SERVER_ASSET),
                            ) { received, total -> progress = received to total }
                        }
                    codeServerStream.use { codeServerStreamOrNull ->
                        installer.install(request, rootfsStream, codeServerStreamOrNull) { stage = it }
                    }
                }
                withContext(Dispatchers.Main) {
                    refreshEnvironments()
                    showWizard = false
                }
            } catch (failure: Throwable) {
                error = failure.message ?: failure.javaClass.simpleName
            } finally {
                busy = false
                progress = null
                stage = null
            }
        }
    }

    fun delete(environment: Environment) {
        scope.launch(Dispatchers.IO) {
            installer.delete(environment.id)
            withContext(Dispatchers.Main) { refreshEnvironments() }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.environments_storage, formatBytes(registry.totalBytes())),
            style = MaterialTheme.typography.bodySmall,
        )

        error?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (busy) {
            BusyCard(
                stage = stage,
                progress = progress,
            )
        } else if (showWizard || environments.isEmpty()) {
            Wizard(
                catalog = catalog,
                arch = arch,
                existingNames = environments.map { it.name },
                enabled = catalog != null,
                onRefresh = {
                    loadCatalog(refresh = true)
                },
                onInstall = { name, distro, codeServer -> install(name, distro, codeServer) },
                onCancel = if (environments.isEmpty()) null else ({ showWizard = false }),
            )
        } else {
            environments.forEach { environment ->
                EnvironmentCard(
                    environment = environment,
                    onDelete = { delete(environment) },
                )
            }
            Button(
                onClick = { showWizard = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.environments_new))
            }
            TextButton(
                onClick = { loadCatalog(refresh = true) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.catalog_refresh))
            }
        }
    }
}

@Composable
private fun BusyCard(
    stage: InstallStage?,
    progress: Pair<Long, Long>?,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val label =
                when (stage) {
                    InstallStage.EXTRACT_ROOTFS -> stringResource(R.string.install_stage_rootfs)
                    InstallStage.INSTALL_CODE_SERVER -> stringResource(R.string.install_stage_code_server)
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
    onInstall: (String, DistroArtifact, CodeServerArtifact?) -> Unit,
    onCancel: (() -> Unit)?,
) {
    val distros = remember(catalog, arch) { catalog?.distrosFor(arch).orEmpty() }
    val codeServers = remember(catalog, arch) { catalog?.codeServerFor(arch).orEmpty() }
    var name by remember { mutableStateOf("") }
    var distroIndex by remember(distros) { mutableStateOf(0) }
    var codeServerIndex by remember(codeServers) { mutableStateOf(if (codeServers.isEmpty()) -1 else 0) }
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

            Text(text = stringResource(R.string.wizard_distro), style = MaterialTheme.typography.titleSmall)
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
                        onInstall(name.trim(), distro, codeServers.getOrNull(codeServerIndex))
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

@Composable
private fun EnvironmentCard(
    environment: Environment,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = environment.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text =
                    stringResource(
                        R.string.environments_details,
                        environment.distroVersion,
                        environment.arch,
                        environment.codeServerVersion ?: stringResource(R.string.wizard_none),
                        formatBytes(environment.sizeBytes),
                    ),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.environments_delete))
            }
        }
    }
}

private fun bundledAsset(
    enabled: Boolean,
    asset: String,
): String? = if (enabled) asset else null

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
