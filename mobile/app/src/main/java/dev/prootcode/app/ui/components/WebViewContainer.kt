package dev.prootcode.app.ui.components

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun WebViewContainer(
    url: String,
    modifier: Modifier = Modifier,
    webViewClient: WebViewClient = WebViewClient(),
    webChromeClient: WebChromeClient? = null,
    onWebViewCreated: (WebView) -> Unit = {},
    settings: (android.webkit.WebSettings) -> Unit = {},
) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
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
                    settings(this.settings)
                    this.webViewClient = webViewClient
                    if (webChromeClient != null) this.webChromeClient = webChromeClient
                    onWebViewCreated(this)
                    loadUrl(url)
                }.also { webView = it }
        },
        modifier = modifier,
    )
}
