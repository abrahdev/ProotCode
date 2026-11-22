package dev.prootcode.app.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import dev.prootcode.app.R
import dev.prootcode.app.data.preferences.AppPreferences
import dev.prootcode.app.data.preferences.LinkOpenBehavior
import dev.prootcode.app.ui.components.SettingRow
import dev.prootcode.app.ui.components.SettingsGroup
import dev.prootcode.app.ui.components.SheetScaffold
import dev.prootcode.app.ui.components.SwitchRow

@Composable
fun PreferencesSheet(
    preferences: AppPreferences,
    onDismiss: () -> Unit,
    onTheme: () -> Unit,
    onAccessibility: () -> Unit,
) {
    val linkBehavior by preferences.linkBehavior.collectAsState()

    SheetScaffold(
        title = stringResource(R.string.sheet_preferences_title),
        onDismiss = onDismiss,
        scrollable = false,
    ) {
        SettingsGroup(title = stringResource(R.string.settings_general)) {
            SettingRow(
                title = stringResource(R.string.settings_theme),
                subtitle = stringResource(R.string.preferences_theme_sub),
                onClick = onTheme,
            )
            SettingRow(
                title = stringResource(R.string.sheet_accessibility_title),
                subtitle = stringResource(R.string.preferences_accessibility_sub),
                onClick = onAccessibility,
            )
            SwitchRow(
                title = stringResource(R.string.preferences_open_links),
                subtitle = stringResource(R.string.preferences_open_links_sub),
                checked = linkBehavior == LinkOpenBehavior.IN_APP,
                onCheckedChange = { enabled ->
                    preferences.setLinkBehavior(
                        if (enabled) LinkOpenBehavior.IN_APP else LinkOpenBehavior.SYSTEM,
                    )
                },
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
