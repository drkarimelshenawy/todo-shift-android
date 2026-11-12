package com.todoshift.app.v11

import android.content.Context
import com.todoshift.app.backup.Neo4LocalBackup
import com.todoshift.app.data.AppStorage
import com.todoshift.app.data.Workplace
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FinancialWriteValidationTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    private fun rejects(action: () -> Unit) {
        try {
            action()
            fail("Unsafe financial write must be rejected")
        } catch (_: IllegalArgumentException) { }
    }

    private fun payment(amount: Double, allocations: List<PaymentAllocationV11> = emptyList()) =
        PaymentV11("payment", amount, "2026-11-11", "2026-11", allocations = allocations)

    private fun unchanged(before: V11Database, raw: String?) {
        assertEquals(before, V11Storage(context).load())
        assertEquals(raw, context.getSharedPreferences("todo_shift_v11", Context.MODE_PRIVATE).getString("database", null))
        assertFalse(File(context.filesDir, "neo4-restore-journal.json").exists())
    }

    @Test fun directStorageWriteRejectsHugeFinitePaymentWithoutChangingRawData() {
        val before = V11Runtime(context).start()
        val raw = context.getSharedPreferences("todo_shift_v11", Context.MODE_PRIVATE).getString("database", null)
        rejects { V11Storage(context).save(before.copy(payments = listOf(payment(1e100)))) }
        unchanged(before, raw)
    }

    @Test fun compareAndSaveRejectsUnbalancedAllocation() {
        val runtime = V11Runtime(context)
        val before = runtime.start()
        val raw = context.getSharedPreferences("todo_shift_v11", Context.MODE_PRIVATE).getString("database", null)
        val invalid = before.copy(payments = listOf(payment(100.0,
            listOf(PaymentAllocationV11("a", "2026-11", amount = 140.0)))))
        rejects { runtime.saveIfCurrent(before, invalid) }
        unchanged(before, raw)
    }

    @Test fun invalidTransactionDoesNotPrepareEffectsOrCreateJournal() {
        val before = V11Runtime(context).start()
        val raw = context.getSharedPreferences("todo_shift_v11", Context.MODE_PRIVATE).getString("database", null)
        val effects = context.getSharedPreferences(NeoCommittedEffects.STORE_NAME, Context.MODE_PRIVATE)
        val effectsBefore = effects.all.toMap()
        var checkpoints = 0
        rejects {
            Neo4LocalBackup(context).commitDatabase(before,
                before.copy(payments = listOf(payment(1e100)))) { checkpoints++ }
        }
        assertEquals(0, checkpoints)
        assertEquals(effectsBefore, effects.all)
        unchanged(before, raw)
    }

    @Test fun unsafeWorkplaceRateDoesNotChangeEitherDatabase() {
        val before = V11Runtime(context).start()
        val legacy = AppStorage(context).load()
        val raw = context.getSharedPreferences("todo_shift_v11", Context.MODE_PRIVATE).getString("database", null)
        rejects {
            Neo4LocalBackup(context).updateWorkplaces(legacy, before,
                legacy.workplaces + Workplace("unsafe", "Unsafe", "#123456", fullDayRate = 1e100))
        }
        assertEquals(legacy, AppStorage(context).load())
        unchanged(before, raw)
    }

    @Test fun validCentAllocationCanBeSavedEditedAndReopened() {
        val runtime = V11Runtime(context)
        val before = runtime.start()
        val first = before.copy(payments = listOf(payment(100.01,
            listOf(PaymentAllocationV11("a", "2026-11", amount = 100.01)))))
        runtime.saveIfCurrent(before, first)
        val edited = first.copy(payments = listOf(payment(200.02,
            listOf(PaymentAllocationV11("a", "2026-11", amount = 100.01),
                PaymentAllocationV11("b", "2026-12", amount = 100.01)))))
        Neo4LocalBackup(context).commitDatabase(first, edited)
        assertEquals(edited, runtime.start())
    }

    @Test fun staleSnapshotStillCannotOverwriteAValidNewerPayment() {
        val runtime = V11Runtime(context)
        val before = runtime.start()
        val current = before.copy(payments = listOf(payment(10.0)))
        runtime.saveIfCurrent(before, current)
        try {
            runtime.saveIfCurrent(before, before.copy(payments = listOf(payment(20.0))))
            fail("Stale snapshot must be rejected")
        } catch (_: IllegalStateException) { }
        assertEquals(current, runtime.start())
    }

    @Test fun aggregateMagnitudeCannotOverflowLaterLongSums() {
        rejects { FinancialDataValidation.validateMoneyRange(List(11000) { 9_000_000_000_000.0 }) }
        FinancialDataValidation.validateMoneyRange(listOf(9_000_000_000_000.0, -10.0))
    }
}
