package dev.prootcode.app.ui.codeserver

import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class CodeServerModifierState {
    var ctrlActive by mutableStateOf(false)
    var ctrlLocked by mutableStateOf(false)
    var altActive by mutableStateOf(false)
    var altLocked by mutableStateOf(false)

    val ctrl: Boolean get() = ctrlLocked || ctrlActive

    val alt: Boolean get() = altLocked || altActive

    val hasModifiers: Boolean get() = ctrl || alt

    fun metaState(): Int {
        var state = 0
        if (ctrl) state = state or KeyEvent.META_CTRL_ON
        if (alt) state = state or KeyEvent.META_ALT_ON
        return state
    }

    fun toggleCtrl() {
        if (ctrlLocked) {
            ctrlLocked = false
            ctrlActive = false
        } else {
            ctrlActive = !ctrlActive
        }
    }

    fun toggleCtrlLock() {
        ctrlLocked = !ctrlLocked
        ctrlActive = ctrlLocked
    }

    fun toggleAlt() {
        if (altLocked) {
            altLocked = false
            altActive = false
        } else {
            altActive = !altActive
        }
    }

    fun toggleAltLock() {
        altLocked = !altLocked
        altActive = altLocked
    }

    fun consume() {
        if (!ctrlLocked) ctrlActive = false
        if (!altLocked) altActive = false
    }
}

fun keyCodeForChar(char: Char): Int? =
    when (char) {
        in 'a'..'z' -> KeyEvent.KEYCODE_A + (char - 'a')
        in 'A'..'Z' -> KeyEvent.KEYCODE_A + (char - 'A')
        in '0'..'9' -> KeyEvent.KEYCODE_0 + (char - '0')
        ' ' -> KeyEvent.KEYCODE_SPACE
        '/' -> KeyEvent.KEYCODE_SLASH
        '-' -> KeyEvent.KEYCODE_MINUS
        '.' -> KeyEvent.KEYCODE_PERIOD
        ',' -> KeyEvent.KEYCODE_COMMA
        ';' -> KeyEvent.KEYCODE_SEMICOLON
        '=' -> KeyEvent.KEYCODE_EQUALS
        '[' -> KeyEvent.KEYCODE_LEFT_BRACKET
        ']' -> KeyEvent.KEYCODE_RIGHT_BRACKET
        '\\' -> KeyEvent.KEYCODE_BACKSLASH
        '\n' -> KeyEvent.KEYCODE_ENTER
        else -> null
    }

fun commitModifiedText(
    text: CharSequence,
    modifierState: CodeServerModifierState,
    send: (keyCode: Int, metaState: Int) -> Unit,
): Boolean {
    if (text.length != 1 || !modifierState.hasModifiers) return false
    val char = text[0]
    val keyCode = keyCodeForChar(char) ?: return false
    var metaState = modifierState.metaState()
    if (char.isUpperCase()) metaState = metaState or KeyEvent.META_SHIFT_ON
    send(keyCode, metaState)
    modifierState.consume()
    return true
}
