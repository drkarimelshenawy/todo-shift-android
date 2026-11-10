package com.todoshift.app.v11.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
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
class WorkplaceSaveAcceptanceTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun editorRetainsDraftAcrossUnknownAcknowledgementAndClosesOnlyAfterCommit() {
        val restoration = StateRestorationTester(compose)
        val status = mutableStateOf(NeoEditorWriteState())
        val requests = mutableListOf<List<Workplace>>()
        var completion: ((Boolean) -> Unit)? = null
        restoration.setContent {
            NeoEnvironment(NeoAppearance(language = NeoLanguage.ENGLISH), true, {}) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
                    CompositionLocalProvider(
                        LocalNeoEditorWriteState provides status.value,
                        LocalNeoWorkplaceCommitRequest provides
                            { next, done ->
                                requests.add(next)
                                completion = done
                                status.value = NeoEditorWriteState(pending = true)
                            },
                    ) {
                        V11WorkplacesScreen(
                            emptyList(),
                            {},
                            { fail("Must use checked root contract") },
                        )
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Add workplace").performClick()
        compose.onNodeWithText("Workplace name").performTextInput("Retained workplace fixture")
        listOf("24 hours", "Long", "Night", "Morning", "Evening").forEach {
            compose.onNodeWithText(it).performScrollTo().performTextInput("0")
        }
        compose.onNodeWithText("Save").assertIsEnabled().performClick()
        compose.onNodeWithText("Retained workplace fixture").assertExists()
        compose.runOnIdle {
            completion!!(false)
            status.value = NeoEditorWriteState(error = "Injected I/O")
        }
        compose.onNodeWithText("Retained workplace fixture").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Retained workplace fixture").assertExists()
        compose.onNodeWithText("Save").performClick()
        assertEquals(2, requests.size)
        assertEquals(requests[0].single().id, requests[1].single().id)
        compose.runOnIdle {
            completion!!(true)
            status.value = NeoEditorWriteState()
        }
        compose.onNodeWithText("Workplace name").assertDoesNotExist()
    }
}
