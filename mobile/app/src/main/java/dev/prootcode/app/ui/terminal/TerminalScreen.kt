package dev.prootcode.app.ui.terminal

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.termux.terminal.TerminalSession
import com.termux.terminal.TextStyle
import com.termux.view.TerminalView
import dev.prootcode.app.R
import dev.prootcode.app.data.environment.Environment
import dev.prootcode.app.data.preferences.AppPreferences
import dev.prootcode.app.data.preferences.TerminalColorTheme
import dev.prootcode.app.ui.components.ClosableTabRow
import dev.prootcode.app.ui.settings.ShortcutsEditorSheet
import dev.prootcode.app.ui.theme.TerminalPalette
import dev.prootcode.app.ui.theme.terminalPalette
import dev.prootcode.app.ui.util.KeyboardUtils
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun TerminalScreen(
    environment: Environment?,
    darkTheme: Boolean,
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        TerminalContent(
            environment = environment,
            environmentId = environmentId,
            palette =
                terminalPaletteFor(
                    theme = themeSettings.terminalColorTheme,
                    hue = themeSettings.customHue,
                    darkTheme = darkTheme,
                    dynamicColor = themeSettings.dynamicColor,
                    colorScheme = MaterialTheme.colorScheme,
                ),
            fontFamily = themeSettings.terminalFontFamily,
            fontSize = themeSettings.terminalFontSize,
            onBack = onBack,
            contentPadding = padding,
            snackbarHostState = snackbarHostState,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TerminalContent(
    environment: Environment?,
    environmentId: String,
    palette: TerminalPalette,
    fontFamily: String,
    fontSize: Int,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    snackbarHostState: SnackbarHostState,
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
    val keyboardVisible = WindowInsets.isImeVisible
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val navBottom = WindowInsets.navigationBars.getBottom(density)
    val imeExtra = with(density) { (imeBottom - navBottom).coerceAtLeast(0).toDp() }
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

    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    DisposableEffect(Unit) {
        TerminalSessionManager.onScreenUpdated = { terminalView?.onScreenUpdated() }
        TerminalSessionManager.onSessionFinished = { finishedEnvironmentId, session ->
            mainHandler.post {
                if (finishedEnvironmentId == environmentId) {
                    TerminalSessionManager.sessionExited(context, finishedEnvironmentId, session)
                    sessions.remove(session)
                    if (sessions.isEmpty()) {
                        onBack()
                    } else {
                        selected = selected.coerceIn(0, sessions.lastIndex)
                    }
                }
            }
        }
        onDispose {
            TerminalSessionManager.onScreenUpdated = null
            TerminalSessionManager.onSessionFinished = null
        }
    }

    LaunchedEffect(palette, sessions.size) {
        sessions.forEach { applyPalette(it, palette) }
        terminalView?.onScreenUpdated()
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

    fun switchSession(toNext: Boolean) {
        if (sessions.size < 2) return
        val step = if (toNext) 1 else -1
        selected = (selected + step + sessions.size) % sessions.size
    }

    fun closeSession(session: TerminalSession) {
        val index = sessions.indexOf(session)
        if (index < 0) return
        TerminalSessionManager.close(context, environmentId, session)
        sessions.removeAt(index)
        selected =
            when {
                sessions.isEmpty() -> 0
                index < selected -> selected - 1
                index == selected -> index.coerceAtMost(sessions.lastIndex)
                else -> selected.coerceAtMost(sessions.lastIndex)
            }
    }

    fun copySelection() {
        val view = terminalView ?: return
        val text = view.selectedText ?: view.storedSelectedText
        if (text.isNullOrBlank()) {
            Toast.makeText(context, R.string.terminal_copy_empty, Toast.LENGTH_SHORT).show()
        } else {
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            clipboard?.setPrimaryClip(ClipData.newPlainText("terminal", text))
        }
    }

    fun pasteFromClipboard() {
        val session = sessions.getOrNull(selected) ?: return
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val text = clipboard
            ?.primaryClip
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
        if (text.isNullOrEmpty()) return
        session.emulator?.paste(text)
    }

    val preferences = remember { AppPreferences.create(context) }
    val externalKeyboardNoticeDismissed by preferences.externalKeyboardNoticeDismissed.collectAsState()
    val externalKeyboardConnected = remember { KeyboardUtils.isExternalKeyboardConnected(context) }
    val keyboardNoticeMessage = stringResource(R.string.terminal_external_keyboard_notice)
    val keyboardNoticeNever = stringResource(R.string.terminal_external_keyboard_never)

    LaunchedEffect(Unit) {
        if (externalKeyboardConnected && !externalKeyboardNoticeDismissed) {
            val result =
                snackbarHostState.showSnackbar(
                    message = keyboardNoticeMessage,
                    actionLabel = keyboardNoticeNever,
                    duration = SnackbarDuration.Long,
                )
            if (result == SnackbarResult.ActionPerformed) {
                preferences.setExternalKeyboardNoticeDismissed()
            }
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(bottom = if (keyboardVisible) imeExtra else 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp).size(40.dp)) {
                Icon(
                    imageVector = Icons.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
            VerticalDivider(
                modifier = Modifier.padding(horizontal = 4.dp).height(24.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            ClosableTabRow(
                titles = sessions.mapIndexed { index, session -> session.mSessionName ?: "Terminal ${index + 1}" },
                selected = selected,
                onSelect = { selected = it },
                onClose = { index -> sessions.getOrNull(index)?.let { closeSession(it) } },
                modifier = Modifier.weight(1f),
            )
        }
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { ctx ->
                    TerminalView(ctx, null)
                        .apply {
                            isFocusable = true
                            isFocusableInTouchMode = true
                            pointerIcon =
                                android.view.PointerIcon.getSystemIcon(
                                    ctx,
                                    android.view.PointerIcon.TYPE_TEXT,
                                )
                            val client = ProotViewClient { if (!keyboardLocked) showKeyboard(ctx, this) }
                            viewClient = client
                            setTerminalViewClient(client)
                            setSwipeListener { toNext -> switchSession(toNext) }
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
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            )
            SmallFloatingActionButton(
                onClick = { createSession() },
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = stringResource(R.string.terminal_new_session_desc),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        ExtraKeysRow(
            client = viewClient,
            session = sessions.getOrNull(selected),
            shortcuts = shortcuts,
            keyboardLocked = keyboardLocked,
            onToggleKeyboardLock = {
                keyboardLocked = !keyboardLocked
                if (keyboardLocked) hideKeyboard(context, terminalView)
            },
            onCustomize = { showShortcutsEditor = true },
            onCopy = { copySelection() },
            onPaste = { pasteFromClipboard() },
            compact = keyboardVisible,
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
    emulator.mColors.mCurrentColors[PROMPT_COLOR_INDEX] = palette.prompt.toArgb()
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
    darkTheme: Boolean,
    dynamicColor: Boolean = false,
    colorScheme: ColorScheme? = null,
): TerminalPalette =
    if (dynamicColor && colorScheme != null) {
        TerminalPalette(
            background = colorScheme.surface,
            foreground = colorScheme.onSurface,
            prompt = colorScheme.primary,
            inputBackground = colorScheme.surfaceContainer,
        )
    } else {
        paletteForTheme(theme, hue, darkTheme)
    }

private fun paletteForTheme(
    theme: TerminalColorTheme,
    hue: Int,
    darkTheme: Boolean,
): TerminalPalette =
    when (theme) {
        TerminalColorTheme.SYSTEM -> terminalPalette(hue, lightMode = !darkTheme)
        TerminalColorTheme.DARK -> terminalPalette(hue)
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
