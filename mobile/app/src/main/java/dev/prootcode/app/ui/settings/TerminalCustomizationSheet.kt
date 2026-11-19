package dev.prootcode.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import dev.prootcode.app.R
import dev.prootcode.app.data.preferences.AppPreferences
import dev.prootcode.app.data.preferences.TerminalColorTheme
import dev.prootcode.app.ui.components.ChoiceChipRow
import dev.prootcode.app.ui.components.LabeledTextField
import dev.prootcode.app.ui.components.SheetScaffold

@Composable
fun TerminalCustomizationSheet(
    preferences: AppPreferences,
    onDismiss: () -> Unit,
) {
    val settings by preferences.theme.collectAsState()
    SheetScaffold(
        title = stringResource(R.string.sheet_terminal_customize_title),
        onDismiss = onDismiss,
    ) {
        SectionLabel(stringResource(R.string.terminal_customize_palette))
        ChoiceChipRow(
            options =
                listOf(
                    stringResource(R.string.terminal_palette_system),
                    stringResource(R.string.terminal_palette_dark),
                    stringResource(R.string.terminal_palette_light),
                    stringResource(R.string.terminal_palette_solarized),
                ),
            selected = settings.terminalColorTheme.ordinal,
            onSelected = { index ->
                preferences.setTerminalColorTheme(TerminalColorTheme.entries[index])
            },
        )
        LabeledTextField(
            label = stringResource(R.string.terminal_font_family),
            value = settings.terminalFontFamily,
            onValueChange = { preferences.setTerminalFontFamily(it) },
        )
        SectionLabel(stringResource(R.string.terminal_font_size))
        ChoiceChipRow(
            options = listOf("12", "14", "16", "18"),
            selected = FONT_SIZES.indexOf(settings.terminalFontSize).coerceAtLeast(0),
            onSelected = { preferences.setTerminalFontSize(FONT_SIZES[it]) },
        )
    }
}

private val FONT_SIZES = listOf(12, 14, 16, 18)
