package dev.prootcode.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

enum class SessionLevel { WORKING, IDLE, ERROR }

data class SessionRow(
    val name: String,
    val status: String,
    val level: SessionLevel,
)

private fun levelColor(level: SessionLevel): Color =
    when (level) {
        SessionLevel.WORKING -> Color(0xFFF59E0B)
        SessionLevel.IDLE -> Color(0xFF22C55E)
        SessionLevel.ERROR -> Color(0xFFEF4444)
    }

@Composable
fun StatusCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
                .padding(14.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun StatusCardGrid(
    cards: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        cards.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { (label, value) ->
                    StatusCard(
                        label = label,
                        value = value,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun SessionListRow(
    session: SessionRow,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(7.dp)
                    .background(levelColor(session.level), CircleShape),
        )
        Text(
            text = session.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = session.status,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (onClose != null) {
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun SessionList(
    sessions: List<SessionRow>,
    onCloseSession: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.medium),
    ) {
        sessions.forEachIndexed { index, session ->
            SessionListRow(
                session = session,
                onClose = onCloseSession?.let { close -> { close(index) } },
            )
        }
    }
}
