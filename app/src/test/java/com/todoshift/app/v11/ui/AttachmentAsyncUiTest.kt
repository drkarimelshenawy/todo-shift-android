package com.todoshift.app.v11.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.todoshift.app.v11.*
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
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
class AttachmentAsyncUiTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = RuntimeEnvironment.getApplication()
    private fun show(section: String, data: V11Database) {
        compose.setContent {
            NeoEnvironment(NeoAppearance(language = NeoLanguage.ENGLISH, reduceMotion = true), true, {}) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
                    NeoModernHub(data, emptyList(), false, false, {}, {}, {},
                        initialSection = section, focused = true, initialOwner = "task")
                }
            }
        }
    }
    private fun file() = File(context.filesDir, "attachments/neo-attachment-fixture.pdf").apply {
        parentFile!!.mkdirs(); writeText("%PDF-1.4 fixture")
    }

    @Test fun detachPublishesUpdatedListWithoutDeletingPrivateFile() {
        val runtime = V11Runtime(context)
        val before = runtime.start()
        val data = before.copy(tasks = listOf(TaskV11("task", "Fixture", date = "2026-11-11")))
        runtime.saveIfCurrent(before, data)
        val file = file()
        NeoSmartStore(context).update { it.put("attachments", JSONArray().put(JSONObject()
            .put("owner", "task").put("name", file.name).put("mime", "application/pdf"))) }
        show("ATTACHMENTS", data)
        compose.waitUntil(10_000L) {
            compose.onAllNodes(hasText("Detach") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Detach").performScrollTo().performClick()
        compose.waitUntil(10_000L) {
            compose.onAllNodesWithText("Detach").fetchSemanticsNodes().isEmpty()
        }
        assertTrue(NeoAttachmentStore(context).list().isEmpty())
        assertTrue(file.exists())
        assertEquals(data, runtime.start())
    }

    @Test fun confirmedDetachedCleanupRefreshesInventoryWithoutChangingRecords() {
        val data = V11Runtime(context).start()
        val file = file()
        show("STORAGE", data)
        compose.waitUntil(10_000L) {
            compose.onAllNodes(hasText("Review detached-file cleanup") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Review detached-file cleanup").performScrollTo().performClick()
        compose.onNodeWithText("Delete detached files").performClick()
        compose.waitUntil(10_000L) { !file.exists() }
        compose.waitUntil(10_000L) {
            compose.onAllNodesWithText("Detached files: 0").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(data, V11Storage(context).load())
    }
}
