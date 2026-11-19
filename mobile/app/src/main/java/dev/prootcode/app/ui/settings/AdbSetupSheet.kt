package dev.prootcode.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.prootcode.app.R
import dev.prootcode.app.ui.components.LabeledTextField
import dev.prootcode.app.ui.components.ProgressSpinner
import dev.prootcode.app.ui.components.SheetScaffold
import dev.prootcode.app.ui.components.StepIndicator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AdbSetupSheet(
    connected: Boolean,
    onConnectedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onMessage: (String) -> Unit,
) {
    var step by remember { mutableIntStateOf(if (connected) STEP_CONNECTED else STEP_ENABLE) }
    var code by remember { mutableStateOf("") }
    var pairAddress by remember { mutableStateOf("") }
    var connectAddress by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val steps =
        listOf(
            stringResource(R.string.adb_step_enable),
            stringResource(R.string.adb_step_pair),
            stringResource(R.string.adb_step_connect),
        )
    val fillFields = stringResource(R.string.adb_fill_fields)
    val pairingSimulated = stringResource(R.string.adb_pairing_simulated)
    val connecting = stringResource(R.string.adb_connecting_simulated)
    val devOptionsMessage = stringResource(R.string.adb_open_dev_options)

    SheetScaffold(
        title = stringResource(R.string.sheet_adb_title),
        onDismiss = onDismiss,
        footer = {
            TextButton(
                onClick = {
                    if (step == STEP_ENABLE || step == STEP_CONNECTED) {
                        onDismiss()
                    } else {
                        step -= 1
                    }
                },
            ) {
                Text(
                    stringResource(
                        if (step == STEP_ENABLE || step == STEP_CONNECTED) {
                            R.string.action_cancel
                        } else {
                            R.string.action_back
                        },
                    ),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Button(
                enabled = !working,
                onClick = {
                    when (step) {
                        STEP_ENABLE -> step = STEP_PAIR
                        STEP_PAIR -> {
                            if (code.isBlank() || pairAddress.isBlank()) {
                                onMessage(fillFields)
                            } else {
                                working = true
                                scope.launch {
                                    delay(1_200)
                                    working = false
                                    step = STEP_CONNECT
                                    onMessage(pairingSimulated)
                                }
                            }
                        }
                        STEP_CONNECT -> {
                            if (connectAddress.isBlank()) {
                                onMessage(fillFields)
                            } else {
                                working = true
                                scope.launch {
                                    delay(1_200)
                                    working = false
                                    onConnectedChange(true)
                                    step = STEP_CONNECTED
                                    onMessage(connecting)
                                }
                            }
                        }
                        else -> onDismiss()
                    }
                },
            ) {
                Text(
                    stringResource(
                        when (step) {
                            STEP_ENABLE -> R.string.action_next
                            STEP_PAIR -> R.string.adb_pair_action
                            STEP_CONNECT -> R.string.adb_connect_action
                            else -> R.string.action_done
                        },
                    ),
                )
            }
        },
    ) {
        if (step != STEP_CONNECTED) {
            StepIndicator(steps = steps, current = step)
        }

        if (step == STEP_CONNECTED) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ProgressSpinner(done = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = stringResource(R.string.adb_connected_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Text(
                    text = stringResource(R.string.adb_connected_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = {
                        onConnectedChange(false)
                        step = STEP_ENABLE
                    },
                ) {
                    Text(stringResource(R.string.adb_disconnect))
                }
            }
        } else {
            when (step) {
                STEP_ENABLE -> {
                    Text(
                        text = stringResource(R.string.adb_enable_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.adb_enable_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(onClick = { onMessage(devOptionsMessage) }) {
                        Text(stringResource(R.string.adb_open_dev_options))
                    }
                }
                STEP_PAIR -> {
                    Text(
                        text = stringResource(R.string.adb_pair_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.adb_pair_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LabeledTextField(
                        label = stringResource(R.string.adb_pairing_code),
                        value = code,
                        onValueChange = { code = it.take(6) },
                        keyboardType = KeyboardType.Number,
                    )
                    LabeledTextField(
                        label = stringResource(R.string.adb_address),
                        value = pairAddress,
                        onValueChange = { pairAddress = it },
                    )
                }
                STEP_CONNECT -> {
                    Text(
                        text = stringResource(R.string.adb_connect_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.adb_connect_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LabeledTextField(
                        label = stringResource(R.string.adb_address),
                        value = connectAddress,
                        onValueChange = { connectAddress = it },
                    )
                }
            }
            if (working) {
                ProgressSpinner(done = false)
            }
        }
    }
}

private const val STEP_ENABLE = 0
private const val STEP_PAIR = 1
private const val STEP_CONNECT = 2
private const val STEP_CONNECTED = 3
