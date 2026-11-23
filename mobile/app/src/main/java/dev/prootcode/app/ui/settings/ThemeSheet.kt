package dev.prootcode.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import dev.prootcode.app.R
import dev.prootcode.app.data.preferences.AppPreferences
import dev.prootcode.app.data.preferences.ThemeMode
import dev.prootcode.app.ui.components.ChoiceChipRow
import dev.prootcode.app.ui.components.HueWheelPicker
import dev.prootcode.app.ui.components.SheetScaffold

@Composable
fun ThemeSheet(
    preferences: AppPreferences,
    onDismiss: () -> Unit,
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
        ChoiceChipRow(
            options =
                listOf(
                    stringResource(R.string.settings_theme_system),
                    stringResource(R.string.settings_theme_light),
                    stringResource(R.string.settings_theme_dark),
                ),
            selected = settings.themeMode.ordinal,
            onSelected = { index -> preferences.setThemeMode(ThemeMode.entries[index]) },
        )

        SectionLabel(stringResource(R.string.terminal_font_size))
        ChoiceChipRow(
            options = FONT_SIZES.map { it.toString() },
            selected = FONT_SIZES.indexOf(settings.terminalFontSize).coerceAtLeast(0),
            onSelected = { index -> preferences.setTerminalFontSize(FONT_SIZES[index]) },
        )
    }
}

private val FONT_SIZES = listOf(12, 14, 16, 18)
