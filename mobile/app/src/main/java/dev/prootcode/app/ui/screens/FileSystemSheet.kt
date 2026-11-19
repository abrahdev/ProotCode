package dev.prootcode.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.prootcode.app.R
import dev.prootcode.app.ui.components.BackupEntry
import dev.prootcode.app.ui.components.BackupItem
import dev.prootcode.app.ui.components.FileEntry
import dev.prootcode.app.ui.components.RootfsBrowser
import dev.prootcode.app.ui.components.SheetScaffold
import java.io.File

@Composable
fun FileSystemSheet(
    rootfs: File,
    onOpenFile: (FileEntry) -> Unit,
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    SheetScaffold(
        title = stringResource(R.string.sheet_files_title),
        onDismiss = onDismiss,
    ) {
        RootfsBrowser(
            root = rootfs,
            onOpenFile = onOpenFile,
            onMessage = onMessage,
        )
    }
}

@Composable
fun BackupsSheet(
    onMessage: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val backups =
        remember {
            mutableStateListOf(
                BackupEntry("ubuntu-26.04-2026-11-08", "2.4 MB · Nov 8, 2026"),
                BackupEntry("debian-13-2026-11-01", "1.8 MB · Nov 1, 2026"),
            )
        }
    val backupsPending = stringResource(R.string.backups_pending)
    val backupsDeleted = stringResource(R.string.backups_deleted)
    SheetScaffold(
        title = stringResource(R.string.sheet_backups_title),
        onDismiss = onDismiss,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { onMessage(backupsPending) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = null,
                )
                Text(stringResource(R.string.config_export_backup))
            }
            OutlinedButton(
                onClick = { onMessage(backupsPending) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Upload,
                    contentDescription = null,
                )
                Text(stringResource(R.string.config_import_backup))
            }
        }
        Text(
            text = stringResource(R.string.backups_previous),
            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
        )
        backups.forEach { entry ->
            BackupItem(
                entry = entry,
                onDelete = {
                    backups.remove(entry)
                    onMessage(backupsDeleted)
                },
            )
        }
        if (backups.isEmpty()) {
            Text(
                text = stringResource(R.string.backups_empty),
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.action_close))
        }
    }
}
