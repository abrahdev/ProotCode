package dev.prootcode.app.ui.settings

import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import dev.prootcode.app.R
import dev.prootcode.app.data.preferences.AppPreferences
import dev.prootcode.app.data.preferences.ThemeMode
import dev.prootcode.app.ui.components.ChoiceChipRow
import dev.prootcode.app.ui.components.HueWheelPicker
import dev.prootcode.app.ui.components.SettingRow
import dev.prootcode.app.ui.components.SettingsGroup
import dev.prootcode.app.ui.components.SheetScaffold

@Composable
fun ThemeSheet(
    preferences: AppPreferences,
    onDismiss: () -> Unit,
    onAbout: () -> Unit,
) {
    val settings by preferences.theme.collectAsState()
    SheetScaffold(
        title = stringResource(R.string.settings_theme),
        onDismiss = onDismiss,
    ) {
        SectionLabel(stringResource(R.string.theme_color))
        ChoiceChipRow(
            options =
                listOf(
                    stringResource(R.string.theme_dynamic),
                    stringResource(R.string.theme_color_pick),
                ),
            selected = if (settings.dynamicColor) 0 else 1,
            onSelected = { index -> preferences.setDynamicColor(index == 0) },
        )
        if (!settings.dynamicColor) {
            HueWheelPicker(
                hue = settings.customHue,
                onHueChange = { preferences.setCustomHue(it) },
            )
        }

        SectionLabel(stringResource(R.string.theme_background))
        ChoiceChipRow(
            options =
                listOf(
                    stringResource(R.string.theme_dynamic),
                    stringResource(R.string.theme_hard),
                ),
            selected = if (settings.hardBackground) 1 else 0,
            onSelected = { preferences.setHardBackground(it == 1) },
        )

        SectionLabel(stringResource(R.string.settings_theme))
        SettingsGroup(title = null) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                SettingRow(
                    title =
                        stringResource(
                            when (mode) {
                                ThemeMode.SYSTEM -> R.string.settings_theme_system
                                ThemeMode.LIGHT -> R.string.settings_theme_light
                                ThemeMode.DARK -> R.string.settings_theme_dark
                            },
                        ),
                    onClick = { preferences.setThemeMode(mode) },
                    showDivider = index != ThemeMode.entries.lastIndex,
                    trailing = {
                        RadioButton(
                            selected = settings.themeMode == mode,
                            onClick = { preferences.setThemeMode(mode) },
                        )
                    },
                )
            }
        }

        TextButton(onClick = onAbout) {
            Text(stringResource(R.string.settings_about))
        }
    }
}
