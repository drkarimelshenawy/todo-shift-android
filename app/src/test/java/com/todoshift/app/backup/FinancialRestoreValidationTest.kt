package com.todoshift.app.backup

import android.content.Context
import com.todoshift.app.v11.*
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FinancialRestoreValidationTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    private fun payment(amount: Double, allocations: List<PaymentAllocationV11> = emptyList()) =
        PaymentV11("payment", amount, "2026-11-11", "2026-11", allocations = allocations)

    private fun allocation(id: String, amount: Double, month: String = "2026-11") =
        PaymentAllocationV11(id, month, amount = amount)

    private fun rejected(candidate: (V11Database) -> V11Database) {
        val original = V11Runtime(context).start()
        val prefs = context.getSharedPreferences("todo_shift_v11", Context.MODE_PRIVATE)
        val rawBefore = prefs.getString("database", null)
        val raw = V11Storage(context).exportJson(candidate(original))
        try {
            Neo4LocalBackup(context).preview(raw.byteInputStream())
            fail("Invalid financial backup must be rejected before restore")
        } catch (_: IllegalArgumentException) {
            // No preferences or transaction journal may be written during rejection.
        } catch (_: java.time.format.DateTimeParseException) {
        }
        assertEquals(rawBefore, prefs.getString("database", null))
        assertEquals(original, V11Storage(context).load())
        assertFalse(File(context.filesDir, "neo4-restore-journal.json").exists())
    }

    @Test fun hugeFinitePaymentIsRejectedBeforeWriting() {
        rejected { it.copy(payments = listOf(payment(1e100))) }
    }

    @Test fun allocationMismatchIsRejectedBeforeWriting() {
        rejected { it.copy(payments = listOf(payment(100.0, listOf(allocation("a", 70.0), allocation("b", 70.0))))) }
    }

    @Test fun negativeAllocationCannotCancelAnotherAllocation() {
        rejected { it.copy(payments = listOf(payment(100.0, listOf(allocation("a", -40.0), allocation("b", 140.0))))) }
    }

    @Test fun duplicateAllocationIdentityIsRejected() {
        rejected { it.copy(payments = listOf(payment(100.0, listOf(allocation("a", 50.0), allocation("a", 50.0))))) }
    }

    @Test fun invalidAllocationMonthIsRejected() {
        rejected { it.copy(payments = listOf(payment(100.0, listOf(allocation("a", 100.0, "2026-13"))))) }
    }

    @Test fun balancedCentsSurviveRestoreAndReopen() {
        val original = V11Runtime(context).start()
        val candidate = original.copy(payments = listOf(payment(100.01,
            listOf(allocation("a", 50.01), allocation("b", 50.0, "2026-12")))))
        val backup = Neo4LocalBackup(context)
        val prepared = backup.preview(V11Storage(context).exportJson(candidate).byteInputStream())
        backup.restore(prepared, prepared.token)
        val reopened = V11Storage(context).load()
        assertEquals(candidate, reopened)
        assertEquals(50.01, V11FinanceEngine.month(reopened, java.time.YearMonth.of(2026, 11)).received, 0.0)
        assertEquals(50.0, V11FinanceEngine.month(reopened, java.time.YearMonth.of(2026, 12)).received, 0.0)
    }

    @Test fun signedSnapshotDifferenceRemainsSupported() {
        val original = V11Runtime(context).start()
        val candidate = original.copy(monthSnapshots = listOf(
            MonthSnapshotV11("snapshot", "2026-11", 100.0, 110.0, 0.0, -10.0, 1L)))
        val backup = Neo4LocalBackup(context)
        val prepared = backup.preview(V11Storage(context).exportJson(candidate).byteInputStream())
        assertEquals(candidate, backup.restore(prepared, prepared.token).database)
    }
}
