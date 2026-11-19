package dev.prootcode.app.ui.codeserver

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.prootcode.app.ui.screens.CodeServerScreen
import dev.prootcode.app.ui.theme.ProotCodeAppTheme
import dev.prootcode.app.ui.util.QuestWindow

class CodeServerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        QuestWindow.applyOrientation(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val environmentId =
            intent.getStringExtra(EXTRA_ENVIRONMENT_ID)
                ?: intent.data?.lastPathSegment
        if (environmentId.isNullOrBlank()) {
            finish()
            return
        }
        setContent {
            ProotCodeAppTheme {
                CodeServerScreen(
                    environmentId = environmentId,
                    onBack = { finish() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_ENVIRONMENT_ID = "environment_id"

        fun start(
            context: Context,
            environmentId: String,
        ) {
            val intent =
                Intent(context, CodeServerActivity::class.java)
                    .putExtra(EXTRA_ENVIRONMENT_ID, environmentId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            QuestWindow.start(context, intent)
        }
    }
}
