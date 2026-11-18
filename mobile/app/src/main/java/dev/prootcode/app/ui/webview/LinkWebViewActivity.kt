package dev.prootcode.app.ui.webview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.prootcode.app.ui.screens.LinkWebViewScreen
import dev.prootcode.app.ui.theme.ProotCodeAppTheme
import dev.prootcode.app.ui.util.QuestWindow

class LinkWebViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        QuestWindow.applyOrientation(this)
        enableEdgeToEdge()
        val url = intent.getStringExtra(EXTRA_URL)
        if (url.isNullOrBlank()) {
            finish()
            return
        }
        setContent {
            ProotCodeAppTheme {
                LinkWebViewScreen(
                    url = url,
                    onBack = { finish() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_URL = "url"

        fun start(
            context: Context,
            url: String,
        ) {
            val intent =
                Intent(context, LinkWebViewActivity::class.java)
                    .putExtra(EXTRA_URL, url)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            QuestWindow.start(context, intent)
        }
    }
}
