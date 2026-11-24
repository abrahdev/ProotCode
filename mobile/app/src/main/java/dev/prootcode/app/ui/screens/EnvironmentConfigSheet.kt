package dev.prootcode.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.prootcode.app.R
import dev.prootcode.app.data.environment.Environment
import dev.prootcode.app.data.environment.EnvironmentSettings
import dev.prootcode.app.ui.components.ChoiceChipRow
import dev.prootcode.app.ui.components.FolderPickerDialog
import dev.prootcode.app.ui.components.IconSelectionSection
import dev.prootcode.app.ui.components.LabeledTextField
import dev.prootcode.app.ui.components.SettingsGroup
import dev.prootcode.app.ui.components.SheetScaffold
import dev.prootcode.app.ui.components.SwitchRow
import dev.prootcode.app.ui.components.formatFileSize
import java.io.File

enum class ConfigKey {
    NAME,
    ICON,
    AUTOSTART,
    LANGUAGE,
    TIMEZONE,
    MOUNTS,
    CACHE,
    AUTOCLEANUP,
    PORT,
    NETWORK_SHARING,
    PROXY,
    ADB,
    USER,
    SSH,
    BACKGROUND,
    PRIORITY,
    DEBUG,
    LOGS,
    BACKUP,
}

@Composable
fun EnvironmentConfigSheet(
    environment: Environment,
    configKey: ConfigKey,
    cacheBytes: Long,
    foldersRoot: File,
    onUpdate: (Environment) -> Unit,
    onOpenAdb: () -> Unit,
    onOpenBackups: () -> Unit,
    onClearCache: () -> Unit,
    onTestBridge: () -> Unit,
    onDelete: () -> Unit,
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var local by remember(environment.id, configKey) { mutableStateOf(environment) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showNameHelp by remember { mutableStateOf(false) }
    val backupPending = stringResource(R.string.config_backup_pending)

    fun settings(transform: (EnvironmentSettings) -> EnvironmentSettings) {
        val updated = local.copy(settings = transform(local.settings))
        local = updated
        onUpdate(updated)
    }

    fun name(value: String) {
        val updated = local.copy(name = value.take(40))
        local = updated
        onUpdate(updated)
    }

    SheetScaffold(
        title = configTitle(configKey),
        onDismiss = onDismiss,
    ) {
        when (configKey) {
            ConfigKey.NAME -> {
                LabeledTextField(
                    label = stringResource(R.string.config_name),
                    value = local.name,
                    onValueChange = ::name,
                    maxLength = 40,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.config_name_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { showNameHelp = !showNameHelp }) {
                        Text(stringResource(R.string.config_commands_help))
                    }
                }
                if (showNameHelp) {
                    Text(
                        text = stringResource(R.string.config_commands),
                        style =
                            MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                            ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                    )
                }
            }

            ConfigKey.ICON -> {
                Text(
                    text = stringResource(R.string.config_icon_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                IconSelectionSection(
                    selectedId = local.settings.icon,
                    onSelect = { icon -> settings { it.copy(icon = icon) } },
                )
            }

            ConfigKey.AUTOSTART ->
                SettingsGroup(title = null) {
                    SwitchRow(
                        title = stringResource(R.string.config_autostart),
                        subtitle = stringResource(R.string.config_autostart_sub),
                        checked = local.settings.autostart,
                        onCheckedChange = { checked -> settings { it.copy(autostart = checked) } },
                    )
                }

            ConfigKey.LANGUAGE -> {
                Text(
                    text = stringResource(R.string.config_language_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ChoiceChipRow(
                    options = EnvironmentSettings.LANGUAGES,
                    selected = EnvironmentSettings.LANGUAGES.indexOf(local.settings.language),
                    onSelected = { index ->
                        settings { it.copy(language = EnvironmentSettings.LANGUAGES[index]) }
                    },
                )
            }

            ConfigKey.TIMEZONE -> {
                LabeledTextField(
                    label = stringResource(R.string.config_timezone),
                    value = local.settings.timezone,
                    onValueChange = { value -> settings { it.copy(timezone = value) } },
                )
                Text(
                    text = stringResource(R.string.config_timezone_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ConfigKey.MOUNTS -> {
                Text(
                    text = stringResource(R.string.config_mounts_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SettingsGroup(title = stringResource(R.string.config_shared_folders)) {
                    SwitchRow(
                        title = stringResource(R.string.config_downloads),
                        subtitle = stringResource(R.string.wizard_mounted_default),
                        checked = local.settings.mountDownloads,
                        onCheckedChange = { checked ->
                            settings { it.copy(mountDownloads = checked) }
                        },
                        showDivider = local.settings.extraMounts.isNotEmpty(),
                    )
                    local.settings.extraMounts.forEach { path ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = path.substringAfterLast('/'),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = path,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(
                                onClick = {
                                    settings { it.copy(extraMounts = it.extraMounts - path) }
                                },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.action_remove),
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
                OutlinedButton(onClick = { showFolderPicker = true }) {
                    Text(stringResource(R.string.config_add_folder))
                }
            }

            ConfigKey.CACHE -> {
                Text(
                    text = stringResource(R.string.config_cache_used, formatFileSize(cacheBytes)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = onClearCache) {
                    Text(stringResource(R.string.config_clear_cache))
                }
            }

            ConfigKey.AUTOCLEANUP ->
                SettingsGroup(title = null) {
                    SwitchRow(
                        title = stringResource(R.string.config_autocleanup),
                        subtitle = stringResource(R.string.config_autocleanup_sub),
                        checked = local.settings.autoCleanup,
                        onCheckedChange = { checked -> settings { it.copy(autoCleanup = checked) } },
                    )
                }

            ConfigKey.PORT -> {
                LabeledTextField(
                    label = stringResource(R.string.config_port),
                    value = local.settings.codeServerPort.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let { port ->
                            settings { it.copy(codeServerPort = port.coerceIn(1024, 65535)) }
                        }
                    },
                    keyboardType = KeyboardType.Number,
                    maxLength = 5,
                )
                Text(
                    text = stringResource(R.string.config_port_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ConfigKey.NETWORK_SHARING ->
                SettingsGroup(title = null) {
                    SwitchRow(
                        title = stringResource(R.string.config_netshare),
                        subtitle = stringResource(R.string.config_netshare_sub),
                        checked = local.settings.networkSharing,
                        onCheckedChange = { checked ->
                            settings { it.copy(networkSharing = checked) }
                        },
                    )
                }

            ConfigKey.PROXY -> {
                LabeledTextField(
                    label = stringResource(R.string.config_proxy),
                    value = local.settings.proxy,
                    onValueChange = { value -> settings { it.copy(proxy = value) } },
                )
                Text(
                    text = stringResource(R.string.config_proxy_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ConfigKey.ADB -> {
                SettingsGroup(title = null) {
                    SwitchRow(
                        title = stringResource(R.string.config_adb_share),
                        subtitle = stringResource(R.string.config_adb_sub),
                        checked = local.settings.adbShare,
                        onCheckedChange = { checked -> settings { it.copy(adbShare = checked) } },
                    )
                }
                OutlinedButton(onClick = onOpenAdb) {
                    Text(stringResource(R.string.wizard_setup_adb))
                }
            }

            ConfigKey.USER -> {
                SettingsGroup(title = null) {
                    SwitchRow(
                        title = stringResource(R.string.config_user_custom),
                        subtitle = stringResource(R.string.config_user_sub),
                        checked = local.settings.customUser,
                        onCheckedChange = { checked ->
                            settings { it.copy(customUser = checked) }
                        },
                    )
                }
                if (local.settings.customUser) {
                    LabeledTextField(
                        label = stringResource(R.string.wizard_user_name),
                        value = local.settings.userName,
                        onValueChange = { value -> settings { it.copy(userName = value) } },
                    )
                }
            }

            ConfigKey.SSH -> {
                SettingsGroup(title = null) {
                    SwitchRow(
                        title = stringResource(R.string.config_ssh),
                        subtitle = stringResource(R.string.config_ssh_sub),
                        checked = local.settings.ssh,
                        onCheckedChange = { checked -> settings { it.copy(ssh = checked) } },
                    )
                }
                if (local.settings.ssh) {
                    LabeledTextField(
                        label = stringResource(R.string.config_ssh_port),
                        value = local.settings.sshPort.toString(),
                        onValueChange = { value ->
                            value.toIntOrNull()?.let { port ->
                                settings { it.copy(sshPort = port.coerceIn(1024, 65535)) }
                            }
                        },
                        keyboardType = KeyboardType.Number,
                        maxLength = 5,
                    )
                }
            }

            ConfigKey.BACKGROUND ->
                SettingsGroup(title = null) {
                    SwitchRow(
                        title = stringResource(R.string.config_bg),
                        subtitle = stringResource(R.string.config_bg_sub),
                        checked = local.settings.backgroundExecution,
                        onCheckedChange = { checked ->
                            settings { it.copy(backgroundExecution = checked) }
                        },
                    )
                }

            ConfigKey.PRIORITY -> {
                Text(
                    text = stringResource(R.string.config_priority_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ChoiceChipRow(
                    options =
                        listOf(
                            stringResource(R.string.priority_low),
                            stringResource(R.string.priority_normal),
                        ),
                    selected =
                        EnvironmentSettings.PRIORITIES
                            .indexOf(local.settings.priority)
                            .coerceAtLeast(0),
                    onSelected = { index ->
                        settings { it.copy(priority = EnvironmentSettings.PRIORITIES[index]) }
                    },
                )
            }

            ConfigKey.DEBUG ->
                SettingsGroup(title = null) {
                    SwitchRow(
                        title = stringResource(R.string.config_debug),
                        subtitle = stringResource(R.string.config_debug_sub),
                        checked = local.settings.debugMode,
                        onCheckedChange = { checked -> settings { it.copy(debugMode = checked) } },
                    )
                }

            ConfigKey.LOGS -> {
                LogViewerContent(environment = local)
                OutlinedButton(onClick = onTestBridge) {
                    Text(stringResource(R.string.bridge_test))
                }
            }

            ConfigKey.BACKUP -> {
                Text(
                    text = stringResource(R.string.config_backup_sub),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onMessage(backupPending) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(stringResource(R.string.config_export_backup))
                    }
                    OutlinedButton(
                        onClick = { onMessage(backupPending) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Upload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(stringResource(R.string.config_import_backup))
                    }
                }
                OutlinedButton(
                    onClick = onOpenBackups,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.backups_previous))
                }
            }
        }
    }

    if (showFolderPicker) {
        FolderPickerDialog(
            root = foldersRoot,
            onPick = { path ->
                settings { it.copy(extraMounts = (it.extraMounts + path).distinct()) }
                showFolderPicker = false
            },
            onDismiss = { showFolderPicker = false },
        )
    }
}

@Composable
internal fun configTitle(key: ConfigKey): String =
    stringResource(
        when (key) {
            ConfigKey.NAME -> R.string.config_machine_name
            ConfigKey.ICON -> R.string.config_icon
            ConfigKey.AUTOSTART -> R.string.config_autostart
            ConfigKey.LANGUAGE -> R.string.config_language
            ConfigKey.TIMEZONE -> R.string.config_timezone
            ConfigKey.MOUNTS -> R.string.config_mounts
            ConfigKey.CACHE -> R.string.config_cache
            ConfigKey.AUTOCLEANUP -> R.string.config_autocleanup
            ConfigKey.PORT -> R.string.config_port
            ConfigKey.NETWORK_SHARING -> R.string.config_netshare
            ConfigKey.PROXY -> R.string.config_proxy
            ConfigKey.ADB -> R.string.config_adb
            ConfigKey.USER -> R.string.config_user
            ConfigKey.SSH -> R.string.config_ssh
            ConfigKey.BACKGROUND -> R.string.config_bg
            ConfigKey.PRIORITY -> R.string.config_priority
            ConfigKey.DEBUG -> R.string.config_debug
            ConfigKey.LOGS -> R.string.config_logs
            ConfigKey.BACKUP -> R.string.config_backup
        },
    )
