package com.todoshift.app.v11

import com.todoshift.app.integration.SalaryCashSync
import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth

class SalaryReceiptOperationsTest {
    private fun receipt() = PaymentV11(
        id = "receipt-one", amount = 120.0, receivedDate = "2026-11-14", accrualMonth = "2026-11",
        allocations = listOf(PaymentAllocationV11("allocation-one", "2026-11", amount = 120.0)),
        cashBookAccountId = "account-one",
    )

    @Test fun correctionAdvancesRevisionAndKeepsAudit() {
        val payment = receipt()
        val db = V11Database(payments = listOf(payment))
        val corrected = SalaryReceiptOperations.update(db, payment.copy(
            amount = 100.0,
            allocations = listOf(payment.allocations.single().copy(amount = 100.0)),
        ))
        assertEquals(1, corrected.payments.single().cashBookRevision)
        assertEquals(payment.id, corrected.payments.single().id)
        assertEquals("PAYMENT_CORRECT", corrected.auditLog.last().action)
        SalaryCashSync.validateWrite(db, corrected)
        assertEquals(100.0, V11FinanceEngine.month(corrected, YearMonth.of(2026, 11)).received, 0.0)
    }

    @Test fun reversalPreservesReceiptAndAllocationAndRemovesItFromReceivedTotal() {
        val payment = receipt()
        val db = V11Database(payments = listOf(payment))
        val reversed = SalaryReceiptOperations.reverse(db, payment.id)
        val retained = reversed.payments.single()
        assertEquals(payment.amount, retained.amount, 0.0)
        assertEquals(payment.allocations, retained.allocations)
        assertNotNull(retained.reversedAt)
        assertEquals(1, retained.cashBookRevision)
        assertEquals("PAYMENT_REVERSE", reversed.auditLog.last().action)
        assertEquals(0.0, V11FinanceEngine.month(reversed, YearMonth.of(2026, 11)).received, 0.0)
        assertEquals(reversed, SalaryReceiptOperations.reverse(reversed, payment.id))
        SalaryCashSync.validateWrite(db, reversed)
        assertThrows(IllegalArgumentException::class.java) { SalaryReceiptOperations.update(reversed, payment) }
    }

    @Test fun writeCannotDropLinkedReceiptOrAlterFinancialDetailsWithoutRevision() {
        val payment = receipt()
        val db = V11Database(payments = listOf(payment))
        assertThrows(IllegalArgumentException::class.java) { SalaryCashSync.validateWrite(db, db.copy(payments = emptyList())) }
        assertThrows(IllegalArgumentException::class.java) { SalaryCashSync.validateWrite(db, db.copy(payments = listOf(payment.copy(amount = 80.0)))) }
        assertThrows(IllegalArgumentException::class.java) { SalaryCashSync.validateTransition(db, SalaryReceiptOperations.reverse(db, payment.id)) }
    }
}
