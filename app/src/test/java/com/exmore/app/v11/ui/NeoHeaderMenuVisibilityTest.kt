package com.exmore.app.v11.ui

import org.junit.Assert.*
import org.junit.Test

class NeoHeaderMenuVisibilityTest {
    @Test fun notesNeverShowAnEmptySecondOverflowMenu() {
        assertFalse(neoHasHeaderContextActions("Notes"))
        assertFalse(neoHasHeaderContextActions(""))
    }

    @Test fun tasksUseTheirOwnPageMenuNotASecondOverlappingGlobalMenu() {
        assertFalse(neoHasHeaderContextActions("Tasks"))
    }

    @Test fun availableGlobalActionsRemainReachable() {
        assertTrue(neoHasHeaderContextActions("Calendar"))
        assertTrue(neoHasHeaderContextActions("Cash Book"))
    }

    @Test fun unknownDestinationsCannotRenderEmptyOverflow() {
        assertFalse(neoHasHeaderContextActions("UNKNOWN"))
    }
}
