package com.todoshift.app.v11.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.todoshift.app.v11.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@LooperMode(LooperMode.Mode.PAUSED)
class PremiumP01UiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun groupedSourceEditReopensAsNineThousandAndInvalidStaysInEditor() {
        val runtime = V11Runtime(RuntimeEnvironment.getApplication())
        val initial = runtime.start()
        val source =
            IncomeSourceV11(id = "p01-source", name = "Fixture", type = IncomeSourceTypeV11.OTHER)
        val db = initial.copy(incomeSources = listOf(source))
        runtime.saveIfCurrent(initial, db)
        compose.setContent {
            NeoEnvironment(NeoAppearance(language = NeoLanguage.ENGLISH), true, {}) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
                    V11EditIncomeSourceDialog(source, {}) { updated ->
                        runtime.saveIfCurrent(db, db.copy(incomeSources = listOf(updated)))
                    }
                }
            }
        }
        compose.onNodeWithText("Fixed monthly income").performTextReplacement("9,00")
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithText("9,00").assertExists()
        assertNull(runtime.start().incomeSources.single().fixedMonthlyAmount)
        compose.onNodeWithText("Fixed monthly income").performTextReplacement("9,000")
        compose.onNodeWithText("Save").performClick()
        assertEquals(
            900000L,
            LocalizedNumbers.cents(runtime.start().incomeSources.single().fixedMonthlyAmount!!),
        )
    }

    @Test
    fun failedQuickTaskKeepsRawDraftAndAllowsRetryWithoutChangingId() {
        val state = mutableStateOf(NeoEditorWriteState())
        val submitted = mutableListOf<TaskV11>()
        compose.setContent {
            NeoEnvironment(NeoAppearance(language = NeoLanguage.ENGLISH), true, {}) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
                    CompositionLocalProvider(LocalNeoEditorWriteState provides state.value) {
                        TaskEditorDialogV11(null, V11Database(), emptyList(), {}) {
                            submitted.add(it)
                            state.value = NeoEditorWriteState(pending = true)
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Title").performTextInput("Retained fixture")
        compose.onNodeWithText("Save").performClick()
        compose.onNodeWithText("Retained fixture").assertExists()
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.runOnIdle { state.value = NeoEditorWriteState(error = "Injected I/O failure") }
        compose.onNodeWithText("Retained fixture").assertExists()
        compose.onNodeWithText("Save").performClick()
        assertEquals(2, submitted.size)
        assertEquals(submitted[0].id, submitted[1].id)
    }

    @Test
    fun completionContractNeverDismissesOnEnqueueOrRejectedWrite() {
        var completion: ((Boolean) -> Unit)? = null
        var dismissed = 0
        compose.setContent {
            CompositionLocalProvider(
                LocalNeoCommitRequest provides { _, done -> completion = done }
            ) {
                val commit = rememberNeoCommit { fail("Must use root result") }
                androidx.compose.material3.Button(
                    onClick = { commit(V11Database()) { dismissed++ } }
                ) {
                    androidx.compose.material3.Text("Submit fixture")
                }
            }
        }
        compose.onNodeWithText("Submit fixture").performClick()
        assertEquals(0, dismissed)
        compose.runOnIdle { completion!!(false) }
        assertEquals(0, dismissed)
        compose.onNodeWithText("Submit fixture").performClick()
        compose.runOnIdle { completion!!(true) }
        assertEquals(1, dismissed)
    }
}
