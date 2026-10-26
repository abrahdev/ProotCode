package dev.prootcode.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.prootcode.app.R
import dev.prootcode.app.spike.SpikeReportWriter
import dev.prootcode.app.spike.SpikeRunner
import dev.prootcode.app.spike.StepResult
import dev.prootcode.app.spike.StepStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SpikeScreen(
    autoRun: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val steps = remember { mutableStateListOf<StepResult>() }
    var running by remember { mutableStateOf(false) }
    var reportPath by remember { mutableStateOf<String?>(null) }
    var passed by remember { mutableStateOf<Boolean?>(null) }

    fun start() {
        if (running) return
        running = true
        passed = null
        steps.clear()
        scope.launch(Dispatchers.IO) {
            val report = SpikeRunner(context.applicationContext).run { step -> steps.add(step) }
            val file = SpikeReportWriter.write(context.applicationContext, report)
            reportPath = file.absolutePath
            passed = report.passed
            running = false
        }
    }

    LaunchedEffect(autoRun) {
        if (autoRun) start()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.spike_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.spike_subtitle),
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(
            onClick = { start() },
            enabled = !running,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text =
                    if (running) {
                        stringResource(R.string.spike_running)
                    } else {
                        stringResource(R.string.spike_run)
                    },
            )
        }
        passed?.let { passedNow ->
            Text(
                text =
                    if (passedNow) {
                        stringResource(R.string.spike_result_pass)
                    } else {
                        stringResource(R.string.spike_result_fail)
                    },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        reportPath?.let { path ->
            Text(
                text = path,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items = steps, key = { it.id }) { step ->
                StepCard(step)
            }
        }
    }
}

@Composable
private fun StepCard(step: StepResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = step.status.name,
                    style = MaterialTheme.typography.labelMedium,
                    color =
                        when (step.status) {
                            StepStatus.PASS -> MaterialTheme.colorScheme.primary
                            StepStatus.FAIL -> MaterialTheme.colorScheme.error
                            StepStatus.SKIP -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
                Text(
                    text = "  ${step.title}  (${step.durationMs} ms)",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            Text(
                text = step.detail,
                style = MaterialTheme.typography.bodySmall,
            )
            if (step.output.isNotBlank()) {
                Text(
                    text = step.output.take(MAX_OUTPUT_CHARS),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

private const val MAX_OUTPUT_CHARS = 1500
