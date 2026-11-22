package dev.prootcode.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.prootcode.app.R
import dev.prootcode.app.data.environment.Environment
import dev.prootcode.app.data.preferences.AppPreferences
import dev.prootcode.app.ui.components.EmptyState
import dev.prootcode.app.ui.components.EnvCard
import dev.prootcode.app.ui.components.InstallingSkeleton
import dev.prootcode.app.ui.components.SessionLevel
import dev.prootcode.app.ui.components.SessionPreview
import dev.prootcode.app.ui.components.SessionRow
import dev.prootcode.app.ui.components.formatFileSize
import dev.prootcode.app.ui.settings.AccessibilitySheet
import dev.prootcode.app.ui.settings.AdbSetupSheet
import dev.prootcode.app.ui.settings.PreferencesSheet
import dev.prootcode.app.ui.settings.ThemeSheet
import dev.prootcode.app.ui.terminal.TerminalActivity
import dev.prootcode.app.ui.terminal.TerminalSessionManager
import dev.prootcode.app.ui.theme.terminalPalette
import dev.prootcode.app.ui.util.QuestWindow
import dev.prootcode.app.ui.viewmodels.HomeViewModel
import dev.prootcode.app.ui.viewmodels.WizardViewModel
import dev.prootcode.app.ui.wizard.NewEnvironmentSheet
import kotlinx.coroutines.delay

private sealed interface HomeSheet {
    data object Preferences : HomeSheet

    data object Theme : HomeSheet

    data object Accessibility : HomeSheet

    data object Adb : HomeSheet

    data object NewEnvironment : HomeSheet

    data class Overview(
        val environmentId: String,
    ) : HomeSheet

    data class Files(
        val environmentId: String,
    ) : HomeSheet
}

@Composable
fun HomeScreen(
    preferences: AppPreferences,
    onOpenEnvironmentDetail: (Environment) -> Unit,
    onOpenTerminal: (Environment) -> Unit,
    onOpenNativeTerminal: () -> Unit,
    onOpenCodeServer: (Environment) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val homeViewModel: HomeViewModel = viewModel { HomeViewModel(context.applicationContext) }
    val wizardViewModel: WizardViewModel = viewModel { WizardViewModel() }
    val state by homeViewModel.uiState.collectAsState()
    val themeSettings by preferences.theme.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val selectedPreviews = remember { mutableStateMapOf<String, Int>() }
    var sheet by remember { mutableStateOf<HomeSheet?>(null) }
    var previewTick by remember { mutableIntStateOf(0) }
    var overviewTick by remember { mutableIntStateOf(0) }
    val stoppedText = stringResource(R.string.overview_stopped)
    val filesPathLabel = stringResource(R.string.files_path_label)
    val fileOpenedText = stringResource(R.string.files_opened_in_vscode)
    val messageText =
        state.messageText
            ?: state.messageRes?.let { res ->
                state.messageArg?.let { arg -> stringResource(res, arg) }
                    ?: stringResource(res)
            }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2_000)
            previewTick += 1
        }
    }

    LaunchedEffect(sheet is HomeSheet.Overview) {
        if (sheet is HomeSheet.Overview) {
            while (true) {
                delay(1_000)
                overviewTick += 1
            }
        }
    }

    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            homeViewModel.clearMessage()
        }
    }

    fun moveEnvironment(
        id: String,
        delta: Int,
    ) {
        val ids = state.environments.map { it.id }.toMutableList()
        val index = ids.indexOf(id)
        val target = index + delta
        if (index < 0 || target !in ids.indices) return
        ids.removeAt(index)
        ids.add(target, id)
        homeViewModel.reorder(ids)
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!state.reorderMode) {
                ExtendedFloatingActionButton(
                    onClick = {
                        wizardViewModel.reset()
                        sheet = HomeSheet.NewEnvironment
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                        )
                    },
                    text = { Text(stringResource(R.string.home_new_environment)) },
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.reorderMode) {
                        TextButton(onClick = { homeViewModel.setReorderMode(false) }) {
                            Text(stringResource(R.string.action_done))
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onOpenNativeTerminal) {
                        Icon(
                            imageVector = Icons.Outlined.Terminal,
                            contentDescription = stringResource(R.string.home_terminal_native),
                        )
                    }
                    IconButton(onClick = { sheet = HomeSheet.Adb }) {
                        Icon(
                            imageVector = Icons.Outlined.DeveloperBoard,
                            contentDescription =
                                stringResource(
                                    if (state.adbConnected) {
                                        R.string.home_adb_connected
                                    } else {
                                        R.string.home_adb_setup
                                    },
                                ),
                            tint =
                                if (state.adbConnected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                    IconButton(onClick = { sheet = HomeSheet.Preferences }) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.sheet_preferences_title),
                        )
                    }
                }
            }

            if (state.busy && sheet != HomeSheet.NewEnvironment) {
                item {
                    InstallingSkeleton(stage = state.stage, progress = state.progress)
                }
            }

            state.error?.let { message ->
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { homeViewModel.clearError() }) {
                            Text(stringResource(R.string.action_close))
                        }
                    }
                }
            }

            if (state.environments.isEmpty()) {
                item {
                    EmptyState(
                        title = stringResource(R.string.home_empty_title),
                        message = stringResource(R.string.home_empty_body),
                        action = {
                            Button(
                                onClick = {
                                    wizardViewModel.reset()
                                    sheet = HomeSheet.NewEnvironment
                                },
                            ) {
                                Text(stringResource(R.string.home_new_environment))
                            }
                        },
                    )
                }
            } else {
                items(state.environments, key = { it.id }) { environment ->
                    val sessions =
                        remember(environment.id, previewTick) {
                            TerminalSessionManager.sessions(environment.id)
                        }
                    val previews =
                        sessions.mapIndexed { index, session ->
                            SessionPreview(
                                title = session.mSessionName ?: "Terminal ${index + 1}",
                                lines = TerminalSessionManager.previewLines(session),
                            )
                        }
                    val selected = selectedPreviews[environment.id] ?: if (previews.isEmpty()) -1 else 0
                    val palette = terminalPalette(themeSettings.customHue)
                    val markIcon =
                        dev.prootcode.app.ui.components.ProotIcons
                            .resolve(environment.settings.icon)
                    EnvCard(
                        name = environment.name,
                        status =
                            stringResource(
                                R.string.home_ready_storage,
                                formatFileSize(environment.sizeBytes),
                            ),
                        markText = environment.name,
                        markIcon = markIcon,
                        installing = false,
                        sessions = previews,
                        selectedSession = selected,
                        previewBackground = palette.background,
                        previewForeground = palette.foreground,
                        codeServerInstalled = environment.codeServerVersion != null,
                        showButtonLabels = themeSettings.showTerminalButtonLabels,
                        reorderMode = state.reorderMode,
                        canMoveUp = state.environments.firstOrNull()?.id != environment.id,
                        canMoveDown = state.environments.lastOrNull()?.id != environment.id,
                        onMoveUp = { moveEnvironment(environment.id, -1) },
                        onMoveDown = { moveEnvironment(environment.id, 1) },
                        onSelectSession = { index -> selectedPreviews[environment.id] = index },
                        onOpenDetail = { onOpenEnvironmentDetail(environment) },
                        onLongPress = { homeViewModel.setReorderMode(true) },
                        onOpenOverview = { sheet = HomeSheet.Overview(environment.id) },
                        onOpenTerminal = { onOpenTerminal(environment) },
                        onOpenWindow = {
                            val intent =
                                Intent(context, TerminalActivity::class.java)
                                    .putExtra(TerminalActivity.EXTRA_ENVIRONMENT_ID, environment.id)
                                    .addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK,
                                    )
                            QuestWindow.start(context, intent)
                        },
                        onOpenFiles = { sheet = HomeSheet.Files(environment.id) },
                        onOpenCodeServer = { onOpenCodeServer(environment) },
                    )
                }
                item {
                    Text(
                        text =
                            stringResource(
                                R.string.environments_storage,
                                formatFileSize(state.totalBytes),
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    when (val current = sheet) {
        null -> Unit
        HomeSheet.Preferences ->
            PreferencesSheet(
                preferences = preferences,
                onDismiss = { sheet = null },
                onTheme = { sheet = HomeSheet.Theme },
                onAccessibility = { sheet = HomeSheet.Accessibility },
            )
        HomeSheet.Theme ->
            ThemeSheet(
                preferences = preferences,
                onDismiss = { sheet = HomeSheet.Preferences },
            )
        HomeSheet.Accessibility ->
            AccessibilitySheet(
                preferences = preferences,
                onDismiss = { sheet = HomeSheet.Preferences },
            )
        HomeSheet.Adb ->
            AdbSetupSheet(
                connected = state.adbConnected,
                onConnectedChange = homeViewModel::setAdbConnected,
                onDismiss = { sheet = null },
                onMessage = homeViewModel::showText,
            )
        HomeSheet.NewEnvironment ->
            NewEnvironmentSheet(
                viewModel = wizardViewModel,
                catalog = state.catalog,
                arch = homeViewModel.arch,
                existingNames = state.environments.map { it.name },
                busy = state.busy,
                stage = state.stage,
                progress = state.progress,
                error = state.error,
                onInstall = { input ->
                    homeViewModel.install(input) {
                        sheet = null
                        wizardViewModel.reset()
                    }
                },
                onCancelInstall = { homeViewModel.cancelInstall() },
                onRefreshCatalog = { homeViewModel.loadCatalog(refresh = true) },
                onOpenAdb = { sheet = HomeSheet.Adb },
                onDismiss = {
                    sheet = null
                    if (!state.busy) wizardViewModel.reset()
                },
            )
        is HomeSheet.Overview -> {
            val environment = state.environments.firstOrNull { it.id == current.environmentId }
            if (environment == null) {
                sheet = null
            } else {
                val sessions =
                    remember(environment.id, previewTick) {
                        TerminalSessionManager.sessions(environment.id)
                    }
                val uptimeMillis =
                    remember(environment.id, overviewTick) {
                        TerminalSessionManager.uptimeMillis(environment.id)
                    }
                val ramBytes =
                    remember(environment.id, overviewTick) {
                        TerminalSessionManager.ramBytes(environment.id)
                    }
                EnvironmentOverviewSheet(
                    name = environment.name,
                    status = stringResource(R.string.status_ready),
                    uptime = formatUptime(uptimeMillis),
                    ram = formatFileSize(ramBytes),
                    storage = formatFileSize(environment.sizeBytes),
                    sessions =
                        sessions.mapIndexed { index, session ->
                            SessionRow(
                                name = session.mSessionName ?: "Terminal ${index + 1}",
                                status =
                                    if (index == 0) {
                                        stringResource(R.string.overview_foreground)
                                    } else {
                                        stringResource(R.string.overview_background)
                                    },
                                level = SessionLevel.IDLE,
                            )
                        },
                    onCloseSession = { index ->
                        sessions.getOrNull(index)?.let { session ->
                            TerminalSessionManager.close(context, environment.id, session)
                            previewTick += 1
                        }
                    },
                    onStopAll = {
                        TerminalSessionManager.closeAll(context, environment.id)
                        homeViewModel.showText(stoppedText)
                        sheet = null
                    },
                    onDismiss = { sheet = null },
                )
            }
        }
        is HomeSheet.Files -> {
            val environment = state.environments.firstOrNull { it.id == current.environmentId }
            FileSystemSheet(
                rootfs = homeViewModel.rootfsDir(current.environmentId),
                onOpenFile = { entry ->
                    if (environment != null) {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard?.setPrimaryClip(
                            ClipData.newPlainText(filesPathLabel, entry.path),
                        )
                        onOpenCodeServer(environment)
                        homeViewModel.showText(fileOpenedText)
                    }
                },
                onMessage = homeViewModel::showText,
                onDismiss = { sheet = null },
            )
        }
    }
}

@Composable
private fun formatUptime(millis: Long): String {
    val seconds = millis / 1_000
    return when {
        seconds >= 3_600 ->
            stringResource(R.string.overview_uptime_hm, seconds / 3_600, (seconds % 3_600) / 60)
        seconds >= 60 -> stringResource(R.string.overview_uptime_ms, seconds / 60, seconds % 60)
        else -> stringResource(R.string.overview_uptime_s, seconds)
    }
}
