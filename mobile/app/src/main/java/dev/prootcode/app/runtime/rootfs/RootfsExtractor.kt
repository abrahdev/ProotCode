package dev.prootcode.app.runtime.rootfs

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

object RootfsExtractor {
    private const val MARKER = ".extracted"

    fun isExtracted(
        rootfsDir: File,
        markerValue: String,
    ): Boolean {
        val marker = File(rootfsDir, MARKER)
        return marker.isFile && marker.readText() == markerValue
    }

    fun extract(
        input: InputStream,
        rootfsDir: File,
        markerValue: String,
        onProgress: (Int) -> Unit = {},
    ): Int {
        Files.createDirectories(rootfsDir.toPath())
        val rootPath = rootfsDir.canonicalFile.toPath()
        var entries = 0
        TarArchiveInputStream(GzipCompressorInputStream(BufferedInputStream(input))).use { tar ->
            while (true) {
                val entry = tar.nextEntry ?: break
                val name = entry.name.removePrefix("./")
                if (name.isEmpty() || name == ".") continue
                val target = File(rootfsDir, name)
                if (!target.canonicalFile.toPath().startsWith(rootPath)) {
                    throw IOException("unsafe archive entry: $name")
                }
                when {
                    entry.isDirectory -> Files.createDirectories(target.toPath())
                    entry.isSymbolicLink -> createSymlink(target, entry.linkName)
                    entry.isLink -> createHardLink(rootfsDir, entry.linkName, target)
                    else -> {
                        target.parentFile?.let { Files.createDirectories(it.toPath()) }
                        target.outputStream().use { out -> tar.copyTo(out) }
                        applyMode(target, entry.mode)
                    }
                }
                entries++
                if (entries % 1000 == 0) onProgress(entries)
            }
        }
        File(rootfsDir, MARKER).writeText(markerValue)
        onProgress(entries)
        return entries
    }

    private fun createSymlink(
        target: File,
        linkName: String,
    ) {
        target.parentFile?.let { Files.createDirectories(it.toPath()) }
        Files.deleteIfExists(target.toPath())
        Files.createSymbolicLink(target.toPath(), Paths.get(linkName))
    }

    private fun createHardLink(
        rootfsDir: File,
        linkName: String,
        target: File,
    ) {
        target.parentFile?.let { Files.createDirectories(it.toPath()) }
        val normalized = linkName.removePrefix("./")
        val source = File(rootfsDir, normalized)
        Files.deleteIfExists(target.toPath())
        try {
            Files.createLink(target.toPath(), source.toPath())
        } catch (linkError: IOException) {
            try {
                Files.createSymbolicLink(target.toPath(), Paths.get("/$normalized"))
            } catch (symlinkError: IOException) {
                Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    private fun applyMode(
        file: File,
        mode: Int,
    ) {
        file.setReadable(true, true)
        file.setWritable(true, true)
        file.setExecutable(mode and 0x49 != 0, false)
    }
}
