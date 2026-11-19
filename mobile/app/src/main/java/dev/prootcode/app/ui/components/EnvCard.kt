package dev.prootcode.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.prootcode.app.R

data class SessionPreview(
    val title: String,
    val lines: List<String>,
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun EnvCard(
    name: String,
    status: String,
    markText: String,
    markIcon: ImageVector?,
    installing: Boolean,
    sessions: List<SessionPreview>,
    selectedSession: Int,
    previewBackground: Color,
    previewForeground: Color,
    codeServerInstalled: Boolean,
    showButtonLabels: Boolean,
    reorderMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onSelectSession: (Int) -> Unit,
    onOpenDetail: () -> Unit,
    onLongPress: () -> Unit,
    onOpenOverview: () -> Unit,
    onOpenTerminal: () -> Unit,
    onOpenWindow: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenCodeServer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .then(
                            if (reorderMode) {
                                Modifier
                            } else {
                                Modifier.combinedClickable(
                                    onClick = onOpenDetail,
                                    onLongClick = onLongPress,
                                )
                            },
                        ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MarkBox(text = markText, icon = markIcon)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (reorderMode) {
                    ReorderControls(
                        canMoveUp = canMoveUp,
                        canMoveDown = canMoveDown,
                        onMoveUp = onMoveUp,
                        onMoveDown = onMoveDown,
                    )
                } else {
                    IconButton(onClick = onOpenOverview) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = stringResource(R.string.home_more),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (installing) {
                ExpressiveLinearProgressIndicator(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    EnvActionButton(
                        icon = Icons.Outlined.Terminal,
                        label =
                            if (showButtonLabels) {
                                stringResource(R.string.home_action_terminal)
                            } else {
                                null
                            },
                        filled = true,
                        enabled = !reorderMode,
                        onClick = onOpenTerminal,
                    )
                    EnvActionButton(
                        icon = Icons.Outlined.OpenInNew,
                        label =
                            if (showButtonLabels) {
                                stringResource(R.string.home_action_window)
                            } else {
                                null
                            },
                        enabled = !reorderMode,
                        onClick = onOpenWindow,
                    )
                    EnvActionButton(
                        icon = Icons.Outlined.Folder,
                        label = null,
                        enabled = !reorderMode,
                        onClick = onOpenFiles,
                    )
                    if (codeServerInstalled) {
                        EnvActionButton(
                            icon = Icons.Outlined.Code,
                            label = null,
                            enabled = !reorderMode,
                            onClick = onOpenCodeServer,
                        )
                    }
                }
            }

            if (!installing && sessions.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        sessions.forEachIndexed { index, session ->
                            FilterChip(
                                selected = index == selectedSession,
                                onClick = { onSelectSession(index) },
                                label = { Text(session.title) },
                            )
                        }
                        FilterChip(
                            selected = selectedSession < 0,
                            onClick = { onSelectSession(-1) },
                            label = { Text(stringResource(R.string.wizard_none)) },
                        )
                    }
                    val preview = sessions.getOrNull(selectedSession)
                    if (preview != null) {
                        TerminalPreviewBox(
                            lines = preview.lines,
                            background = previewBackground,
                            foreground = previewForeground,
                            onClick = onOpenTerminal,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            if (reorderMode) {
                Text(
                    text = stringResource(R.string.home_reorder_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
