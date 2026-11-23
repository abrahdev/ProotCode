package dev.prootcode.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object Hue {
    const val DEFAULT = 152
    const val MIN = 0
    const val MAX = 359
}

data class TerminalPalette(
    val background: Color,
    val foreground: Color,
    val prompt: Color,
    val inputBackground: Color,
)

private fun hsl(
    hue: Int,
    saturation: Float,
    lightness: Float,
    alpha: Float = 1f,
): Color =
    Color.hsl(
        hue = hue.coerceIn(Hue.MIN, Hue.MAX).toFloat(),
        saturation = saturation.coerceIn(0f, 1f),
        lightness = lightness.coerceIn(0f, 1f),
        alpha = alpha,
    )

fun terminalPalette(
    hue: Int,
    lightMode: Boolean = false,
): TerminalPalette =
    when (hue) {
        else ->
            TerminalPalette(
                background = hsl(hue, 0.15f, 0.07f),
                foreground = hsl(hue, 0.20f, if (lightMode) 0.72f else 0.88f),
                prompt = hsl(hue, 0.65f, if (lightMode) 0.62f else 0.74f),
                inputBackground = hsl(hue, 0.15f, 0.11f),
            )
    }

fun hueColorScheme(
    hue: Int,
    dark: Boolean,
    hardBackground: Boolean = false,
): ColorScheme {
    val h = hue.coerceIn(Hue.MIN, Hue.MAX)
    return if (dark) {
        darkColorScheme(
            primary = hsl(h, 0.65f, 0.74f),
            onPrimary = hsl(h, 0.60f, 0.14f),
            primaryContainer = hsl(h, 0.40f, 0.26f),
            onPrimaryContainer = hsl(h, 0.75f, 0.90f),
            inversePrimary = hsl(h, 0.50f, 0.40f),
            secondary = hsl(h, 0.18f, 0.48f),
            onSecondary = hsl(h, 0.25f, 0.95f),
            secondaryContainer = hsl(h, 0.18f, 0.26f),
            onSecondaryContainer = hsl(h, 0.25f, 0.90f),
            tertiary = hsl((h + 60) % 360, 0.35f, 0.62f),
            onTertiary = hsl((h + 60) % 360, 0.40f, 0.16f),
            tertiaryContainer = hsl((h + 60) % 360, 0.30f, 0.24f),
            onTertiaryContainer = hsl((h + 60) % 360, 0.45f, 0.88f),
            background = if (hardBackground) Color.Black else hsl(h, 0.12f, 0.09f),
            onBackground = hsl(h, 0.10f, 0.90f),
            surface = if (hardBackground) Color.Black else hsl(h, 0.12f, 0.09f),
            onSurface = hsl(h, 0.10f, 0.90f),
            surfaceVariant = hsl(h, 0.12f, 0.18f),
            onSurfaceVariant = hsl(h, 0.08f, 0.76f),
            surfaceContainerLowest = if (hardBackground) Color(0xFF101010) else hsl(h, 0.12f, 0.07f),
            surfaceContainerLow = if (hardBackground) Color(0xFF1C1C1C) else hsl(h, 0.12f, 0.14f),
            surfaceContainer = if (hardBackground) Color(0xFF262626) else hsl(h, 0.12f, 0.18f),
            surfaceContainerHigh = if (hardBackground) Color(0xFF2E2E2E) else hsl(h, 0.12f, 0.22f),
            surfaceContainerHighest = if (hardBackground) Color(0xFF363636) else hsl(h, 0.12f, 0.26f),
            surfaceDim = if (hardBackground) Color.Black else hsl(h, 0.12f, 0.08f),
            surfaceBright = if (hardBackground) Color(0xFF2E2E2E) else hsl(h, 0.12f, 0.28f),
            outline = hsl(h, 0.06f, 0.60f),
            outlineVariant = hsl(h, 0.08f, 0.28f),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            errorContainer = Color(0xFF93000A),
            onErrorContainer = Color(0xFFFFDAD6),
            inverseSurface = hsl(h, 0.12f, 0.90f),
            inverseOnSurface = hsl(h, 0.12f, 0.14f),
            scrim = Color.Black,
        )
    } else {
        lightColorScheme(
            primary = hsl(h, 0.50f, 0.36f),
            onPrimary = Color.White,
            primaryContainer = hsl(h, 0.70f, 0.88f),
            onPrimaryContainer = hsl(h, 0.60f, 0.12f),
            inversePrimary = hsl(h, 0.60f, 0.70f),
            secondary = hsl(h, 0.20f, 0.48f),
            onSecondary = Color.White,
            secondaryContainer = hsl(h, 0.25f, 0.88f),
            onSecondaryContainer = hsl(h, 0.30f, 0.14f),
            tertiary = hsl((h + 60) % 360, 0.30f, 0.40f),
            onTertiary = Color.White,
            tertiaryContainer = hsl((h + 60) % 360, 0.40f, 0.88f),
            onTertiaryContainer = hsl((h + 60) % 360, 0.45f, 0.16f),
            background = if (hardBackground) Color.White else hsl(h, 0.20f, 0.98f),
            onBackground = hsl(h, 0.15f, 0.10f),
            surface = if (hardBackground) Color.White else hsl(h, 0.20f, 0.98f),
            onSurface = hsl(h, 0.15f, 0.10f),
            surfaceVariant = hsl(h, 0.18f, 0.92f),
            onSurfaceVariant = hsl(h, 0.10f, 0.30f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = if (hardBackground) Color(0xFFF4F4F4) else hsl(h, 0.18f, 0.94f),
            surfaceContainer = if (hardBackground) Color.White else hsl(h, 0.20f, 0.98f),
            surfaceContainerHigh = if (hardBackground) Color(0xFFF0F0F0) else hsl(h, 0.18f, 0.93f),
            surfaceContainerHighest = if (hardBackground) Color(0xFFE8E8E8) else hsl(h, 0.18f, 0.90f),
            surfaceDim = if (hardBackground) Color(0xFFE8E8E8) else hsl(h, 0.18f, 0.90f),
            surfaceBright = hsl(h, 0.20f, 0.98f),
            outline = hsl(h, 0.08f, 0.50f),
            outlineVariant = hsl(h, 0.10f, 0.80f),
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002),
            inverseSurface = hsl(h, 0.15f, 0.20f),
            inverseOnSurface = hsl(h, 0.15f, 0.95f),
            scrim = Color.Black,
        )
    }
}

val ProotShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )

object Spacing {
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}
