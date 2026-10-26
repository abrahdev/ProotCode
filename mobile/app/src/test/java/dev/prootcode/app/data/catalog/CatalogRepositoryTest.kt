package dev.prootcode.app.data.catalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException

class CatalogRepositoryTest {
    @TempDir
    lateinit var tempDir: File

    private val cacheFile: File get() = File(tempDir, "catalog.json")

    @Test
    fun `refresh stores the remote catalog in the cache`() {
        val remote = FakeSource { catalogJson("remote") }
        val repository = repository(remote = remote, bundled = FakeSource { catalogJson("bundled") })

        val catalog = repository.load(refresh = true)

        assertEquals("remote", catalog.distros.single().id)
        assertEquals(1, remote.calls)
        assertTrue(cacheFile.readText().contains("\"remote\""))
    }

    @Test
    fun `falls back to the cache when the remote fails`() {
        cacheFile.writeText(catalogJson("cached"))
        val remote = FakeSource { throw IOException("offline") }
        val repository = repository(remote = remote, bundled = FakeSource { catalogJson("bundled") })

        val catalog = repository.load(refresh = true)

        assertEquals("cached", catalog.distros.single().id)
    }

    @Test
    fun `falls back to the cache when the remote is invalid`() {
        cacheFile.writeText(catalogJson("cached"))
        val remote = FakeSource { "not json" }
        val repository = repository(remote = remote, bundled = FakeSource { catalogJson("bundled") })

        val catalog = repository.load(refresh = true)

        assertEquals("cached", catalog.distros.single().id)
    }

    @Test
    fun `does not call the remote when refreshing is disabled`() {
        cacheFile.writeText(catalogJson("cached"))
        val remote = FakeSource { catalogJson("remote") }
        val repository = repository(remote = remote, bundled = FakeSource { catalogJson("bundled") })

        val catalog = repository.load(refresh = false)

        assertEquals("cached", catalog.distros.single().id)
        assertEquals(0, remote.calls)
    }

    @Test
    fun `falls back to the bundled catalog when the cache is invalid`() {
        cacheFile.writeText("not json")
        val repository = repository(remote = null, bundled = FakeSource { catalogJson("bundled") })

        val catalog = repository.load()

        assertEquals("bundled", catalog.distros.single().id)
    }

    @Test
    fun `falls back to the bundled catalog when the remote fails without cache`() {
        val repository =
            repository(
                remote = FakeSource { throw IOException("offline") },
                bundled = FakeSource { catalogJson("bundled") },
            )

        val catalog = repository.load(refresh = true)

        assertEquals("bundled", catalog.distros.single().id)
    }

    @Test
    fun `fails when neither the cache nor the bundled catalog are valid`() {
        val repository = repository(remote = null, bundled = FakeSource { "not json" })

        assertThrows(CatalogException::class.java) { repository.load() }
    }

    private fun repository(
        remote: CatalogSource?,
        bundled: CatalogSource,
    ): CatalogRepository = CatalogRepository(cacheFile, remote, bundled)

    private fun catalogJson(id: String): String =
        """{"schemaVersion":1,"distros":[{"id":"$id","name":"Ubuntu","version":"26.04",""" +
            """"arch":"x86_64","url":"https://example.com/rootfs.tar.gz",""" +
            """"sha256":"$SHA","size":1,"bundled":true}]}"""

    private class FakeSource(
        private val body: () -> String,
    ) : CatalogSource {
        var calls = 0
            private set

        override fun load(): String {
            calls++
            return body()
        }
    }

    private companion object {
        private const val SHA = "a496a960472ce474a59590b8987d3a1135d3cbef1991f3b1abe8cacfea8bf85a"
    }
}
