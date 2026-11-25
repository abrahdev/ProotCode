package dev.prootcode.app.ui.codeserver

import android.view.KeyEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CodeServerModifierTest {
    @Test
    fun `maps letters and digits to key codes`() {
        assertEquals(KeyEvent.KEYCODE_A, keyCodeForChar('a'))
        assertEquals(KeyEvent.KEYCODE_J, keyCodeForChar('j'))
        assertEquals(KeyEvent.KEYCODE_J, keyCodeForChar('J'))
        assertEquals(KeyEvent.KEYCODE_0, keyCodeForChar('0'))
        assertNull(keyCodeForChar('€'))
    }

    @Test
    fun `commits typed text with the latched modifier`() {
        val state = CodeServerModifierState()
        state.toggleCtrl()
        var sent: Pair<Int, Int>? = null

        val consumed =
            commitModifiedText("j", state) { keyCode, metaState ->
                sent = keyCode to metaState
            }

        assertTrue(consumed)
        assertEquals(KeyEvent.KEYCODE_J, sent?.first)
        assertEquals(KeyEvent.META_CTRL_ON, sent?.second)
        assertFalse(state.hasModifiers)
    }

    @Test
    fun `keeps a locked modifier until tapped again`() {
        val state = CodeServerModifierState()
        state.toggleCtrlLock()
        var sent: Pair<Int, Int>? = null

        commitModifiedText("j", state) { keyCode, metaState -> sent = keyCode to metaState }
        commitModifiedText("k", state) { keyCode, metaState -> sent = keyCode to metaState }

        assertTrue(state.hasModifiers)
        assertEquals(KeyEvent.KEYCODE_K, sent?.first)
        assertEquals(KeyEvent.META_CTRL_ON, sent?.second)
    }

    @Test
    fun `ignores plain typing without modifiers`() {
        val state = CodeServerModifierState()

        assertFalse(commitModifiedText("j", state) { _, _ -> })
    }
}
