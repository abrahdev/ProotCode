package dev.prootcode.app.spike

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SpikeRunner(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val environment = ProotEnvironment(appContext)
    private val runner = CommandRunner()

    fun run(onStep: (StepResult) -> Unit): SpikeReport {
        environment.prepare()
        val abi = assetAbi()
        val steps = mutableListOf<StepResult>()

        fun record(result: StepResult) {
            steps.add(result)
            Log.i(TAG, "${result.status} ${result.id}: ${result.detail}")
            result.output
                .lineSequence()
                .filter { it.isNotBlank() }
                .take(MAX_LOGGED_LINES)
                .forEach { Log.i(TAG, "  $it") }
            onStep(result)
        }

        record(
            runStep("device-info", "Device information") {
                val detail =
                    "API ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE}), " +
                        "targetSdk ${appContext.applicationInfo.targetSdkVersion}, " +
                        "abi $abi, device ${Build.MANUFACTURER} ${Build.MODEL}"
                StepOutcome(StepStatus.PASS, detail, "")
            },
        )

        val extraction = runStep("rootfs-extract", "Extract Ubuntu 26.04 rootfs") { extractionOutcome(abi) }
        record(extraction)
        if (extraction.status != StepStatus.PASS) {
            record(blocked("rootfs-dependent", "Skipped: rootfs is not available"))
            return report(abi, steps)
        }

        environment.writeResolvConf(environment.dnsServers())

        val expectedDenied = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        record(
            runStep("exec-datadir-control", "Exec binary from filesDir (control)") { controlOutcome(expectedDenied) },
        )
        record(runStep("exec-nativelib-control", "Exec binary from nativeLibraryDir (control)") { nativeLibOutcome() })

        record(
            runStep("proot-true", "proot /bin/true") {
                guestOutcome("/bin/true")
            },
        )
        record(
            runStep("proot-true-noseccomp", "proot /bin/true without seccomp") {
                guestOutcome("/bin/true", noSeccomp = true)
            },
        )
        record(
            runStep("proot-uid", "proot id -u") {
                guestOutcome("id -u", expected = "0")
            },
        )
        record(
            runStep("bridge-write", "Write to ~/projects_bridge from proot") {
                bridgeWriteOutcome()
            },
        )
        record(
            runStep("bridge-read", "Read from ~/projects_bridge inside proot") {
                bridgeReadOutcome()
            },
        )
        record(
            runStep("apt-update", "apt-get update") {
                guestOutcome("apt-get update", timeoutSeconds = 600, retryWithoutSeccomp = true)
            },
        )
        record(
            runStep("apt-install", "apt-get install hello") {
                guestOutcome(
                    "apt-get install -y --no-install-recommends hello",
                    timeoutSeconds = 900,
                    retryWithoutSeccomp = true,
                )
            },
        )
        record(
            runStep("installed-binary", "Run installed hello binary") {
                guestOutcome("hello", expected = "Hello, world!")
            },
        )

        return report(abi, steps)
    }

    private fun report(
        abi: String,
        steps: List<StepResult>,
    ): SpikeReport =
        SpikeReport(
            sdk = Build.VERSION.SDK_INT,
            abi = abi,
            targetSdk = appContext.applicationInfo.targetSdkVersion,
            timestamp =
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
                    .apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date()),
            steps = steps.toList(),
        )

    private fun extractionOutcome(abi: String): StepOutcome {
        val asset = RootfsExtractor.assetPath(abi)
        val available =
            try {
                appContext.assets.open(asset).close()
                true
            } catch (error: IOException) {
                false
            }
        if (!available) {
            return StepOutcome(
                StepStatus.FAIL,
                "missing asset $asset; run mobile/tools/fetch-proot-spike-artifacts.sh $abi",
                "",
            )
        }
        if (RootfsExtractor.isExtracted(environment.rootfsDir)) {
            return StepOutcome(StepStatus.PASS, "already extracted", "")
        }
        var entries = 0
        RootfsExtractor.extract(appContext, abi, environment.rootfsDir) { count -> entries = count }
        return StepOutcome(StepStatus.PASS, "$entries entries extracted", "")
    }

    private fun controlOutcome(expectedDenied: Boolean): StepOutcome {
        val control = File(environment.controlDir, "control")
        environment.prootLoader.copyTo(control, overwrite = true)
        control.setExecutable(true, false)
        return try {
            val result = runner.run(listOf(control.absolutePath), emptyMap(), SHORT_TIMEOUT_SECONDS)
            val status = if (expectedDenied) StepStatus.FAIL else StepStatus.PASS
            StepOutcome(status, "exec allowed, exit=${result.exitCode}", result.output)
        } catch (error: IOException) {
            val denied = isPermissionDenied(error)
            val status = if (denied == expectedDenied) StepStatus.PASS else StepStatus.FAIL
            val detail = if (denied) "exec denied: ${error.message}" else "unexpected error: ${error.message}"
            StepOutcome(status, detail, "")
        }
    }

    private fun nativeLibOutcome(): StepOutcome =
        try {
            val result = runner.run(listOf(environment.prootLoader.absolutePath), emptyMap(), SHORT_TIMEOUT_SECONDS)
            StepOutcome(StepStatus.PASS, "exec allowed, exit=${result.exitCode}", result.output)
        } catch (error: IOException) {
            StepOutcome(StepStatus.FAIL, "exec denied: ${error.message}", "")
        }

    private fun bridgeWriteOutcome(): StepOutcome {
        val file = File(environment.projectsDir, "proot.txt")
        file.delete()
        val outcome = guestOutcome("echo hello-from-proot > /root/projects_bridge/proot.txt")
        if (outcome.status != StepStatus.PASS) return outcome
        if (!file.isFile) {
            return StepOutcome(StepStatus.FAIL, "file written inside proot is not visible from the app", outcome.output)
        }
        val content = file.readText().trim()
        val status = if (content == "hello-from-proot") StepStatus.PASS else StepStatus.FAIL
        return StepOutcome(status, "app reads: '$content'", outcome.output)
    }

    private fun bridgeReadOutcome(): StepOutcome {
        val file = File(environment.projectsDir, "app.txt")
        file.writeText("hello-from-android\n")
        val outcome =
            guestOutcome(
                "cat /root/projects_bridge/app.txt",
                expected = "hello-from-android",
            )
        if (outcome.status != StepStatus.PASS) return outcome
        return StepOutcome(StepStatus.PASS, "proot reads the file written by the app", outcome.output)
    }

    private fun guestOutcome(
        script: String,
        noSeccomp: Boolean = false,
        timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS,
        expected: String? = null,
        retryWithoutSeccomp: Boolean = false,
    ): StepOutcome {
        var result =
            runner.run(
                environment.guestCommand(script),
                environment.prootEnvironment(noSeccomp),
                timeoutSeconds,
            )
        var retried = false
        if (retryWithoutSeccomp && !noSeccomp && result.output.contains(SECCOMP_ASSERTION)) {
            retried = true
            result =
                runner.run(
                    environment.guestCommand(script),
                    environment.prootEnvironment(true),
                    timeoutSeconds,
                )
        }
        val status =
            when {
                result.timedOut -> StepStatus.FAIL
                expected != null -> if (result.output.contains(expected)) StepStatus.PASS else StepStatus.FAIL
                result.exitCode != 0 -> StepStatus.FAIL
                else -> StepStatus.PASS
            }
        val detail =
            buildString {
                append("exit=").append(result.exitCode)
                if (result.timedOut) append(", timed out")
                if (retried) append(", retried without seccomp")
                append(" in ").append(result.durationMs).append(" ms")
            }
        return StepOutcome(status, detail, result.output)
    }

    private fun isPermissionDenied(error: IOException): Boolean {
        val message = error.message.orEmpty()
        return message.contains("=13") || message.contains("Permission denied", ignoreCase = true)
    }

    private fun runStep(
        id: String,
        title: String,
        block: () -> StepOutcome,
    ): StepResult {
        val startedAt = System.currentTimeMillis()
        val outcome =
            try {
                block()
            } catch (error: Throwable) {
                StepOutcome(
                    StepStatus.FAIL,
                    "${error.javaClass.simpleName}: ${error.message}",
                    error.stackTraceToString(),
                )
            }
        return StepResult(
            id = id,
            title = title,
            status = outcome.status,
            detail = outcome.detail,
            output = outcome.output,
            durationMs = System.currentTimeMillis() - startedAt,
        )
    }

    private fun blocked(
        id: String,
        detail: String,
    ): StepResult = StepResult(id, "Remaining checks", StepStatus.SKIP, detail, "", 0)

    private fun assetAbi(): String =
        Build.SUPPORTED_ABIS.firstOrNull { it in ASSET_ABIS }
            ?: Build.SUPPORTED_ABIS.firstOrNull().orEmpty()

    private data class StepOutcome(
        val status: StepStatus,
        val detail: String,
        val output: String,
    )

    companion object {
        private const val TAG = "ProotSpike"
        private const val MAX_LOGGED_LINES = 60
        private const val SHORT_TIMEOUT_SECONDS = 15L
        private const val DEFAULT_TIMEOUT_SECONDS = 120L
        private const val SECCOMP_ASSERTION = "IS_IN_SYSENTER"
        private val ASSET_ABIS = setOf("arm64-v8a", "x86_64")
    }
}
