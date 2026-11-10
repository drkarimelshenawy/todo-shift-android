package com.todoshift.app.v11

import android.graphics.Color
import com.todoshift.app.widget.Neo4WidgetPainter
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetPaintParityTest {
    @Test
    fun disabledDecorationStillRendersWorkplaceBandAndOffHasOwnMarker() {
        val segment = ShiftSegmentV11("s", ShiftKind.MORNING, "clinic")
        val bitmap =
            Neo4WidgetPainter.render(
                listOf(segment),
                mapOf("clinic" to Color.rgb(0, 164, 184)),
                NeoAppearance(showShiftPaint = false),
            )
        assertEquals(Color.rgb(0, 164, 184), bitmap.getPixel(32, 8))
        assertEquals(0, Color.alpha(bitmap.getPixel(32, 48)))
        val off =
            Neo4WidgetPainter.render(
                listOf(ShiftSegmentV11("off", ShiftKind.OFF)),
                emptyMap(),
                NeoAppearance(showShiftPaint = false),
            )
        assertTrue(
            (0 until 64).any { y -> (0 until 64).any { x -> Color.alpha(off.getPixel(x, y)) > 0 } }
        )
        assertNull(segment.startMinute)
    }
}
