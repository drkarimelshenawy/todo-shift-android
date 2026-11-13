package com.todoshift.app.v11.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Test

class NeoVc39RegressionTest {
    @Test fun calendarFourFiveSixWeeksStayInsideSameHeight() {
        val available = 620.dp
        val header = 20.dp
        for (weeks in 4..6) {
            val each = fittedCalendarRowHeight(available, header, weeks)
            assertEquals((available - header).value, each.value * weeks, .01f)
        }
    }

    @Test fun monthRowSizingDoesNotDependOnWorkplaceOrShiftLabels() {
        // Both preferences change only cell contents, not this fit calculation.
        assertEquals(100.dp, fittedCalendarRowHeight(620.dp, 20.dp, 6))
        assertEquals(120.dp, fittedCalendarRowHeight(620.dp, 20.dp, 5))
        assertEquals(150.dp, fittedCalendarRowHeight(620.dp, 20.dp, 4))
    }

    @Test fun bilingualSettingsIndexFindsSameControlByEitherLanguage() {
        val ar = NeoSettingsSearch.search("اسم مكان العمل")
        val en = NeoSettingsSearch.search("workplace name")
        assertTrue(ar.any { it.destination == "CALENDAR_DISPLAY" && it.en == "Show workplace name" })
        assertTrue(en.any { it.destination == "CALENDAR_DISPLAY" && it.en == "Show workplace name" })
    }

    @Test fun colloquialAliasesAndShortTyposResolve() {
        assertTrue(NeoSettingsSearch.search("الوان الشغل").any { it.destination == "PAINT_STYLE" })
        assertTrue(NeoSettingsSearch.search("widjet").any { it.destination == "WIDGETS" })
        assertTrue(NeoSettingsSearch.search("shift names").any { it.destination == "CALENDAR_DISPLAY" })
    }

    @Test fun emptySearchNeverPretendsToMatchSettings() {
        assertTrue(NeoSettingsSearch.search("   ").isEmpty())
    }

    @Test fun widgetsAndPrivacyOptionsAreIndexed() {
        assertTrue(NeoSettingsSearch.search("شفافية الويدجت").any { it.destination == "WIDGETS" })
        assertTrue(NeoSettingsSearch.search("backup").any { it.destination == "BACKUP" })
    }
}
