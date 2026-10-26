package dev.prootcode.app.spike

data class StepResult(
    val id: String,
    val title: String,
    val status: StepStatus,
    val detail: String,
    val output: String,
    val durationMs: Long,
)

enum class StepStatus {
    PASS,
    FAIL,
    SKIP,
}

data class SpikeReport(
    val sdk: Int,
    val abi: String,
    val targetSdk: Int,
    val timestamp: String,
    val steps: List<StepResult>,
) {
    val passed: Boolean get() = steps.none { it.status == StepStatus.FAIL }
}
