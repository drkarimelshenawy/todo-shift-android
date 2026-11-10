package com.todoshift.app.v11.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import com.todoshift.app.v11.*
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
class AuditFixesUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun obligationDueDaySurvivesRestoration() {
        val selectedDay = (java.time.LocalDate.now().dayOfMonth % 31 + 1).toString()
        val restore = StateRestorationTester(compose)
        restore.setContent {
            NeoEnvironment(NeoAppearance(language = NeoLanguage.ENGLISH), true, {}) {
                NeoObligationForm(V11Database(), {}, {})
            }
        }
        compose.onNodeWithText("Due day").performScrollTo()
        compose.onNodeWithText(java.time.LocalDate.now().dayOfMonth.toString()).performClick()
        compose.onNodeWithText(selectedDay).performScrollTo().performClick()
        restore.emulateSavedInstanceStateRestore()
        compose.onNodeWithText(selectedDay).assertExists()
    }

    @Test
    fun saveAndLeaveWaitsForSuccessfulWriteAndFailureKeepsDraft() {
        val gate = NeoLeaveGate()
        var reply: ((Boolean) -> Unit)? = null
        var exited = false
        var writes = 0
        val pending = mutableStateOf(false)
        val text = mutableStateOf("old")
        compose.setContent {
            NeoEnvironment(NeoAppearance(language = NeoLanguage.ENGLISH), true, {}) {
                CompositionLocalProvider(
                    LocalNeoLeaveGate provides gate,
                    LocalNeoEditorWriteState provides NeoEditorWriteState(pending = pending.value),
                    LocalNeoCommitRequest provides
                        { _, done ->
                            writes++
                            pending.value = true
                            reply = { result ->
                                pending.value = false
                                done(result)
                            }
                        },
                ) {
                    val commit = rememberNeoCommit({})
                    val exit = rememberNeoEditorExit(listOf(text.value), { exited = true })
                    androidx.compose.foundation.layout.Column {
                        androidx.compose.material3.OutlinedTextField(
                            text.value,
                            { text.value = it },
                            label = { androidx.compose.material3.Text("Draft") },
                        )
                        exit.SaveButton(onClick = { commit(V11Database()) {} }) {
                            androidx.compose.material3.Text("Save")
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Draft").performTextReplacement("changed")
        compose.runOnIdle { gate.request { exited = true } }
        compose.onNodeWithText("Save and leave").performClick()
        compose.onNodeWithText("Discard").assertIsNotEnabled()
        compose.runOnIdle {
            org.robolectric.shadows.ShadowDialog.getShownDialogs()
                .last { it.isShowing }
                .onBackPressed()
            assertFalse(exited)
            reply!!(false)
        }
        compose.onNodeWithText("changed").assertExists()
        compose.onNodeWithText("Save and leave").performClick()
        compose.runOnIdle {
            assertFalse(exited)
            assertEquals(2, writes)
            reply!!(true)
            assertTrue(exited)
        }
    }

    @Test
    fun resetFiltersAlsoClearsKindAndPeriod() {
        val month = YearMonth.of(2026, 11)
        val state =
            mutableStateOf(
                NeoAppearance(
                    language = NeoLanguage.ENGLISH,
                    timelineFilter = NeoTimelineFilter.PAST,
                )
            )
        compose.setContent {
            NeoEnvironment(state.value, true, { state.value = it }) {
                NeoShiftTimeline(V11Database(), emptyList(), month, {}, { _, _ -> })
            }
        }
        compose.onNodeWithContentDescription("Filters").performClick()
        compose.onAllNodesWithText("All").onLast().performClick()
        compose.onNodeWithText("Night").performClick()
        compose.onNodeWithText("Reset filters").performClick()
        assertEquals(NeoTimelineFilter.ALL, state.value.timelineFilter)
        compose.onNodeWithText("Night").assertDoesNotExist()
    }
}
