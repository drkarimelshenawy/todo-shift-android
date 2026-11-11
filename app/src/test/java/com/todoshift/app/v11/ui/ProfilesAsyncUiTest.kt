package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.Column
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
class ProfilesAsyncUiTest {
    @get:Rule val compose = createComposeRule()
    private fun show() {
        compose.setContent {
            val draft = NeoAppearance(language = NeoLanguage.ENGLISH)
            NeoEnvironment(draft, true, {}) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
                    Column { Neo4ProfilesStudio(draft, {}) }
                }
            }
        }
    }

    @Test fun profileSaveAndDeleteCompleteAndReopen() {
        val store = Neo4Profiles(RuntimeEnvironment.getApplication())
        show()
        compose.onNodeWithText("Profile name").performTextInput("Night layout")
        compose.waitUntil(10_000L) {
            compose.onAllNodes(hasText("Save preview as profile") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Save preview as profile").performClick()
        compose.waitUntil(10_000L) { store.list().size == 1 }
        compose.waitUntil(10_000L) {
            compose.onAllNodes(hasText("Night layout") and hasClickAction()).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals("Night layout", store.list().single().name)
        compose.onNodeWithText("Delete").performClick()
        compose.waitUntil(10_000L) { store.list().isEmpty() }
        // The IO commit can finish before the coroutine publishes the refreshed UI list.
        compose.waitUntil(10_000L) {
            compose.onAllNodesWithText("Night layout").fetchSemanticsNodes().isEmpty()
        }
        compose.onNodeWithText("Night layout").assertDoesNotExist()
    }

    @Test fun failedProfileWriteKeepsNameAndRawPreferences() {
        val prefs = RuntimeEnvironment.getApplication().getSharedPreferences("todo_shift_neo4_profiles", 0)
        prefs.edit().putString("profiles", "{broken-original").commit()
        show()
        compose.onNodeWithText("Profile name").performTextInput("Retained draft")
        compose.waitUntil(10_000L) {
            compose.onAllNodes(hasText("Save preview as profile") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Save preview as profile").performClick()
        compose.waitUntil(10_000L) {
            compose.onAllNodes(hasText("Save preview as profile") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals("{broken-original", prefs.getString("profiles", null))
        compose.onNodeWithText("Retained draft").assertExists()
    }
}

