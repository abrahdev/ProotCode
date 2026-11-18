package dev.prootcode.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import dev.prootcode.app.R
import dev.prootcode.app.data.preferences.AppPreferences
import dev.prootcode.app.ui.components.SettingsGroup
import dev.prootcode.app.ui.components.SheetScaffold
import dev.prootcode.app.ui.components.SwitchRow

@Composable
fun AccessibilitySheet(
    preferences: AppPreferences,
    onDismiss: () -> Unit,
) {
    val settings by preferences.theme.collectAsState()
    SheetScaffold(
        title = stringResource(R.string.sheet_accessibility_title),
        onDismiss = onDismiss,
        scrollable = false,
    ) {
        SettingsGroup(title = null) {
            SwitchRow(
                title = stringResource(R.string.access_show_labels),
                subtitle = stringResource(R.string.access_show_labels_sub),
                checked = settings.showTerminalButtonLabels,
                onCheckedChange = { preferences.setShowTerminalButtonLabels(it) },
            )
        }
    }
}
