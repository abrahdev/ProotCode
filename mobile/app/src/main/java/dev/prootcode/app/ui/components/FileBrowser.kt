package dev.prootcode.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.prootcode.app.R
import java.util.Locale

data class Breadcrumb(
    val label: String,
    val path: String,
)

data class FileEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long? = null,
    val mounted: Boolean = false,
)

fun breadcrumbs(path: String): List<Breadcrumb> {
    val normalized = path.trim().ifBlank { "/" }
    val result = mutableListOf(Breadcrumb("/", "/"))
    var current = ""
    normalized.split('/').filter { it.isNotBlank() }.forEach { part ->
        current += "/$part"
        result += Breadcrumb("/$part", current)
    }
    return result
}

fun formatFileSize(bytes: Long): String =
    when {
        bytes >= 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }

@Composable
fun FileBrowser(
    path: String,
    entries: List<FileEntry>,
    onNavigate: (String) -> Unit,
    onOpenFile: (FileEntry) -> Unit,
    onDeleteFile: (FileEntry) -> Unit,
    onShareFile: (FileEntry) -> Unit,
    modifier: Modifier = Modifier,
    parentLabel: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        val crumbs = breadcrumbs(path)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            crumbs.forEachIndexed { index, crumb ->
                val current = index == crumbs.lastIndex
                AssistChip(
                    onClick = { onNavigate(crumb.path) },
                    label = { Text(crumb.label) },
                    colors =
                        AssistChipDefaults.assistChipColors(
                            containerColor =
                                if (current) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                            labelColor =
                                if (current) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        ),
                )
            }
        }
        Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            if (path != "/" && parentLabel != null) {
                FileBrowserRow(
                    icon = Icons.Outlined.ArrowUpward,
                    name = parentLabel,
                    detail = null,
                    mounted = false,
                    file = null,
                    onClick = { onNavigate(path.substringBeforeLast('/').ifBlank { "/" }) },
                    onDeleteFile = onDeleteFile,
                    onShareFile = onShareFile,
                )
            }
            entries.forEach { entry ->
                FileBrowserRow(
                    icon =
                        when {
                            entry.isDirectory -> Icons.Outlined.Folder
                            else -> Icons.Outlined.Description
                        },
                    name = entry.name,
                    detail = entry.sizeBytes?.let { formatFileSize(it) },
                    mounted = entry.mounted,
                    file = entry.takeUnless { it.isDirectory },
                    onClick = {
                        if (entry.isDirectory) onNavigate(entry.path) else onOpenFile(entry)
                    },
                    onDeleteFile = onDeleteFile,
                    onShareFile = onShareFile,
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun FileBrowserRow(
    icon: ImageVector,
    name: String,
    detail: String?,
    mounted: Boolean,
    file: FileEntry?,
    onClick: () -> Unit,
    onDeleteFile: (FileEntry) -> Unit,
    onShareFile: (FileEntry) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = if (mounted && icon == Icons.Outlined.Folder) Icons.Outlined.FolderOpen else icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (mounted) {
                Text(
                    text = stringResource(R.string.files_mounted),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (detail != null) {
            Text(
                text = detail,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (file != null) {
            IconButton(
                onClick = { menuOpen = true },
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = stringResource(R.string.files_actions, name),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.files_share)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onShareFile(file)
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.files_delete)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onDeleteFile(file)
                    },
                )
            }
        }
    }
}
