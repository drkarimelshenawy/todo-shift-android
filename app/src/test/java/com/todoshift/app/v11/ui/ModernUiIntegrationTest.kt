package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@LooperMode(LooperMode.Mode.PAUSED)
class ModernUiIntegrationTest {
    @get:Rule val compose = createComposeRule()

    private fun content(block: @Composable () -> Unit) {
        compose.setContent {
            NeoEnvironment(
                NeoAppearance(
                    language = NeoLanguage.ENGLISH,
                    uiDirection = NeoDirection.LTR,
                    reduceMotion = true,
                ),
                true,
                {},
            ) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
                    CompositionLocalProvider(LocalV11NeoEnabled provides true) {
                        Box(Modifier.width(360.dp).height(640.dp)) { block() }
                    }
                }
            }
        }
    }

    @Test
    fun quickTaskRequiresTitleAndKeepsAdvancedOptionsCollapsed() {
        var saved: TaskV11? = null
        content { TaskEditorDialogV11(null, V11Database(), emptyList(), {}, { saved = it }) }
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithText("Title").performTextInput("Synthetic quick task")
        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle {
            assertEquals("Synthetic quick task", saved!!.title)
            assertNull(saved!!.reminderAt)
        }
    }

    @Test
    fun localizedPaymentInputPersistsTheExactNormalizedAmount() {
        var saved: PaymentV11? = null
        content { NeoPaymentForm(V11Database(), YearMonth.of(2026, 11), null, {}, { saved = it }) }
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("٩٬٠٠٠٫٥٠")
        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle {
            assertEquals(9000.5, saved!!.amount, 0.0)
            assertEquals(900050L, saved!!.allocations.sumOf { LocalizedNumbers.cents(it.amount) })
        }
    }

    @Test
    fun paymentCancelRequiresExplicitDiscardAfterEditing() {
        var dismissed = false
        content {
            NeoPaymentForm(V11Database(), YearMonth.of(2026, 11), null, { dismissed = true }, {})
        }
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("10")
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Discard changes?").assertExists()
        compose.runOnIdle { assertFalse(dismissed) }
        compose.onNodeWithText("Keep editing").performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Discard").performClick()
        compose.runOnIdle { assertTrue(dismissed) }
    }

    @Test
    fun calendarNotesExistIndependentlyOfShifts() {
        val day = LocalDate.parse("2026-11-10")
        val db =
            V11Database(
                dayItems =
                    listOf(
                        DayItemV11(
                            "n",
                            day.toString(),
                            type = DayItemTypeV11.NOTE,
                            title = "📌 Synthetic pinned note",
                        )
                    )
            )
        content {
            NeoCalendarArea(
                YearMonth.from(day),
                db,
                emptyList(),
                emptySet(),
                day,
                false,
                emptySet(),
                {},
                {},
                {},
                {},
            )
        }
        compose.onNodeWithText("Synthetic pinned note").assertExists()
    }

    @Test
    fun organizeHubDoesNotExposePaymentSearchWhenLocked() {
        val db =
            V11Database(
                payments =
                    listOf(
                        PaymentV11("p", 100.0, "2026-11-10", "2026-11", note = "Private payment")
                    )
            )
        content { NeoModernHub(db, emptyList(), false, false, {}, {}, {}) }
        compose.onNode(hasSetTextAction()).performTextInput("Private")
        compose.onNodeWithText("Private payment", substring = true).assertDoesNotExist()
    }
}
