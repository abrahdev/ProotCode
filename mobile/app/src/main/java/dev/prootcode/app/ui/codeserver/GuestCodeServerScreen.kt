package dev.prootcode.app.ui.codeserver

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.os.SystemClock
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.prootcode.app.R
import dev.prootcode.app.ui.components.ExpressiveLinearProgressIndicator
import dev.prootcode.app.ui.components.SheetScaffold
import dev.prootcode.app.ui.components.WebViewContainer
import dev.prootcode.app.ui.screens.MAX_ZOOM_PERCENT
import dev.prootcode.app.ui.screens.MIN_ZOOM_PERCENT
import dev.prootcode.app.ui.screens.MOBILE_CSS_SCRIPT
import dev.prootcode.app.ui.screens.autoLoginScript
import dev.prootcode.app.ui.util.LinkOpener
import org.json.JSONObject
import kotlin.math.roundToInt

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GuestCodeServerScreen(
    guestUrl: String,
    guestPassword: String?,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val layoutPreferences =
        remember { context.getSharedPreferences("code_server_layout", Context.MODE_PRIVATE) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var textZoomPercent by remember { mutableIntStateOf(layoutPreferences.getInt(GUEST_ZOOM_KEY, 100)) }
    val modifierState = remember { CodeServerModifierState() }
    val guestHost = remember(guestUrl) { runCatching { Uri.parse(guestUrl).host }.getOrNull() }

    LaunchedEffect(textZoomPercent) {
        webView?.settings?.textZoom = textZoomPercent
        layoutPreferences.edit().putInt(GUEST_ZOOM_KEY, textZoomPercent).apply()
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
        val text =
            clipboard
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

    fun openLink(uri: Uri): Boolean {
        LinkOpener.open(context, uri.toString())
        return true
    }

    fun handleNavigation(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return false
        if (uri.host == guestHost) return false
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
            runCatching { LinkOpener.open(context, url) }
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

    fun copyPassword() {
        val value = guestPassword ?: return
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.code_server_password), value))
    }

    fun copyUrl() {
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.code_server_url), guestUrl))
    }

    fun shareAccess() {
        val text = context.getString(R.string.code_server_share_message, guestUrl, guestPassword.orEmpty())
        val intent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.code_server_share)),
        )
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

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
    ) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            if (progress in 1..99) {
                ExpressiveLinearProgressIndicator(
                    progress = progress / 100f,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            WebViewContainer(
                url = guestUrl,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                settings = { webSettings ->
                    webSettings.javaScriptEnabled = true
                    webSettings.domStorageEnabled = true
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
                                MOBILE_CSS_SCRIPT + autoLoginScript(guestPassword),
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
                onWebViewCreated = { created -> webView = created },
                onCommitText = { text ->
                    commitModifiedText(text, modifierState) { keyCode, metaState ->
                        sendKey(keyCode, metaState)
                    }
                },
            )
            CodeServerShortcutsBar(
                modifierState = modifierState,
                onKey = { keyCode, metaState -> sendKey(keyCode, metaState) },
                onCopy = { copyFromWebView() },
                onPaste = { pasteIntoWebView() },
                onShowKeyboard = { showKeyboard() },
            )
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(onClick = { webView?.reload() }) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = stringResource(R.string.code_server_reload),
                    )
                }
                IconButton(onClick = { showSettings = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.code_server_settings),
                    )
                }
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.action_close),
                    )
                }
            }
        }
    }

    if (showSettings) {
        GuestSettingsSheet(
            url = guestUrl,
            password = guestPassword,
            zoomPercent = textZoomPercent,
            onZoomChange = { textZoomPercent = it },
            onCopyPassword = { copyPassword() },
            onCopyUrl = { copyUrl() },
            onShare = { shareAccess() },
            onDismiss = { showSettings = false },
        )
    }
}

@Composable
private fun GuestSettingsSheet(
    url: String,
    password: String?,
    zoomPercent: Int,
    onZoomChange: (Int) -> Unit,
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
            IconButton(enabled = password != null, onClick = onCopyPassword) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.code_server_password_copy),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.code_server_url),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                )
            }
            IconButton(onClick = onCopyUrl) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = stringResource(R.string.code_server_url_copy),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onShare) {
                Text(stringResource(R.string.code_server_share))
            }
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.action_done))
            }
        }
    }
}

private const val GUEST_ZOOM_KEY = "guest_zoom"
