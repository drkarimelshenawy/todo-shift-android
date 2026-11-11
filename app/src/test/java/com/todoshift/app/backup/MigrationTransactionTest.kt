package com.todoshift.app.backup

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import com.todoshift.app.data.*
import com.todoshift.app.v11.*
import java.io.File
import java.lang.reflect.Proxy
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MigrationTransactionTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private fun legacy(): AppData {
        val store = AppStorage(context)
        store.initializeFirstInstall()
        val data = AppData(
            workplaces = listOf(Workplace("place", "Historical", "#123456", fullDayRate = 100.01)),
            shifts = listOf(ShiftEntry("2026-11-10", ShiftType.FULL_DAY, listOf("place"))),
            payments = listOf(Payment("historical-payment", "2026-11-10", 75.01)))
        store.save(data)
        return data
    }
    private fun rawLegacy() = context.getSharedPreferences("todo_shift_data", 0).getString("database", null)
    private fun journal() = File(context.filesDir, "neo4-restore-journal.json")

    /** Models a write becoming visible before its durable acknowledgement reports failure. */
    private fun failedAcknowledgements(count: Int): Context {
        var remaining = count
        val delegate = context.getSharedPreferences("todo_shift_v11", 0)
        val failing = object : SharedPreferences by delegate {
            override fun edit(): SharedPreferences.Editor {
                val editor = delegate.edit()
                return Proxy.newProxyInstance(SharedPreferences.Editor::class.java.classLoader,
                    arrayOf(SharedPreferences.Editor::class.java)) { proxy, method, args ->
                    val result = method.invoke(editor, *(args ?: emptyArray()))
                    when {
                        method.name == "commit" && remaining > 0 -> { remaining--; false }
                        result is SharedPreferences.Editor -> proxy
                        else -> result
                    }
                } as SharedPreferences.Editor
            }
        }
        return object : ContextWrapper(context) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
                if (name == "todo_shift_v11") failing else super.getSharedPreferences(name, mode)
        }
    }

    @Test fun failedMigrationAcknowledgementRestoresMissingStateBeforeRetry() {
        val old = legacy()
        val raw = rawLegacy()
        try { V11Runtime(failedAcknowledgements(1)).start(); fail("Migration must report failure") }
        catch (_: IllegalStateException) { }
        assertFalse(V11Storage(context).hasDatabase())
        assertFalse(V11Storage(context).wasMigrated())
        assertEquals(raw, rawLegacy())
        assertFalse(journal().exists())
        val runtime = V11Runtime(context)
        val migrated = runtime.start()
        assertEquals(old.shifts.map { it.date }, migrated.days.map { it.date })
        assertEquals("historical-payment", migrated.payments.single().id)
        assertEquals(migrated, runtime.start())
        assertEquals(raw, rawLegacy())
    }

    @Test fun failedRollbackRetainsJournalForNextStartupRecovery() {
        legacy()
        val raw = rawLegacy()
        try { V11Runtime(failedAcknowledgements(2)).start(); fail("Migration must fail closed") }
        catch (failure: IllegalStateException) { assertTrue(failure.suppressed.isNotEmpty()) }
        assertTrue(journal().exists())
        val migrated = V11Runtime(context).start()
        assertEquals("historical-payment", migrated.payments.single().id)
        assertEquals(raw, rawLegacy())
        assertFalse(journal().exists())
    }

    @Test fun interruptionAfterMigrationWriteIsRecoveredWithoutDuplicateRecords() {
        val old = legacy()
        val raw = rawLegacy()
        try {
            Neo4LocalBackup(context).migrateLegacy(old, "2026-11-10") {
                if (it == "database") throw AssertionError("Simulated process interruption")
            }
            fail("Must interrupt")
        } catch (_: AssertionError) { }
        assertTrue(journal().exists())
        val runtime = V11Runtime(context)
        val migrated = runtime.start()
        assertEquals(1, migrated.days.size)
        assertEquals(1, migrated.payments.size)
        assertEquals(migrated, runtime.start())
        assertEquals(raw, rawLegacy())
        assertFalse(journal().exists())
    }

    @Test fun unsafeLegacyMoneyCannotBePersistedAsMigratedData() {
        val old = legacy()
        val unsafe = old.copy(payments = listOf(old.payments.single().copy(amount = 1e100)))
        AppStorage(context).save(unsafe)
        val raw = rawLegacy()
        try { V11Runtime(context).start(); fail("Unsafe migration must be rejected") }
        catch (_: IllegalArgumentException) { }
        assertFalse(V11Storage(context).hasDatabase())
        assertFalse(V11Storage(context).wasMigrated())
        assertEquals(raw, rawLegacy())
        assertFalse(journal().exists())
    }
}
