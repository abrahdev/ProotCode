package dev.prootcode.app.spike

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object SpikeReportWriter {
    private const val TAG = "ProotSpike"
    private const val FILE_NAME = "report"

    fun write(
        context: Context,
        report: SpikeReport,
    ): File {
        val json =
            JSONObject().apply {
                put("sdk", report.sdk)
                put("abi", report.abi)
                put("targetSdk", report.targetSdk)
                put("timestamp", report.timestamp)
                put("passed", report.passed)
                put(
                    "steps",
                    JSONArray().apply {
                        report.steps.forEach { step ->
                            put(
                                JSONObject().apply {
                                    put("id", step.id)
                                    put("title", step.title)
                                    put("status", step.status.name)
                                    put("detail", step.detail)
                                    put("durationMs", step.durationMs)
                                    put("output", step.output)
                                },
                            )
                        }
                    },
                )
            }
        val text = renderText(report)
        val directories =
            listOf(
                File(context.filesDir, "diagnostics"),
                File(context.getExternalFilesDir(null) ?: context.filesDir, "diagnostics"),
            )
        var primary: File? = null
        directories.forEach { directory ->
            directory.mkdirs()
            File(directory, "$FILE_NAME.json").writeText(json.toString(2))
            File(directory, "$FILE_NAME.txt").writeText(text)
            if (primary == null) primary = File(directory, "$FILE_NAME.json")
        }
        Log.i(TAG, "report: ${primary?.absolutePath}")
        return primary ?: File(context.filesDir, "$FILE_NAME.json")
    }

    private fun renderText(report: SpikeReport): String =
        buildString {
            appendLine("ProotCode proot spike")
            appendLine(
                "API ${report.sdk}, targetSdk ${report.targetSdk}, abi ${report.abi}, at ${report.timestamp}",
            )
            appendLine(if (report.passed) "RESULT: PASS" else "RESULT: FAIL")
            appendLine()
            report.steps.forEach { step ->
                appendLine("[${step.status}] ${step.id} - ${step.title} (${step.durationMs} ms)")
                appendLine("  ${step.detail}")
                step.output.lineSequence().filter { it.isNotBlank() }.forEach { line ->
                    appendLine("  | $line")
                }
            }
        }
}
