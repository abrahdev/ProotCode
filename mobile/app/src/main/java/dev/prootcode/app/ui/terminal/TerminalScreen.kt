package dev.prootcode.app.ui.terminal

import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.view.TerminalView
import dev.prootcode.app.R
import dev.prootcode.app.data.environment.Environment

private const val TEXT_SIZE = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    environment: Environment,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var terminalView by remember { mutableStateOf<TerminalView?>(null) }
    val session =
        remember(environment.id) {
            TerminalSessionFactory(context).create(environment) {
                terminalView?.onScreenUpdated()
            }
        }

    DisposableEffect(session) {
        onDispose { session.finishIfRunning() }
    }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(environment.name) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.terminal_back))
                    }
                },
            )
        },
    ) { padding ->
        AndroidView(
            factory = { ctx ->
                TerminalView(ctx, null)
                    .apply {
                        isFocusable = true
                        isFocusableInTouchMode = true
                        setTerminalViewClient(
                            ProotViewClient {
                                requestFocus()
                                ctx.getSystemService(InputMethodManager::class.java)
                                    ?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                            },
                        )
                        setTextSize(TEXT_SIZE)
                        attachSession(session)
                        requestFocus()
                        post {
                            ctx.getSystemService(InputMethodManager::class.java)
                                ?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                        }
                    }.also { terminalView = it }
            },
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(padding),
        )
    }
}
