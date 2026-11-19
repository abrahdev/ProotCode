package dev.prootcode.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun HueWheelPicker(
    hue: Int,
    onHueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors =
        remember {
            (0..12).map { Color.hsl((it * 30f) % 360f, 0.7f, 0.5f) }
        }
    var widthPx by remember { mutableIntStateOf(0) }

    fun hueAt(x: Float): Int =
        if (widthPx <= 0) {
            hue
        } else {
            (((x / widthPx) * 359f).roundToInt()).coerceIn(0, 359)
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(40.dp)
                .onSizeChanged { widthPx = it.width },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(colors)),
        )
        Box(
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .offset {
                        val center = widthPx * (hue.coerceIn(0, 359) / 359f)
                        IntOffset((center - 12.dp.toPx()).roundToInt(), 0)
                    }.size(24.dp)
                    .background(Color.White, CircleShape)
                    .border(2.dp, Color.Black.copy(alpha = 0.25f), CircleShape),
        )
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .pointerInput(widthPx) {
                        detectTapGestures { offset -> onHueChange(hueAt(offset.x)) }
                    }.pointerInput(widthPx) {
                        detectDragGestures { change, _ ->
                            onHueChange(hueAt(change.position.x))
                            change.consume()
                        }
                    },
        )
    }
}
