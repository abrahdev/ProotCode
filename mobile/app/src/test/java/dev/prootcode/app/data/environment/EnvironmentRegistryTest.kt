package dev.prootcode.app.data.environment

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class EnvironmentRegistryTest {
    @TempDir
    lateinit var tempDir: File

    private val registry: EnvironmentRegistry get() = EnvironmentRegistry(File(tempDir, "environments.json"))

    @Test
    fun `adds and loads environments`() {
        registry.add(environment(id = "one", name = "Ubuntu", size = 100))
        registry.add(environment(id = "two", name = "Debian", size = 200))

        val environments = registry.load()

        assertEquals(2, environments.size)
        assertEquals("one", environments.first().id)
        assertEquals(300, registry.totalBytes())
    }

    @Test
    fun `keeps a null code-server version`() {
        registry.add(environment(id = "one", name = "Ubuntu", codeServerVersion = null))

        assertNull(registry.load().single().codeServerVersion)
    }

    @Test
    fun `replaces an environment with the same id`() {
        registry.add(environment(id = "one", name = "Ubuntu", size = 100))
        registry.add(environment(id = "one", name = "Ubuntu 2", size = 150))

        assertEquals(1, registry.load().size)
        assertEquals("Ubuntu 2", registry.load().single().name)
        assertEquals(150, registry.totalBytes())
    }

    @Test
    fun `removes an environment`() {
        registry.add(environment(id = "one", name = "Ubuntu"))
        registry.add(environment(id = "two", name = "Debian"))

        registry.remove("one")

        assertEquals(listOf("two"), registry.load().map { it.id })
    }

    @Test
    fun `detects duplicate names ignoring case`() {
        registry.add(environment(id = "one", name = "Ubuntu"))

        assertTrue(registry.isNameTaken("ubuntu"))
        assertFalse(registry.isNameTaken("Debian"))
    }

    @Test
    fun `returns an empty list for a corrupt file`() {
        File(tempDir, "environments.json").writeText("not json")

        assertTrue(registry.load().isEmpty())
    }

    private fun environment(
        id: String,
        name: String,
        size: Long = 0,
        codeServerVersion: String? = "4.143.1",
    ): Environment =
        Environment(
            id = id,
            name = name,
            distroId = "ubuntu-26.04",
            distroVersion = "26.04",
            arch = "arm64-v8a",
            codeServerVersion = codeServerVersion,
            createdAtMillis = 1,
            sizeBytes = size,
        )
}
