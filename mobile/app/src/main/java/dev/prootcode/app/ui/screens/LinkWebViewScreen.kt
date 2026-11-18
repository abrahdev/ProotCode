package dev.prootcode.app.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.prootcode.app.R
import dev.prootcode.app.ui.webview.LinkWebViewActivity

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LinkWebViewScreen(
    url: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val host = remember(url) { runCatching { Uri.parse(url).host }.getOrNull() }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var currentUrl by remember { mutableStateOf(url) }
    var addressInput by remember { mutableStateOf(url) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    BackHandler {
        val view = webView
        if (view != null && view.canGoBack()) {
            view.goBack()
        } else {
            onBack()
        }
    }

    LaunchedEffect(reloadKey) {
        if (reloadKey > 0) webView?.reload()
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }

    fun navigateToAddress() {
        val target = normalizeAddress(addressInput) ?: return
        addressInput = target
        webView?.loadUrl(target)
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.action_back),
                            )
                        }
                    },
                    title = {
                        Text(
                            text = host?.takeIf { it.isNotBlank() } ?: stringResource(R.string.web_link_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    actions = {
                        IconButton(onClick = { reloadKey += 1 }) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = stringResource(R.string.web_link_reload),
                            )
                        }
                        IconButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)))
                                }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.OpenInNew,
                                contentDescription = stringResource(R.string.web_link_open_browser),
                            )
                        }
                    },
                )
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = addressInput,
                        onValueChange = { addressInput = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(stringResource(R.string.web_link_address)) },
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go,
                            ),
                        keyboardActions = KeyboardActions(onGo = { navigateToAddress() }),
                    )
                    IconButton(onClick = { navigateToAddress() }) {
                        Icon(
                            imageVector = Icons.Outlined.Send,
                            contentDescription = stringResource(R.string.web_link_go),
                        )
                    }
                    IconButton(onClick = { webView?.goBack() }, enabled = canGoBack) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.web_link_back),
                        )
                    }
                    IconButton(onClick = { webView?.goForward() }, enabled = canGoForward) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowForward,
                            contentDescription = stringResource(R.string.web_link_forward),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            if (progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
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
                            webViewClient =
                                object : WebViewClient() {
                                    override fun onPageStarted(
                                        view: WebView,
                                        pageUrl: String,
                                        favicon: Bitmap?,
                                    ) {
                                        currentUrl = pageUrl
                                        addressInput = pageUrl
                                        canGoBack = view.canGoBack()
                                        canGoForward = view.canGoForward()
                                    }

                                    override fun onPageFinished(
                                        view: WebView,
                                        pageUrl: String,
                                    ) {
                                        currentUrl = pageUrl
                                        addressInput = pageUrl
                                        canGoBack = view.canGoBack()
                                        canGoForward = view.canGoForward()
                                    }

                                    override fun doUpdateVisitedHistory(
                                        view: WebView,
                                        url: String,
                                        isReload: Boolean,
                                    ) {
                                        canGoBack = view.canGoBack()
                                        canGoForward = view.canGoForward()
                                    }

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView,
                                        request: WebResourceRequest,
                                    ): Boolean {
                                        val scheme = request.url.scheme?.lowercase()
                                        if (scheme != "http" && scheme != "https") {
                                            runCatching {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_VIEW, request.url),
                                                )
                                            }
                                            return true
                                        }
                                        return false
                                    }
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
                                        val popup = WebView(view.context)
                                        popup.settings.javaScriptEnabled = true
                                        var opened = false

                                        fun openPopup(url: String) {
                                            if (opened || url.isBlank() || url == "about:blank") return
                                            opened = true
                                            runCatching {
                                                LinkWebViewActivity.start(view.context, url)
                                            }
                                            popup.destroy()
                                        }
                                        popup.webViewClient =
                                            object : WebViewClient() {
                                                override fun shouldOverrideUrlLoading(
                                                    v: WebView,
                                                    request: WebResourceRequest,
                                                ): Boolean {
                                                    openPopup(request.url.toString())
                                                    return true
                                                }

                                                @Deprecated("Deprecated in Java")
                                                override fun shouldOverrideUrlLoading(
                                                    v: WebView,
                                                    url: String,
                                                ): Boolean {
                                                    openPopup(url)
                                                    return true
                                                }

                                                override fun onPageStarted(
                                                    v: WebView,
                                                    url: String,
                                                    favicon: Bitmap?,
                                                ) {
                                                    openPopup(url)
                                                }
                                            }
                                        val transport = resultMsg.obj as WebView.WebViewTransport
                                        transport.webView = popup
                                        resultMsg.sendToTarget()
                                        return true
                                    }
                                }
                            loadUrl(url)
                        }.also { webView = it }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

internal fun normalizeAddress(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null
    val schemeEnd = trimmed.indexOf("://")
    val firstSlash = trimmed.indexOf('/').takeIf { it >= 0 } ?: trimmed.length
    if (schemeEnd < 0) {
        val colon = trimmed.indexOf(':')
        if (colon in 0 until firstSlash) return null
    }
    val scheme =
        if (schemeEnd >= 0) {
            trimmed.substring(0, schemeEnd).lowercase()
        } else {
            "https"
        }
    if (scheme != "http" && scheme != "https") return null
    val withScheme = if (schemeEnd >= 0) trimmed else "$scheme://$trimmed"
    val authority = withScheme.substringAfter("://").substringBefore("/").substringBefore("?")
    val host = authority.substringAfterLast("@").substringBefore(":")
    if (host.isBlank()) return null
    return withScheme
}
