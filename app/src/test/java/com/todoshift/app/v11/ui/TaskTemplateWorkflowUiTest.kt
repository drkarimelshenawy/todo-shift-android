package com.todoshift.app.v11.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
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
@Config(sdk=[28])
@LooperMode(LooperMode.Mode.PAUSED)
class TaskTemplateWorkflowUiTest {
    @get:Rule val compose=createComposeRule()
    @Test fun templateCreateUseDuplicateDateCopyAndDeleteKeepPreviouslyCreatedTasks() {
        val store=NeoSmartStore(RuntimeEnvironment.getApplication())
        var db by mutableStateOf(V11Database())
        compose.setContent {NeoEnvironment(NeoAppearance(language=NeoLanguage.ENGLISH,reduceMotion=true),true,{}) {
            V11AppThemeProvider(V11AppTheme.TODO_SHIFT,V11Experience.PREMIUM) {NeoTaskTemplatesScreen(db,emptyList(),{db=it},{})}
        }}
        compose.waitUntil(5000) { !compose.onNodeWithText("New template").fetchSemanticsNode().config.contains(SemanticsProperties.Disabled) }
        compose.onNodeWithText("New template").performClick()
        compose.onNodeWithText("List name").performTextInput("Daily clinic")
        compose.onNodeWithText("One task per line").performTextInput("Check equipment\nReview schedule")
        compose.onNodeWithText("Save template").performClick()
        compose.waitUntil(5000) {store.templates().size==1}
        compose.waitUntil(5000) {compose.onAllNodesWithText("Save template").fetchSemanticsNodes().isEmpty()}
        compose.onNodeWithText("Use template").performClick();compose.onNodeWithText("Create tasks").performClick()
        compose.runOnIdle {assertEquals(2,db.tasks.size)}
        val saved=db.tasks
        compose.onNodeWithText("Use template").performClick();compose.onNodeWithText("Create tasks").performClick()
        compose.onNodeWithText("Already created for this date").assertExists()
        compose.runOnIdle {assertEquals(saved,db.tasks)}
        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Duplicate").performClick();compose.waitUntil(5000) {store.templates().size==2}
        compose.waitForIdle();compose.onAllNodesWithText("Delete").onFirst().performClick()
        compose.onNodeWithText("Delete template only?").assertExists()
        compose.onAllNodesWithText("Delete").onLast().performClick();compose.waitUntil(5000) {store.templates().size==1}
        compose.runOnIdle {assertEquals(saved,db.tasks)}
    }
}
