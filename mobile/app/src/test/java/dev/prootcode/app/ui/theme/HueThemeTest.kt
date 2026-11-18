package dev.prootcode.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class HueThemeTest {
    @Test
    fun `light and dark schemes differ`() {
        val light = hueColorScheme(Hue.DEFAULT, dark = false)
        val dark = hueColorScheme(Hue.DEFAULT, dark = true)

        assertNotEquals(light.primary, dark.primary)
        assertNotEquals(light.surface, dark.surface)
    }

    @Test
    fun `different hues produce different primary colors`() {
        val green = hueColorScheme(152, dark = false).primary
        val blue = hueColorScheme(220, dark = false).primary

        assertNotEquals(green, blue)
    }

    @Test
    fun `hard background uses pure black and white`() {
        assertEquals(Color.Black, hueColorScheme(152, dark = true, hardBackground = true).surface)
        assertEquals(Color.White, hueColorScheme(152, dark = false, hardBackground = true).surface)
    }

    @Test
    fun `hue is clamped to the supported range`() {
        val low = hueColorScheme(-40, dark = false).primary
        val high = hueColorScheme(999, dark = false).primary

        assertEquals(hueColorScheme(Hue.MIN, dark = false).primary, low)
        assertEquals(hueColorScheme(Hue.MAX, dark = false).primary, high)
    }

    @Test
    fun `terminal palette exposes distinct roles`() {
        val palette = terminalPalette(Hue.DEFAULT)

        assertNotEquals(palette.background, palette.foreground)
        assertNotEquals(palette.foreground, palette.prompt)
    }
}
