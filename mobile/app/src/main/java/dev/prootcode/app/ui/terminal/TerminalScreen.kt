package dev.prootcode.app.ui.terminal

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.KeyboardHide
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.termux.terminal.TerminalSession
import com.termux.terminal.TextStyle
import com.termux.view.TerminalView
import dev.prootcode.app.R
import dev.prootcode.app.data.environment.Environment
import dev.prootcode.app.data.preferences.AppPreferences
import dev.prootcode.app.data.preferences.TerminalColorTheme
import dev.prootcode.app.ui.components.AppScaffold
import dev.prootcode.app.ui.components.ClosableTabRow
import dev.prootcode.app.ui.settings.ShortcutsEditorSheet
import dev.prootcode.app.ui.theme.TerminalPalette
import dev.prootcode.app.ui.theme.terminalPalette
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    environment: Environment?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val preferences = remember { AppPreferences.create(context) }
    val themeSettings by preferences.theme.collectAsState()
    val environmentId = environment?.id ?: TerminalSessionManager.NATIVE_ENVIRONMENT_ID
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notificationDenied = stringResource(R.string.terminal_notifications_denied)
    val notificationPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                scope.launch { snackbarHostState.showSnackbar(notificationDenied) }
            }
        }

    LaunchedEffect(environmentId) {
        activity?.setTaskDescription(
            ActivityManager.TaskDescription(environment?.name ?: "Shell"),
        )
    }

    LaunchedEffect(Unit) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    BackHandler(onBack = onBack)

    AppScaffold(
        title = environment?.name ?: stringResource(R.string.home_terminal_native),
        onBack = onBack,
        snackbarHostState = snackbarHostState,
    ) { padding ->
        TerminalContent(
            environment = environment,
            environmentId = environmentId,
            palette =
                terminalPaletteFor(
                    theme = themeSettings.terminalColorTheme,
                    hue = themeSettings.customHue,
                ),
            fontFamily = themeSettings.terminalFontFamily,
            fontSize = themeSettings.terminalFontSize,
            modifier = Modifier.padding(padding).fillMaxSize(),
        )
    }
}

@Composable
fun TerminalContent(
    environment: Environment?,
    environmentId: String,
    palette: TerminalPalette,
    fontFamily: String,
    fontSize: Int,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sessions = remember(environmentId) { mutableStateListOf<TerminalSession>() }
    var selected by remember(environmentId) { mutableIntStateOf(0) }
    var terminalView by remember { mutableStateOf<TerminalView?>(null) }
    var viewClient by remember { mutableStateOf<ProotViewClient?>(null) }
    var shortcuts by remember { mutableStateOf(TerminalShortcutStore.create(context).load()) }
    var showShortcutsEditor by remember { mutableStateOf(false) }
    var keyboardLocked by remember { mutableStateOf(false) }
    val shortcutStore = remember { TerminalShortcutStore.create(context) }

    LaunchedEffect(environmentId) {
        if (TerminalSessionManager.sessions(environmentId).isEmpty()) {
            if (environment == null) {
                TerminalSessionManager.createNative(context)
            } else {
                TerminalSessionManager.create(context, environment)
            }
        }
        sessions.clear()
        sessions.addAll(TerminalSessionManager.sessions(environmentId))
        selected = sessions.lastIndex.coerceAtLeast(0)
    }

    DisposableEffect(Unit) {
        TerminalSessionManager.onScreenUpdated = { terminalView?.onScreenUpdated() }
        onDispose { TerminalSessionManager.onScreenUpdated = null }
    }

    LaunchedEffect(selected, palette, sessions.size) {
        sessions.getOrNull(selected)?.let { session ->
            applyPalette(session, palette)
            terminalView?.onScreenUpdated()
        }
    }

    fun createSession() {
        val session =
            if (environment == null) {
                TerminalSessionManager.createNative(context)
            } else {
                TerminalSessionManager.create(context, environment)
            }
        sessions.add(session)
        selected = sessions.lastIndex
    }

    fun closeSession(session: TerminalSession) {
        TerminalSessionManager.close(context, environmentId, session)
        sessions.remove(session)
        selected = selected.coerceIn(0, sessions.lastIndex.coerceAtLeast(0))
    }

    Column(modifier = modifier.fillMaxSize().imePadding()) {
        ClosableTabRow(
            titles = sessions.mapIndexed { index, session -> session.mSessionName ?: "Terminal ${index + 1}" },
            selected = selected,
            onSelect = { selected = it },
            onClose = { index -> sessions.getOrNull(index)?.let { closeSession(it) } },
            onNew = { createSession() },
            actions = {
                IconButton(
                    onClick = {
                        keyboardLocked = !keyboardLocked
                        if (keyboardLocked) hideKeyboard(context, terminalView)
                    },
                ) {
                    Icon(
                        imageVector =
                            if (keyboardLocked) {
                                Icons.Outlined.KeyboardHide
                            } else {
                                Icons.Outlined.Keyboard
                            },
                        contentDescription =
                            stringResource(
                                if (keyboardLocked) {
                                    R.string.terminal_keyboard_unlock
                                } else {
                                    R.string.terminal_keyboard_lock
                                },
                            ),
                    )
                }
            },
        )
        AndroidView(
            factory = { ctx ->
                TerminalView(ctx, null)
                    .apply {
                        isFocusable = true
                        isFocusableInTouchMode = true
                        pointerIcon = android.view.PointerIcon.getSystemIcon(ctx, android.view.PointerIcon.TYPE_TEXT)
                        val client = ProotViewClient { if (!keyboardLocked) showKeyboard(ctx, this) }
                        viewClient = client
                        setTerminalViewClient(client)
                        applyFont(this, fontFamily, fontSize)
                        sessions.getOrNull(selected)?.let { session ->
                            attachSession(session)
                            applyPalette(session, palette)
                        }
                        requestFocus()
                        if (!keyboardLocked) post { showKeyboard(ctx, this) }
                    }.also { terminalView = it }
            },
            update = { view ->
                val session = sessions.getOrNull(selected)
                if (session != null && view.mTermSession !== session) {
                    view.attachSession(session)
                    applyPalette(session, palette)
                    view.requestFocus()
                }
                applyFont(view, fontFamily, fontSize)
            },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
        ExtraKeysRow(
            client = viewClient,
            session = sessions.getOrNull(selected),
            shortcuts = shortcuts,
            onCustomize = { showShortcutsEditor = true },
        )
    }

    if (showShortcutsEditor) {
        ShortcutsEditorSheet(
            store = shortcutStore,
            onDismiss = {
                showShortcutsEditor = false
                shortcuts = shortcutStore.load()
            },
        )
    }
}

private fun applyPalette(
    session: TerminalSession,
    palette: TerminalPalette,
) {
    val emulator = session.emulator ?: return
    emulator.mColors.mCurrentColors[TextStyle.COLOR_INDEX_BACKGROUND] = palette.background.toArgb()
    emulator.mColors.mCurrentColors[TextStyle.COLOR_INDEX_FOREGROUND] = palette.foreground.toArgb()
    emulator.mColors.mCurrentColors[TextStyle.COLOR_INDEX_CURSOR] = palette.prompt.toArgb()
}

private fun applyFont(
    view: TerminalView,
    family: String,
    size: Int,
) {
    if (family.isNotBlank()) {
        runCatching {
            view.setTypeface(android.graphics.Typeface.create(family, android.graphics.Typeface.NORMAL))
        }
    }
    val scaled = (size * view.resources.displayMetrics.scaledDensity).roundToInt()
    view.setTextSize(scaled)
}

internal fun terminalPaletteFor(
    theme: TerminalColorTheme,
    hue: Int,
): TerminalPalette =
    when (theme) {
        TerminalColorTheme.SYSTEM, TerminalColorTheme.DARK -> terminalPalette(hue)
        TerminalColorTheme.LIGHT ->
            TerminalPalette(
                background = Color.hsl(hue.toFloat(), 0.20f, 0.98f),
                foreground = Color.hsl(hue.toFloat(), 0.15f, 0.10f),
                prompt = Color.hsl(hue.toFloat(), 0.50f, 0.36f),
                inputBackground = Color.hsl(hue.toFloat(), 0.18f, 0.94f),
            )
        TerminalColorTheme.SOLARIZED ->
            TerminalPalette(
                background = Color(0xFF002B36),
                foreground = Color(0xFF839496),
                prompt = Color(0xFFB58900),
                inputBackground = Color(0xFF073642),
            )
    }

private fun showKeyboard(
    context: Context,
    view: TerminalView,
) {
    view.requestFocus()
    val imm = context.getSystemService(InputMethodManager::class.java)
    imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
}

private fun hideKeyboard(
    context: Context,
    view: TerminalView?,
) {
    if (view == null) return
    val imm = context.getSystemService(InputMethodManager::class.java)
    imm?.hideSoftInputFromWindow(view.windowToken, 0)
}
