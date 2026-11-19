package dev.prootcode.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.prootcode.app.data.preferences.AppPreferences
import dev.prootcode.app.data.preferences.ThemeMode

@Composable
fun ProotCodeAppTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val preferences = remember { AppPreferences.create(context) }
    val settings by preferences.theme.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val darkTheme =
        when (settings.themeMode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
    ProotCodeTheme(
        darkTheme = darkTheme,
        dynamicColor = settings.dynamicColor,
        customHue = settings.customHue,
        hardBackground = settings.hardBackground,
        content = content,
    )
}
