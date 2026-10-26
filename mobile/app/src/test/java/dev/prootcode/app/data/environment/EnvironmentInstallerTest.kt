package dev.prootcode.app.data.environment

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

class EnvironmentInstallerTest {
    @TempDir
    lateinit var tempDir: File

    private val environmentsDir: File get() = File(tempDir, "environments")
    private val registry: EnvironmentRegistry get() = EnvironmentRegistry(File(tempDir, "environments.json"))
    private val installer: EnvironmentInstaller get() = EnvironmentInstaller(environmentsDir, registry)

    @Test
    fun `installs the rootfs, code-server and dns configuration`() {
        val environment =
            installer.install(
                request = request(),
                rootfs = ByteArrayInputStream(tarGz { file("./bin/true", "ELF") }),
                codeServer = ByteArrayInputStream(tarGz { file("./code-server/bin/code-server", "#!/bin/sh") }),
            )

        val rootfs = File(environmentsDir, "one/rootfs")
        assertEquals("ELF", File(rootfs, "bin/true").readText())
        assertTrue(File(rootfs, "opt/code-server/code-server/bin/code-server").isFile)
        assertEquals("nameserver 10.0.2.3\n", File(rootfs, "etc/resolv.conf").readText())
        assertEquals(environment, registry.load().single())
        assertTrue(environment.sizeBytes > 0)
        assertEquals("4.143.1", environment.codeServerVersion)
    }

    @Test
    fun `installs without code-server`() {
        val environment =
            installer.install(
                request = request(codeServerVersion = null),
                rootfs = ByteArrayInputStream(tarGz { file("./bin/true", "ELF") }),
                codeServer = null,
            )

        assertFalse(File(environmentsDir, "one/rootfs/opt/code-server").exists())
        assertEquals(null, environment.codeServerVersion)
    }

    @Test
    fun `reinstalling clears the previous environment`() {
        val rootfs = tarGz { file("./bin/true", "ELF") }
        installer.install(request(), ByteArrayInputStream(rootfs), null)
        File(environmentsDir, "one/rootfs/leftover.txt").writeText("old")

        installer.install(request(), ByteArrayInputStream(rootfs), null)

        assertFalse(File(environmentsDir, "one/rootfs/leftover.txt").exists())
        assertEquals(1, registry.load().size)
    }

    @Test
    fun `deletes the environment and its registry entry`() {
        installer.install(request(), ByteArrayInputStream(tarGz { file("./bin/true", "ELF") }), null)

        installer.delete("one")

        assertFalse(File(environmentsDir, "one").exists())
        assertTrue(registry.load().isEmpty())
    }

    private fun request(codeServerVersion: String? = "4.143.1"): InstallRequest =
        InstallRequest(
            id = "one",
            name = "Ubuntu",
            distroId = "ubuntu-26.04",
            distroVersion = "26.04",
            arch = "arm64-v8a",
            codeServerVersion = codeServerVersion,
            dnsServers = listOf("10.0.2.3"),
        )

    private fun tarGz(block: TarArchiveOutputStream.() -> Unit): ByteArray {
        val bytes = ByteArrayOutputStream()
        GzipCompressorOutputStream(bytes).use { gzip ->
            TarArchiveOutputStream(gzip).use { tar ->
                tar.block()
            }
        }
        return bytes.toByteArray()
    }

    private fun TarArchiveOutputStream.file(
        name: String,
        content: String,
    ) {
        val data = content.toByteArray()
        putArchiveEntry(
            TarArchiveEntry(name).apply {
                size = data.size.toLong()
                mode = 493
            },
        )
        write(data)
        closeArchiveEntry()
    }
}
