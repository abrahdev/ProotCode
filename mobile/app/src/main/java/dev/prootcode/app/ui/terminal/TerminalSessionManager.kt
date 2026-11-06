package dev.prootcode.app.ui.terminal

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.termux.terminal.TerminalSession
import dev.prootcode.app.data.environment.Environment

object TerminalSessionManager {
    private val sessionsByEnvironment = mutableMapOf<String, MutableList<TerminalSession>>()
    private val counters = mutableMapOf<String, Int>()
    var onScreenUpdated: (() -> Unit)? = null

    fun sessions(environmentId: String): List<TerminalSession> = sessionsByEnvironment[environmentId].orEmpty()

    fun create(
        context: Context,
        environment: Environment,
    ): TerminalSession {
        val session =
            TerminalSessionFactory(context).create(environment) {
                onScreenUpdated?.invoke()
            }
        val number = (counters[environment.id] ?: 0) + 1
        counters[environment.id] = number
        session.mSessionName = "Terminal $number"
        sessionsByEnvironment.getOrPut(environment.id) { mutableListOf() }.add(session)
        ensureService(context)
        return session
    }

    fun close(
        context: Context,
        environmentId: String,
        session: TerminalSession,
    ) {
        session.finishIfRunning()
        sessionsByEnvironment[environmentId]?.remove(session)
        if (sessionsByEnvironment[environmentId].isNullOrEmpty()) {
            sessionsByEnvironment.remove(environmentId)
        }
        ensureService(context)
    }

    fun closeAll(
        context: Context,
        environmentId: String,
    ) {
        sessionsByEnvironment.remove(environmentId)?.forEach { it.finishIfRunning() }
        counters.remove(environmentId)
        ensureService(context)
    }

    fun hasSessions(): Boolean = sessionsByEnvironment.values.any { it.isNotEmpty() }

    private fun ensureService(context: Context) {
        val intent = Intent(context, TerminalService::class.java)
        if (hasSessions()) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.stopService(intent)
        }
    }
}
