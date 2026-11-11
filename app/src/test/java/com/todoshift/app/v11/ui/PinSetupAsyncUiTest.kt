package com.todoshift.app.v11.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.todoshift.app.v11.*
import java.util.concurrent.atomic.AtomicBoolean
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
class PinSetupAsyncUiTest {
    @get:Rule val compose = createComposeRule()

    private fun show(store: V11SecurityStore, changing: Boolean, saved: AtomicBoolean) {
        compose.setContent {
            NeoEnvironment(NeoAppearance(language = NeoLanguage.ENGLISH), true, {}) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
                    PinSetupDialogV3(store, changing, {}, { saved.set(true) })
                }
            }
        }
    }

    @Test fun initialPinIsAcknowledgedOnlyAfterSaving() {
        val store = V11SecurityStore(RuntimeEnvironment.getApplication())
        val saved = AtomicBoolean(false)
        show(store, false, saved)
        compose.onNodeWithText("New PIN").performTextInput("4826")
        compose.onNodeWithText("Confirm PIN").performTextInput("4826")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(10_000L) { saved.get() }
        assertTrue(store.verifyPin("4826"))
    }

    @Test fun wrongCurrentPinKeepsOldSecretAndDraftThenAllowsValidRetry() {
        val store = V11SecurityStore(RuntimeEnvironment.getApplication())
        assertTrue(store.setPin("4826"))
        val saved = AtomicBoolean(false)
        show(store, true, saved)
        compose.onNodeWithText("Current PIN").performTextInput("0000")
        compose.onNodeWithText("New PIN").performTextInput("1234")
        compose.onNodeWithText("Confirm PIN").performTextInput("1234")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(10_000L) {
            compose.onAllNodesWithText("Incorrect current PIN").fetchSemanticsNodes().isNotEmpty()
        }
        assertFalse(saved.get())
        assertTrue(store.verifyPin("4826"))
        compose.onNodeWithText("Current PIN").performTextReplacement("4826")
        compose.onNodeWithText("Save").performClick()
        compose.waitUntil(10_000L) { saved.get() }
        assertTrue(store.verifyPin("1234"))
        assertFalse(store.verifyPin("4826"))
    }
}
