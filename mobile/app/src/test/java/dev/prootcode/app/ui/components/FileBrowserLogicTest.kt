package dev.prootcode.app.ui.components

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class FileBrowserLogicTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `builds breadcrumbs from the root`() {
        assertEquals(
            listOf(
                Breadcrumb("/", "/"),
                Breadcrumb("/home", "/home"),
                Breadcrumb("/user", "/home/user"),
            ),
            breadcrumbs("/home/user"),
        )
    }

    @Test
    fun `breadcrumbs normalize blank paths`() {
        assertEquals(listOf(Breadcrumb("/", "/")), breadcrumbs(""))
        assertEquals(listOf(Breadcrumb("/", "/")), breadcrumbs("/"))
    }

    @Test
    fun `lists directories before files`() {
        File(tempDir, "z-folder").mkdirs()
        File(tempDir, "a-folder").mkdirs()
        File(tempDir, "notes.txt").writeText("hello")

        val entries = listRootfsEntries(tempDir, "/")

        assertEquals(listOf("a-folder", "z-folder", "notes.txt"), entries.map { it.name })
        assertTrue(entries.first().isDirectory)
        assertEquals(5L, entries.last().sizeBytes)
        assertEquals("/a-folder", entries.first().path)
    }

    @Test
    fun `lists nested paths`() {
        File(tempDir, "home/user").mkdirs()
        File(tempDir, "home/user/file.txt").writeText("x")

        val entries = listRootfsEntries(tempDir, "/home/user")

        assertEquals("/home/user/file.txt", entries.single().path)
    }

    @Test
    fun `missing directories return an empty list`() {
        assertTrue(listRootfsEntries(tempDir, "/missing").isEmpty())
    }
}
