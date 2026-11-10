package com.todoshift.app.v11.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.todoshift.app.MainActivity
import com.todoshift.app.v11.*
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/** Real Root -> task editor -> journal failure/CAS -> retry; fixtures only, no phone claim. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rUS-w360dp-h800dp-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
class PremiumActivitySaveTest {
    @get:Rule(order = 0)
    val fixture =
        object : ExternalResource() {
            override fun before() {
                val c = RuntimeEnvironment.getApplication()
                V11Runtime(c).start()
                V11UiSettingsStore(c).save(V11UiSettings(experience = V11Experience.PREMIUM))
                NeoAppearanceStore(c)
                    .save(NeoAppearance(language = NeoLanguage.ENGLISH, reduceMotion = true))
            }
        }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    private val ui by lazy { ActivitySemantics { compose.mainClock.advanceTimeBy(32) } }

    private fun addDraft(title: String) {
        ui.click("Tasks")
        ui.click("Add task")
        ui.input("Title", title)
    }

    private fun awaitError() {
        ui.click("العودة للمحرر / Return to editor")
    }

    @Test
    fun actualJournalIoFailureKeepsDraftAndRetryCommitsOnlyOnce() {
        val c = RuntimeEnvironment.getApplication()
        addDraft("Retained Activity draft")
        val journal = File(c.filesDir, "neo4-restore-journal.json")
        assertTrue(journal.mkdir())
        File(journal, "obstruction").writeText("synthetic")
        val backup = File(c.filesDir, "neo4-restore-journal.json.bak")
        assertTrue(backup.mkdir())
        File(backup, "obstruction").writeText("synthetic")
        ui.click("Save")
        awaitError()
        assertTrue(ui.has("Retained Activity draft"))
        assertTrue(journal.deleteRecursively())
        assertTrue(backup.deleteRecursively())
        assertTrue(V11Runtime(c).start().tasks.isEmpty())
        ui.click("Save")
        ui.until { V11Runtime(c).start().tasks.size == 1 }
        ui.until { !ui.has("Title") }
        assertEquals("Retained Activity draft", V11Runtime(c).start().tasks.single().title)
    }

    @Test
    fun concurrentExternalWriteRejectsStaleRootWithoutDiscardingEitherRecord() {
        val c = RuntimeEnvironment.getApplication()
        addDraft("Unsaved Root draft")
        val runtime = V11Runtime(c)
        val old = runtime.start()
        val external = TaskV11("external", "External confirmed record", date = "2026-11-10")
        com.todoshift.app.backup
            .Neo4LocalBackup(c)
            .commitDatabase(old, old.copy(tasks = listOf(external)))
        ui.click("Save")
        awaitError()
        assertTrue(ui.has("Unsaved Root draft"))
        assertEquals(listOf(external), runtime.start().tasks)
        ui.click("Save")
        ui.click("إعادة التحميل مع حفظ المسودة / Reload and keep draft")
        ui.until { !ui.has("حالة الحفظ / Save status") }
        assertTrue(ui.has("Unsaved Root draft"))
        ui.click("Save")
        ui.until { runtime.start().tasks.size == 2 }
        assertEquals(external, runtime.start().tasks.first { it.id == external.id })
        assertEquals(1, runtime.start().tasks.count { it.title == "Unsaved Root draft" })
    }

    @Test
    fun externalWriteRefreshesOnResumeWithoutDroppingOpenDraft() {
        addDraft("Retained resume draft")
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        val runtime = V11Runtime(RuntimeEnvironment.getApplication())
        val before = runtime.start()
        runtime.saveIfCurrent(
            before,
            before.copy(
                tasks =
                    before.tasks +
                        TaskV11(
                            "external-resume",
                            "External",
                            date = java.time.LocalDate.now().toString(),
                        )
            ),
        )
        compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        ui.await("Retained resume draft")
        ui.await("External") // Wait for the asynchronous resume read to reach the actual list.
        ui.click("Save")
        ui.until { runtime.start().tasks.size == 2 }
        assertTrue(runtime.start().tasks.any { it.id == "external-resume" })
    }

    @Test
    fun noteDraftAndGuardSurviveActualActivityRecreation() {
        ui.longClickDay(java.time.LocalDate.now().toString())
        ui.click("New note")
        ui.input("Calendar note text", "Retained calendar draft")
        compose.activityRule.scenario.recreate()
        ui.await("Retained calendar draft")
        ui.click("Cancel")
        ui.await("Discard changes?")
        ui.click("Keep editing")
        ui.await("Retained calendar draft")
        ui.click("Cancel")
        ui.await("Discard changes?")
        ui.click("Save and leave")
        val runtime = V11Runtime(RuntimeEnvironment.getApplication())
        ui.until { runtime.start().dayItems.any { it.title == "Retained calendar draft" } }
        val savedId = runtime.start().dayItems.single().id
        ui.click("Retained calendar draft")
        ui.input("Calendar note text", "Edited restored note")
        compose.activityRule.scenario.recreate()
        ui.await("Edited restored note")
        ui.click("Save")
        ui.until { runtime.start().dayItems.singleOrNull()?.title == "Edited restored note" }
        assertEquals(savedId, runtime.start().dayItems.single().id)
    }
}
