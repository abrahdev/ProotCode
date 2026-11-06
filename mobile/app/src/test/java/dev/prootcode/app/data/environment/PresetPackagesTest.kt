package dev.prootcode.app.data.environment

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PresetPackagesTest {
    @Test
    fun `maps the essentials preset`() {
        assertEquals(
            listOf("git", "curl", "wget", "build-essential"),
            PresetPackages.packagesFor(listOf(Presets.ESSENTIALS)),
        )
    }

    @Test
    fun `maps the editors preset`() {
        assertEquals(
            listOf("neovim"),
            PresetPackages.packagesFor(listOf(Presets.EDITORS)),
        )
    }

    @Test
    fun `merges and deduplicates presets`() {
        val packages =
            PresetPackages.packagesFor(
                listOf(Presets.ESSENTIALS, Presets.LANGUAGES, Presets.UTILITIES),
            )

        assertEquals(packages.distinct(), packages)
        assertTrue(packages.containsAll(listOf("git", "python3", "htop")))
    }

    @Test
    fun `ignores unknown presets`() {
        assertTrue(PresetPackages.packagesFor(listOf("unknown")).isEmpty())
    }

    @Test
    fun `returns no script when there are no packages`() {
        assertNull(PresetScripts.packageScript(emptyList()))
    }

    @Test
    fun `builds an apt script with the packages`() {
        val script = PresetScripts.packageScript(listOf("git", "curl"))

        assertTrue(script!!.contains("apt-get update"))
        assertTrue(script.contains("apt-get install -y git curl"))
        assertTrue(script.contains("DEBIAN_FRONTEND=noninteractive"))
    }

    @Test
    fun `installs opencode with the official installer`() {
        val script = PresetScripts.openCodeScript()

        assertTrue(script.contains("opencode.ai/install"))
        assertTrue(script.contains("nodejs npm"))
        assertTrue(script.contains("opencode --version"))
    }
}
