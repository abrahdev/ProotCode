package dev.prootcode.app.ui.screens

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.os.SystemClock
import android.view.KeyEvent
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.KeyboardHide
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.prootcode.app.R
import dev.prootcode.app.runtime.codeserver.CodeServerStatus
import dev.prootcode.app.ui.codeserver.CodeServerModifierState
import dev.prootcode.app.ui.codeserver.CodeServerShortcutsBar
import dev.prootcode.app.ui.codeserver.commitModifiedText
import dev.prootcode.app.ui.components.ExpressiveCircularProgressIndicator
import dev.prootcode.app.ui.components.ExpressiveLinearProgressIndicator
import dev.prootcode.app.ui.components.SettingsGroup
import dev.prootcode.app.ui.components.SheetScaffold
import dev.prootcode.app.ui.components.SwitchRow
import dev.prootcode.app.ui.components.WebViewContainer
import dev.prootcode.app.ui.util.LinkOpener
import dev.prootcode.app.ui.util.QuestWindow
import dev.prootcode.app.ui.viewmodels.CodeServerViewModel
import org.json.JSONObject
import kotlin.math.roundToInt

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CodeServerScreen(
    environmentId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: CodeServerViewModel =
        viewModel { CodeServerViewModel(context.applicationContext, environmentId) }
    val status by viewModel.status.collectAsState()
    val environment by viewModel.environment.collectAsState()
    val loaded by viewModel.loaded.collectAsState()
    val password by viewModel.password.collectAsState()
    val lan by viewModel.lan.collectAsState()
    val lanAddress by viewModel.lanAddress.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val modifierState = remember { CodeServerModifierState() }
    var progress by remember { mutableIntStateOf(0) }
    var actionsExpanded by remember { mutableStateOf(true) }
    var textZoomPercent by remember { mutableIntStateOf(DEFAULT_ZOOM_PERCENT) }
    val layoutPreferences =
        remember { context.getSharedPreferences("code_server_layout", Context.MODE_PRIVATE) }
    val savedDockSide =
        runCatching {
            DockSide.valueOf(
                layoutPreferences.getString("dock_side_$environmentId", DockSide.RIGHT.name)
                    ?: DockSide.RIGHT.name,
            )
        }.getOrDefault(DockSide.RIGHT)
    val dockSideState = remember { mutableStateOf(savedDockSide) }
    val hasSavedPosition = remember { layoutPreferences.contains("bar_x_$environmentId") }
    var defaultPositionApplied by remember { mutableStateOf(hasSavedPosition) }
    var dragOffset by remember {
        mutableStateOf(
            Offset(
                layoutPreferences.getFloat("bar_x_$environmentId", 0f),
                layoutPreferences.getFloat("bar_y_$environmentId", 0f),
            ),
        )
    }
    var barSize by remember { mutableStateOf(IntSize.Zero) }
    var isDragging by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val runningPort = (status as? CodeServerStatus.Running)?.port

    LaunchedEffect(actionsExpanded, isDragging) {
        if (actionsExpanded && !isDragging) {
            kotlinx.coroutines.delay(AUTO_HIDE_DELAY_MS)
            if (!isDragging) actionsExpanded = false
        }
    }
    LaunchedEffect(status) {
        if (status !is CodeServerStatus.Running) actionsExpanded = false
    }
    LaunchedEffect(textZoomPercent) {
        webView?.settings?.textZoom = textZoomPercent
    }

    fun showKeyboard() {
        val view = webView ?: return
        val imm = context.getSystemService(InputMethodManager::class.java)
        view.requestFocus()
        imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    fun copyFromWebView() {
        webView?.evaluateJavascript(
            "(function(){ try { document.execCommand('copy'); } catch (e) {} })();",
            null,
        )
    }

    fun pasteIntoWebView() {
        val view = webView ?: return
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        val text = clipboard
            ?.primaryClip
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
        if (text.isNullOrEmpty()) return
        val escaped = JSONObject.quote(text)
        view.evaluateJavascript(
            "(function(){ try { var dt = new DataTransfer(); dt.setData('text/plain', $escaped);" +
                " var ev = new ClipboardEvent('paste', { clipboardData: dt, bubbles: true, cancelable: true });" +
                " (document.activeElement || document.body).dispatchEvent(ev); } catch (e) {} })();",
            null,
        )
    }

    var keyboardLocked by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        context.findActivity()?.window?.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING,
        )
    }

    fun toggleKeyboardLock() {
        keyboardLocked = !keyboardLocked
        val window = context.findActivity()?.window
        val imm = context.getSystemService(InputMethodManager::class.java)
        if (keyboardLocked) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            window?.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING,
            )
            webView?.let { imm?.hideSoftInputFromWindow(it.windowToken, 0) }
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
            webView?.let { imm?.restartInput(it) }
        }
    }

    fun sendKey(
        keyCode: Int,
        metaState: Int,
    ) {
        val view = webView ?: return
        view.requestFocus()
        val now = SystemClock.uptimeMillis()
        view.dispatchKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, metaState))
        view.dispatchKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, metaState))
    }

    val passwordLabel = stringResource(R.string.code_server_password)
    val urlLabel = stringResource(R.string.code_server_url)
    val shareLabel = stringResource(R.string.code_server_share)

    fun copyPassword() {
        val value = password ?: return
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(passwordLabel, value))
    }

    fun reachableUrl(): String? {
        val port = (status as? CodeServerStatus.Running)?.port ?: return null
        val ip = lanAddress
        return if (lan && ip != null) {
            "http://$ip:$port/"
        } else {
            "http://$CODE_SERVER_HOST:$port/"
        }
    }

    val shareText =
        stringResource(
            R.string.code_server_share_message,
            reachableUrl().orEmpty(),
            password.orEmpty(),
        )

    fun copyUrl() {
        val url = reachableUrl() ?: return
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(urlLabel, url))
    }

    fun shareAccess() {
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
        context.startActivity(Intent.createChooser(intent, shareLabel))
    }

    val latestPassword = rememberUpdatedState(password)

    fun openLink(uri: Uri): Boolean {
        LinkOpener.open(context, uri.toString())
        return true
    }

    fun handleNavigation(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return false
        if (uri.host == CODE_SERVER_HOST && uri.port == runningPort) return false
        return openLink(uri)
    }

    fun openPopup(
        parent: WebView,
        resultMsg: Message,
    ) {
        val popup = WebView(parent.context)
        popup.settings.javaScriptEnabled = true
        var opened = false

        fun forward(url: String) {
            if (opened || url.isBlank() || url == "about:blank") return
            opened = true
            runCatching {
                LinkOpener.open(context, url)
            }
            popup.destroy()
        }
        popup.webViewClient =
            object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    forward(request.url.toString())
                    return true
                }

                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    url: String,
                ): Boolean {
                    forward(url)
                    return true
                }

                override fun onPageStarted(
                    view: WebView,
                    url: String,
                    favicon: Bitmap?,
                ) {
                    forward(url)
                }
            }
        val transport = resultMsg.obj as WebView.WebViewTransport
        transport.webView = popup
        resultMsg.sendToTarget()
    }

    BackHandler {
        val view = webView
        if (view != null && view.canGoBack()) {
            view.goBack()
        } else {
            onBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }

    val fullBleed = QuestWindow.isVrDevice(context)

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .then(if (fullBleed) Modifier else Modifier.safeDrawingPadding())
                .clipToBounds(),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val bottomInsetPx = with(density) { 24.dp.toPx() }
        val maxDragX = ((widthPx - barSize.width) / 2f).coerceAtLeast(0f)
        val maxDragY = (heightPx - barSize.height - bottomInsetPx).coerceAtLeast(0f)
        val dragBounds = DragBounds(maxDragX, maxDragY)
        LaunchedEffect(dragBounds) {
            if (!isDragging) {
                if (!defaultPositionApplied) {
                    dragOffset = Offset(dragBounds.maxX, -dragBounds.maxY / 2f)
                    dockSideState.value = DockSide.RIGHT
                    defaultPositionApplied = true
                } else {
                    val snapped = dockOffset(dragOffset, dragBounds)
                    if (snapped != dragOffset) dragOffset = snapped
                    dockSideState.value = resolveDockSide(snapped, dragBounds)
                }
            }
        }
        when {
            !loaded -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ExpressiveCircularProgressIndicator(modifier = Modifier.size(48.dp))
                }
            }
            environment == null -> {
                Text(
                    text = stringResource(R.string.code_server_not_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            status is CodeServerStatus.Starting -> {
                StatusMessage(
                    title = stringResource(R.string.code_server_starting),
                    body = stringResource(R.string.code_server_starting_hint),
                    showSpinner = true,
                )
            }
            status is CodeServerStatus.Failed -> {
                StatusMessage(
                    title = stringResource(R.string.code_server_failed),
                    body = (status as CodeServerStatus.Failed).message,
                    showSpinner = false,
                    scrollable = true,
                ) {
                    Button(onClick = { viewModel.start() }) {
                        Text(stringResource(R.string.code_server_retry))
                    }
                }
            }
            status is CodeServerStatus.Stopped -> {
                StatusMessage(
                    title = stringResource(R.string.code_server_stopped),
                    body = stringResource(R.string.code_server_starting_hint),
                    showSpinner = false,
                ) {
                    Button(onClick = { viewModel.start() }) {
                        Text(stringResource(R.string.code_server_start))
                    }
                }
            }
            status is CodeServerStatus.Running -> {
                val port = (status as CodeServerStatus.Running).port
                Column(modifier = Modifier.fillMaxSize().imePadding()) {
                    if (progress in 1..99) {
                        ExpressiveLinearProgressIndicator(
                            progress = progress / 100f,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    key(port) {
                        WebViewContainer(
                            url = "http://$CODE_SERVER_HOST:$port/",
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            settings = { webSettings ->
                                webSettings.javaScriptEnabled = true
                                webSettings.domStorageEnabled = true
                                webSettings.databaseEnabled = true
                                webSettings.mediaPlaybackRequiresUserGesture = false
                                webSettings.allowFileAccess = false
                                webSettings.allowContentAccess = false
                                webSettings.setSupportMultipleWindows(true)
                                webSettings.setSupportZoom(true)
                                webSettings.builtInZoomControls = true
                                webSettings.displayZoomControls = false
                                webSettings.textZoom = textZoomPercent
                            },
                            webViewClient =
                                object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView,
                                        request: WebResourceRequest,
                                    ): Boolean = handleNavigation(request.url)

                                    @Deprecated("Deprecated in Java")
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView,
                                        url: String,
                                    ): Boolean = handleNavigation(Uri.parse(url))

                                    override fun onPageFinished(
                                        view: WebView,
                                        url: String,
                                    ) {
                                        super.onPageFinished(view, url)
                                        view.evaluateJavascript(
                                            MOBILE_CSS_SCRIPT + autoLoginScript(latestPassword.value),
                                            null,
                                        )
                                    }
                                },
                            webChromeClient =
                                object : WebChromeClient() {
                                    override fun onProgressChanged(
                                        view: WebView,
                                        newProgress: Int,
                                    ) {
                                        progress = newProgress
                                    }

                                    override fun onCreateWindow(
                                        view: WebView,
                                        isDialog: Boolean,
                                        isUserGesture: Boolean,
                                        resultMsg: Message,
                                    ): Boolean {
                                        openPopup(view, resultMsg)
                                        return true
                                    }
                                },
                            onWebViewCreated = { created ->
                                webView = created
                            },
                            imeEnabled = { !keyboardLocked },
                            onCommitText = { text ->
                                commitModifiedText(text, modifierState) { keyCode, metaState ->
                                    sendKey(keyCode, metaState)
                                }
                            },
                            modifierMetaState = { modifierState.metaState() },
                            onModifierConsumed = { modifierState.consume() },
                        )
                    }
                    CodeServerShortcutsBar(
                        modifierState = modifierState,
                        onKey = { keyCode, metaState -> sendKey(keyCode, metaState) },
                        onCopy = { copyFromWebView() },
                        onPaste = { pasteIntoWebView() },
                        onShowKeyboard = { showKeyboard() },
                    )
                }

                val offsetSpec: AnimationSpec<Offset> =
                    if (isDragging) {
                        snap()
                    } else {
                        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                    }
                val collapsedOverhang =
                    when (dockSideState.value) {
                        DockSide.LEFT,
                        DockSide.RIGHT,
                        -> barSize.width / 2f
                        DockSide.TOP,
                        DockSide.BOTTOM,
                        -> barSize.height / 2f
                    }
                val targetOffset =
                    if (actionsExpanded || isDragging) {
                        dragOffset
                    } else {
                        collapsedOffset(dragOffset, dockSideState.value, collapsedOverhang)
                    }
                val animatedOffset by animateOffsetAsState(
                    targetValue = targetOffset,
                    animationSpec = offsetSpec,
                    label = "codeServerActionsOffset",
                )

                if (actionsExpanded && !isDragging) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = { actionsExpanded = false },
                                ),
                    )
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                            .offset {
                                IntOffset(
                                    animatedOffset.x.roundToInt(),
                                    animatedOffset.y.roundToInt(),
                                )
                            }.onSizeChanged { barSize = it }
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClickLabel = stringResource(R.string.code_server_expand),
                                onClick = { actionsExpanded = true },
                            ).pointerInput(maxDragX, maxDragY) {
                                detectDragGestures(
                                    onDragStart = {
                                        isDragging = true
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                        val bounds = DragBounds(maxDragX, maxDragY)
                                        val snapped = dockOffset(dragOffset, bounds)
                                        dockSideState.value = resolveDockSide(snapped, bounds)
                                        dragOffset = snapped
                                        layoutPreferences
                                            .edit()
                                            .putFloat("bar_x_$environmentId", snapped.x)
                                            .putFloat("bar_y_$environmentId", snapped.y)
                                            .putString(
                                                "dock_side_$environmentId",
                                                dockSideState.value.name,
                                            ).apply()
                                    },
                                    onDragCancel = { isDragging = false },
                                ) { change, amount ->
                                    change.consume()
                                    dragOffset =
                                        clampDragOffset(
                                            Offset(dragOffset.x + amount.x, dragOffset.y + amount.y),
                                            DragBounds(maxDragX, maxDragY),
                                        )
                                }
                            },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        if (dockSideState.value != DockSide.RIGHT) {
                            DragHandle()
                        }
                        if (actionsExpanded) {
                            IconButton(onClick = { webView?.reload() }) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = stringResource(R.string.code_server_reload),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            IconButton(onClick = { toggleKeyboardLock() }) {
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
                                                R.string.code_server_keyboard_unlock
                                            } else {
                                                R.string.code_server_keyboard_lock
                                            },
                                        ),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            IconButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("http://$CODE_SERVER_HOST:$port/"),
                                            ),
                                        )
                                    }
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                    contentDescription = stringResource(R.string.code_server_open_browser),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            IconButton(onClick = { showSettings = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = stringResource(R.string.code_server_settings),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.stop()
                                    onBack()
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Stop,
                                    contentDescription = stringResource(R.string.code_server_stop),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        if (dockSideState.value == DockSide.RIGHT) {
                            DragHandle()
                        }
                    }
                }
            }
        }

        if (showSettings) {
            CodeServerSettingsSheet(
                url = reachableUrl(),
                password = password,
                lan = lan,
                lanAddress = lanAddress,
                zoomPercent = textZoomPercent,
                onZoomChange = { textZoomPercent = it },
                onLanChange = viewModel::setLan,
                onRegenerate = viewModel::regeneratePassword,
                onCopyPassword = { copyPassword() },
                onCopyUrl = { copyUrl() },
                onShare = { shareAccess() },
                onDismiss = { showSettings = false },
            )
        }
    }
}

@Composable
private fun CodeServerSettingsSheet(
    url: String?,
    password: String?,
    lan: Boolean,
    lanAddress: String?,
    zoomPercent: Int,
    onZoomChange: (Int) -> Unit,
    onLanChange: (Boolean) -> Unit,
    onRegenerate: () -> Unit,
    onCopyPassword: () -> Unit,
    onCopyUrl: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit,
) {
    SheetScaffold(
        title = stringResource(R.string.code_server_settings),
        onDismiss = onDismiss,
        footer = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_done))
            }
        },
    ) {
        Text(
            text = stringResource(R.string.code_server_zoom),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = zoomPercent.toFloat(),
                onValueChange = { onZoomChange(it.roundToInt()) },
                valueRange = MIN_ZOOM_PERCENT.toFloat()..MAX_ZOOM_PERCENT.toFloat(),
                steps = (MAX_ZOOM_PERCENT - MIN_ZOOM_PERCENT) / 10 - 1,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$zoomPercent%",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        SettingsGroup(title = null) {
            SwitchRow(
                title = stringResource(R.string.code_server_lan),
                subtitle = stringResource(R.string.code_server_lan_sub),
                checked = lan,
                onCheckedChange = onLanChange,
                showDivider = true,
            )
        }
        Text(
            text = stringResource(R.string.code_server_password),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = password.orEmpty(),
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onCopyPassword) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.code_server_password_copy),
                )
            }
        }
        Text(
            text = stringResource(R.string.code_server_password_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.code_server_url),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text =
                        url ?: if (lan) {
                            stringResource(R.string.code_server_lan_unavailable)
                        } else {
                            stringResource(R.string.code_server_url_unavailable)
                        },
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                )
            }
            IconButton(enabled = url != null, onClick = onCopyUrl) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.code_server_url_copy),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled = url != null, onClick = onShare) {
                Text(stringResource(R.string.code_server_share))
            }
            OutlinedButton(onClick = onRegenerate) {
                Text(stringResource(R.string.code_server_password_regenerate))
            }
        }
        if (lan && lanAddress == null) {
            Text(
                text = stringResource(R.string.code_server_lan_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusMessage(
    title: String,
    body: String,
    showSpinner: Boolean,
    scrollable: Boolean = false,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (showSpinner) {
            ExpressiveCircularProgressIndicator(modifier = Modifier.size(48.dp))
            Box(modifier = Modifier.padding(top = 16.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style =
                if (scrollable) {
                    MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                } else {
                    MaterialTheme.typography.bodyMedium
                },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )

        if (action != null) {
            Box(modifier = Modifier.padding(top = 16.dp)) { action() }
        }
    }
}

@Composable
private fun DragHandle() {
    Box(
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.DragIndicator,
            contentDescription = stringResource(R.string.code_server_move),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val CODE_SERVER_HOST = "127.0.0.1"
private const val AUTO_HIDE_DELAY_MS = 3000L
internal const val MOBILE_CSS_SCRIPT =
    "(function(){" +
        "if(document.getElementById('prootcode-mobile-css'))return;" +
        "var s=document.createElement('style');s.id='prootcode-mobile-css';" +
        "s.textContent='.monaco-workbench{border-radius:18px;overflow:hidden!important;}'+" +
        "'html,body{background:transparent!important;}';" +
        "document.head.appendChild(s);" +
        "})();"

internal fun autoLoginScript(password: String?): String {
    if (password.isNullOrBlank()) return ""
    val quoted = JSONObject.quote(password)
    return "(function(){" +
        "var p=document.querySelector('input[type=password]');" +
        "if(!p)return;" +
        "if(sessionStorage.getItem('prootcode_auto_login'))return;" +
        "sessionStorage.setItem('prootcode_auto_login','1');" +
        "p.value=$quoted;" +
        "p.dispatchEvent(new Event('input',{bubbles:true}));" +
        "var f=p.form||p.closest('form');" +
        "if(f)f.submit();" +
        "})();"
}

private const val DEFAULT_ZOOM_PERCENT = 100

internal const val MIN_ZOOM_PERCENT = 50
internal const val MAX_ZOOM_PERCENT = 200
internal const val ZOOM_STEP_PERCENT = 10

internal enum class DockSide {
    LEFT,
    RIGHT,
    TOP,
    BOTTOM,
}

internal data class DragBounds(
    val maxX: Float,
    val maxY: Float,
)

internal fun clampDragOffset(
    offset: Offset,
    bounds: DragBounds,
): Offset =
    Offset(
        offset.x.coerceIn(-bounds.maxX, bounds.maxX),
        offset.y.coerceIn(-bounds.maxY, 0f),
    )

internal fun resolveDockSide(
    offset: Offset,
    bounds: DragBounds,
): DockSide {
    val clamped = clampDragOffset(offset, bounds)
    var best = -clamped.y
    var side = DockSide.BOTTOM
    val left = bounds.maxX + clamped.x
    if (left < best) {
        best = left
        side = DockSide.LEFT
    }
    val right = bounds.maxX - clamped.x
    if (right < best) {
        best = right
        side = DockSide.RIGHT
    }
    if (bounds.maxY + clamped.y < best) {
        side = DockSide.TOP
    }
    return side
}

internal fun dockOffset(
    offset: Offset,
    bounds: DragBounds,
): Offset {
    val clamped = clampDragOffset(offset, bounds)
    return when (resolveDockSide(clamped, bounds)) {
        DockSide.LEFT -> Offset(-bounds.maxX, clamped.y)
        DockSide.RIGHT -> Offset(bounds.maxX, clamped.y)
        DockSide.TOP -> Offset(clamped.x, -bounds.maxY)
        DockSide.BOTTOM -> Offset(clamped.x, 0f)
    }
}

internal fun collapsedOffset(
    offset: Offset,
    side: DockSide,
    overhang: Float,
): Offset =
    when (side) {
        DockSide.LEFT -> Offset(offset.x - overhang, offset.y)
        DockSide.RIGHT -> Offset(offset.x + overhang, offset.y)
        DockSide.TOP -> Offset(offset.x, offset.y - overhang)
        DockSide.BOTTOM -> Offset(offset.x, offset.y + overhang)
    }

internal fun nextZoomLevel(
    current: Int,
    step: Int,
): Int = (current + step).coerceIn(MIN_ZOOM_PERCENT, MAX_ZOOM_PERCENT)

internal fun nextZoomCycle(
    current: Int,
    step: Int,
): Int {
    val next = current + step
    return when {
        next > MAX_ZOOM_PERCENT -> MIN_ZOOM_PERCENT
        next < MIN_ZOOM_PERCENT -> MAX_ZOOM_PERCENT
        else -> next
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
