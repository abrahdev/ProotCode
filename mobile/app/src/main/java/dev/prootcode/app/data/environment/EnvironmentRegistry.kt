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

    fun update(environment: Environment) {
        val current = load()
        if (current.none { it.id == environment.id }) return
        save(current.map { if (it.id == environment.id) environment else it })
    }

    fun reorder(ids: List<String>) {
        val current = load()
        val byId = current.associateBy { it.id }
        val ordered = ids.mapNotNull { byId[it] } + current.filterNot { it.id in ids }
        save(ordered)
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
            put("presets", JSONArray(presets))
            put("settings", settings.toJson())
        }

    private fun EnvironmentSettings.toJson(): JSONObject =
        JSONObject().apply {
            put("icon", icon)
            put("autostart", autostart)
            put("language", language)
            put("timezone", timezone)
            put("mountDownloads", mountDownloads)
            put("downloadsPath", downloadsPath ?: JSONObject.NULL)
            put("extraMounts", JSONArray(extraMounts))
            put("autoCleanup", autoCleanup)
            put("codeServerPort", codeServerPort)
            put("codeServerLan", codeServerLan)
            put("networkSharing", networkSharing)
            put("proxy", proxy)
            put("adbShare", adbShare)
            put("customUser", customUser)
            put("userName", userName)
            put("ssh", ssh)
            put("sshPort", sshPort)
            put("backgroundExecution", backgroundExecution)
            put("priority", priority)
            put("debugMode", debugMode)
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
            settings = optJSONObject("settings")?.toEnvironmentSettings() ?: EnvironmentSettings(),
        )

    private fun JSONObject.toEnvironmentSettings(): EnvironmentSettings {
        val defaults = EnvironmentSettings()
        return EnvironmentSettings(
            icon = optString("icon", defaults.icon),
            autostart = optBoolean("autostart", defaults.autostart),
            language = optString("language", defaults.language),
            timezone = optString("timezone", defaults.timezone),
            mountDownloads = optBoolean("mountDownloads", defaults.mountDownloads),
            downloadsPath = if (isNull("downloadsPath")) null else optString("downloadsPath"),
            extraMounts =
                optJSONArray("extraMounts")
                    ?.let { array ->
                        (0 until array.length())
                            .mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
                    }.orEmpty(),
            autoCleanup = optBoolean("autoCleanup", defaults.autoCleanup),
            codeServerPort = optInt("codeServerPort", defaults.codeServerPort),
            codeServerLan = optBoolean("codeServerLan", defaults.codeServerLan),
            networkSharing = optBoolean("networkSharing", defaults.networkSharing),
            proxy = optString("proxy", defaults.proxy),
            adbShare = optBoolean("adbShare", defaults.adbShare),
            customUser = optBoolean("customUser", defaults.customUser),
            userName = optString("userName", defaults.userName),
            ssh = optBoolean("ssh", defaults.ssh),
            sshPort = optInt("sshPort", defaults.sshPort),
            backgroundExecution = optBoolean("backgroundExecution", defaults.backgroundExecution),
            priority =
                optString("priority", defaults.priority)
                    .takeIf { it in EnvironmentSettings.PRIORITIES }
                    ?: defaults.priority,
            debugMode = optBoolean("debugMode", defaults.debugMode),
        )
    }
}
