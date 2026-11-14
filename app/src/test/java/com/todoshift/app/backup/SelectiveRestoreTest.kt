package com.todoshift.app.backup

import android.content.Context
import com.todoshift.app.v11.*
import com.todoshift.cashbook.core.Ledger
import com.todoshift.cashbook.ui.AndroidLedgerStore
import java.io.ByteArrayOutputStream
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SelectiveRestoreTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val storage get() = V11Storage(context)
    private fun archive(db: V11Database, scope: Set<BackupSection>): Neo4PreparedRestore {
        val backup = Neo4LocalBackup(context)
        return backup.preview(ByteArrayOutputStream().also { backup.export(db, it, scope) }.toByteArray().inputStream())
    }
    @Test fun settingsOnlyReplacePreservesEveryRecordAndCreatesSafetySnapshot() {
        val original = V11Runtime(context).start()
        val before = original.copy(tasks = listOf(TaskV11("t", "keep", date = "2026-11-14")))
        storage.save(before)
        val backup = Neo4LocalBackup(context)
        val prepared = archive(before, setOf(BackupSection.SETTINGS))
        assertEquals(setOf(BackupSection.SETTINGS), prepared.preview.availableSections)
        assertEquals(before, backup.restore(prepared, prepared.token, setOf(BackupSection.SETTINGS), RestoreMode.REPLACE).database)
        assertTrue(java.io.File(context.filesDir, "safety-copies").listFiles().orEmpty().any { it.extension == "zip" })
    }
    @Test fun taskMergeIsIdempotentAndDoesNotReplacePayments() {
        val original = V11Runtime(context).start()
        val task = TaskV11("t", "incoming", date = "2026-11-14")
        storage.save(original.copy(tasks = listOf(task)))
        val prepared = archive(storage.load(), setOf(BackupSection.TASKS))
        val payment = PaymentV11("p", 12.5, "2026-11-14", "2026-11")
        storage.save(original.copy(payments = listOf(payment)))
        val backup = Neo4LocalBackup(context)
        repeat(2) { backup.restore(prepared, prepared.token, setOf(BackupSection.TASKS), RestoreMode.MERGE) }
        assertEquals(listOf(task), storage.load().tasks)
        assertEquals(listOf(payment), storage.load().payments)
        assertTrue(backup.inspect(prepared, setOf(BackupSection.TASKS), RestoreMode.MERGE).duplicates >= 1)
    }
    @Test fun interruptedCombinedCashRestoreRollsBackBothStores() {
        val original = V11Runtime(context).start()
        val ledger = Ledger(AndroidLedgerStore(context)); ledger.load()
        ledger.addAccount("cash", "EGP", 10000L)
        val prepared = archive(original, setOf(BackupSection.CASHBOOK, BackupSection.SALARY))
        ledger.addAccount("retained", "EGP", 4000L)
        val previous = ledger.backup()
        val backup = Neo4LocalBackup(context)
        backup.restoreCheckpoint = { if (it == "cashbook") error("interrupted") }
        try { backup.restore(prepared, prepared.token, prepared.preview.availableSections, RestoreMode.REPLACE); fail("Expected interruption") } catch (_: IllegalStateException) { }
        ledger.load()
        assertArrayEquals(previous, ledger.backup())
        assertEquals(original, storage.load())
    }
    @Test fun inspectedConflictsAreRealAndMergeCannotOverwriteThem() {
        val original = V11Runtime(context).start()
        storage.save(original.copy(tasks = listOf(TaskV11("t", "old", date = "2026-11-14"))))
        val prepared = archive(storage.load(), setOf(BackupSection.TASKS))
        val changed = storage.load().copy(tasks = listOf(storage.load().tasks.single().copy(title = "new")))
        storage.save(changed)
        val backup = Neo4LocalBackup(context)
        assertEquals(1, backup.inspect(prepared, setOf(BackupSection.TASKS), RestoreMode.MERGE).conflicts)
        try { backup.restore(prepared, prepared.token, setOf(BackupSection.TASKS), RestoreMode.MERGE); fail("Merge must reject conflicts") } catch (_: IllegalArgumentException) { }
        assertEquals(changed, storage.load())
    }

    @Test fun taskOnlyExportCarriesReferencedShiftAndPreservesCalendarEvents() {
        val original = V11Runtime(context).start()
        val day = DayEntryV11("d", "2026-11-14", original.calendars.first().id,
            listOf(ShiftSegmentV11("s", ShiftKind.FULL_DAY)))
        val task = TaskV11("t", "shift task", date = "2026-11-14", linkedShiftId = "s")
        val note = DayItemV11("n", "2026-11-14", type = DayItemTypeV11.NOTE, title = "note")
        val event = DayItemV11("e", "2026-11-14", type = DayItemTypeV11.REMINDER, title = "keep event")
        storage.save(original.copy(days = listOf(day), tasks = listOf(task), dayItems = listOf(note, event)))
        val prepared = archive(storage.load(), setOf(BackupSection.TASKS))
        storage.save(original.copy(dayItems = listOf(event)))
        val result = Neo4LocalBackup(context).restore(prepared, prepared.token, setOf(BackupSection.TASKS), RestoreMode.REPLACE).database
        assertEquals(listOf(day), result.days)
        assertEquals(listOf(task), result.tasks)
        assertEquals(setOf("e", "n"), result.dayItems.map { it.id }.toSet())
    }

    @Test fun staleInspectedStateCannotAuthorizeAnotherRestore() {
        val original = V11Runtime(context).start()
        val prepared = archive(original, setOf(BackupSection.SETTINGS))
        val backup = Neo4LocalBackup(context)
        val preview = backup.inspect(prepared, setOf(BackupSection.SETTINGS), RestoreMode.REPLACE)
        val changed = original.copy(tasks = listOf(TaskV11("new", "added after preview", date = "2026-11-14")))
        storage.save(changed)
        try { backup.restore(prepared, prepared.token, setOf(BackupSection.SETTINGS), RestoreMode.REPLACE, preview.currentFingerprint); fail("Expected stale preview rejection") } catch (_: IllegalStateException) { }
        assertEquals(changed, storage.load())
    }

    @Test fun encryptedBackupRequiresPasswordBeforeInspectedPreviewAndWrongPasswordCannotWrite() {
        val original = V11Runtime(context).start()
        val backup = Neo4LocalBackup(context)
        val secret = "independent backup phrase 44".toCharArray()
        val encrypted = ByteArrayOutputStream().also { backup.exportEncrypted(original, it, BackupSection.entries.toSet(), secret) }.toByteArray()
        try { backup.preview(encrypted.inputStream()); fail("Password required before preview") } catch (required: BackupPasswordRequired) { assertArrayEquals(encrypted, required.envelope) }
        try { backup.preview(encrypted.inputStream(), "incorrect password".toCharArray()); fail("Wrong password must fail") } catch (_: IllegalArgumentException) { }
        assertEquals(original, storage.load())
        val prepared = backup.preview(encrypted.inputStream(), secret)
        assertEquals(original.calendars.size, backup.validate(prepared).calendars)
        assertEquals(original, backup.restore(prepared, prepared.token).database)
    }
}
