package dev.prootcode.app.ui.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import dev.prootcode.app.data.preferences.AppPreferences
import dev.prootcode.app.data.preferences.LinkOpenBehavior
import dev.prootcode.app.ui.webview.LinkWebViewActivity

object LinkOpener {
    fun open(
        context: Context,
        url: String,
    ) {
        if (url.isBlank()) return
        when (AppPreferences.create(context).linkBehavior.value) {
            LinkOpenBehavior.IN_APP -> LinkWebViewActivity.start(context, url)
            LinkOpenBehavior.SYSTEM ->
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
        }
    }
}
