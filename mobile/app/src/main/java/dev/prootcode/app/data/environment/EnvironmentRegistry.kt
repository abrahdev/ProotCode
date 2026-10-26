package dev.prootcode.app.data.environment

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class EnvironmentRegistry(
    private val file: File,
) {
    fun load(): List<Environment> {
        if (!file.isFile) return emptyList()
        val array =
            runCatching { JSONArray(file.readText()) }.getOrElse { return emptyList() }
        return (0 until array.length()).mapNotNull { index ->
            runCatching { array.getJSONObject(index).toEnvironment() }.getOrNull()
        }
    }

    fun add(environment: Environment) {
        val updated = load().filterNot { it.id == environment.id } + environment
        save(updated)
    }

    fun remove(id: String) {
        save(load().filterNot { it.id == id })
    }

    fun totalBytes(): Long = load().sumOf { it.sizeBytes }

    fun isNameTaken(name: String): Boolean = load().any { it.name.equals(name, ignoreCase = true) }

    private fun save(environments: List<Environment>) {
        file.parentFile?.mkdirs()
        file.writeText(JSONArray(environments.map { it.toJson() }).toString(2))
    }

    private fun Environment.toJson(): JSONObject =
        JSONObject().apply {
            put("id", id)
            put("name", name)
            put("distroId", distroId)
            put("distroVersion", distroVersion)
            put("arch", arch)
            put("codeServerVersion", codeServerVersion ?: JSONObject.NULL)
            put("createdAtMillis", createdAtMillis)
            put("sizeBytes", sizeBytes)
            put("presets", org.json.JSONArray(presets))
        }

    private fun JSONObject.toEnvironment(): Environment =
        Environment(
            id = getString("id"),
            name = getString("name"),
            distroId = getString("distroId"),
            distroVersion = getString("distroVersion"),
            arch = getString("arch"),
            codeServerVersion = if (isNull("codeServerVersion")) null else getString("codeServerVersion"),
            createdAtMillis = getLong("createdAtMillis"),
            sizeBytes = getLong("sizeBytes"),
            presets =
                optJSONArray("presets")
                    ?.let { array ->
                        (0 until array.length())
                            .mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
                    }.orEmpty(),
        )
}
