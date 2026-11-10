package com.todoshift.app.v11.ui

import android.content.Intent
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.todoshift.app.MainActivity
import com.todoshift.app.v11.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rUS-w360dp-h800dp-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
class PremiumNavigationActivityTest {
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

    private lateinit var launchIntent: Intent

    @Before
    fun retainHarnessLaunchIdentity() {
        launchIntent = Intent(compose.activity.intent)
    }

    @After
    fun restoreHarnessLaunchIdentity() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            compose.activity.intent = launchIntent
        }
    }

    private fun back() =
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            compose.activity.onBackPressedDispatcher.onBackPressed()
        }

    private fun warm(intent: Intent) {
        // Resolve the current resumed host after recreation, rather than sending to a
        // host obtained independently of ActivityScenario's lifecycle synchronization.
        compose.activityRule.scenario.onActivity { activity ->
            assertTrue(
                activity.lifecycle.currentState.isAtLeast(
                    androidx.lifecycle.Lifecycle.State.RESUMED
                )
            )
            ReflectionHelpers.callInstanceMethod<Unit>(
                activity,
                "onNewIntent",
                ReflectionHelpers.ClassParameter.from(Intent::class.java, intent),
            )
            assertEquals(intent.action, activity.intent.action)
        }
    }

    @Test
    fun everyMainDestinationPairKeepsActualBodyAndSelectionAndBackReturnsHome() {
        val tabs = listOf("Calendar", "Schedules", "Tasks", "Finance", "Settings")
        for (from in tabs) for (to in tabs) {
            ui.click(from)
            ui.click(to)
            ui.until { ui.selected(to) }
            when (to) {
                "Tasks" -> ui.await("Add task")
                "Settings" -> ui.await("Appearance & Design")
                "Calendar" -> ui.await("Manage calendars")
                "Schedules" -> ui.await("Summary")
            }
        }
        ui.click("Tasks")
        back()
        ui.until { ui.selected("Calendar") }
        assertFalse(compose.activity.isFinishing)
    }

    @Test
    fun invalidRawAccentIsGuardedOnTabSwitchAndDiscardActuallyLeavesStudio() {
        ui.click("Settings")
        ui.click("Appearance & Design")
        compose.onAllNodesWithText("Appearance Studio").assertCountEquals(1)
        compose.onAllNodesWithContentDescription("Back").assertCountEquals(1)
        ui.click("Colors and background")
        ui.input("Custom accent #RRGGBB", "#12")
        compose.activityRule.scenario.recreate()
        ui.await("#12")
        ui.click("Tasks")
        ui.await("Discard preview changes?")
        assertTrue(ui.selected("Settings"))
        ui.click("Keep editing")
        assertTrue(ui.has("#12"))
        ui.click("Tasks")
        ui.await("Discard preview changes?")
        ui.click("Discard")
        ui.until { ui.selected("Tasks") }
        ui.await("Add task")
        assertFalse(ui.has("Appearance Studio"))
    }

    @Test
    fun warmTaskLinkCannotDiscardDirtyTaskAndDeletedTargetGetsSafeFeedback() {
        ui.click("Tasks")
        ui.click("Add task")
        ui.input("Title", "Retained warm-link draft")
        warm(
            Intent()
                .putExtra("todo_shift_task_action", "open")
                .putExtra("todo_shift_task_open_id", "deleted-fixture")
        )
        ui.await("Discard changes?")
        ui.click("Keep editing")
        assertTrue(ui.has("Retained warm-link draft"))
        warm(
            Intent()
                .putExtra("todo_shift_task_action", "open")
                .putExtra("todo_shift_task_open_id", "deleted-fixture")
        )
        ui.click("Discard")
        ui.await("Task unavailable")
        assertTrue(V11Runtime(RuntimeEnvironment.getApplication()).start().tasks.isEmpty())
    }

    @Test
    fun recreationRetainsDirtyTaskAndConsumedEntryDoesNotOpenItAgainAfterSave() {
        ui.click("Tasks")
        ui.click("Add task")
        ui.input("Title", "Recreated Activity draft")
        compose.activityRule.scenario.recreate()
        ui.await("Recreated Activity draft")
        assertTrue(ui.selected("Tasks"))
        ui.click("Save")
        val runtime = V11Runtime(RuntimeEnvironment.getApplication())
        ui.until { runtime.start().tasks.size == 1 }
        ui.until { !ui.has("Title") }
        compose.activityRule.scenario.recreate()
        ui.await("Recreated Activity draft")
        assertFalse(ui.has("Title"))
        assertEquals(1, runtime.start().tasks.size)
    }

    @Test
    fun warmFinanceEntryShowsLockInsteadOfProtectedRecords() {
        // This test exercises a warm entry, not delivery during asynchronous boot.
        ui.await("Manage calendars")
        val c = RuntimeEnvironment.getApplication()
        val security = V11SecurityStore(c)
        assertTrue(security.setPin("4826"))
        security.save(V11SecuritySettings(mode = V11LockMode.PIN, protectAccounts = true))
        val runtime = V11Runtime(c)
        val old = runtime.start()
        runtime.saveIfCurrent(
            old,
            old.copy(
                payments =
                    listOf(
                        PaymentV11(
                            "private",
                            123456.0,
                            "2026-11-10",
                            "2026-11",
                            note = "Private fixture secret",
                        )
                    )
            ),
        )
        compose.activityRule.scenario.recreate()
        ui.await("Manage calendars")
        assertTrue(ui.selected("Calendar"))
        assertFalse(ui.has("Private fixture secret"))
        warm(Intent("com.todoshift.app.OPEN_FINANCE"))
        ui.until { ui.selected("Finance") }
        ui.await("PIN")
        assertFalse(ui.has("Private fixture secret"))
    }
}
