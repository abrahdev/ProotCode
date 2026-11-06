package dev.prootcode.app.ui.terminal

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.termux.terminal.TerminalSession
import dev.prootcode.app.R

enum class ExtraKey { CTRL, ALT, ESC, TAB, LEFT, UP, DOWN, RIGHT }

class ExtraKeysConfig(
    private val preferences: SharedPreferences,
) {
    var enabled: Set<ExtraKey>
        get() =
            preferences
                .getStringSet(KEY, null)
                ?.mapNotNull { runCatching { ExtraKey.valueOf(it) }.getOrNull() }
                ?.toSet()
                ?: ExtraKey.entries.toSet()
        set(value) {
            preferences.edit().putStringSet(KEY, value.map { it.name }.toSet()).apply()
        }

    private companion object {
        private const val KEY = "extra_keys"
    }
}

@Composable
fun ExtraKeysRow(
    client: ProotViewClient?,
    session: TerminalSession?,
    config: ExtraKeysConfig,
) {
    var enabled by remember { mutableStateOf(config.enabled) }
    var showConfig by remember { mutableStateOf(false) }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ExtraKey.entries.filter { it in enabled }.forEach { key ->
            when (key) {
                ExtraKey.CTRL ->
                    KeyButton(stringResource(R.string.terminal_key_ctrl), client?.ctrlActive == true) {
                        client?.let { it.ctrlActive = !it.ctrlActive }
                    }
                ExtraKey.ALT ->
                    KeyButton(stringResource(R.string.terminal_key_alt), client?.altActive == true) {
                        client?.let { it.altActive = !it.altActive }
                    }
                ExtraKey.ESC -> KeyButton(stringResource(R.string.terminal_key_esc)) { send(session, "\u001b") }
                ExtraKey.TAB -> KeyButton(stringResource(R.string.terminal_key_tab)) { send(session, "\t") }
                ExtraKey.LEFT -> KeyButton(stringResource(R.string.terminal_key_left)) { send(session, "\u001b[D") }
                ExtraKey.UP -> KeyButton(stringResource(R.string.terminal_key_up)) { send(session, "\u001b[A") }
                ExtraKey.DOWN -> KeyButton(stringResource(R.string.terminal_key_down)) { send(session, "\u001b[B") }
                ExtraKey.RIGHT -> KeyButton(stringResource(R.string.terminal_key_right)) { send(session, "\u001b[C") }
            }
        }
        TextButton(onClick = { showConfig = true }) {
            Text(stringResource(R.string.terminal_keys))
        }
    }

    if (showConfig) {
        var selection by remember { mutableStateOf(enabled) }
        AlertDialog(
            onDismissRequest = { showConfig = false },
            title = { Text(stringResource(R.string.terminal_keys_title)) },
            text = {
                Column {
                    ExtraKey.entries.forEach { key ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = key in selection,
                                onCheckedChange = { checked ->
                                    selection = if (checked) selection + key else selection - key
                                },
                            )
                            Text(key.label())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        config.enabled = selection
                        enabled = selection
                        showConfig = false
                    },
                ) {
                    Text("OK")
                }
            },
        )
    }
}

@Composable
private fun KeyButton(
    label: String,
    active: Boolean = false,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Text(
            text = label,
            color = if (active) MaterialTheme.colorScheme.primary else Color.Unspecified,
        )
    }
}

private fun send(
    session: TerminalSession?,
    sequence: String,
) {
    if (session == null) return
    val bytes = sequence.toByteArray(Charsets.UTF_8)
    session.write(bytes, 0, bytes.size)
}

@Composable
private fun ExtraKey.label(): String =
    stringResource(
        when (this) {
            ExtraKey.CTRL -> R.string.terminal_key_ctrl
            ExtraKey.ALT -> R.string.terminal_key_alt
            ExtraKey.ESC -> R.string.terminal_key_esc
            ExtraKey.TAB -> R.string.terminal_key_tab
            ExtraKey.LEFT -> R.string.terminal_key_left
            ExtraKey.UP -> R.string.terminal_key_up
            ExtraKey.DOWN -> R.string.terminal_key_down
            ExtraKey.RIGHT -> R.string.terminal_key_right
        },
    )
