package dev.prootcode.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
    darkColorScheme(
        primary = ProotCodeGreen,
        onPrimary = ProotCodeNavy,
        secondary = ProotCodeGreenDark,
        onSecondary = ProotCodeOnNavy,
        background = ProotCodeNavy,
        onBackground = ProotCodeOnNavy,
        surface = ProotCodeNavy,
        onSurface = ProotCodeOnNavy,
        surfaceVariant = ProotCodeSurfaceDark,
        onSurfaceVariant = ProotCodeOnNavy,
        outline = ProotCodeOutline,
        error = Color(0xFFFF6B6B),
        onError = ProotCodeNavy,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = ProotCodeGreenDark,
        onPrimary = Color.White,
        secondary = ProotCodeSlate,
        onSecondary = Color.White,
        background = ProotCodeSurface,
        onBackground = ProotCodeOnSurface,
        surface = ProotCodeSurface,
        onSurface = ProotCodeOnSurface,
        surfaceVariant = Color(0xFFE2E8F0),
        onSurfaceVariant = ProotCodeSlate,
        outline = ProotCodeOutline,
        error = Color(0xFFB3261E),
        onError = Color.White,
    )

@Composable
fun ProotCodeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = ProotCodeTypography,
        content = content,
    )
}
