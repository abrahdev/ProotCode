package dev.prootcode.app.ui.terminal

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.termux.terminal.TerminalSession
import dev.prootcode.app.data.environment.Environment
import java.io.File

object TerminalSessionManager {
    const val NATIVE_ENVIRONMENT_ID = "native"

    private val sessionsByEnvironment = mutableMapOf<String, MutableList<TerminalSession>>()
    private val counters = mutableMapOf<String, Int>()
    private val firstOpenedAt = mutableMapOf<String, Long>()
    var onScreenUpdated: (() -> Unit)? = null
    var onSessionFinished: ((String, TerminalSession) -> Unit)? = null

    fun sessions(environmentId: String): List<TerminalSession> = sessionsByEnvironment[environmentId].orEmpty()

    fun uptimeMillis(environmentId: String): Long =
        firstOpenedAt[environmentId]
            ?.let { System.currentTimeMillis() - it }
            ?.coerceAtLeast(0L) ?: 0L

    fun ramBytes(environmentId: String): Long = sessions(environmentId).sumOf { session -> rssBytes(session.pid) }

    fun create(
        context: Context,
        environment: Environment,
    ): TerminalSession {
        val session =
            TerminalSessionFactory(context).create(
                environment = environment,
                onScreenUpdated = { onScreenUpdated?.invoke() },
                onSessionFinished = { finished ->
                    onSessionFinished?.invoke(environment.id, finished)
                },
            )
        register(context, environment.id, session, "Terminal")
        return session
    }

    fun createNative(context: Context): TerminalSession {
        val session =
            TerminalSessionFactory(context).createNative(
                onScreenUpdated = { onScreenUpdated?.invoke() },
                onSessionFinished = { finished ->
                    onSessionFinished?.invoke(NATIVE_ENVIRONMENT_ID, finished)
                },
            )
        register(context, NATIVE_ENVIRONMENT_ID, session, "Shell")
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
            firstOpenedAt.remove(environmentId)
        }
        ensureService(context)
    }

    fun closeAll(
        context: Context,
        environmentId: String,
    ) {
        sessionsByEnvironment.remove(environmentId)?.forEach { it.finishIfRunning() }
        counters.remove(environmentId)
        firstOpenedAt.remove(environmentId)
        ensureService(context)
    }

    fun sessionExited(
        context: Context,
        environmentId: String,
        session: TerminalSession,
    ) {
        val sessions = sessionsByEnvironment[environmentId] ?: return
        if (!sessions.remove(session)) return
        if (sessions.isEmpty()) {
            sessionsByEnvironment.remove(environmentId)
            firstOpenedAt.remove(environmentId)
        }
        ensureService(context)
    }

    fun hasSessions(): Boolean = sessionsByEnvironment.values.any { it.isNotEmpty() }

    fun previewLines(
        session: TerminalSession,
        maxLines: Int = 6,
    ): List<String> =
        runCatching {
            session.emulator
                ?.screen
                ?.transcriptText
                ?.lineSequence()
                ?.filter { it.isNotBlank() }
                ?.toList()
                ?.takeLast(maxLines)
                ?: emptyList()
        }.getOrDefault(emptyList())

    private fun register(
        context: Context,
        environmentId: String,
        session: TerminalSession,
        prefix: String,
    ) {
        val number = (counters[environmentId] ?: 0) + 1
        counters[environmentId] = number
        session.mSessionName = "$prefix $number"
        firstOpenedAt.putIfAbsent(environmentId, System.currentTimeMillis())
        sessionsByEnvironment.getOrPut(environmentId) { mutableListOf() }.add(session)
        ensureService(context)
    }

    private fun rssBytes(pid: Int): Long {
        if (pid <= 0) return 0L
        return runCatching {
            File("/proc/$pid/status").useLines { lines ->
                lines
                    .firstOrNull { it.startsWith("VmRSS:") }
                    ?.substringAfter(':')
                    ?.trim()
                    ?.substringBefore(' ')
                    ?.toLongOrNull()
                    ?.times(1024L) ?: 0L
            }
        }.getOrDefault(0L)
    }

    private fun ensureService(context: Context) {
        val intent = Intent(context, TerminalService::class.java)
        if (hasSessions()) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.stopService(intent)
        }
    }
}
