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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.prootcode.app.R
import dev.prootcode.app.runtime.codeserver.CodeServerStatus
import dev.prootcode.app.ui.viewmodels.CodeServerViewModel
import dev.prootcode.app.ui.webview.LinkWebViewActivity

@OptIn(ExperimentalMaterial3Api::class)
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
    val reloadKey by viewModel.reloadKey.collectAsState()

    var webView by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    val runningPort = (status as? CodeServerStatus.Running)?.port

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

    Scaffold(
        topBar = {
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
                    Column {
                        Text(
                            text = stringResource(R.string.code_server_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        if (runningPort != null) {
                            Text(
                                text = stringResource(R.string.code_server_port, runningPort),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    if (runningPort != null) {
                        IconButton(onClick = { viewModel.reload() }) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = stringResource(R.string.code_server_reload),
                            )
                        }
                        IconButton(
                            onClick = {
                                val intent =
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("http://$CODE_SERVER_HOST:$runningPort/"),
                                    )
                                context.startActivity(intent)
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.OpenInNew,
                                contentDescription =
                                    stringResource(R.string.code_server_open_browser),
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
                },
            )
        },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
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
