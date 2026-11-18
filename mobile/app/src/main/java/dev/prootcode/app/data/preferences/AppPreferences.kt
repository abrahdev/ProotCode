package dev.prootcode.app.data.preferences

import android.content.Context
import android.content.SharedPreferences
import dev.prootcode.app.ui.theme.Hue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class TerminalColorTheme {
    SYSTEM,
    DARK,
    LIGHT,
    SOLARIZED,
}

data class AppThemeSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val customHue: Int = Hue.DEFAULT,
    val hardBackground: Boolean = false,
    val showTerminalButtonLabels: Boolean = false,
    val terminalColorTheme: TerminalColorTheme = TerminalColorTheme.SYSTEM,
    val terminalFontSize: Int = 14,
    val terminalFontFamily: String = "",
)

class AppPreferences(
    private val preferences: SharedPreferences,
) {
    private val state = MutableStateFlow(read())

    val theme: StateFlow<AppThemeSettings> = state.asStateFlow()

    fun refresh() {
        state.value = read()
    }

    fun setThemeMode(mode: ThemeMode) {
        preferences.edit().putString(KEY_THEME_MODE, mode.name).apply()
        refresh()
    }

    fun setDynamicColor(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
        refresh()
    }

    fun setCustomHue(hue: Int) {
        preferences.edit().putInt(KEY_CUSTOM_HUE, hue.coerceIn(Hue.MIN, Hue.MAX)).apply()
        refresh()
    }

    fun setHardBackground(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_HARD_BACKGROUND, enabled).apply()
        refresh()
    }

    fun setShowTerminalButtonLabels(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_SHOW_BUTTON_LABELS, enabled).apply()
        refresh()
    }

    fun setTerminalColorTheme(theme: TerminalColorTheme) {
        preferences.edit().putString(KEY_TERMINAL_COLOR_THEME, theme.name).apply()
        refresh()
    }

    fun setTerminalFontSize(size: Int) {
        preferences.edit().putInt(KEY_TERMINAL_FONT_SIZE, size.coerceIn(8, 32)).apply()
        refresh()
    }

    fun setTerminalFontFamily(family: String) {
        preferences.edit().putString(KEY_TERMINAL_FONT_FAMILY, family.trim()).apply()
        refresh()
    }

    private fun read(): AppThemeSettings =
        AppThemeSettings(
            themeMode =
                preferences
                    .getString(KEY_THEME_MODE, null)
                    ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.SYSTEM,
            dynamicColor = preferences.getBoolean(KEY_DYNAMIC_COLOR, true),
            customHue = preferences.getInt(KEY_CUSTOM_HUE, Hue.DEFAULT),
            hardBackground = preferences.getBoolean(KEY_HARD_BACKGROUND, false),
            showTerminalButtonLabels = preferences.getBoolean(KEY_SHOW_BUTTON_LABELS, false),
            terminalColorTheme =
                preferences
                    .getString(KEY_TERMINAL_COLOR_THEME, null)
                    ?.let { runCatching { TerminalColorTheme.valueOf(it) }.getOrNull() }
                    ?: TerminalColorTheme.SYSTEM,
            terminalFontSize = preferences.getInt(KEY_TERMINAL_FONT_SIZE, 14),
            terminalFontFamily = preferences.getString(KEY_TERMINAL_FONT_FAMILY, "").orEmpty(),
        )

    companion object {
        private const val FILE = "app_preferences"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_CUSTOM_HUE = "custom_hue"
        private const val KEY_HARD_BACKGROUND = "hard_background"
        private const val KEY_SHOW_BUTTON_LABELS = "show_terminal_button_labels"
        private const val KEY_TERMINAL_COLOR_THEME = "terminal_color_theme"
        private const val KEY_TERMINAL_FONT_SIZE = "terminal_font_size"
        private const val KEY_TERMINAL_FONT_FAMILY = "terminal_font_family"

        fun create(context: Context): AppPreferences =
            AppPreferences(context.getSharedPreferences(FILE, Context.MODE_PRIVATE))
    }
}
