package dev.prootcode.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ExpressiveCircularProgressIndicator(
    progress: Float? = null,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    strokeWidth: Dp = 4.dp,
) {
    val phase by
        rememberInfiniteTransition(label = "expressive_progress").animateFloat(
            initialValue = 0f,
            targetValue = 6.283185f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1600, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "expressive_progress_phase",
        )
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.toPx()
        val radius = (size.minDimension - stroke) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val sweep =
            if (progress == null) {
                6.283185f
            } else {
                progress.coerceIn(0f, 1f) * 6.283185f
            }
        if (progress != null) {
            drawCircle(color = trackColor, radius = radius, center = center, style = Stroke(stroke))
        }
        val path = Path()
        val steps = 140
        val start = -1.5707964f
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val angle = start + sweep * t
            val wave = radius * 0.1f * sin(5f * angle + phase)
            val r = radius + wave
            val x = center.x + r * cos(angle)
            val y = center.y + r * sin(angle)
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

@Composable
fun ExpressiveLinearProgressIndicator(
    progress: Float? = null,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.secondaryContainer,
    height: Dp = 6.dp,
) {
    val phase by
        rememberInfiniteTransition(label = "expressive_linear").animateFloat(
            initialValue = 0f,
            targetValue = 6.283185f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1400, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "expressive_linear_phase",
        )
    Canvas(modifier = modifier.height(height)) {
        val width = size.width
        val h = size.height
        val amplitude = h * 0.14f
        val radius = CornerRadius(h / 2f, h / 2f)
        drawRoundRect(
            color = trackColor,
            topLeft = Offset(0f, h * 0.4f),
            size = Size(width, h * 0.2f),
            cornerRadius = CornerRadius(h * 0.1f, h * 0.1f),
        )

        val segmentStart: Float
        val segmentWidth: Float
        if (progress == null) {
            segmentWidth = width * 0.35f
            segmentStart = ((phase / 6.283185f) % 1f) * (width - segmentWidth)
        } else {
            segmentStart = 0f
            segmentWidth = width * progress.coerceIn(0f, 1f)
        }
        if (segmentWidth <= 0f) return@Canvas
        val end = segmentStart + segmentWidth
        val bounds =
            Path().apply {
                addRoundRect(RoundRect(segmentStart, 0f, end, h, radius))
            }
        val wave = Path()
        val steps = (segmentWidth / 2f).toInt().coerceAtLeast(16)
        for (i in 0..steps) {
            val x = segmentStart + segmentWidth * i / steps
            val y = amplitude + amplitude * sin(18.849556f * x / width + phase)
            if (i == 0) {
                wave.moveTo(x, y)
            } else {
                wave.lineTo(x, y)
            }
        }
        wave.lineTo(end, h)
        wave.lineTo(segmentStart, h)
        wave.close()
        clipPath(bounds) {
            drawPath(path = wave, color = color)
        }
    }
}
