package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@LooperMode(LooperMode.Mode.PAUSED)
class ModernToolScreensTest {
    @get:Rule val compose = createComposeRule()

    private fun open(label: String, expected: String) {
        val db =
            V11Database(
                calendars = listOf(AppCalendarV11("c", "Calendar", "#123456")),
                tasks = listOf(TaskV11("task", "Synthetic task", date = "2026-11-10")),
            )
        compose.setContent {
            NeoEnvironment(
                NeoAppearance(language = NeoLanguage.ENGLISH, reduceMotion = true),
                true,
                {},
            ) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
                    Box(Modifier.width(360.dp).height(640.dp)) {
                        NeoModernHub(
                            db,
                            listOf(Workplace("w", "Work", "#123456")),
                            true,
                            true,
                            {},
                            {},
                            {},
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("Search", useUnmergedTree = true).performClick()
        compose.onNodeWithText(label, useUnmergedTree = true).performScrollTo().performClick()
        compose
            .onAllNodesWithText(expected, substring = true, useUnmergedTree = true)
            .onFirst()
            .assertExists()
    }

    @Test
    fun copyReviewScreen() {
        open("Copy shifts", "Copy shifts only")
    }

    @Test
    fun shiftTemplateScreen() {
        open("Shift favorite", "Shift type")
    }

    @Test
    fun taskTemplateScreen() {
        open("Task templates", "New template")
        compose.waitForIdle()
        compose.onNodeWithText("New template").performClick()
        compose.onNodeWithText("One task per line").assertExists()
    }

    @Test
    fun effectiveRateScreen() {
        open("Dated rates", "Effective from")
    }

    @Test
    fun monthComparisonScreen() {
        open("Compare months", "First month")
    }

    @Test
    fun workplaceStatementScreen() {
        open("Workplace statement", "Unassigned general receipts")
    }

    @Test
    fun allocationScreen() {
        open("Payment allocation", "Comma-separated accrual months")
    }

    @Test
    fun monthReviewScreen() {
        open("Month review", "Review month closing")
    }

    @Test
    fun dataHealthScreen() {
        open("Data health", "Repair reminder registrations")
    }

    @Test
    fun notificationScreen() {
        open("Notifications", "Task reminders")
    }

    @Test
    fun attachmentScreen() {
        open("Attachments", "Task or note")
    }

    @Test
    fun focusScreen() {
        open("Focus", "Temporary focus")
    }

    @Test
    fun usageScreen() {
        open("Local counts", "Optional on-device counts")
    }
}
