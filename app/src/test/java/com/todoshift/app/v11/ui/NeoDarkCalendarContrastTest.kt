package com.todoshift.app.v11.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.todoshift.app.v11.*
import org.junit.Assert.*
import org.junit.Test

class NeoDarkCalendarContrastTest {
    private val appearance = NeoAppearance(brightness = NeoBrightness.AMOLED)

    @Test fun dayNumberInkTracksBackgroundWithoutAnyOpaqueDateStrip() {
        val full = listOf(ShiftSegmentV11("full", ShiftKind.FULL_DAY))
        val half = listOf(ShiftSegmentV11("night", ShiftKind.NIGHT))
        val quarter = listOf(ShiftSegmentV11("morning", ShiftKind.MORNING))
        val fallback = Color.White
        // Standard workplace fallback #8BBACB is light: use dark ink over a painted top.
        assertEquals(Color.Black, neoCalendarOverlayInk(full, emptyList(), appearance, .04f, fallback))
        assertEquals(Color.White, neoCalendarOverlayInk(half, emptyList(), appearance, .04f, fallback))
        assertEquals(Color.Black, neoCalendarOverlayInk(half, emptyList(), appearance, .75f, fallback))
        assertEquals(Color.Black, neoCalendarOverlayInk(quarter, emptyList(), appearance, .1f, fallback))
        assertEquals(Color.White, neoCalendarOverlayInk(quarter, emptyList(), appearance, .6f, fallback))
    }

    @Test fun monthRowsRemainInsideFixedHeightWithGapsForEveryPossibleMonth() {
        val gridHeight = 650.dp
        val headerHeight = 24.dp
        val gap = 3.dp
        for (rows in 4..6) {
            val rowHeight = fittedCalendarRowHeight(gridHeight - gap * rows, headerHeight, rows)
            assertTrue(rowHeight > 0.dp)
            assertTrue(headerHeight + (rowHeight + gap) * rows <= gridHeight + .01.dp)
        }
        assertEquals(Neo4Coverage(0f, 1f), neo4Coverage(ShiftKind.FULL_DAY))
        assertEquals(Neo4Coverage(0f, .5f), neo4Coverage(ShiftKind.LONG))
        assertEquals(Neo4Coverage(0f, .25f), neo4Coverage(ShiftKind.MORNING))
        assertEquals(Neo4Coverage(.25f, .5f), neo4Coverage(ShiftKind.EVENING))
        assertEquals(Neo4Coverage(.5f, 1f), neo4Coverage(ShiftKind.NIGHT))
    }
}
