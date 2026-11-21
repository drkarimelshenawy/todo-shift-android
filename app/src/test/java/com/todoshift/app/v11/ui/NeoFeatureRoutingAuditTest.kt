package com.todoshift.app.v11.ui

import org.junit.Assert.*
import org.junit.Test

class NeoFeatureRoutingAuditTest {
    @Test fun originalFinanceAndTasksToolsNeverRouteToSettingsContent() {
        for (id in listOf("RATES", "STATEMENT", "MONTH_REVIEW", "PAYMENTS",
            "TASK_TEMPLATES", "SHIFT_TEMPLATE", "ATTACHMENTS", "COPY",
            "COMPARE", "FOCUS", "WORK_SUMMARY")) {
            assertFalse("$id must open its dedicated tool",
                NeoFeatureRoutePolicy.isSettingsPage(id))
        }
    }

    @Test fun realSettingsKeepTheirOnlyOwner() {
        for (id in NeoSettingsHierarchy.inlinePages +
            setOf("APPEARANCE", "LANGUAGE", "UI_DIRECTION", "NAV_DIRECTION",
                "GRID_DIRECTION", "WEEKDAY_DIRECTION", "WIDGETS",
                "SECURITY", "BACKUP", "LEGAL")) {
            assertTrue("$id must keep settings owner",
                NeoFeatureRoutePolicy.isSettingsPage(id))
        }
    }

    @Test fun workplacesAndCalendarManagerRemainTheirDedicatedDestinations() {
        assertFalse(NeoFeatureRoutePolicy.isSettingsPage("WORKPLACES"))
        assertFalse(NeoFeatureRoutePolicy.isSettingsPage("CALENDAR_MANAGER"))
    }
}
