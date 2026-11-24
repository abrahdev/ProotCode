package dev.prootcode.app.ui.terminal

import dev.prootcode.app.runtime.codeserver.CodeServerManager
import java.io.File
import kotlin.math.roundToInt

data class EnvironmentMetrics(
    val cpuPercent: Int,
    val ramBytes: Long,
)

object ResourceMonitor {
    data class CpuSample(
        val ticks: Long,
        val atMillis: Long,
    )

    private const val CLOCK_TICKS_PER_SECOND = 100.0
    private val lastSamples = mutableMapOf<String, CpuSample>()

    @Synchronized
    fun cpuPercent(environmentId: String): Int {
        val (metrics, sample) = sample(environmentId, lastSamples[environmentId])
        lastSamples[environmentId] = sample
        return metrics.cpuPercent
    }

    fun ramBytes(environmentId: String): Long =
        rootPids(environmentId)
            .flatMap { processTree(it) }
            .distinct()
            .sumOf { rssBytes(it) }

    fun sample(
        environmentId: String,
        previous: CpuSample?,
    ): Pair<EnvironmentMetrics, CpuSample> {
        val pids =
            rootPids(environmentId)
                .flatMap { processTree(it) }
                .distinct()
        val ticks = pids.sumOf { cpuTicks(it) }
        val ram = pids.sumOf { rssBytes(it) }
        val now = System.currentTimeMillis()
        val cpu =
            if (previous == null || now <= previous.atMillis) {
                0
            } else {
                val elapsedSeconds = (now - previous.atMillis) / 1000.0
                val usedSeconds = (ticks - previous.ticks).coerceAtLeast(0) / CLOCK_TICKS_PER_SECOND
                ((usedSeconds / elapsedSeconds) * 100).roundToInt().coerceIn(0, 100)
            }
        return EnvironmentMetrics(
            cpuPercent = cpu,
            ramBytes = ram,
        ) to CpuSample(ticks = ticks, atMillis = now)
    }

    private fun rootPids(environmentId: String): List<Int> =
        buildList {
            TerminalSessionManager
                .sessions(environmentId)
                .mapNotNull { session -> session.pid.takeIf { it > 0 } }
                .forEach(::add)
            CodeServerManager.processId(environmentId)?.let(::add)
        }

    internal fun processTree(pid: Int): List<Int> {
        if (pid <= 0) return emptyList()
        val result = linkedSetOf<Int>()
        val pending = ArrayDeque<Int>()
        pending.add(pid)
        while (pending.isNotEmpty()) {
            val current = pending.removeFirst()
            if (!result.add(current)) continue
            runCatching {
                File("/proc/$current/task/$current/children")
                    .readText()
                    .trim()
                    .split(' ')
                    .mapNotNull { it.toIntOrNull() }
                    .forEach { pending.add(it) }
            }
        }
        return result.toList()
    }

    private fun cpuTicks(pid: Int): Long =
        runCatching {
            val stat = File("/proc/$pid/stat").readText()
            val fields = stat.substringAfterLast(')').trim().split(' ')
            val utime = fields.getOrNull(11)?.toLongOrNull() ?: 0L
            val stime = fields.getOrNull(12)?.toLongOrNull() ?: 0L
            utime + stime
        }.getOrDefault(0L)

    private fun rssBytes(pid: Int): Long =
        runCatching {
            File("/proc/$pid/status").useLines { lines ->
                lines
                    .firstOrNull { it.startsWith("VmRSS:") }
                    ?.substringAfter(':')
                    ?.trim()
                    ?.substringBefore(' ')
                    ?.toLongOrNull()
                    ?.times(1024L)
                    ?: 0L
            }
        }.getOrDefault(0L)
}
