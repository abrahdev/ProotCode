package dev.prootcode.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.prootcode.app.R
import dev.prootcode.app.ui.components.LabeledTextField
import dev.prootcode.app.ui.components.ReorderControls
import dev.prootcode.app.ui.components.SheetScaffold
import dev.prootcode.app.ui.terminal.ShortcutKind
import dev.prootcode.app.ui.terminal.TerminalShortcut
import dev.prootcode.app.ui.terminal.TerminalShortcutStore

@Composable
fun ShortcutsEditorSheet(
    store: TerminalShortcutStore,
    onDismiss: () -> Unit,
) {
    val shortcuts = remember { mutableStateListOf<TerminalShortcut>().apply { addAll(store.load()) } }
    var label by remember { mutableStateOf("") }
    var payload by remember { mutableStateOf("") }
    var customCounter by remember { mutableIntStateOf(0) }

    fun commit() {
        store.save(shortcuts.toList())
    }

    SheetScaffold(
        title = stringResource(R.string.sheet_shortcuts_title),
        onDismiss = onDismiss,
        footer = {
            TextButton(
                onClick = {
                    shortcuts.clear()
                    shortcuts.addAll(store.reset())
                },
            ) {
                Text(stringResource(R.string.action_reset))
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    commit()
                    onDismiss()
                },
            ) {
                Text(stringResource(R.string.action_done))
            }
        },
    ) {
        Text(
            text = stringResource(R.string.shortcuts_drag_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        shortcuts.forEachIndexed { index, shortcut ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ReorderControls(
                    canMoveUp = index > 0,
                    canMoveDown = index < shortcuts.lastIndex,
                    onMoveUp = {
                        val item = shortcuts.removeAt(index)
                        shortcuts.add(index - 1, item)
                        commit()
                    },
                    onMoveDown = {
                        val item = shortcuts.removeAt(index)
                        shortcuts.add(index + 1, item)
                        commit()
                    },
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shortcut.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    if (shortcut.custom) {
                        Text(
                            text = shortcut.payload,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Switch(
                    checked = shortcut.enabled,
                    onCheckedChange = { checked ->
                        shortcuts[index] = shortcut.copy(enabled = checked)
                        commit()
                    },
                )
                if (shortcut.custom) {
                    IconButton(
                        onClick = {
                            shortcuts.removeAt(index)
                            commit()
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.shortcuts_delete),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        Text(
            text = stringResource(R.string.shortcuts_add),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        LabeledTextField(
            label = stringResource(R.string.shortcuts_label),
            value = label,
            onValueChange = { label = it },
            maxLength = 8,
        )
        LabeledTextField(
            label = stringResource(R.string.shortcuts_text),
            value = payload,
            onValueChange = { payload = it },
        )
        Button(
            enabled = label.isNotBlank() && payload.isNotEmpty(),
            onClick = {
                customCounter += 1
                shortcuts.add(
                    TerminalShortcut(
                        id = "custom-$customCounter",
                        label = label.trim(),
                        payload = payload,
                        kind = ShortcutKind.TEXT,
                        enabled = true,
                        custom = true,
                    ),
                )
                label = ""
                payload = ""
                commit()
            },
        ) {
            Text(stringResource(R.string.action_add))
        }
    }
}
