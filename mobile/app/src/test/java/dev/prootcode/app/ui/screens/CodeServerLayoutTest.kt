package dev.prootcode.app.ui.screens

import androidx.compose.ui.geometry.Offset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CodeServerLayoutTest {
    private val bounds = DragBounds(maxX = 100f, maxY = 500f)

    @Test
    fun `clamps drag offsets inside the screen bounds`() {
        assertEquals(Offset(100f, -500f), clampDragOffset(Offset(400f, -900f), bounds))
        assertEquals(Offset(-100f, 0f), clampDragOffset(Offset(-400f, 90f), bounds))
    }

    @Test
    fun `resolves the closest edge`() {
        assertEquals(DockSide.LEFT, resolveDockSide(Offset(-90f, -100f), bounds))
        assertEquals(DockSide.RIGHT, resolveDockSide(Offset(90f, -100f), bounds))
        assertEquals(DockSide.TOP, resolveDockSide(Offset(0f, -490f), bounds))
        assertEquals(DockSide.BOTTOM, resolveDockSide(Offset(0f, 0f), bounds))
    }

    @Test
    fun `docking snaps to the closest edge`() {
        assertEquals(Offset(-100f, -100f), dockOffset(Offset(-90f, -100f), bounds))
        assertEquals(Offset(100f, -100f), dockOffset(Offset(90f, -100f), bounds))
        assertEquals(Offset(0f, -500f), dockOffset(Offset(0f, -490f), bounds))
        assertEquals(Offset(0f, 0f), dockOffset(Offset(0f, -10f), bounds))
    }

    @Test
    fun `collapsed offset overhangs the docked edge`() {
        assertEquals(
            Offset(-130f, -100f),
            collapsedOffset(Offset(-100f, -100f), DockSide.LEFT, 30f),
        )
        assertEquals(
            Offset(130f, -100f),
            collapsedOffset(Offset(100f, -100f), DockSide.RIGHT, 30f),
        )
        assertEquals(
            Offset(0f, -530f),
            collapsedOffset(Offset(0f, -500f), DockSide.TOP, 30f),
        )
        assertEquals(Offset(0f, 30f), collapsedOffset(Offset.Zero, DockSide.BOTTOM, 30f))
    }

    @Test
    fun `zoom steps stay within limits`() {
        assertEquals(110, nextZoomLevel(100, ZOOM_STEP_PERCENT))
        assertEquals(90, nextZoomLevel(100, -ZOOM_STEP_PERCENT))
        assertEquals(MIN_ZOOM_PERCENT, nextZoomLevel(MIN_ZOOM_PERCENT, -ZOOM_STEP_PERCENT))
        assertEquals(MAX_ZOOM_PERCENT, nextZoomLevel(MAX_ZOOM_PERCENT, ZOOM_STEP_PERCENT))
    }
}
