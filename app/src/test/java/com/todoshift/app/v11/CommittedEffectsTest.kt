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
        NeoCommittedEffects(c).prepare(previous)
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
}
