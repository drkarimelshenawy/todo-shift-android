package com.todoshift.app.v11

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CommittedEffectsTest {
    @Test
    fun serviceFailureSurvivesNewInstanceAndRetriesFromConfirmedSnapshot() {
        val c = RuntimeEnvironment.getApplication()
        val previous =
            V11Database(tasks = listOf(TaskV11("removed", "Old fixture", date = "2026-11-10")))
        val confirmed =
            previous.copy(tasks = listOf(TaskV11("kept", "New fixture", date = "2026-11-10")))
        NeoCommittedEffects(c).prepare(previous, confirmed)
        try {
            NeoCommittedEffects(c)
                .reconcile(confirmed, {}, { error("Injected service failure") }, {})
            fail("Must fail")
        } catch (expected: IllegalStateException) {}
        val cancelled = mutableListOf<String>()
        var scheduled: V11Database? = null
        NeoCommittedEffects(c).reconcile(confirmed, { cancelled.add(it) }, { scheduled = it }, {})
        assertEquals(listOf("removed"), cancelled)
        assertEquals(confirmed, scheduled)
        NeoCommittedEffects(c)
            .reconcile(confirmed, { fail("Already acknowledged") }, { fail("No repeat") }, {})
    }

    @Test
    fun unrelatedDataSaveDoesNotCancelAnUnchangedReminder() {
        val c = RuntimeEnvironment.getApplication()
        val before =
            V11Database(
                tasks =
                    listOf(
                        TaskV11(
                            "unchanged",
                            "Fixture",
                            date = "2026-11-10",
                            reminderAt = 123456789L,
                        )
                    )
            )
        NeoCommittedEffects(c).prepare(before, before)
        var scheduled = false
        NeoCommittedEffects(c)
            .reconcile(
                before,
                { fail("Unchanged reminder must not be cancelled") },
                { scheduled = true },
                {},
            )
        assertTrue(scheduled)
    }

    @Test
    fun retryJobIsPersistedAndAcknowledgedEffectsCancelIt() {
        val c = RuntimeEnvironment.getApplication()
        val q = NeoCommittedEffects(c)
        q.prepare(V11Database(), V11Database())
        val scheduler = c.getSystemService(android.app.job.JobScheduler::class.java)
        val job = scheduler.getPendingJob(NeoCommittedEffects.RETRY_JOB_ID)
        assertNotNull(job)
        assertTrue(job!!.isPersisted)
        assertEquals(NeoEffectsJobService::class.java.name, job.service.className)
        q.reconcile(V11Database(), {}, {}, {})
        assertNull(scheduler.getPendingJob(NeoCommittedEffects.RETRY_JOB_ID))
    }

    @Test
    fun rolledBackDataWriteDoesNotCancelAnExistingUnchangedAlarm() {
        val c = RuntimeEnvironment.getApplication()
        val runtime = V11Runtime(c)
        val before = runtime.start()
        val task =
            TaskV11(
                "rollback-alarm",
                "Old fixture",
                date = "2026-11-10",
                reminderAt = System.currentTimeMillis() + 60000,
            )
        val old = before.copy(tasks = listOf(task))
        runtime.saveIfCurrent(before, old)
        com.todoshift.app.reminders.ToDoShiftReminder.schedule(c, task)
        val alarms =
            org.robolectric.Shadows.shadowOf(
                c.getSystemService(android.app.AlarmManager::class.java)
            )
        val original = requireNotNull(alarms.peekNextScheduledAlarm()).operation
        val next = old.copy(tasks = listOf(task.copy(title = "Uncommitted fixture")))
        try {
            com.todoshift.app.backup.Neo4LocalBackup(c).commitDatabase(old, next) {
                if (it == "database") error("Injected acknowledgement failure")
            }
            fail("Must fail")
        } catch (expected: IllegalStateException) {}
        assertEquals(old, runtime.start())
        assertFalse(
            c.getSharedPreferences(NeoCommittedEffects.STORE_NAME, 0).getBoolean("pending", false)
        )
        NeoCommittedEffects(c)
            .reconcile(old, { fail("Rollback must not cancel a valid alarm") }, {}, {})
        assertEquals(original, requireNotNull(alarms.peekNextScheduledAlarm()).operation)
    }

    @Test
    fun retryReplaysNewestCommittedTaskRatherThanAnEarlierQueuedSnapshot() {
        val c = RuntimeEnvironment.getApplication()
        val runtime = V11Runtime(c)
        val first = runtime.start()
        val task =
            TaskV11(
                "newest",
                "Earlier fixture",
                date = "2026-11-10",
                reminderAt = System.currentTimeMillis() + 60000,
            )
        val middle = first.copy(tasks = listOf(task))
        val latest = middle.copy(tasks = listOf(task.copy(title = "Latest confirmed fixture")))
        val writes = com.todoshift.app.backup.Neo4LocalBackup(c)
        writes.commitDatabase(first, middle)
        writes.commitDatabase(middle, latest)
        NeoCommittedEffects(c).reconcileCurrent()
        val alarms =
            org.robolectric.Shadows.shadowOf(
                c.getSystemService(android.app.AlarmManager::class.java)
            )
        val pending = requireNotNull(requireNotNull(alarms.peekNextScheduledAlarm()).operation)
        val intent = org.robolectric.Shadows.shadowOf(pending).savedIntent
        assertEquals(
            "Latest confirmed fixture",
            intent.getStringExtra(com.todoshift.app.reminders.ToDoShiftReminder.EXTRA_TITLE),
        )
        assertFalse(
            c.getSharedPreferences(NeoCommittedEffects.STORE_NAME, 0).getBoolean("pending", false)
        )
    }
}
