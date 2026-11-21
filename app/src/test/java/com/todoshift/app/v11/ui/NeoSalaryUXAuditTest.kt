package com.todoshift.app.v11.ui

import com.todoshift.app.v11.*
import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth

class NeoSalaryUXAuditTest {
    @Test fun reversedReceiptShowsItsOriginalAllocationWithoutAffectingBalance() {
        val p = PaymentV11(
            "p", 135.0, "2026-12-05", "2026-11", reversedAt = 100L,
            allocations = listOf(
                PaymentAllocationV11("nov", "2026-11", amount = 100.0),
                PaymentAllocationV11("dec", "2026-12", amount = 35.0)
            )
        )
        val nov = YearMonth.of(2026, 11)
        assertEquals(0L, NeoSalaryDashboardLogic.portionCents(p, nov))
        assertEquals(10000L, NeoSalaryDashboardLogic.historicalPortionCents(p, nov))
        assertEquals(3500L,
            NeoSalaryDashboardLogic.historicalPortionCents(p, nov.plusMonths(1)))
    }

    @Test fun routingPolicyDoesNotConfuseFunctionGroupWithSettingsScreen() {
        assertTrue(NeoFeatureCatalog.get("STATEMENT")?.group == "Salary")
        assertFalse(NeoFeatureRoutePolicy.isSettingsPage("STATEMENT"))
        assertFalse(NeoFeatureRoutePolicy.isSettingsPage("MONTH_REVIEW"))
        assertFalse(NeoFeatureRoutePolicy.isSettingsPage("TASK_TEMPLATES"))
        assertTrue(NeoFeatureRoutePolicy.isSettingsPage("CALENDAR_DISPLAY"))
    }
}
