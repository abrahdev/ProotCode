package dev.prootcode.app.data.catalog

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class CatalogException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

object CatalogParser {
    const val SUPPORTED_SCHEMA_VERSION = 1
    val SUPPORTED_ARCHES = setOf("x86_64", "arm64-v8a")

    private val SHA256 = Regex("[a-fA-F0-9]{64}")

    fun parse(json: String): Catalog {
        val root =
            try {
                JSONObject(json)
            } catch (error: JSONException) {
                throw CatalogException("invalid catalog JSON: ${error.message}", error)
            }
        val schemaVersion = root.optInt("schemaVersion", -1)
        if (schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            throw CatalogException("unsupported schema version: $schemaVersion")
        }
        val distros = parseDistros(root.optJSONArray("distros"))
        val codeServer = parseCodeServer(root.optJSONArray("codeServer"))
        return Catalog(schemaVersion, distros, codeServer)
    }

    private fun parseDistros(array: JSONArray?): List<DistroArtifact> {
        val distros =
            array?.let { entries ->
                (0 until entries.length()).map { index ->
                    parseDistro(entries.getJSONObject(index))
                }
            } ?: emptyList()
        val duplicates = distros.groupBy { it.id to it.arch }.filterValues { it.size > 1 }.keys
        if (duplicates.isNotEmpty()) {
            throw CatalogException("duplicate distro entries: $duplicates")
        }
        return distros
    }

    private fun parseDistro(entry: JSONObject): DistroArtifact =
        DistroArtifact(
            id = requiredString(entry, "id", "distro"),
            name = requiredString(entry, "name", "distro"),
            version = requiredString(entry, "version", "distro"),
            arch = requiredArch(entry, "distro"),
            url = requiredHttpsUrl(entry, "distro"),
            sha256 = requiredSha256(entry, "distro"),
            size = requiredSize(entry, "distro"),
            bundled = requiredBoolean(entry, "bundled", "distro"),
        )

    private fun parseCodeServer(array: JSONArray?): List<CodeServerArtifact> {
        val entries =
            array?.let { items ->
                (0 until items.length()).map { index ->
                    parseCodeServerEntry(items.getJSONObject(index))
                }
            } ?: emptyList()
        val duplicates = entries.groupBy { it.version to it.arch }.filterValues { it.size > 1 }.keys
        if (duplicates.isNotEmpty()) {
            throw CatalogException("duplicate code-server entries: $duplicates")
        }
        return entries
    }

    private fun parseCodeServerEntry(entry: JSONObject): CodeServerArtifact =
        CodeServerArtifact(
            version = requiredString(entry, "version", "code-server"),
            arch = requiredArch(entry, "code-server"),
            url = requiredHttpsUrl(entry, "code-server"),
            sha256 = requiredSha256(entry, "code-server"),
            size = requiredSize(entry, "code-server"),
            bundled = requiredBoolean(entry, "bundled", "code-server"),
        )

    private fun requiredString(
        entry: JSONObject,
        key: String,
        context: String,
    ): String {
        val value = entry.optString(key, "").trim()
        if (value.isEmpty()) throw CatalogException("$context: missing $key")
        return value
    }

    private fun requiredArch(
        entry: JSONObject,
        context: String,
    ): String {
        val arch = requiredString(entry, "arch", context)
        if (arch !in SUPPORTED_ARCHES) throw CatalogException("$context: unsupported arch $arch")
        return arch
    }

    private fun requiredHttpsUrl(
        entry: JSONObject,
        context: String,
    ): String {
        val url = requiredString(entry, "url", context)
        if (!url.startsWith("https://")) throw CatalogException("$context: url must use https")
        return url
    }

    private fun requiredSha256(
        entry: JSONObject,
        context: String,
    ): String {
        val sha256 = requiredString(entry, "sha256", context)
        if (!SHA256.matches(sha256)) throw CatalogException("$context: invalid sha256")
        return sha256.lowercase()
    }

    private fun requiredSize(
        entry: JSONObject,
        context: String,
    ): Long {
        val size = entry.optLong("size", -1)
        if (size <= 0) throw CatalogException("$context: size must be positive")
        return size
    }

    private fun requiredBoolean(
        entry: JSONObject,
        key: String,
        context: String,
    ): Boolean {
        if (!entry.has(key)) throw CatalogException("$context: missing $key")
        return entry.getBoolean(key)
    }
}
