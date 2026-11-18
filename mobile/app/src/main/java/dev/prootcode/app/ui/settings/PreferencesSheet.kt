package dev.prootcode.app.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.prootcode.app.R
import dev.prootcode.app.ui.components.SettingRow
import dev.prootcode.app.ui.components.SettingsGroup
import dev.prootcode.app.ui.components.SheetScaffold

@Composable
fun PreferencesSheet(
    onDismiss: () -> Unit,
    onTheme: () -> Unit,
    onTerminalCustomization: () -> Unit,
    onAccessibility: () -> Unit,
    onAbout: () -> Unit,
) {
    SheetScaffold(
        title = stringResource(R.string.sheet_preferences_title),
        onDismiss = onDismiss,
        scrollable = false,
    ) {
        SettingsGroup(title = stringResource(R.string.settings_theme)) {
            SettingRow(
                title = stringResource(R.string.settings_theme),
                subtitle = stringResource(R.string.preferences_theme_sub),
                onClick = onTheme,
            )
            SettingRow(
                title = stringResource(R.string.sheet_terminal_customize_title),
                subtitle = stringResource(R.string.preferences_terminal_sub),
                onClick = onTerminalCustomization,
            )
            SettingRow(
                title = stringResource(R.string.sheet_accessibility_title),
                subtitle = stringResource(R.string.preferences_accessibility_sub),
                onClick = onAccessibility,
            )
            SettingRow(
                title = stringResource(R.string.settings_about),
                onClick = onAbout,
                showDivider = false,
            )
        }
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}
