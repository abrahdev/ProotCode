package dev.prootcode.app.ui.util

import android.content.pm.ActivityInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class QuestWindowTest {
    @Test
    fun `detects VR devices by manufacturer`() {
        assertTrue(QuestWindow.isVrDevice("Oculus", "Quest 3", false))
        assertTrue(QuestWindow.isVrDevice("oculus", "Headset", false))
    }

    @Test
    fun `detects VR devices by model`() {
        assertTrue(QuestWindow.isVrDevice("Unknown", "Quest 2", false))
    }

    @Test
    fun `detects VR devices by system feature`() {
        assertTrue(QuestWindow.isVrDevice("Generic", "Generic", true))
    }

    @Test
    fun `regular phones are not VR devices`() {
        assertFalse(QuestWindow.isVrDevice("Google", "Pixel 9", false))
    }

    @Test
    fun `landscape is requested only for VR devices`() {
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
            QuestWindow.requestedOrientation(isVrDevice = true),
        )
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED,
            QuestWindow.requestedOrientation(isVrDevice = false),
        )
    }
}
