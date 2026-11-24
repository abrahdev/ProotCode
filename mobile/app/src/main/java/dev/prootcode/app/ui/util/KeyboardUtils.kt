package dev.prootcode.app.ui.util

import android.content.Context
import android.content.res.Configuration
import android.hardware.input.InputManager
import android.os.Build
import android.view.InputDevice

object KeyboardUtils {
    fun isExternalKeyboardConnected(context: Context): Boolean {
        val inputManager = context.getSystemService(InputManager::class.java) ?: return false
        return inputManager.inputDeviceIds.any { id ->
            val device = inputManager.getInputDevice(id) ?: return@any false
            if (device.isVirtual || !device.supportsSource(InputDevice.SOURCE_KEYBOARD)) {
                return@any false
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                device.isExternal
            } else {
                context.resources.configuration.keyboard != Configuration.KEYBOARD_NOKEYS
            }
        }
    }
}
