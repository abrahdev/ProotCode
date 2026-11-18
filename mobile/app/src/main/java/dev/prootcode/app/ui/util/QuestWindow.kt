package dev.prootcode.app.ui.util

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.os.Build
import android.os.Bundle

object QuestWindow {
    private const val PANEL_WIDTH = 1280
    private const val PANEL_HEIGHT = 800

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

    internal fun isVrDevice(
        manufacturer: String,
        model: String,
        hasStandaloneVrFeature: Boolean,
    ): Boolean =
        manufacturer.equals("Oculus", ignoreCase = true) ||
            model.contains("Quest", ignoreCase = true) ||
            hasStandaloneVrFeature

    fun isVrDevice(context: Context): Boolean =
        isVrDevice(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            hasStandaloneVrFeature =
                context.packageManager.hasSystemFeature("oculus.hardware.standalone_vr"),
        )

    internal fun requestedOrientation(isVrDevice: Boolean): Int =
        if (isVrDevice) {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }

    fun applyOrientation(activity: Activity) {
        activity.requestedOrientation = requestedOrientation(isVrDevice(activity))
    }

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
