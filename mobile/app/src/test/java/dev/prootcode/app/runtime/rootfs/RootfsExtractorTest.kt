package dev.prootcode.app.runtime.rootfs

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarConstants
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files

class RootfsExtractorTest {
    @TempDir
    lateinit var tempDir: File

    private val rootfs: File get() = File(tempDir, "rootfs")

    @Test
    fun `extracts files, symlinks and hardlinks and writes the marker`() {
        val archive =
            archive {
                directory("./bin")
                file("./bin/true", "ELF", mode = 493)
                symlink("./bin/sh", "true")
                hardlink("./bin/ls", "./bin/true")
            }

        val entries = RootfsExtractor.extract(ByteArrayInputStream(archive), rootfs, "v1")

        assertEquals(4, entries)
        assertEquals("ELF", File(rootfs, "bin/true").readText())
        assertTrue(Files.isSymbolicLink(File(rootfs, "bin/sh").toPath()))
        assertEquals("true", Files.readSymbolicLink(File(rootfs, "bin/sh").toPath()).toString())
        val hardlink = File(rootfs, "bin/ls")
        assertTrue(
            Files.isSymbolicLink(hardlink.toPath()) ||
                (hardlink.isFile && hardlink.readText() == "ELF"),
        )
        assertTrue(RootfsExtractor.isExtracted(rootfs, "v1"))
        assertFalse(RootfsExtractor.isExtracted(rootfs, "v2"))
    }

    @Test
    fun `marks the file as executable when the mode says so`() {
        assumePosix()
        val archive = archive { file("./bin/true", "ELF", mode = 493) }

        RootfsExtractor.extract(ByteArrayInputStream(archive), rootfs, "v1")

        assertTrue(File(rootfs, "bin/true").canExecute())
    }

    @Test
    fun `rejects entries outside the rootfs`() {
        val archive = archive { file("../evil", "nope") }

        val error =
            assertThrows(IOException::class.java) {
                RootfsExtractor.extract(ByteArrayInputStream(archive), rootfs, "v1")
            }

        assertTrue(error.message.orEmpty().contains("unsafe"))
        assertFalse(File(tempDir, "evil").exists())
    }

    private fun archive(block: TarArchiveOutputStream.() -> Unit): ByteArray {
        val bytes = ByteArrayOutputStream()
        GzipCompressorOutputStream(bytes).use { gzip ->
            TarArchiveOutputStream(gzip).use { tar ->
                tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
                tar.block()
            }
        }
        return bytes.toByteArray()
    }

    private fun TarArchiveOutputStream.directory(name: String) {
        putArchiveEntry(TarArchiveEntry("$name/"))
        closeArchiveEntry()
    }

    private fun TarArchiveOutputStream.file(
        name: String,
        content: String,
        mode: Int = 420,
    ) {
        val data = content.toByteArray()
        putArchiveEntry(
            TarArchiveEntry(name).apply {
                size = data.size.toLong()
                this.mode = mode
            },
        )
        write(data)
        closeArchiveEntry()
    }

    private fun TarArchiveOutputStream.symlink(
        name: String,
        linkTarget: String,
    ) {
        putArchiveEntry(
            TarArchiveEntry(name, TarConstants.LF_SYMLINK).apply {
                linkName = linkTarget
            },
        )
        closeArchiveEntry()
    }

    private fun TarArchiveOutputStream.hardlink(
        name: String,
        linkTarget: String,
    ) {
        putArchiveEntry(
            TarArchiveEntry(name, TarConstants.LF_LINK).apply {
                linkName = linkTarget
            },
        )
        closeArchiveEntry()
    }

    private fun assumePosix() {
        assumeTrue(File.separatorChar == '/', "requires a POSIX filesystem")
    }
}
