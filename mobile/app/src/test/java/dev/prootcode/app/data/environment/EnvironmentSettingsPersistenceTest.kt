package dev.prootcode.app.data.environment

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class EnvironmentSettingsPersistenceTest {
    @TempDir
    lateinit var tempDir: File

    private val registry: EnvironmentRegistry get() = EnvironmentRegistry(File(tempDir, "environments.json"))

    @Test
    fun `persists environment settings`() {
        val settings =
            EnvironmentSettings(
                icon = "server",
                autostart = true,
                language = "Español",
                timezone = "Europe/Madrid",
                mountDownloads = false,
                extraMounts = listOf("/tmp/one", "/tmp/two"),
                autoCleanup = false,
                codeServerPort = 9090,
                codeServerLan = true,
                networkSharing = false,
                proxy = "http://proxy:8080",
                adbShare = true,
                customUser = true,
                userName = "dev",
                ssh = true,
                sshPort = 2200,
                backgroundExecution = false,
                priority = EnvironmentSettings.PRIORITY_LOW,
                debugMode = true,
            )
        registry.add(environment(settings = settings))

        assertEquals(settings, registry.load().single().settings)
    }

    @Test
    fun `defaults settings when the field is missing`() {
        File(tempDir, "environments.json").writeText(
            """
            [
              {
                "id": "one",
                "name": "Ubuntu",
                "distroId": "ubuntu",
                "distroVersion": "26.04",
                "arch": "arm64-v8a",
                "codeServerVersion": null,
                "createdAtMillis": 1,
                "sizeBytes": 2
              }
            ]
            """.trimIndent(),
        )

        assertEquals(EnvironmentSettings(), registry.load().single().settings)
    }

    @Test
    fun `reorders environments`() {
        registry.add(environment(id = "one", settings = EnvironmentSettings()))
        registry.add(environment(id = "two", settings = EnvironmentSettings()))
        registry.add(environment(id = "three", settings = EnvironmentSettings()))

        registry.reorder(listOf("three", "one", "two"))

        assertEquals(listOf("three", "one", "two"), registry.load().map { it.id })
    }

    @Test
    fun `reorder keeps environments missing from the requested order`() {
        registry.add(environment(id = "one"))
        registry.add(environment(id = "two"))

        registry.reorder(listOf("two"))

        assertEquals(listOf("two", "one"), registry.load().map { it.id })
    }

    @Test
    fun `update only touches an existing environment`() {
        registry.add(environment(id = "one", name = "Ubuntu"))
        val updated = registry.load().single().copy(name = "Debian", settings = EnvironmentSettings(icon = "code"))

        registry.update(updated)
        registry.update(environment(id = "missing"))

        assertEquals(listOf(updated), registry.load())
        val reloaded = registry.load().single()
        assertTrue(reloaded.settings.icon == "code")
    }

    private fun environment(
        id: String = "one",
        name: String = "Ubuntu",
        settings: EnvironmentSettings = EnvironmentSettings(),
    ): Environment =
        Environment(
            id = id,
            name = name,
            distroId = "ubuntu",
            distroVersion = "26.04",
            arch = "arm64-v8a",
            codeServerVersion = null,
            createdAtMillis = 1,
            sizeBytes = 2,
            settings = settings,
        )
}
