package dev.prootcode.app.ui.terminal

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.termux.terminal.TerminalSession
import dev.prootcode.app.R

@Composable
fun ExtraKeysRow(
    client: ProotViewClient?,
    session: TerminalSession?,
    shortcuts: List<TerminalShortcut>,
    onCustomize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        shortcuts.filter { it.enabled }.forEach { shortcut ->
            val active =
                when (shortcut.id) {
                    "ctrl" -> client?.ctrlActive == true
                    "alt" -> client?.altActive == true
                    else -> false
                }
            KeyPill(label = shortcut.label, active = active) {
                when (shortcut.kind) {
                    ShortcutKind.MODIFIER -> {
                        when (shortcut.id) {
                            "ctrl" -> client?.let { it.ctrlActive = !it.ctrlActive }
                            "alt" -> client?.let { it.altActive = !it.altActive }
                        }
                    }
                    ShortcutKind.KEY, ShortcutKind.TEXT -> send(session, shortcut.payload)
                }
            }
        }
        IconButton(onClick = onCustomize) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.terminal_edit_shortcuts),
                modifier = Modifier.height(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun KeyPill(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color =
            if (active) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        contentColor =
            if (active) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        modifier = Modifier.height(40.dp).widthIn(min = 44.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
        }
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
