package com.todoshift.app.v11

import org.junit.Assert.*
import org.junit.Test

class NeoTaskCommitTest {
    private val task = TaskV11("stable-draft", "Fixture", date = "2026-11-10")

    @Test
    fun failureBeforeCommitRetainsStoreAndRetryDoesNotDuplicate() {
        var db = V11Database()
        var fail = true
        var effects = 0
        val commit =
            NeoTaskCommit(
                { db },
                { expected, next ->
                    assertEquals(expected, db)
                    if (fail) error("Injected before CAS")
                    db = next
                },
                { effects++ },
            )
        assertEquals(NeoTaskCommit.Result.Retry, commit.submit(task))
        assertTrue(db.tasks.isEmpty())
        assertEquals(0, effects)
        fail = false
        assertEquals(NeoTaskCommit.Result.Committed(false), commit.submit(task))
        assertEquals(NeoTaskCommit.Result.Committed(false), commit.submit(task))
        assertEquals(listOf(task), db.tasks)
    }

    @Test
    fun ambiguousFailureAfterCommitIsResolvedBeforeRetry() {
        var db = V11Database()
        var writes = 0
        val commit =
            NeoTaskCommit(
                { db },
                { _, next ->
                    db = next
                    writes++
                    if (writes == 1) error("Injected after commit")
                },
                {},
            )
        assertEquals(NeoTaskCommit.Result.Committed(false), commit.submit(task))
        assertEquals(NeoTaskCommit.Result.Committed(false), commit.submit(task))
        assertEquals(3, writes)
        assertEquals(listOf(task), db.tasks)
    }

    @Test
    fun changedPayloadWithSameIdConflictsAndLeavesUnrelatedRecordsUntouched() {
        val other = task.copy(id = "other")
        val db = V11Database(tasks = listOf(task, other))
        val commit =
            NeoTaskCommit({ db }, { _, _ -> fail("Must not write") }, { fail("Must not schedule") })
        assertEquals(NeoTaskCommit.Result.Conflict, commit.submit(task.copy(title = "Changed")))
        assertEquals(listOf(task, other), db.tasks)
    }

    @Test
    fun sideEffectFailureDoesNotMisreportDataFailureOrDuplicateRecords() {
        var db = V11Database()
        var fail = true
        var writes = 0
        val commit =
            NeoTaskCommit(
                { db },
                { _, next ->
                    db = next
                    writes++
                },
                { if (fail) error("Service unavailable") },
            )
        assertEquals(NeoTaskCommit.Result.Committed(true), commit.submit(task))
        fail = false
        assertEquals(NeoTaskCommit.Result.Committed(false), commit.submit(task))
        assertEquals(2, writes)
        assertEquals(listOf(task), db.tasks)
    }

    @Test
    fun visibleMemoryWithoutDurableAcknowledgementIsNeverReportedCommitted() {
        var visible = V11Database()
        var durable = visible
        var allowDisk = false
        var effects = 0
        val commit =
            NeoTaskCommit(
                { visible },
                { _, next ->
                    visible = next
                    if (!allowDisk) error("Disk failed after memory updated")
                    durable = next
                },
                { effects++ },
            )
        assertEquals(NeoTaskCommit.Result.Retry, commit.submit(task))
        assertEquals(NeoTaskCommit.Result.Retry, commit.submit(task))
        assertTrue(durable.tasks.isEmpty())
        assertEquals(0, effects)
        allowDisk = true
        assertEquals(NeoTaskCommit.Result.Committed(false), commit.submit(task))
        assertEquals(listOf(task), durable.tasks)
    }
    @Test
    fun platformSideEffectsCannotExecuteWhileDatabaseGateIsOwned() {
        var db = V11Database()
        var gateWasAvailable = false
        val commit = NeoTaskCommit(
            { db }, { _, next -> db = next },
            {
                val latch = java.util.concurrent.CountDownLatch(1)
                val worker = Thread {
                    synchronized(com.todoshift.app.backup.Neo4DataGate.monitor) {
                        latch.countDown()
                    }
                }
                worker.start()
                gateWasAvailable = latch.await(2, java.util.concurrent.TimeUnit.SECONDS)
                worker.join(2500)
            },
        )
        assertEquals(NeoTaskCommit.Result.Committed(false), commit.submit(task))
        assertTrue("Widget/AlarmManager callbacks must run without the storage gate", gateWasAvailable)
        assertEquals(listOf(task), db.tasks)
    }

}
