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
    private var currentTarget: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        QuestWindow.applyOrientation(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        currentTarget = targetKey(intent)
        val guestUrl = intent.getStringExtra(EXTRA_GUEST_URL)
        val guestPassword = intent.getStringExtra(EXTRA_GUEST_PASSWORD)
        if (!guestUrl.isNullOrBlank()) {
            setContent {
                ProotCodeAppTheme {
                    GuestCodeServerScreen(
                        guestUrl = guestUrl,
                        guestPassword = guestPassword,
                        onBack = { finish() },
                    )
                }
            }
            return
        }
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val target = targetKey(intent)
        if (target != currentTarget) {
            currentTarget = target
            recreate()
        }
    }

    private fun targetKey(intent: Intent?): String? {
        val guestUrl = intent?.getStringExtra(EXTRA_GUEST_URL)
        if (!guestUrl.isNullOrBlank()) return "guest:$guestUrl"
        val environmentId =
            intent?.getStringExtra(EXTRA_ENVIRONMENT_ID)
                ?: intent?.data?.lastPathSegment
        return environmentId?.let { "environment:$it" }
    }

    companion object {
        const val EXTRA_ENVIRONMENT_ID = "environment_id"
        const val EXTRA_GUEST_URL = "guest_url"
        const val EXTRA_GUEST_PASSWORD = "guest_password"

        fun start(
            context: Context,
            environmentId: String,
        ) {
            val intent =
                Intent(context, CodeServerActivity::class.java)
                    .putExtra(EXTRA_ENVIRONMENT_ID, environmentId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            QuestWindow.start(context, intent)
        }

        fun startGuest(
            context: Context,
            url: String,
            password: String,
        ) {
            val intent =
                Intent(context, CodeServerActivity::class.java)
                    .putExtra(EXTRA_GUEST_URL, url)
                    .putExtra(EXTRA_GUEST_PASSWORD, password)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            QuestWindow.start(context, intent)
        }
    }
}
