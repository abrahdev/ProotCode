package dev.prootcode.app.ui.wizard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
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
import dev.prootcode.app.runtime.proot.TreeUriPaths
import dev.prootcode.app.ui.components.DistroOptionCard
import dev.prootcode.app.ui.components.ExpressiveCircularProgressIndicator
import dev.prootcode.app.ui.components.IconSelectionSection
import dev.prootcode.app.ui.components.LabeledTextField
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
    val aptPackage: String? = null,
    val readyBadge: Boolean = false,
)

private fun aptTool(
    id: String,
    titleRes: Int,
    subtitleRes: Int,
    group: String,
): ToolDefinition = ToolDefinition(id, titleRes, subtitleRes, group, id)

private val TOOL_GROUPS =
    listOf(
        "essentials" to R.string.wizard_group_essentials,
        "code" to R.string.wizard_group_code,
        "network" to R.string.wizard_group_network,
        "languages" to R.string.wizard_group_languages,
        "utilities" to R.string.wizard_group_utilities,
    )

private val TOOL_DEFINITIONS =
    listOf(
        aptTool("git", R.string.tool_git, R.string.tool_git_sub, "essentials"),
        aptTool(
            "build-essential",
            R.string.tool_build_essential,
            R.string.tool_build_essential_sub,
            "essentials",
        ),
        aptTool("curl", R.string.tool_curl, R.string.tool_curl_sub, "essentials"),
        aptTool(
            "ca-certificates",
            R.string.tool_ca_certificates,
            R.string.tool_ca_certificates_sub,
            "essentials",
        ),
        aptTool("wget", R.string.tool_wget, R.string.tool_wget_sub, "essentials"),
        aptTool("unzip", R.string.tool_unzip, R.string.tool_unzip_sub, "essentials"),
        aptTool("xz-utils", R.string.tool_xz_utils, R.string.tool_xz_utils_sub, "essentials"),
        aptTool("bzip2", R.string.tool_bzip2, R.string.tool_bzip2_sub, "essentials"),
        aptTool("less", R.string.tool_less, R.string.tool_less_sub, "essentials"),
        aptTool("nano", R.string.tool_nano, R.string.tool_nano_sub, "essentials"),
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
        aptTool(
            "openssh-client",
            R.string.tool_openssh,
            R.string.tool_openssh_sub,
            "network",
        ),
        aptTool("rsync", R.string.tool_rsync, R.string.tool_rsync_sub, "network"),
        aptTool("iproute2", R.string.tool_iproute2, R.string.tool_iproute2_sub, "network"),
        aptTool("iputils-ping", R.string.tool_ping, R.string.tool_ping_sub, "network"),
        aptTool(
            "bind9-dnsutils",
            R.string.tool_dnsutils,
            R.string.tool_dnsutils_sub,
            "network",
        ),
        aptTool("python3", R.string.tool_python, R.string.tool_python_sub, "languages"),
        aptTool("nodejs", R.string.tool_nodejs, R.string.tool_nodejs_sub, "languages"),
        aptTool("golang", R.string.tool_golang, R.string.tool_golang_sub, "languages"),
        aptTool("rustc", R.string.tool_rustc, R.string.tool_rustc_sub, "languages"),
        aptTool("htop", R.string.tool_htop, R.string.tool_htop_sub, "utilities"),
        aptTool("ripgrep", R.string.tool_ripgrep, R.string.tool_ripgrep_sub, "utilities"),
        aptTool("tmux", R.string.tool_tmux, R.string.tool_tmux_sub, "utilities"),
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
    val context = LocalContext.current
    var folderError by remember { mutableStateOf(false) }

    fun handleFolder(
        uri: Uri?,
        downloads: Boolean,
    ) {
        val picked = uri ?: run {
            folderError = true
            return
        }
        val path = TreeUriPaths.resolve(picked)?.takeIf { File(it).isDirectory }
        if (path == null) {
            folderError = true
            return
        }
        TreeUriPaths.persist(context, picked)
        folderError = false
        if (downloads) {
            viewModel.setMountDownloads(true, path)
        } else {
            viewModel.addMount(path)
        }
    }
    val downloadsPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            handleFolder(uri, downloads = true)
        }
    val extraPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            handleFolder(uri, downloads = false)
        }

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

    fun packages(): List<String> =
        TOOL_DEFINITIONS
            .filter { state.tools[it.id] == true && it.aptPackage != null }
            .mapNotNull { it.aptPackage }
            .distinct()

    fun buildInput(): EnvironmentInstallInput? {
        val selected = distro ?: return null
        return EnvironmentInstallInput(
            name = state.name.trim().ifBlank { "${selected.name} ${selected.version}" },
            distro = selected,
            codeServer = codeServer,
            presets = presets(),
            packages = packages(),
            settings =
                EnvironmentSettings(
                    icon = state.icon,
                    mountDownloads = state.mountDownloads,
                    downloadsPath = state.downloadsPath,
                    extraMounts = state.extraMounts,
                    adbShare = state.adbShare,
                    customUser = state.userName.isNotBlank(),
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
        maxContentHeightFraction =
            when (state.step) {
                STEP_DISTRO -> 0.62f
                STEP_TOOLS -> 0.86f
                STEP_MOUNTS -> 0.58f
                STEP_CONFIG -> 0.8f
                else -> 0.64f
            },
        header = {
            StepIndicator(steps = steps, current = state.step)
        },
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
        when (state.step) {
            STEP_DISTRO -> {
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
                            versions =
                                if (index == state.distroIndex) {
                                    distroGroup.artifacts.map { it.version }
                                } else {
                                    emptyList()
                                },
                            selectedVersion =
                                if (index == state.distroIndex) state.versionIndex else 0,
                            logoRes =
                                when {
                                    distroGroup.name.contains("Ubuntu") -> R.drawable.ic_distro_ubuntu
                                    distroGroup.name.contains("Debian") -> R.drawable.ic_distro_debian
                                    else -> null
                                },
                        )
                    }
                }
            }

            STEP_TOOLS -> {
                TOOL_GROUPS.forEach { (groupId, titleRes) ->
                    val groupTools = TOOL_DEFINITIONS.filter { it.group == groupId }
                    ToolGroup(
                        title = stringResource(titleRes),
                        tools = groupTools,
                        state = state.tools,
                        onToggle = viewModel::toggleTool,
                        onSelectAll = { select ->
                            groupTools.forEach { tool ->
                                if ((state.tools[tool.id] ?: false) != select) viewModel.toggleTool(tool.id)
                            }
                        },
                    )
                }
            }

            STEP_MOUNTS -> {
                SettingsGroup(title = stringResource(R.string.config_downloads)) {
                    SwitchRow(
                        title = stringResource(R.string.wizard_downloads_toggle),
                        subtitle = stringResource(R.string.wizard_downloads_sub),
                        checked = state.mountDownloads,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                downloadsPicker.launch(null)
                            } else {
                                viewModel.setMountDownloads(false)
                            }
                        },
                    )
                    state.downloadsPath?.let { path ->
                        Text(
                            text = path,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        )
                    }
                }
                SettingsGroup(title = stringResource(R.string.config_shared_folders)) {
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
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = if (state.extraMounts.isEmpty()) 160.dp else 0.dp)
                                .padding(bottom = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (folderError) {
                                Text(
                                    text = stringResource(R.string.wizard_folder_unavailable),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            OutlinedButton(onClick = { extraPicker.launch(null) }) {
                                Text(stringResource(R.string.config_add_folder))
                            }
                        }
                    }
                }
            }

            STEP_CONFIG -> {
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
                LabeledTextField(
                    label = stringResource(R.string.wizard_user_name),
                    value = state.userName,
                    onValueChange = viewModel::setUserName,
                    placeholder = stringResource(R.string.wizard_user_placeholder),
                    supportingText = stringResource(R.string.wizard_user_empty_hint),
                    maxLength = 32,
                )
                SettingsGroup(title = stringResource(R.string.wizard_access_title)) {
                    SwitchRow(
                        title = stringResource(R.string.wizard_share_adb),
                        subtitle = stringResource(R.string.wizard_share_adb_sub),
                        checked = state.adbShare,
                        onCheckedChange = viewModel::setAdbShare,
                    )
                }
                if (state.adbShare) {
                    OutlinedButton(onClick = onOpenAdb) {
                        Text(stringResource(R.string.wizard_setup_adb))
                    }
                }
                SettingsGroup(title = stringResource(R.string.config_icon)) {
                    IconSelectionSection(
                        selectedId = state.icon,
                        onSelect = viewModel::setIcon,
                    )
                }
            }

            STEP_INSTALL -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text =
                            when (stage) {
                                InstallStage.EXTRACT_ROOTFS -> stringResource(R.string.install_stage_rootfs)
                                InstallStage.INSTALL_PACKAGES ->
                                    stringResource(R.string.install_stage_packages)
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
                            InstallStage.INSTALL_PACKAGES,
                            InstallStage.INSTALL_CODE_SERVER,
                            InstallStage.READY,
                            -> StepState.DONE
                            null -> StepState.PENDING
                        },
                        when (stage) {
                            InstallStage.INSTALL_PACKAGES,
                            InstallStage.INSTALL_CODE_SERVER,
                            -> StepState.ACTIVE
                            InstallStage.READY -> StepState.DONE
                            else -> StepState.PENDING
                        },
                        when (stage) {
                            InstallStage.READY -> StepState.DONE
                            null -> StepState.PENDING
                            else -> StepState.PENDING
                        },
                    )
                val downloadProgress =
                    progress?.let { if (it.second > 0) it.first.toFloat() / it.second else null }
                val activeProgress =
                    when (stage) {
                        InstallStage.EXTRACT_ROOTFS -> 0.35f
                        InstallStage.INSTALL_PACKAGES,
                        InstallStage.INSTALL_CODE_SERVER,
                        -> 0.7f
                        InstallStage.READY -> 1f
                        null -> downloadProgress ?: 0.15f
                    }
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
                                    ExpressiveCircularProgressIndicator(
                                        progress = activeProgress,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 3.dp,
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
}

@Composable
private fun ToolGroup(
    title: String,
    tools: List<ToolDefinition>,
    state: Map<String, Boolean>,
    onToggle: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
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
        SettingsGroup(title = null) {
            tools.forEachIndexed { index, tool ->
                ToolRow(
                    title = stringResource(tool.titleRes),
                    subtitle = stringResource(tool.subtitleRes),
                    checked = state[tool.id] == true,
                    onCheckedChange = { onToggle(tool.id) },
                    readyBadge = tool.readyBadge,
                    showDivider = index != tools.lastIndex,
                )
            }
        }
    }
}

private const val STEP_DISTRO = 0
private const val STEP_TOOLS = 1
private const val STEP_MOUNTS = 2
private const val STEP_CONFIG = 3
private const val STEP_INSTALL = 4
