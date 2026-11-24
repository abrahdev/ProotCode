package dev.prootcode.app.ui.components

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.prootcode.app.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs

private data class BatteryPowerState(
    val watts: Float,
    val charging: Boolean,
    val available: Boolean,
)

@Composable
fun BatteryPowerBadge(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var state by remember { mutableStateOf(BatteryPowerState(0f, false, false)) }

    LaunchedEffect(Unit) {
        while (isActive) {
            state = readBatteryPower(context)
            delay(REFRESH_MS)
        }
    }

    if (!state.available) return

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(percent = 50),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector =
                    if (state.charging) {
                        Icons.Outlined.BatteryChargingFull
                    } else {
                        Icons.Outlined.BatteryFull
                    },
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint =
                    if (state.charging) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
            Text(
                text = stringResource(R.string.battery_power_watts, state.watts),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun readBatteryPower(context: Context): BatteryPowerState {
    val manager = context.getSystemService(BatteryManager::class.java) ?: return BatteryPowerState(0f, false, false)
    val current = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
    val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val voltage = battery?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
    if (current == Int.MIN_VALUE || current == 0 || voltage <= 0) {
        return BatteryPowerState(0f, false, false)
    }
    val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val charging =
        status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    val magnitude = abs(current).toFloat() * voltage / 1_000_000_000f
    val watts = if (charging) magnitude else -magnitude
    return BatteryPowerState(watts = watts, charging = charging, available = true)
}

private const val REFRESH_MS = 1_000L
