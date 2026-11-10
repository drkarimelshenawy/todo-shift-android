package com.todoshift.app.v11

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FinancialRequestIdentityTest {
    private fun rejected(action: () -> Unit) {
        var blocked = false
        try {
            action()
        } catch (expected: IllegalStateException) {
            blocked = true
        }
        assertTrue("A committed request ID must not create another record", blocked)
    }

    @Test
    fun receiptIdentityAndCentsReopenWithoutDuplicateOnReplay() {
        val runtime = V11Runtime(RuntimeEnvironment.getApplication())
        val before = runtime.start()
        fun add(db: V11Database) =
            V11AccountsController.addPayment(
                db,
                MoneyInput.parse("9,000").amount!!,
                "2026-11-10",
                "2026-11",
                null,
                null,
                "Fixture",
                "receipt-request",
            )
        val next = add(before)
        runtime.saveIfCurrent(before, next)
        rejected { add(runtime.start()) }
        assertEquals(next, runtime.start())
        assertEquals("receipt-request", next.payments.single().id)
        assertEquals(900000L, LocalizedNumbers.cents(next.payments.single().amount))
        assertEquals(next.payments.single().id, next.auditLog.last().entityId)
    }

    @Test
    fun obligationAndPaymentReplayPreserveClampingAndTheExistingLedger() {
        val runtime = V11Runtime(RuntimeEnvironment.getApplication())
        val before = runtime.start()
        fun add(db: V11Database) =
            V11AccountsController.addObligation(
                db,
                "Fixture",
                ObligationTypeV11.INSTALLMENT,
                9000.0,
                1000.0,
                "2026-11-01",
                1,
                9,
                3,
                "",
                "obligation-request",
            )
        val debt = add(before)
        rejected { add(debt) }
        fun pay(db: V11Database) =
            V11AccountsController.payObligation(
                db,
                "obligation-request",
                "2026-11",
                1200.0,
                null,
                "obligation-payment-request",
            )
        val paid = pay(debt)
        assertEquals("obligation-payment-request", paid.obligationPayments.single().id)
        assertEquals(100000L, LocalizedNumbers.cents(paid.obligationPayments.single().amount))
        runtime.saveIfCurrent(before, paid)
        rejected { pay(runtime.start()) }
        assertEquals(paid, runtime.start())
    }

    @Test
    fun sourceAndMethodReplayCannotCreateAnotherIdentity() {
        val before = V11Database()
        val source =
            V11AccountsController.addIncomeSource(
                before,
                "Fixture",
                IncomeSourceTypeV11.OTHER,
                9000.0,
                "source-request",
            )
        rejected {
            V11AccountsController.addIncomeSource(
                source,
                "Changed",
                IncomeSourceTypeV11.OTHER,
                1.0,
                "source-request",
            )
        }
        val method = V11AccountsController.addPaymentMethod(source, "Fixture", "method-request")
        rejected { V11AccountsController.addPaymentMethod(method, "Changed", "method-request") }
        assertEquals("source-request", method.incomeSources.single().id)
        assertEquals(9000.0, method.incomeSources.single().fixedMonthlyAmount!!, 0.0)
        assertEquals("method-request", method.paymentMethods.single().id)
    }

    @Test
    fun dayItemRequestCannotBeAppendedTwice() {
        val before = V11Database()
        val next =
            V11DayOrganizerController.add(
                before,
                "2026-11-10",
                null,
                DayItemTypeV11.REMINDER,
                "Fixture",
                recordId = "day-item-request",
            )
        rejected {
            V11DayOrganizerController.add(
                next,
                "2026-11-10",
                null,
                DayItemTypeV11.REMINDER,
                "Fixture",
                recordId = "day-item-request",
            )
        }
        assertEquals("day-item-request", next.dayItems.single().id)
    }
}
