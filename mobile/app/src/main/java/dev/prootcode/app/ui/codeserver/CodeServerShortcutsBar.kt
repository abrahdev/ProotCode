package dev.prootcode.app.ui.codeserver

import android.view.KeyEvent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.prootcode.app.R

@Composable
fun CodeServerShortcutsBar(
    onKey: (keyCode: Int, metaState: Int) -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onShowKeyboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var ctrlLocked by remember { mutableStateOf(false) }
    var ctrlActive by remember { mutableStateOf(false) }
    var altLocked by remember { mutableStateOf(false) }
    var altActive by remember { mutableStateOf(false) }

    val ctrl = ctrlLocked || ctrlActive
    val alt = altLocked || altActive

    fun buildMetaState(shift: Boolean): Int {
        var state = 0
        if (ctrl) state = state or KeyEvent.META_CTRL_ON
        if (alt) state = state or KeyEvent.META_ALT_ON
        if (shift) state = state or KeyEvent.META_SHIFT_ON
        return state
    }

    fun send(
        keyCode: Int,
        shift: Boolean = false,
    ) {
        onKey(keyCode, buildMetaState(shift))
        if (!ctrlLocked) ctrlActive = false
        if (!altLocked) altActive = false
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KeyPill(label = "Esc", onClick = { send(KeyEvent.KEYCODE_ESCAPE) })
                KeyPill(label = "Tab", onClick = { send(KeyEvent.KEYCODE_TAB) })
                KeyPill(
                    label = "Ctrl",
                    active = ctrl,
                    locked = ctrlLocked,
                    onClick = { ctrlActive = !ctrlActive },
                    onLongClick = {
                        ctrlLocked = !ctrlLocked
                        ctrlActive = ctrlLocked
                    },
                )
                KeyPill(
                    label = "Alt",
                    active = alt,
                    locked = altLocked,
                    onClick = { altActive = !altActive },
                    onLongClick = {
                        altLocked = !altLocked
                        altActive = altLocked
                    },
                )
                KeyPill(label = "↑", onClick = { send(KeyEvent.KEYCODE_DPAD_UP) })
                KeyPill(label = "↓", onClick = { send(KeyEvent.KEYCODE_DPAD_DOWN) })
                KeyPill(label = "←", onClick = { send(KeyEvent.KEYCODE_DPAD_LEFT) })
                KeyPill(label = "→", onClick = { send(KeyEvent.KEYCODE_DPAD_RIGHT) })
                KeyPill(label = "/", onClick = { send(KeyEvent.KEYCODE_SLASH) })
                KeyPill(label = "-", onClick = { send(KeyEvent.KEYCODE_MINUS) })
                KeyPill(label = "|", onClick = { send(KeyEvent.KEYCODE_BACKSLASH, shift = true) })
                KeyPill(label = "~", onClick = { send(KeyEvent.KEYCODE_GRAVE, shift = true) })
            }
            VerticalDivider(
                modifier = Modifier.padding(horizontal = 6.dp).height(20.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KeyIconButton(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.code_server_copy),
                    onClick = onCopy,
                )
                KeyIconButton(
                    imageVector = Icons.Outlined.ContentPaste,
                    contentDescription = stringResource(R.string.code_server_paste),
                    onClick = onPaste,
                )
                KeyIconButton(
                    imageVector = Icons.Outlined.Keyboard,
                    contentDescription = stringResource(R.string.code_server_show_keyboard),
                    onClick = onShowKeyboard,
                )
            }
        }
    }
}

@Composable
private fun KeyIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier =
                Modifier
                    .size(width = 38.dp, height = 32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Box(
            modifier =
                Modifier
                    .padding(top = 2.dp)
                    .width(16.dp)
                    .height(2.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeyPill(
    label: String,
    onClick: () -> Unit,
    active: Boolean = false,
    locked: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(9.dp),
            color =
                when {
                    locked -> MaterialTheme.colorScheme.primary
                    active -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                },
            contentColor =
                when {
                    locked -> MaterialTheme.colorScheme.onPrimary
                    active -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                },
            modifier =
                Modifier
                    .height(32.dp)
                    .widthIn(min = 38.dp)
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onClick,
                        onLongClick = onLongClick,
                    ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp).fillMaxHeight(),
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
                    .padding(top = 2.dp)
                    .width(16.dp)
                    .height(2.dp)
                    .background(
                        if (locked) MaterialTheme.colorScheme.primary else Color.Transparent,
                        RoundedCornerShape(1.dp),
                    ),
        )
    }
}
