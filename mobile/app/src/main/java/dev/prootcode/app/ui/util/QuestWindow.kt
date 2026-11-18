package dev.prootcode.app.ui.util

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Build
import android.os.Bundle

object QuestWindow {
    private const val PANEL_WIDTH = 1280
    private const val PANEL_HEIGHT = 720

    fun start(
        context: Context,
        intent: Intent,
    ) {
        val options = launchOptions(context)
        if (options != null) {
            context.startActivity(intent, options)
        } else {
            context.startActivity(intent)
        }
    }

    fun isVrDevice(context: Context): Boolean =
        Build.MANUFACTURER.equals("Oculus", ignoreCase = true) ||
            Build.MODEL.contains("Quest", ignoreCase = true) ||
            context.packageManager.hasSystemFeature("oculus.hardware.standalone_vr")

    private fun launchOptions(context: Context): Bundle? {
        if (!isVrDevice(context)) return null
        return runCatching {
            ActivityOptions
                .makeBasic()
                .apply { setLaunchBounds(Rect(0, 0, PANEL_WIDTH, PANEL_HEIGHT)) }
                .toBundle()
        }.getOrNull()
    }
}
