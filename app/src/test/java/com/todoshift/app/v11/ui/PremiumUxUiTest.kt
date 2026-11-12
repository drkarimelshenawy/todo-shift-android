package com.todoshift.app.v11.ui

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.todoshift.app.data.Workplace
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
@Config(sdk=[28])
@LooperMode(LooperMode.Mode.PAUSED)
class PremiumUxUiTest {
    @get:Rule val compose=createComposeRule()
    private fun render(block: @Composable ()->Unit) {
        compose.setContent { NeoEnvironment(NeoAppearance(language=NeoLanguage.ENGLISH,reduceMotion=true),true,{}) {
            V11AppThemeProvider(V11AppTheme.TODO_SHIFT,V11Experience.PREMIUM) {
                CompositionLocalProvider(LocalV11NeoEnabled provides true) {Box(Modifier.width(360.dp).height(700.dp)) {block()}}
            }
        } }
    }
    @Test fun firstTapOnlySelectsSecondTapOpensAndLongPressKeepsQuickSummary() {
        val date=LocalDate.parse("2026-11-12");var selected by mutableStateOf(date.minusDays(1));var opens=0
        render { NeoCalendarArea(YearMonth.from(date),V11Database(),emptyList(),emptySet(),selected,false,emptySet(),{selected=it},{},{},{opens++}) }
        val day=compose.onNodeWithContentDescription(date.toString(),substring=true)
        day.performClick();compose.runOnIdle {assertEquals(0,opens);assertEquals(date,selected)}
        day.performClick();compose.runOnIdle {assertEquals(1,opens)}
        day.performTouchInput {longClick()};compose.onNodeWithText("New note").assertExists()
    }
    @Test fun completedTitleHasStrikeThroughAndTitleClickDoesNotToggle() {
        var toggles=0;var edits=0
        render {TaskCardV11(TaskV11("a","Completed fixture",date="2026-11-12",status=TaskStatusV11.COMPLETED),{toggles++},{edits++},{})}
        val results=mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Completed fixture",useUnmergedTree=true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) {it(results)}
        assertEquals(TextDecoration.LineThrough,results.single().layoutInput.style.textDecoration)
        compose.onNodeWithText("Completed fixture").performClick();compose.runOnIdle {assertEquals(1,edits);assertEquals(0,toggles)}
        compose.onNode(isToggleable()).performClick();compose.runOnIdle {assertEquals(1,toggles)}
    }
    @Test fun unifiedEditorRetainsNotesRepeatAndLinksWhenSavingAnExistingTask() {
        val task=TaskV11("a","Existing",details="Keep note",date="2026-11-12",repeat=TaskRepeatV11.WEEKLY,calendarId="c",workplaceId="w")
        var saved: TaskV11?=null
        render {TaskEditorDialogV11(task,V11Database(calendars=listOf(AppCalendarV11("c","Calendar","#00A4B8"))),listOf(Workplace("w","Clinic","#00A4B8")),{}) {saved=it}}
        compose.onNodeWithText("Title").performTextReplacement("Changed title")
        compose.onNodeWithText("Save").performClick()
        assertEquals("Changed title",saved?.title);assertEquals(task.details,saved?.details)
        assertEquals(task.repeat,saved?.repeat);assertEquals(task.calendarId,saved?.calendarId);assertEquals(task.workplaceId,saved?.workplaceId);assertEquals(task.id,saved?.id)
    }
    @Test fun calendarVisibilityDoesNotChangeCellHeight() {
        val date=LocalDate.parse("2026-11-12");var ids by mutableStateOf(setOf("c"))
        val db=V11Database(calendars=listOf(AppCalendarV11("c","Calendar","#00A4B8")),days=listOf(DayEntryV11("d",date.toString(),"c",listOf(ShiftSegmentV11("s",ShiftKind.FULL_DAY,"w")))),dayItems=listOf(DayItemV11("n",date.toString(),type=DayItemTypeV11.NOTE,title="Long note",calendarId="c")))
        render {NeoCalendarArea(YearMonth.from(date),db,listOf(Workplace("w","Long workplace name","#00A4B8")),ids,date,false,emptySet(),{},{},{},{})}
        val before=compose.onNodeWithContentDescription(date.toString(),substring=true).fetchSemanticsNode().boundsInRoot
        compose.runOnIdle {ids=emptySet()}
        val after=compose.onNodeWithContentDescription(date.toString(),substring=true).fetchSemanticsNode().boundsInRoot
        assertEquals(before.height,after.height,.1f)
        compose.onNodeWithText("×1").assertDoesNotExist()
    }
}
