package dev.prootcode.app.ui.wizard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.prootcode.app.R
import dev.prootcode.app.data.catalog.Catalog
import dev.prootcode.app.data.catalog.CodeServerArtifact
import dev.prootcode.app.data.catalog.DistroArtifact
import dev.prootcode.app.data.environment.EnvironmentSettings
import dev.prootcode.app.data.environment.InstallStage
import dev.prootcode.app.data.environment.Presets
import dev.prootcode.app.ui.components.DistroOptionCard
import dev.prootcode.app.ui.components.FolderPickerDialog
import dev.prootcode.app.ui.components.IconSelectionSection
import dev.prootcode.app.ui.components.LabeledTextField
import dev.prootcode.app.ui.components.PlaceholderBlock
import dev.prootcode.app.ui.components.ProgressSpinner
import dev.prootcode.app.ui.components.SettingsGroup
import dev.prootcode.app.ui.components.SheetScaffold
import dev.prootcode.app.ui.components.StepIndicator
import dev.prootcode.app.ui.components.StepState
import dev.prootcode.app.ui.components.SwitchRow
import dev.prootcode.app.ui.components.ToolRow
import dev.prootcode.app.ui.viewmodels.EnvironmentInstallInput
import dev.prootcode.app.ui.viewmodels.WizardViewModel
import java.io.File

private data class DistroGroup(
    val name: String,
    val artifacts: List<DistroArtifact>,
)

private data class ToolDefinition(
    val id: String,
    val titleRes: Int,
    val subtitleRes: Int,
    val group: String,
    val readyBadge: Boolean = false,
)

private val TOOL_DEFINITIONS =
    listOf(
        ToolDefinition("git", R.string.tool_git, R.string.tool_git_sub, "essentials"),
        ToolDefinition(
            "build-essential",
            R.string.tool_build_essential,
            R.string.tool_build_essential_sub,
            "essentials",
        ),
        ToolDefinition("curl", R.string.tool_curl, R.string.tool_curl_sub, "essentials"),
        ToolDefinition(
            "code-server",
            R.string.tool_code_server,
            R.string.tool_code_server_sub,
            "code",
            readyBadge = true,
        ),
        ToolDefinition(
            "opencode",
            R.string.tool_opencode,
            R.string.tool_opencode_sub,
            "code",
            readyBadge = true,
        ),
    )

@Composable
fun NewEnvironmentSheet(
    viewModel: WizardViewModel,
    catalog: Catalog?,
    arch: String,
    existingNames: List<String>,
    busy: Boolean,
    stage: InstallStage?,
    progress: Pair<Long, Long>?,
    error: String?,
    foldersRoot: File,
    onInstall: (EnvironmentInstallInput) -> Unit,
    onCancelInstall: () -> Unit,
    onRefreshCatalog: () -> Unit,
    onOpenAdb: () -> Unit,
    onDismiss: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val groups =
        remember(catalog, arch) {
            catalog
                ?.distrosFor(arch)
                ?.groupBy { it.name }
                ?.map { (name, artifacts) -> DistroGroup(name, artifacts) }
                .orEmpty()
        }
    val group = groups.getOrNull(state.distroIndex)
    val distro = group?.artifacts?.getOrNull(state.versionIndex) ?: group?.artifacts?.firstOrNull()
    val codeServer: CodeServerArtifact? =
        if (state.tools["code-server"] == true) {
            catalog?.codeServerFor(arch)?.firstOrNull()
        } else {
            null
        }
    val duplicate = existingNames.any { it.equals(state.name.trim(), ignoreCase = true) }
    val steps =
        listOf(
            stringResource(R.string.wizard_step_distro),
            stringResource(R.string.wizard_step_tools),
            stringResource(R.string.wizard_step_mounts),
            stringResource(R.string.wizard_step_config),
            stringResource(R.string.wizard_step_install),
        )
    var showFolderPicker by remember { mutableStateOf(false) }

    LaunchedEffect(state.step, catalog) {
        if (state.step == 3 && state.name.isBlank() && distro != null) {
            viewModel.setName("${distro.name} ${distro.version}")
        }
    }

    fun presets(): List<String> =
        buildList {
            val essentials = listOf("git", "build-essential", "curl").any { state.tools[it] == true }
            if (essentials) add(Presets.ESSENTIALS)
            if (state.tools["opencode"] == true) add(Presets.EDITORS)
        }

    fun buildInput(): EnvironmentInstallInput? {
        val selected = distro ?: return null
        return EnvironmentInstallInput(
            name = state.name.trim().ifBlank { "${selected.name} ${selected.version}" },
            distro = selected,
            codeServer = codeServer,
            presets = presets(),
            settings =
                EnvironmentSettings(
                    icon = state.icon,
                    mountDownloads = state.mountDownloads,
                    extraMounts = state.extraMounts,
                    adbShare = state.adbShare,
                    customUser = state.customUser,
                    userName = state.userName,
                ),
        )
    }

    val canInstall = catalog != null && distro != null && !duplicate && state.name.isNotBlank()

    SheetScaffold(
        title =
            stringResource(
                if (state.step == STEP_INSTALL) R.string.wizard_installing_title else R.string.wizard_title,
            ),
        onDismiss = onDismiss,
        footer = {
            TextButton(
                onClick = {
                    when {
                        state.step == STEP_INSTALL -> onDismiss()
                        state.step == 0 -> onDismiss()
                        else -> viewModel.back()
                    }
                },
            ) {
                Text(
                    stringResource(
                        when {
                            state.step == STEP_INSTALL -> R.string.wizard_hide
                            state.step == 0 -> R.string.action_cancel
                            else -> R.string.action_back
                        },
                    ),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (state.step == STEP_INSTALL) {
                Button(
                    enabled = !busy,
                    onClick = {
                        if (!busy) onDismiss()
                    },
                ) {
                    Text(stringResource(R.string.action_done))
                }
            } else {
                Button(
                    enabled =
                        when (state.step) {
                            STEP_CONFIG -> canInstall
                            else -> catalog != null && distro != null
                        },
                    onClick = {
                        if (state.step == STEP_CONFIG) {
                            val input = buildInput() ?: return@Button
                            onInstall(input)
                        }
                        viewModel.next()
                    },
                ) {
                    Text(
                        stringResource(
                            if (state.step == STEP_CONFIG) R.string.action_install else R.string.action_next,
                        ),
                    )
                }
            }
        },
    ) {
        StepIndicator(steps = steps, current = state.step)

        when (state.step) {
            STEP_DISTRO -> {
                Text(
                    text = stringResource(R.string.wizard_choose_distro),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.wizard_choose_distro_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (catalog == null) {
                    Text(
                        text = stringResource(R.string.wizard_no_distros),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedButton(onClick = onRefreshCatalog) {
                        Text(stringResource(R.string.catalog_refresh))
                    }
                } else {
                    groups.forEachIndexed { index, distroGroup ->
                        DistroOptionCard(
                            name = distroGroup.name,
                            subtitle =
                                stringResource(
                                    if (distroGroup.name.contains("Ubuntu")) {
                                        R.string.distro_ubuntu_sub
                                    } else {
                                        R.string.distro_debian_sub
                                    },
                                ),
                            selected = index == state.distroIndex,
                            onSelect = { viewModel.selectDistro(index) },
                            versions = distroGroup.artifacts.map { it.version },
                            selectedVersion =
                                if (index == state.distroIndex) state.versionIndex else 0,
                            onSelectVersion = { versionIndex ->
                                viewModel.selectDistro(index)
                                viewModel.selectVersion(versionIndex)
                            },
                            versionsCollapsible = distroGroup.artifacts.size > 5,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.wizard_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            STEP_TOOLS -> {
                Text(
                    text = stringResource(R.string.wizard_choose_tools),
                    style = MaterialTheme.typography.titleMedium,
                )
                val selectedCount = state.tools.count { it.value }
                Text(
                    text = stringResource(R.string.wizard_tools_count, selectedCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ToolGroup(
                    title = stringResource(R.string.wizard_group_essentials),
                    tools = TOOL_DEFINITIONS.filter { it.group == "essentials" },
                    state = state.tools,
                    onToggle = viewModel::toggleTool,
                    onSelectAll = { select ->
                        TOOL_DEFINITIONS.filter { it.group == "essentials" }.forEach { tool ->
                            if ((state.tools[tool.id] ?: false) != select) viewModel.toggleTool(tool.id)
                        }
                    },
                )
                ToolGroup(
                    title = stringResource(R.string.wizard_group_code),
                    tools = TOOL_DEFINITIONS.filter { it.group == "code" },
                    state = state.tools,
                    onToggle = viewModel::toggleTool,
                    onSelectAll = { select ->
                        TOOL_DEFINITIONS.filter { it.group == "code" }.forEach { tool ->
                            if ((state.tools[tool.id] ?: false) != select) viewModel.toggleTool(tool.id)
                        }
                    },
                )
                Text(
                    text = stringResource(R.string.wizard_tools_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PlaceholderBlock(text = stringResource(R.string.wizard_more_placeholder))
            }

            STEP_MOUNTS -> {
                Text(
                    text = stringResource(R.string.wizard_step_mounts),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.wizard_mounts_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SettingsGroup(title = stringResource(R.string.config_shared_folders)) {
                    SwitchRow(
                        title = stringResource(R.string.config_downloads),
                        subtitle = stringResource(R.string.wizard_mounted_default),
                        checked = state.mountDownloads,
                        onCheckedChange = viewModel::setMountDownloads,
                        showDivider = state.extraMounts.isNotEmpty(),
                    )
                    state.extraMounts.forEachIndexed { index, path ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = path.substringAfterLast('/'),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = path,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(
                                onClick = { viewModel.removeMount(path) },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.action_remove),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        if (index != state.extraMounts.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
                OutlinedButton(onClick = { showFolderPicker = true }) {
                    Text(stringResource(R.string.config_add_folder))
                }
            }

            STEP_CONFIG -> {
                Text(
                    text = stringResource(R.string.wizard_step_config),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.wizard_config_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LabeledTextField(
                    label = stringResource(R.string.wizard_name),
                    value = state.name,
                    onValueChange = viewModel::setName,
                    isError = duplicate,
                    supportingText =
                        if (duplicate) {
                            stringResource(R.string.wizard_name_taken)
                        } else {
                            null
                        },
                    maxLength = 40,
                )
                Text(
                    text = stringResource(R.string.wizard_icon_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.wizard_icon_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconSelectionSection(
                    selectedId = state.icon,
                    onSelect = viewModel::setIcon,
                )
                SettingsGroup(title = stringResource(R.string.wizard_access_title)) {
                    SwitchRow(
                        title = stringResource(R.string.wizard_share_adb),
                        subtitle = stringResource(R.string.wizard_share_adb_sub),
                        checked = state.adbShare,
                        onCheckedChange = viewModel::setAdbShare,
                        showDivider = true,
                    )
                    SwitchRow(
                        title = stringResource(R.string.wizard_custom_user),
                        subtitle = stringResource(R.string.wizard_custom_user_sub),
                        checked = state.customUser,
                        onCheckedChange = viewModel::setCustomUser,
                    )
                }
                if (state.adbShare) {
                    OutlinedButton(onClick = onOpenAdb) {
                        Text(stringResource(R.string.wizard_setup_adb))
                    }
                }
                if (state.customUser) {
                    LabeledTextField(
                        label = stringResource(R.string.wizard_user_name),
                        value = state.userName,
                        onValueChange = viewModel::setUserName,
                    )
                }
            }

            STEP_INSTALL -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ProgressSpinner(done = false)
                    Text(
                        text = stringResource(R.string.wizard_installing_title),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text =
                            when (stage) {
                                InstallStage.EXTRACT_ROOTFS -> stringResource(R.string.install_stage_rootfs)
                                InstallStage.INSTALL_CODE_SERVER ->
                                    stringResource(R.string.install_stage_code_server)
                                InstallStage.READY -> stringResource(R.string.install_stage_ready)
                                null ->
                                    if (progress != null) {
                                        stringResource(R.string.install_downloading)
                                    } else {
                                        stringResource(R.string.wizard_preparing)
                                    }
                            },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    val current = progress
                    if (current != null && current.second > 0) {
                        Text(
                            text = "${current.first * 100 / current.second}%",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    if (error != null) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onCancelInstall() }) {
                                Text(stringResource(R.string.install_cancel))
                            }
                        }
                    }
                }
                val stepStates =
                    listOf(
                        when {
                            stage != null -> StepState.DONE
                            busy -> StepState.ACTIVE
                            else -> StepState.PENDING
                        },
                        when (stage) {
                            InstallStage.EXTRACT_ROOTFS -> StepState.ACTIVE
                            InstallStage.INSTALL_CODE_SERVER, InstallStage.READY -> StepState.DONE
                            null -> StepState.PENDING
                        },
                        when (stage) {
                            InstallStage.INSTALL_CODE_SERVER -> StepState.ACTIVE
                            InstallStage.READY -> StepState.DONE
                            InstallStage.EXTRACT_ROOTFS, null -> StepState.PENDING
                        },
                        when (stage) {
                            InstallStage.READY -> StepState.DONE
                            null -> StepState.PENDING
                            else -> StepState.PENDING
                        },
                    )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf(
                        stringResource(R.string.wizard_step_download),
                        stringResource(R.string.wizard_step_extract),
                        stringResource(R.string.wizard_step_tools_install, state.tools.count { it.value }),
                        stringResource(R.string.wizard_step_configure),
                    ).forEachIndexed { index, label ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            when (stepStates[index]) {
                                StepState.DONE ->
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                StepState.ACTIVE ->
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                    )
                                StepState.PENDING ->
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                            }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color =
                                    if (stepStates[index] == StepState.PENDING) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFolderPicker) {
        FolderPickerDialog(
            root = foldersRoot,
            onPick = { path ->
                viewModel.addMount(path)
                showFolderPicker = false
            },
            onDismiss = { showFolderPicker = false },
        )
    }
}

@Composable
private fun ToolGroup(
    title: String,
    tools: List<ToolDefinition>,
    state: Map<String, Boolean>,
    onToggle: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = { onSelectAll(tools.any { state[it.id] != true }) },
            ) {
                Text(stringResource(R.string.wizard_select_all))
            }
        }
        tools.forEach { tool ->
            ToolRow(
                title = stringResource(tool.titleRes),
                subtitle = stringResource(tool.subtitleRes),
                checked = state[tool.id] == true,
                onCheckedChange = { onToggle(tool.id) },
                readyBadge = tool.readyBadge,
            )
        }
    }
}

private const val STEP_DISTRO = 0
private const val STEP_TOOLS = 1
private const val STEP_MOUNTS = 2
private const val STEP_CONFIG = 3
private const val STEP_INSTALL = 4
