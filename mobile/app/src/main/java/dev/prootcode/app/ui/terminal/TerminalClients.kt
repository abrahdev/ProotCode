package dev.prootcode.app.ui.terminal

import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
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

    override fun readControlKey(): Boolean = false

    override fun readAltKey(): Boolean = false

    override fun readShiftKey(): Boolean = false

    override fun readFnKey(): Boolean = false

    override fun onCodePoint(
        codePoint: Int,
        ctrlDown: Boolean,
        session: TerminalSession?,
    ): Boolean = false

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
