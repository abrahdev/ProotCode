package dev.prootcode.app.ui.terminal

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView
import dev.prootcode.app.R
import dev.prootcode.app.data.environment.Environment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    environment: Environment,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sessions = remember { mutableStateListOf<TerminalSession>() }
    var selected by remember { mutableStateOf(0) }
    var terminalView by remember { mutableStateOf<TerminalView?>(null) }
    var viewClient by remember { mutableStateOf<ProotViewClient?>(null) }
    val keysConfig =
        remember {
            ExtraKeysConfig(context.getSharedPreferences("extra_keys", Context.MODE_PRIVATE))
        }
    val notificationPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val activity = context as? Activity

    LaunchedEffect(environment.id) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (TerminalSessionManager.sessions(environment.id).isEmpty()) {
            TerminalSessionManager.create(context, environment)
        }
        sessions.clear()
        sessions.addAll(TerminalSessionManager.sessions(environment.id))
        selected = sessions.lastIndex.coerceAtLeast(0)
    }

    DisposableEffect(Unit) {
        TerminalSessionManager.onScreenUpdated = { terminalView?.onScreenUpdated() }
        onDispose { TerminalSessionManager.onScreenUpdated = null }
    }

    val windowTitle =
        sessions.getOrNull(selected)?.mSessionName?.let { "${environment.name} · $it" }
            ?: environment.name
    LaunchedEffect(windowTitle) {
        activity?.setTaskDescription(ActivityManager.TaskDescription(windowTitle))
    }

    BackHandler(onBack = onBack)

    fun createSession() {
        sessions.add(TerminalSessionManager.create(context, environment))
        selected = sessions.lastIndex
    }

    fun closeSession(session: TerminalSession) {
        TerminalSessionManager.close(context, environment.id, session)
        sessions.remove(session)
        if (sessions.isEmpty()) {
            onBack()
        } else {
            selected = selected.coerceIn(0, sessions.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(windowTitle) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.terminal_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TabRow(
                    selectedTabIndex = selected,
                    modifier = Modifier.weight(1f),
                ) {
                    sessions.forEachIndexed { index, session ->
                        Tab(
                            selected = index == selected,
                            onClick = { selected = index },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(session.mSessionName ?: "Terminal ${index + 1}")
                                    TextButton(onClick = { closeSession(session) }) {
                                        Text(stringResource(R.string.terminal_close_session))
                                    }
                                }
                            },
                        )
                    }
                }
                TextButton(onClick = { createSession() }) {
                    Text(stringResource(R.string.terminal_new_session))
                }
            }

            AndroidView(
                factory = { ctx ->
                    TerminalView(ctx, null)
                        .apply {
                            isFocusable = true
                            isFocusableInTouchMode = true
                            val client = ProotViewClient { showKeyboard(ctx, this) }
                            viewClient = client
                            setTerminalViewClient(client)
                            setTextSize(TEXT_SIZE)
                            sessions.getOrNull(selected)?.let { attachSession(it) }
                            requestFocus()
                            post { showKeyboard(ctx, this) }
                        }.also { terminalView = it }
                },
                update = { view ->
                    val session = sessions.getOrNull(selected)
                    if (session != null && view.mTermSession !== session) {
                        view.attachSession(session)
                        view.requestFocus()
                    }
                },
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
            )

            ExtraKeysRow(
                client = viewClient,
                session = sessions.getOrNull(selected),
                config = keysConfig,
            )
        }
    }
}

private fun showKeyboard(
    context: Context,
    view: TerminalView,
) {
    view.requestFocus()
    val imm = context.getSystemService(InputMethodManager::class.java)
    imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
}

private const val TEXT_SIZE = 20
