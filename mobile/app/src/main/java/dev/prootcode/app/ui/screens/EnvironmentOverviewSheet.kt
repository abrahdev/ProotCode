package dev.prootcode.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.prootcode.app.R
import dev.prootcode.app.ui.components.SessionList
import dev.prootcode.app.ui.components.SessionRow
import dev.prootcode.app.ui.components.SheetScaffold
import dev.prootcode.app.ui.components.StatusCardGrid

@Composable
fun EnvironmentOverviewSheet(
    name: String,
    status: String,
    uptime: String,
    ram: String,
    storage: String,
    sessions: List<SessionRow>,
    onCloseSession: (Int) -> Unit,
    onStopAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmStop by remember { mutableStateOf(false) }
    SheetScaffold(
        title = stringResource(R.string.sheet_overview_title),
        onDismiss = onDismiss,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
        )
        StatusCardGrid(
            cards =
                listOf(
                    stringResource(R.string.overview_status) to status,
                    stringResource(R.string.overview_uptime) to uptime,
                    stringResource(R.string.overview_ram) to ram,
                    stringResource(R.string.overview_storage) to storage,
                ),
        )
        Text(
            text = stringResource(R.string.overview_sessions),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        if (sessions.isEmpty()) {
            Text(
                text = stringResource(R.string.overview_no_sessions),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            SessionList(
                sessions = sessions,
                onCloseSession = onCloseSession,
            )
        }
        if (confirmStop) {
            Text(
                text = stringResource(R.string.stop_all_text),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { confirmStop = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onStopAll,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                ) {
                    Text(stringResource(R.string.action_stop))
                }
            }
        } else {
            Button(
                onClick = { confirmStop = true },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_stop_all))
            }
        }
    }
}
