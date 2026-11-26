package dev.prootcode.app.ui.components

import android.view.KeyEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
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
    imeEnabled: () -> Boolean = { true },
    onCommitText: (CharSequence) -> Boolean = { false },
    modifierMetaState: () -> Int = { 0 },
    onModifierConsumed: () -> Unit = {},
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
            object : WebView(ctx) {
                override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
                    if (!imeEnabled()) return null
                    val base = super.onCreateInputConnection(outAttrs) ?: return null
                    return ModifierInputConnection(base, onCommitText)
                }

                override fun onCheckIsTextEditor(): Boolean = imeEnabled()

                override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                    val extra = modifierMetaState()
                    if (extra == 0) return super.dispatchKeyEvent(event)
                    val modified =
                        KeyEvent(
                            event.downTime,
                            event.eventTime,
                            event.action,
                            event.keyCode,
                            event.repeatCount,
                            event.metaState or extra,
                            event.deviceId,
                            event.scanCode,
                            event.flags,
                            event.source,
                        )
                    val handled = super.dispatchKeyEvent(modified)
                    if (event.action == KeyEvent.ACTION_UP) onModifierConsumed()
                    return handled
                }
            }.apply {
                isClickable = true
                isFocusable = true
                isFocusableInTouchMode = true
                this.settings.javaScriptCanOpenWindowsAutomatically = true
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

private class ModifierInputConnection(
    target: InputConnection,
    private val onText: (CharSequence) -> Boolean,
) : InputConnectionWrapper(target, false) {
    private var suppressed: String? = null

    override fun setComposingText(
        text: CharSequence?,
        newCursorPosition: Int,
    ): Boolean {
        val value = text?.toString().orEmpty()
        if (value.isNotEmpty() && onText(value)) {
            suppressed = value
            return true
        }
        return super.setComposingText(text, newCursorPosition)
    }

    override fun commitText(
        text: CharSequence?,
        newCursorPosition: Int,
    ): Boolean {
        val value = text?.toString().orEmpty()
        val pending = suppressed
        if (pending != null) {
            suppressed = null
            if (pending == value) return true
        }
        if (value.isNotEmpty() && onText(value)) return true
        return super.commitText(text, newCursorPosition)
    }

    override fun finishComposingText(): Boolean {
        suppressed = null
        return super.finishComposingText()
    }
}
