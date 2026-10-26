package dev.prootcode.app.data.catalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CatalogParserTest {
    @Test
    fun `parses distros and code-server entries`() {
        val catalog = CatalogParser.parse(catalog())

        assertEquals(1, catalog.distros.size)
        assertEquals("ubuntu-26.04", catalog.distros.single().id)
        assertEquals("x86_64", catalog.distros.single().arch)
        assertEquals(1, catalog.codeServer.size)
        assertEquals("4.143.1", catalog.codeServer.single().version)
    }

    @Test
    fun `filters entries by arch`() {
        val catalog =
            CatalogParser.parse(
                catalog(
                    distros = listOf(distro(), distro(arch = "arm64-v8a")),
                    codeServer = listOf(codeServer(), codeServer(arch = "arm64-v8a")),
                ),
            )

        assertEquals(1, catalog.distrosFor("x86_64").size)
        assertEquals(1, catalog.distrosFor("arm64-v8a").size)
        assertEquals(1, catalog.codeServerFor("x86_64").size)
        assertTrue(catalog.codeServerFor("armeabi-v7a").isEmpty())
    }

    @Test
    fun `accepts a catalog without arrays`() {
        val catalog = CatalogParser.parse("""{"schemaVersion":1}""")

        assertTrue(catalog.distros.isEmpty())
        assertTrue(catalog.codeServer.isEmpty())
    }

    @Test
    fun `rejects invalid json`() {
        assertFails("""{"schemaVersion":""")
    }

    @Test
    fun `rejects an unsupported schema version`() {
        assertFails("""{"schemaVersion":2}""")
        assertFails("""{"schemaVersion":0}""")
    }

    @Test
    fun `rejects an invalid sha256`() {
        assertFails(catalog(distros = listOf(distro(sha = "abc"))))
        assertFails(catalog(distros = listOf(distro(sha = "z".repeat(64)))))
    }

    @Test
    fun `rejects an unsupported arch`() {
        assertFails(catalog(distros = listOf(distro(arch = "armeabi-v7a"))))
        assertFails(catalog(codeServer = listOf(codeServer(arch = "i686"))))
    }

    @Test
    fun `rejects a url without https`() {
        assertFails(catalog(distros = listOf(distro(url = "http://example.com/rootfs.tar.gz"))))
    }

    @Test
    fun `rejects a non positive size`() {
        assertFails(catalog(distros = listOf(distro(size = 0))))
        assertFails(catalog(codeServer = listOf(codeServer(size = -1))))
    }

    @Test
    fun `rejects a missing bundled flag`() {
        val entry =
            """{"id":"ubuntu-26.04","name":"Ubuntu","version":"26.04","arch":"x86_64",""" +
                """"url":"$URL","sha256":"$SHA","size":1}"""
        assertFails(catalog(distros = listOf(entry)))
    }

    @Test
    fun `rejects duplicate entries`() {
        assertFails(catalog(distros = listOf(distro(), distro())))
        assertFails(catalog(codeServer = listOf(codeServer(), codeServer())))
    }

    @Test
    fun `rejects a missing required field`() {
        val entry =
            """{"id":"ubuntu-26.04","name":"Ubuntu","arch":"x86_64","url":"$URL",""" +
                """"sha256":"$SHA","size":1,"bundled":true}"""
        assertFails(catalog(distros = listOf(entry)))
    }

    private fun assertFails(json: String) {
        assertThrows(CatalogException::class.java) { CatalogParser.parse(json) }
    }

    private fun catalog(
        distros: List<String> = listOf(distro()),
        codeServer: List<String> = listOf(codeServer()),
    ): String =
        """{"schemaVersion":1,"distros":[${distros.joinToString(",")}],""" +
            """"codeServer":[${codeServer.joinToString(",")}]}"""

    private fun distro(
        id: String = "ubuntu-26.04",
        name: String = "Ubuntu 26.04 LTS",
        version: String = "26.04",
        arch: String = "x86_64",
        url: String = URL,
        sha: String = SHA,
        size: Long = 34931253,
        bundled: Boolean = true,
    ): String =
        """{"id":"$id","name":"$name","version":"$version","arch":"$arch","url":"$url",""" +
            """"sha256":"$sha","size":$size,"bundled":$bundled}"""

    private fun codeServer(
        version: String = "4.143.1",
        arch: String = "x86_64",
        url: String = CODE_SERVER_URL,
        sha: String = SHA,
        size: Long = 229583113,
        bundled: Boolean = true,
    ): String =
        """{"version":"$version","arch":"$arch","url":"$url",""" +
            """"sha256":"$sha","size":$size,"bundled":$bundled}"""

    private companion object {
        private const val URL = "https://example.com/rootfs.tar.gz"
        private const val CODE_SERVER_URL = "https://example.com/code-server.tar.gz"
        private const val SHA = "a496a960472ce474a59590b8987d3a1135d3cbef1991f3b1abe8cacfea8bf85a"
    }
}
