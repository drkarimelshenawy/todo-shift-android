package com.todoshift.app.v11.ui

import com.todoshift.app.v11.*
import com.todoshift.cashbook.ui.SalaryIncomeLedgerBridge
import java.time.YearMonth
import java.time.LocalDate
import java.util.UUID
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NeoReceiptCashBookPremiumTest {
    @Test fun partialReceiptAcrossMonthsNeverExceedsOutstandingOrMutatesSalary() {
        val source = IncomeSourceV11(
            "source", "Work", IncomeSourceTypeV11.FIXED_SALARY,
            workplaceId = "work", fixedMonthlyAmount = 1000.0,
        )
        val db = V11Database(incomeSources = listOf(source))
        val before = db.copy()
        val months = listOf(YearMonth.parse("2026-10"), YearMonth.parse("2026-11"))
        assertEquals(150000L, receiptDraftMinor(db, "work", months,
            mapOf("2026-10" to "500", "2026-11" to "1000")))
        assertNull(receiptDraftMinor(db, "work", months,
            mapOf("2026-10" to "500", "2026-11" to "1000.01")))
        assertNull(receiptDraftMinor(db, "work", months,
            mapOf("2026-10" to "", "2026-11" to "1000")))
        assertNull(receiptDraftMinor(db, "work", months,
            mapOf("2026-10" to "0", "2026-11" to "0")))
        assertEquals(before, db)
        assertTrue(db.payments.isEmpty())
    }

    @Test fun receivingAccountStartsAtZeroAndPostingIsIdempotent() {
        val bridge = SalaryIncomeLedgerBridge(RuntimeEnvironment.getApplication())
        val name = "Cash receipt " + UUID.randomUUID().toString()
        val account = bridge.createReceivingAccount(name)
        assertEquals(name, account.name)
        assertEquals("EGP", account.currency)
        assertEquals(0L, account.balanceMinor)
        assertThrows(IllegalArgumentException::class.java) {
            bridge.createReceivingAccount(name)
        }
        val receiptId = UUID.randomUUID().toString()
        val first = bridge.post(receiptId, account.id, 1890000L, LocalDate.parse("2026-11-23"))
        val retry = bridge.post(receiptId, account.id, 1890000L, LocalDate.parse("2026-11-23"))
        assertEquals(first, retry)
        val reloaded = SalaryIncomeLedgerBridge(RuntimeEnvironment.getApplication())
            .accounts().single { it.id == account.id }
        assertEquals(1890000L, reloaded.balanceMinor)
        assertEquals(first, bridge.find(receiptId))
    }
}
