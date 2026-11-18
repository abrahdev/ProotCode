package dev.prootcode.app.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material.icons.outlined.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.prootcode.app.R
import dev.prootcode.app.runtime.codeserver.CodeServerStatus
import dev.prootcode.app.ui.util.QuestWindow
import dev.prootcode.app.ui.viewmodels.CodeServerViewModel
import dev.prootcode.app.ui.webview.LinkWebViewActivity
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

    var webView by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var showActions by remember { mutableStateOf(false) }
    var pointerNear by remember { mutableStateOf(false) }
    val layoutPreferences =
        remember { context.getSharedPreferences("code_server_layout", Context.MODE_PRIVATE) }
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
    val barCenter = remember { mutableStateOf(Offset.Zero) }
    val runningPort = (status as? CodeServerStatus.Running)?.port

    val actionsInteraction = remember { MutableInteractionSource() }
    val actionsHovered by actionsInteraction.collectIsHoveredAsState()
    val density = LocalDensity.current
    val hoverZone = with(density) { HOVER_ZONE_DP.dp.toPx() }

    LaunchedEffect(actionsHovered, pointerNear, isDragging) {
        if (actionsHovered || isDragging) {
            showActions = true
        } else if (!pointerNear) {
            kotlinx.coroutines.delay(700)
            if (!actionsHovered && !pointerNear && !isDragging) showActions = false
        }
    }
    LaunchedEffect(status) {
        if (status !is CodeServerStatus.Running) showActions = false
    }

    fun openLink(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase()
        if (scheme == "http" || scheme == "https") {
            return runCatching {
                LinkWebViewActivity.start(context, uri.toString())
            }.isSuccess
        }
        return runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }.isSuccess
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
                LinkWebViewActivity.start(context, url)
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
                .then(if (fullBleed) Modifier else Modifier.safeDrawingPadding()),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val bottomInsetPx = with(density) { 24.dp.toPx() }
        val maxDragX = ((widthPx - barSize.width) / 2f).coerceAtLeast(0f)
        val maxDragY = (heightPx - barSize.height - bottomInsetPx).coerceAtLeast(0f)
        SideEffect {
            barCenter.value =
                Offset(
                    widthPx / 2f + dragOffset.x,
                    heightPx - bottomInsetPx - barSize.height / 2f + dragOffset.y,
                )
        }
        when {
            !loaded -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
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
                Column(modifier = Modifier.fillMaxSize()) {
                    if (progress in 1..99) {
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    key(port) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx)
                                    .apply {
                                        layoutParams =
                                            ViewGroup.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                            )
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        settings.databaseEnabled = true
                                        settings.mediaPlaybackRequiresUserGesture = false
                                        settings.allowFileAccess = false
                                        settings.allowContentAccess = false
                                        settings.setSupportMultipleWindows(true)
                                        settings.setSupportZoom(true)
                                        settings.builtInZoomControls = true
                                        settings.displayZoomControls = false
                                        setOnHoverListener { view, event ->
                                            when (event.actionMasked) {
                                                MotionEvent.ACTION_HOVER_ENTER,
                                                MotionEvent.ACTION_HOVER_MOVE,
                                                -> {
                                                    showActions =
                                                        event.x > view.width - hoverZone &&
                                                        event.y < hoverZone
                                                }
                                                MotionEvent.ACTION_HOVER_EXIT -> {
                                                    showActions = false
                                                }
                                            }
                                            false
                                        }
                                        setOnTouchListener { view, event ->
                                            if (event.actionMasked == MotionEvent.ACTION_UP) {
                                                val center = barCenter.value
                                                val dx = event.x - center.x
                                                val dy = event.y - center.y
                                                if (dx * dx + dy * dy <= hoverZone * hoverZone) {
                                                    showActions = !showActions
                                                }
                                            }
                                            false
                                        }
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
                                            }
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
                                            }
                                        loadUrl("http://$CODE_SERVER_HOST:$port/")
                                    }.also { webView = it }
                            },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showActions,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                            .offset {
                                IntOffset(
                                    dragOffset.x.coerceIn(-maxDragX, maxDragX).roundToInt(),
                                    dragOffset.y.coerceIn(-maxDragY, 0f).roundToInt(),
                                )
                            }.onSizeChanged { barSize = it }
                            .pointerInput(maxDragX, maxDragY) {
                                detectDragGestures(
                                    onDragStart = { isDragging = true },
                                    onDragEnd = { isDragging = false },
                                    onDragCancel = { isDragging = false },
                                ) { change, amount ->
                                    change.consume()
                                    dragOffset =
                                        Offset(
                                            (dragOffset.x + amount.x)
                                                .coerceIn(-maxDragX, maxDragX),
                                            (dragOffset.y + amount.y)
                                                .coerceIn(-maxDragY, 0f),
                                        )
                                }
                            },
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 },
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier.hoverable(actionsInteraction),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .pointerInput(maxDragX, maxDragY) {
                                            detectDragGestures(
                                                onDragStart = { isDragging = true },
                                                onDragEnd = {
                                                    isDragging = false
                                                    layoutPreferences
                                                        .edit()
                                                        .putFloat(
                                                            "bar_x_$environmentId",
                                                            dragOffset.x,
                                                        ).putFloat(
                                                            "bar_y_$environmentId",
                                                            dragOffset.y,
                                                        ).apply()
                                                },
                                                onDragCancel = { isDragging = false },
                                            ) { change, amount ->
                                                change.consume()
                                                dragOffset =
                                                    Offset(
                                                        (dragOffset.x + amount.x)
                                                            .coerceIn(-maxDragX, maxDragX),
                                                        (dragOffset.y + amount.y)
                                                            .coerceIn(-maxDragY, 0f),
                                                    )
                                            }
                                        }.padding(horizontal = 8.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DragIndicator,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.Outlined.ArrowBack,
                                    contentDescription = stringResource(R.string.action_back),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            IconButton(onClick = { webView?.reload() }) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = stringResource(R.string.code_server_reload),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            IconButton(onClick = { webView?.zoomIn() }) {
                                Icon(
                                    imageVector = Icons.Outlined.ZoomIn,
                                    contentDescription = stringResource(R.string.code_server_zoom_in),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                            IconButton(onClick = { webView?.zoomOut() }) {
                                Icon(
                                    imageVector = Icons.Outlined.ZoomOut,
                                    contentDescription = stringResource(R.string.code_server_zoom_out),
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
                                    imageVector = Icons.Outlined.OpenInNew,
                                    contentDescription =
                                        stringResource(R.string.code_server_open_browser),
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
                    }
                }
            }
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
            CircularProgressIndicator()
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

private const val CODE_SERVER_HOST = "127.0.0.1"

private const val HOVER_ZONE_DP = 220
