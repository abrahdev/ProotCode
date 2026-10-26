package dev.prootcode.app.data.catalog

data class Catalog(
    val schemaVersion: Int,
    val distros: List<DistroArtifact>,
    val codeServer: List<CodeServerArtifact>,
) {
    fun distrosFor(arch: String): List<DistroArtifact> = distros.filter { it.arch == arch }

    fun codeServerFor(arch: String): List<CodeServerArtifact> = codeServer.filter { it.arch == arch }
}

data class DistroArtifact(
    val id: String,
    val name: String,
    val version: String,
    val arch: String,
    val url: String,
    val sha256: String,
    val size: Long,
    val bundled: Boolean,
)

data class CodeServerArtifact(
    val version: String,
    val arch: String,
    val url: String,
    val sha256: String,
    val size: Long,
    val bundled: Boolean,
)
