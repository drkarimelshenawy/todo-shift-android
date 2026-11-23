package com.todoshift.app.v11.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
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
class PremiumWorkflowUiTest {
    @get:Rule val compose = createComposeRule()

    private fun content(block: @Composable () -> Unit) {
        compose.setContent {
            NeoEnvironment(
                NeoAppearance(language = NeoLanguage.ENGLISH, reduceMotion = true),
                true,
                {},
            ) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
                    CompositionLocalProvider(LocalV11NeoEnabled provides true) { block() }
                }
            }
        }
    }

    @Test
    fun genuineEmptyTodayIsDifferentFromZeroSearchResults() {
        content { V11TasksScreen(V11Database(), emptyList(), {}, fixedNotesTab = false) }
        compose.onNodeWithText("No tasks for today").assertExists()
        compose.onNodeWithContentDescription("Search").performClick()
        compose.onNodeWithText("Search").performTextInput("absent")
        compose.onNodeWithText("No matching tasks").assertExists()
        compose.onNodeWithText("No tasks for today").assertDoesNotExist()
    }

    @Test
    fun legacyShiftSummaryDoesNotDisplayInventedMidnightClock() {
        val day =
            DayEntryV11(
                "legacy",
                "2026-11-10",
                "a",
                listOf(ShiftSegmentV11("shift", ShiftKind.FULL_DAY)),
            )
        val row = neoShiftOccurrences(V11Database(days = listOf(day)), setOf("a")).single()
        content { NeoShiftSummary(row, emptyList()) }
        compose.onNodeWithText("2026-11-10 • Clock times not recorded").assertExists()
        compose.onAllNodesWithText("00:00", substring = true).assertCountEquals(0)
    }

    @Test
    fun liveRootRelockHidesFinancialControlsEvenBeforeStalePropsChange() {
        val allowed = mutableStateOf(true)
        content {
            CompositionLocalProvider(LocalNeoFinanceAccess provides { allowed.value }) {
                NeoModernHub(
                    V11Database(),
                    emptyList(),
                    true,
                    true,
                    {},
                    {},
                    {},
                    initialSection = "RATES",
                    focused = true,
                )
            }
        }
        compose.onNodeWithText("Amount").assertExists()
        compose.runOnIdle { allowed.value = false }
        // The sensitive Amount field must disappear immediately even if the
        // incoming financeAuthorized / moneyVisible props have not yet changed.
        compose.onAllNodesWithText("Amount").assertCountEquals(0)
        // A relocked account must offer Back, never an unlock-bypass reveal button.
        compose.onNodeWithText("Return to Finance and unlock it before showing amounts.").assertExists()
        compose.onNodeWithText("Back to Finance").assertExists()
        compose.onNodeWithText("Show amounts and open page").assertDoesNotExist()
    }
}
