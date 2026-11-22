package dev.prootcode.app.ui.terminal

import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalViewClient

class ProotSessionClient(
    private val onScreenUpdated: () -> Unit,
    private val onSessionFinished: () -> Unit = {},
) : TerminalSessionClient {
    override fun onTextChanged(changedSession: TerminalSession) = onScreenUpdated()

    override fun onTitleChanged(changedSession: TerminalSession) = Unit

    override fun onSessionFinished(finishedSession: TerminalSession) = onSessionFinished()

    override fun onCopyTextToClipboard(
        session: TerminalSession,
        text: String,
    ) = Unit

    override fun onPasteTextFromClipboard(session: TerminalSession?) = Unit

    override fun onBell(session: TerminalSession) = Unit

    override fun onColorsChanged(session: TerminalSession) = Unit

    override fun onTerminalCursorStateChange(state: Boolean) = Unit

    override fun setTerminalShellPid(
        session: TerminalSession,
        pid: Int,
    ) = Unit

    override fun getTerminalCursorStyle(): Int? = null

    override fun logError(
        tag: String,
        message: String,
    ) {
        Log.e(tag, message)
    }

    override fun logWarn(
        tag: String,
        message: String,
    ) {
        Log.w(tag, message)
    }

    override fun logInfo(
        tag: String,
        message: String,
    ) {
        Log.i(tag, message)
    }

    override fun logDebug(
        tag: String,
        message: String,
    ) {
        Log.d(tag, message)
    }

    override fun logVerbose(
        tag: String,
        message: String,
    ) {
        Log.v(tag, message)
    }

    override fun logStackTraceWithMessage(
        tag: String,
        message: String,
        e: Exception,
    ) {
        Log.e(tag, message, e)
    }

    override fun logStackTrace(
        tag: String,
        e: Exception,
    ) {
        Log.e(tag, "stack trace", e)
    }
}

class ProotViewClient(
    private val onKeyboardRequested: () -> Unit,
) : TerminalViewClient {
    var ctrlActive by mutableStateOf(false)
    var altActive by mutableStateOf(false)
    var ctrlLatched by mutableStateOf(false)
    var altLatched by mutableStateOf(false)

    fun toggleModifier(id: String) {
        when (id) {
            "ctrl" -> {
                if (ctrlLatched) {
                    ctrlLatched = false
                    ctrlActive = false
                } else {
                    ctrlActive = !ctrlActive
                }
            }
            "alt" -> {
                if (altLatched) {
                    altLatched = false
                    altActive = false
                } else {
                    altActive = !altActive
                }
            }
        }
    }

    fun toggleModifierLatch(id: String) {
        when (id) {
            "ctrl" -> {
                ctrlLatched = !ctrlLatched
                ctrlActive = ctrlLatched
            }
            "alt" -> {
                altLatched = !altLatched
                altActive = altLatched
            }
        }
    }

    fun consumeModifiers() {
        if (!ctrlLatched) ctrlActive = false
        if (!altLatched) altActive = false
    }

    override fun onScale(scale: Float): Float = 1f

    override fun onSingleTapUp(e: MotionEvent) = onKeyboardRequested()

    override fun shouldBackButtonBeMappedToEscape(): Boolean = false

    override fun shouldEnforceCharBasedInput(): Boolean = false

    override fun shouldUseCtrlSpaceWorkaround(): Boolean = false

    override fun isTerminalViewSelected(): Boolean = true

    override fun copyModeChanged(copyMode: Boolean) = Unit

    override fun onKeyDown(
        keyCode: Int,
        e: KeyEvent,
        session: TerminalSession?,
    ): Boolean = false

    override fun onKeyUp(
        keyCode: Int,
        e: KeyEvent,
    ): Boolean = false

    override fun onLongPress(event: MotionEvent): Boolean = false

    override fun readControlKey(): Boolean = ctrlActive

    override fun readAltKey(): Boolean = altActive

    override fun readShiftKey(): Boolean = false

    override fun readFnKey(): Boolean = false

    override fun onCodePoint(
        codePoint: Int,
        ctrlDown: Boolean,
        session: TerminalSession?,
    ): Boolean {
        consumeModifiers()
        return false
    }

    override fun onEmulatorSet() = Unit

    override fun logError(
        tag: String,
        message: String,
    ) {
        Log.e(tag, message)
    }

    override fun logWarn(
        tag: String,
        message: String,
    ) {
        Log.w(tag, message)
    }

    override fun logInfo(
        tag: String,
        message: String,
    ) {
        Log.i(tag, message)
    }

    override fun logDebug(
        tag: String,
        message: String,
    ) {
        Log.d(tag, message)
    }

    override fun logVerbose(
        tag: String,
        message: String,
    ) {
        Log.v(tag, message)
    }

    override fun logStackTraceWithMessage(
        tag: String,
        message: String,
        e: Exception,
    ) {
        Log.e(tag, message, e)
    }

    override fun logStackTrace(
        tag: String,
        e: Exception,
    ) {
        Log.e(tag, "stack trace", e)
    }
}
