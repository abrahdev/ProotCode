package dev.prootcode.app.data.environment

data class Environment(
    val id: String,
    val name: String,
    val distroId: String,
    val distroVersion: String,
    val arch: String,
    val codeServerVersion: String?,
    val createdAtMillis: Long,
    val sizeBytes: Long,
)
