package dev.prootcode.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import dev.prootcode.app.R
import java.io.File

@Composable
fun FolderPickerDialog(
    root: File,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newFolder by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(listOf<File>()) }

    fun reload() {
        root.mkdirs()
        items =
            root
                .listFiles { file -> file.isDirectory }
                ?.sortedBy { it.name.lowercase() }
                .orEmpty()
    }

    androidx.compose.runtime.LaunchedEffect(root) { reload() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.folder_picker_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LabeledTextField(
                    label = stringResource(R.string.folder_picker_new),
                    value = newFolder,
                    onValueChange = { newFolder = it },
                )
                OutlinedButton(
                    enabled = newFolder.isNotBlank(),
                    onClick = {
                        val folder = File(root, newFolder.trim())
                        folder.mkdirs()
                        newFolder = ""
                        reload()
                    },
                ) {
                    Text(stringResource(R.string.folder_picker_create))
                }
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items.forEach { folder ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(folder.absolutePath) }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Text(text = folder.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                Text(
                    text = root.absolutePath,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
