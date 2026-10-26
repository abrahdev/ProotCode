package dev.prootcode.app.spike

import android.content.Context
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

object RootfsExtractor {
    private const val ASSET_ROOT = "rootfs"
    private const val MARKER = ".extracted"

    fun assetPath(abi: String): String = "$ASSET_ROOT/$abi/ubuntu-26.04-base.bin"

    fun isExtracted(rootfsDir: File): Boolean = File(rootfsDir, MARKER).isFile

    fun extract(
        context: Context,
        abi: String,
        rootfsDir: File,
        onProgress: (Int) -> Unit,
    ) {
        val assetPath = assetPath(abi)
        rootfsDir.deleteRecursively()
        Files.createDirectories(rootfsDir.toPath())
        val rootPath = rootfsDir.canonicalFile.toPath()
        var entries = 0
        context.assets.open(assetPath).use { stream ->
            TarArchiveInputStream(GzipCompressorInputStream(BufferedInputStream(stream))).use { tar ->
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
                        entry.isSymbolicLink -> {
                            Files.createDirectories(target.parentFile.toPath())
                            Files.deleteIfExists(target.toPath())
                            Files.createSymbolicLink(target.toPath(), Paths.get(entry.linkName))
                        }
                        entry.isLink -> createHardLink(rootfsDir, entry.linkName, target)
                        else -> {
                            Files.createDirectories(target.parentFile.toPath())
                            target.outputStream().use { out -> tar.copyTo(out) }
                            applyMode(target, entry.mode)
                        }
                    }
                    entries++
                    if (entries % 1000 == 0) onProgress(entries)
                }
            }
        }
        File(rootfsDir, MARKER).writeText(assetPath)
        onProgress(entries)
    }

    private fun createHardLink(
        rootfsDir: File,
        linkName: String,
        target: File,
    ) {
        Files.createDirectories(target.parentFile.toPath())
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
