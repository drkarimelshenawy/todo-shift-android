package com.todoshift.app.v11

import com.todoshift.app.backup.Neo4LocalBackup
import com.todoshift.app.data.AppStorage
import com.todoshift.app.data.Workplace
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NewReferenceSafetyTest {
    private fun rejects(action: () -> Unit) {
        var rejected = false
        try {
            action()
        } catch (expected: IllegalArgumentException) {
            rejected = true
        }
        assertTrue("Missing creation references must be rejected before writing", rejected)
    }

    @Test
    fun newTaskCannotUseADeletedCalendarButHiddenCalendarsRemainValid() {
        val c = RuntimeEnvironment.getApplication()
        val runtime = V11Runtime(c)
        val before = runtime.start()
        val coordinator = Neo4LocalBackup(c)
        val invalid =
            before.copy(
                tasks =
                    listOf(
                        TaskV11(
                            "task-fixture",
                            "Fixture",
                            date = "2026-11-10",
                            calendarId = "deleted-calendar",
                        )
                    )
            )
        rejects { coordinator.commitDatabase(before, invalid) }
        assertEquals(before, runtime.start())
        val hidden = before.copy(calendars = before.calendars.map { it.copy(visible = false) })
        runtime.saveIfCurrent(before, hidden)
        val valid =
            hidden.copy(
                tasks =
                    listOf(invalid.tasks.single().copy(calendarId = hidden.calendars.first().id))
            )
        assertEquals(valid, coordinator.commitDatabase(hidden, valid))
    }

    @Test
    fun anExistingHistoricalReferenceIsNotRewrittenByAnUnrelatedEdit() {
        val c = RuntimeEnvironment.getApplication()
        val runtime = V11Runtime(c)
        val before = runtime.start()
        val historic =
            before.copy(
                tasks =
                    listOf(
                        TaskV11(
                            "historical-task",
                            "Fixture",
                            date = "2026-11-10",
                            workplaceId = "historical-missing-place",
                        )
                    )
            )
        runtime.saveIfCurrent(before, historic)
        val next = historic.copy(tasks = historic.tasks.map { it.copy(title = "Edited fixture") })
        assertEquals(next, Neo4LocalBackup(c).commitDatabase(historic, next))
        assertEquals("historical-missing-place", runtime.start().tasks.single().workplaceId)
    }

    @Test
    fun aWorkplaceWithACommittedTaskCannotBeRemovedThroughTheBackend() {
        val c = RuntimeEnvironment.getApplication()
        val runtime = V11Runtime(c)
        val before = runtime.start()
        val legacy = AppStorage(c)
        val oldLegacy = legacy.load()
        val linked =
            oldLegacy.copy(
                workplaces =
                    oldLegacy.workplaces + Workplace("linked-fixture", "Fixture", "#00A4B8")
            )
        legacy.save(linked)
        val withTask =
            before.copy(
                tasks =
                    listOf(
                        TaskV11(
                            "linked-task",
                            "Fixture",
                            date = "2026-11-10",
                            workplaceId = "linked-fixture",
                        )
                    )
            )
        runtime.saveIfCurrent(before, withTask)
        rejects { Neo4LocalBackup(c).updateWorkplaces(linked, withTask, oldLegacy.workplaces) }
        assertEquals(linked, legacy.load())
        assertEquals(withTask, runtime.start())
    }

    @Test
    fun aNewNoteCannotUseADeletedCalendarOrWriteAnyStyleOnRejection() {
        val c = RuntimeEnvironment.getApplication()
        val before = V11Runtime(c).start()
        val metadata = NeoSmartStore(c).snapshot().toString()
        val note =
            DayItemV11(
                "note-fixture",
                "2026-11-10",
                type = DayItemTypeV11.NOTE,
                title = "Fixture",
                calendarId = "deleted-calendar",
            )
        rejects { Neo4LocalBackup(c).saveNote(before, metadata, note, "#00A4B8", true) }
        assertEquals(before, V11Runtime(c).start())
        assertEquals(metadata, NeoSmartStore(c).snapshot().toString())
    }
}
