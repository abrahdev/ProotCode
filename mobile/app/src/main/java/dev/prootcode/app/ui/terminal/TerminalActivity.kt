package dev.prootcode.app.ui.terminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.prootcode.app.data.environment.EnvironmentRegistry
import dev.prootcode.app.ui.theme.ProotCodeTheme
import java.io.File

class TerminalActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
        setContent {
            ProotCodeTheme {
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
