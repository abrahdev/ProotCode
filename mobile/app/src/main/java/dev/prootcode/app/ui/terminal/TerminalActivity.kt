package dev.prootcode.app.ui.terminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import dev.prootcode.app.data.environment.EnvironmentRegistry
import dev.prootcode.app.data.preferences.AppPreferences
import dev.prootcode.app.data.preferences.ThemeMode
import dev.prootcode.app.ui.theme.ProotCodeTheme
import dev.prootcode.app.ui.util.QuestWindow
import java.io.File

class TerminalActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        QuestWindow.applyOrientation(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val environmentId = intent.getStringExtra(EXTRA_ENVIRONMENT_ID)
        val environment =
            environmentId?.let { id ->
                EnvironmentRegistry(File(filesDir, "environments.json"))
                    .load()
                    .firstOrNull { it.id == id }
            }
        if (environment == null) {
            finish()
            return
        }
        val preferences = AppPreferences.create(this)
        setContent {
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
            ) {
                TerminalScreen(
                    environment = environment,
                    onBack = { finish() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_ENVIRONMENT_ID = "environment_id"
    }
}
