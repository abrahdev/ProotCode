package dev.prootcode.app.ui.screens

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.prootcode.app.R
import dev.prootcode.app.data.environment.Environment
import dev.prootcode.app.runtime.codeserver.CodeServerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private data class LogSource(
    val label: String,
    val file: File,
)

@Composable
fun LogViewerContent(environment: Environment) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var selected by remember(environment.id) { mutableIntStateOf(0) }

    val sources by
        produceState(initialValue = emptyList<LogSource>(), environment.id, refresh) {
            value = withContext(Dispatchers.IO) { logSources(context, environment.id) }
        }
    LaunchedEffect(sources.size) {
        if (selected !in sources.indices) selected = 0
    }
    val lines by
        produceState(initialValue = emptyList<String>(), sources, selected, refresh) {
            value =
                withContext(Dispatchers.IO) {
                    sources
                        .getOrNull(selected)
                        ?.file
                        ?.let(::readLogLines)
                        .orEmpty()
                }
        }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.config_logs_sub),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.logs_lines, LOG_LINE_LIMIT),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { refresh += 1 }) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = stringResource(R.string.logs_reload),
                )
            }
        }
        if (sources.isEmpty()) {
            Text(
                text = stringResource(R.string.logs_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                sources.forEachIndexed { index, source ->
                    val current = index == selected
                    AssistChip(
                        onClick = { selected = index },
                        label = { Text(source.label) },
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
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = lines.joinToString("\n").ifBlank { stringResource(R.string.logs_empty) },
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                )
            }
        }
    }
}

private fun logSources(
    context: Context,
    environmentId: String,
): List<LogSource> =
    buildList {
        val codeServer = CodeServerManager.logFile(context, environmentId)
        if (codeServer.exists()) {
            add(LogSource(CODE_SERVER_LABEL, codeServer))
        }
        val installLog = File(context.filesDir, "environments/$environmentId/install.log")
        if (installLog.exists()) {
            add(LogSource("install", installLog))
        }
        val logDir = File(context.filesDir, "environments/$environmentId/rootfs/var/log")
        logDir
            .walkTopDown()
            .maxDepth(2)
            .filter { it.isFile && it.length() <= MAX_LOG_BYTES }
            .sortedBy { it.relativeTo(logDir).path }
            .take(MAX_LOG_FILES)
            .forEach { file -> add(LogSource(file.relativeTo(logDir).path, file)) }
    }

private fun readLogLines(
    file: File,
    maxLines: Int = LOG_LINE_LIMIT,
): List<String> = runCatching { file.readLines().takeLast(maxLines) }.getOrDefault(emptyList())

private const val LOG_LINE_LIMIT = 200
private const val MAX_LOG_FILES = 8
private const val MAX_LOG_BYTES = 2L * 1024L * 1024L
private const val CODE_SERVER_LABEL = "code-server"
