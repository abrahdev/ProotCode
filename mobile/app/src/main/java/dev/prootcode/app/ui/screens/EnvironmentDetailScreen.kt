package dev.prootcode.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.prootcode.app.R
import dev.prootcode.app.data.catalog.CodeServerArtifact
import dev.prootcode.app.ui.components.AppScaffold
import dev.prootcode.app.ui.components.SettingRow
import dev.prootcode.app.ui.components.SettingsGroup
import dev.prootcode.app.ui.components.SheetScaffold
import dev.prootcode.app.ui.settings.AdbSetupSheet
import dev.prootcode.app.ui.viewmodels.EnvironmentDetailViewModel
import dev.prootcode.app.ui.viewmodels.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvironmentDetailScreen(
    environmentId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val detailViewModel: EnvironmentDetailViewModel =
        viewModel { EnvironmentDetailViewModel(context.applicationContext, environmentId) }
    val homeViewModel: HomeViewModel =
        viewModel { HomeViewModel(context.applicationContext) }
    val environment by detailViewModel.environment.collectAsState()
    val loaded by detailViewModel.loaded.collectAsState()
    val homeState by homeViewModel.uiState.collectAsState()

    var configKey by remember { mutableStateOf<ConfigKey?>(null) }
    var showTools by remember { mutableStateOf(false) }
    var showAdb by remember { mutableStateOf(false) }
    var showBackups by remember { mutableStateOf(false) }
    var cacheRefresh by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val cacheCleared = stringResource(R.string.config_cache_cleared)
    val messageText =
        homeState.messageText
            ?: homeState.messageRes?.let { res ->
                homeState.messageArg?.let { arg -> stringResource(res, arg) }
                    ?: stringResource(res)
            }

    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            homeViewModel.clearMessage()
        }
    }

    BackHandler(onBack = onBack)

    if (!loaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val current = environment
    if (current == null) {
        LaunchedEffect(environmentId) { onBack() }
        return
    }

    val rootfs = remember(current.id) { homeViewModel.rootfsDir(current.id) }
    val cacheBytes by
        produceState(initialValue = 0L, current.id, cacheRefresh) {
            value =
                withContext(Dispatchers.IO) {
                    directorySize(File(rootfs, "var/cache")) + directorySize(File(rootfs, "tmp"))
                }
        }

    AppScaffold(
        title = current.name,
        subtitle = stringResource(R.string.status_ready),
        onBack = onBack,
        actions = {
            IconButton(onClick = { showTools = true }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = stringResource(R.string.home_more),
                )
            }
        },
        snackbarHostState = snackbarHostState,
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                SettingsGroup(title = stringResource(R.string.settings_general)) {
                    ConfigRow(ConfigKey.NAME, R.string.config_machine_name_sub) { configKey = it }
                    ConfigRow(ConfigKey.USER, R.string.config_user_sub) { configKey = it }
                    ConfigRow(ConfigKey.ICON, R.string.config_icon_sub) { configKey = it }
                    ConfigRow(ConfigKey.AUTOSTART, R.string.config_autostart_sub) { configKey = it }
                    ConfigRow(ConfigKey.LANGUAGE, R.string.config_language_sub) { configKey = it }
                    ConfigRow(
                        ConfigKey.TIMEZONE,
                        R.string.config_timezone_sub,
                        showDivider = false,
                    ) { configKey = it }
                }
            }
            item {
                SettingsGroup(title = stringResource(R.string.settings_storage)) {
                    ConfigRow(ConfigKey.MOUNTS, R.string.config_mounts_sub) { configKey = it }
                    ConfigRow(ConfigKey.CACHE, R.string.config_cache_sub) { configKey = it }
                    ConfigRow(
                        ConfigKey.AUTOCLEANUP,
                        R.string.config_autocleanup_sub,
                        showDivider = false,
                    ) { configKey = it }
                }
            }
            item {
                SettingsGroup(title = stringResource(R.string.settings_network)) {
                    ConfigRow(ConfigKey.PORT, R.string.config_port_sub) { configKey = it }
                    ConfigRow(
                        ConfigKey.NETWORK_SHARING,
                        R.string.config_netshare_sub,
                    ) { configKey = it }
                    ConfigRow(
                        ConfigKey.PROXY,
                        R.string.config_proxy_sub,
                        showDivider = false,
                    ) { configKey = it }
                }
            }
            item {
                SettingsGroup(title = stringResource(R.string.settings_access)) {
                    ConfigRow(ConfigKey.ADB, R.string.config_adb_sub) { configKey = it }
                    ConfigRow(
                        ConfigKey.SSH,
                        R.string.config_ssh_sub,
                        showDivider = false,
                    ) { configKey = it }
                }
            }
            item {
                SettingsGroup(title = stringResource(R.string.settings_performance)) {
                    ConfigRow(ConfigKey.BACKGROUND, R.string.config_bg_sub) { configKey = it }
                    ConfigRow(
                        ConfigKey.PRIORITY,
                        R.string.config_priority_sub,
                        showDivider = false,
                    ) { configKey = it }
                }
            }
            item {
                SettingsGroup(title = stringResource(R.string.settings_advanced)) {
                    ConfigRow(ConfigKey.DEBUG, R.string.config_debug_sub) { configKey = it }
                    ConfigRow(ConfigKey.LOGS, R.string.config_logs_sub) { configKey = it }
                    ConfigRow(
                        ConfigKey.BACKUP,
                        R.string.config_backup_sub,
                        showDivider = false,
                    ) { configKey = it }
                }
            }
        }
    }

    configKey?.let { key ->
        EnvironmentConfigSheet(
            environment = current,
            configKey = key,
            cacheBytes = cacheBytes,
            foldersRoot = homeViewModel.projectsDir.parentFile ?: homeViewModel.projectsDir,
            onUpdate = { detailViewModel.update(it) },
            onOpenAdb = { showAdb = true },
            onOpenBackups = { showBackups = true },
            onClearCache = {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        File(rootfs, "var/cache").deleteRecursively()
                        File(rootfs, "tmp").deleteRecursively()
                        File(rootfs, "var/cache").mkdirs()
                        File(rootfs, "tmp").mkdirs()
                    }
                    cacheRefresh += 1
                    homeViewModel.showText(cacheCleared)
                }
            },
            onTestBridge = { homeViewModel.testBridge(current) },
            onDelete = {
                homeViewModel.delete(current)
                onBack()
            },
            onMessage = { homeViewModel.showText(it) },
            onDismiss = { configKey = null },
        )
    }

    if (showTools) {
        EnvironmentToolsSheet(
            environmentName = current.name,
            codeServers =
                homeState.catalog
                    ?.codeServerFor(current.arch)
                    .orEmpty()
                    .filter { it.version != current.codeServerVersion },
            onTestBridge = {
                homeViewModel.testBridge(current)
                showTools = false
            },
            onChangeCodeServer = { artifact ->
                homeViewModel.changeCodeServer(current, artifact)
                showTools = false
            },
            onDelete = {
                homeViewModel.delete(current)
                showTools = false
                onBack()
            },
            onDismiss = { showTools = false },
        )
    }

    if (showAdb) {
        AdbSetupSheet(
            connected = current.settings.adbShare,
            onConnectedChange = { connected ->
                detailViewModel.update(
                    current.copy(settings = current.settings.copy(adbShare = connected)),
                )
            },
            onDismiss = { showAdb = false },
            onMessage = { homeViewModel.showText(it) },
        )
    }

    if (showBackups) {
        BackupsSheet(
            onMessage = { homeViewModel.showText(it) },
            onDismiss = { showBackups = false },
        )
    }
}

@Composable
private fun ConfigRow(
    key: ConfigKey,
    subtitleRes: Int,
    showDivider: Boolean = true,
    onOpen: (ConfigKey) -> Unit,
) {
    SettingRow(
        title = configTitle(key),
        subtitle = stringResource(subtitleRes),
        onClick = { onOpen(key) },
        showDivider = showDivider,
    )
}

@Composable
private fun EnvironmentToolsSheet(
    environmentName: String,
    codeServers: List<CodeServerArtifact>,
    onTestBridge: () -> Unit,
    onChangeCodeServer: (CodeServerArtifact) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    SheetScaffold(
        title = environmentName,
        onDismiss = onDismiss,
        scrollable = false,
    ) {
        SettingsGroup(title = null) {
            SettingRow(
                title = stringResource(R.string.bridge_test),
                onClick = onTestBridge,
                trailing = {
                    Icon(
                        imageVector = Icons.Outlined.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            codeServers.forEach { artifact ->
                SettingRow(
                    title = stringResource(R.string.code_server_change, artifact.version),
                    onClick = { onChangeCodeServer(artifact) },
                    trailing = {
                        Icon(
                            imageVector = Icons.Outlined.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
            SettingRow(
                title = stringResource(R.string.environments_delete),
                onClick = onDelete,
                showDivider = false,
                trailing = {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
            )
        }
    }
}

private fun directorySize(directory: File): Long =
    if (!directory.exists()) {
        0L
    } else {
        directory.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
