package com.todoshift.app.v11.ui

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.todoshift.app.MainActivity
import com.todoshift.app.data.AppStorage
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rUS-w360dp-h800dp-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
class PremiumPaintingActivityTest {
    @get:Rule(order = 0)
    val fixture =
        object : ExternalResource() {
            override fun before() {
                val c = RuntimeEnvironment.getApplication()
                V11Runtime(c).start()
                val storage = AppStorage(c)
                storage.save(
                    storage
                        .load()
                        .copy(workplaces = listOf(Workplace("clinic", "Clinic", "#00A4B8")))
                )
                V11UiSettingsStore(c).save(V11UiSettings(experience = V11Experience.PREMIUM))
                NeoAppearanceStore(c)
                    .save(NeoAppearance(language = NeoLanguage.ENGLISH, reduceMotion = true))
            }
        }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()
    private val ui by lazy { ActivitySemantics { compose.mainClock.advanceTimeBy(32) } }

    private fun back() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            compose.activity.onBackPressedDispatcher.onBackPressed()
        }
        compose.mainClock.advanceTimeBy(64)
    }

    @Test
    fun plusEntersPaintingImmediatelyAndBackClosesTypesThenPaintingBeforeSystem() {
        ui.click("Paint shifts")
        ui.await("Eraser")
        assertFalse(ui.has("Quick add"))
        ui.click("Clinic")
        ui.await("Morning")
        back()
        ui.until { !ui.has("Morning") }
        assertTrue(ui.has("Eraser"))
        back()
        ui.until { !ui.has("Eraser") }
        assertFalse(compose.activity.isFinishing)
    }

    @Test
    fun multiSelectionSurvivesRecreationAndExitRequiresExplicitDiscardThenResetsMode() {
        ui.click("Paint shifts")
        ui.click("Painting tools")
        ui.click("Multiple dates")
        ui.clickDay(LocalDate.now().toString())
        ui.await("Review 1")
        compose.activityRule.scenario.recreate()
        ui.await("Review 1")
        ui.click("Done")
        ui.await("Discard selected dates?")
        ui.click("Keep editing")
        ui.await("Review 1")
        ui.click("Done")
        ui.await("Discard selected dates?")
        ui.click("Discard")
        ui.until { !ui.has("Eraser") }
        ui.click("Paint shifts")
        assertFalse(ui.has("Review 1"))
        assertTrue(V11Runtime(RuntimeEnvironment.getApplication()).start().days.isEmpty())
    }

    @Test
    fun paintCommitReopensWithOwnershipAndNormalDayTapReturnsAfterDone() {
        ui.click("Paint shifts")
        ui.clickDay(LocalDate.now().toString())
        val runtime = V11Runtime(RuntimeEnvironment.getApplication())
        ui.until { runtime.start().days.any { it.segments.isNotEmpty() } }
        val first = runtime.start()
        assertEquals(first.calendars.first().id, first.days.single().calendarId)
        ui.click("Done")
        ui.until { !ui.has("Eraser") }
        ui.clickDay(LocalDate.now().toString())
        assertEquals(first.days, runtime.start().days)
        assertFalse(ui.has("Eraser"))
    }

    @Test
    fun dayNoteDeletionRequiresReviewAndCancelPreservesTheStoredNote() {
        ui.await("Paint shifts")
        val runtime = V11Runtime(RuntimeEnvironment.getApplication())
        val old = runtime.start()
        val note =
            DayItemV11(
                "delete-note-fixture",
                LocalDate.now().toString(),
                type = DayItemTypeV11.NOTE,
                title = "Independent note fixture",
            )
        runtime.saveIfCurrent(old, old.copy(dayItems = old.dayItems + note))
        compose.activityRule.scenario.recreate()
        ui.await("Paint shifts")
        ui.longClickDay(LocalDate.now().toString())
        ui.click("Delete")
        ui.await("Delete note?")
        assertTrue(runtime.start().dayItems.any { it.id == note.id })
        ui.click("Cancel")
        assertTrue(runtime.start().dayItems.any { it.id == note.id })
        ui.click("Delete")
        ui.await("Delete note?")
        ui.click("Delete")
        ui.until { runtime.start().dayItems.none { it.id == note.id } }
        assertEquals(old.days, runtime.start().days)
    }
}
