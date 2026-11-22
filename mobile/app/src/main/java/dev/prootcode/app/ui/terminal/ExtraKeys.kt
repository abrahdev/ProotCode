package dev.prootcode.app.ui.terminal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.termux.terminal.TerminalSession
import dev.prootcode.app.R

@OptIn(ExperimentalFoundationApi::class)
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
            if (shortcut.kind == ShortcutKind.MODIFIER) {
                val isLatched =
                    when (shortcut.id) {
                        "ctrl" -> client?.ctrlLatched == true
                        "alt" -> client?.altLatched == true
                        else -> false
                    }
                val active =
                    when (shortcut.id) {
                        "ctrl" -> client?.ctrlActive == true
                        "alt" -> client?.altActive == true
                        else -> false
                    }
                KeyPill(
                    label = shortcut.label,
                    active = active,
                    latched = isLatched,
                    interactionSource = remember(shortcut.id) { MutableInteractionSource() },
                    onClick = { client?.toggleModifier(shortcut.id) },
                    onLongClick = { client?.toggleModifierLatch(shortcut.id) },
                    onDoubleClick = { client?.toggleModifierLatch(shortcut.id) },
                )
            } else {
                KeyPill(
                    label = shortcut.label,
                    active = false,
                    interactionSource = remember(shortcut.id) { MutableInteractionSource() },
                    onClick = {
                        send(session, shortcut.payload)
                        client?.consumeModifiers()
                    },
                )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeyPill(
    label: String,
    active: Boolean,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    latched: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color =
                when {
                    latched -> MaterialTheme.colorScheme.primary
                    active -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                },
            contentColor =
                when {
                    latched -> MaterialTheme.colorScheme.onPrimary
                    active -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
            modifier =
                Modifier
                    .height(40.dp)
                    .widthIn(min = 44.dp)
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onClick,
                        onLongClick = onLongClick,
                        onDoubleClick = onDoubleClick,
                    ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Box(
            modifier =
                Modifier
                    .padding(top = 3.dp)
                    .width(18.dp)
                    .height(3.dp)
                    .background(
                        if (latched) MaterialTheme.colorScheme.primary else Color.Transparent,
                        RoundedCornerShape(2.dp),
                    ),
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
