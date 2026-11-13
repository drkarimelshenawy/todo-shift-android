package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
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
@Config(sdk=[28], qualifiers="ar-rEG-w320dp-h640dp-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
class PremiumCompleteLayoutTest {
    @get:Rule val compose=createComposeRule()
    private fun calendar(month: YearMonth, expectedRows: Int) {
        assertEquals(expectedRows, NeoMonthGrid.dates(month,7).size/7)
        compose.setContent {
            NeoEnvironment(NeoAppearance(language=NeoLanguage.ARABIC,uiDirection=NeoDirection.RTL,
                palette=NeoPalette.FOREST_SAGE,firstWeekday=7,fontScale=1.5f,reduceMotion=true),true,{}) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT,V11Experience.PREMIUM) {
                    CompositionLocalProvider(LocalV11NeoEnabled provides true) {
                        Box(Modifier.width(320.dp).height(430.dp)) {
                            NeoCalendarArea(month,V11Database(),emptyList(),emptySet(),month.atDay(1),false,emptySet(),{},{},{},{})
                        }
                    }
                }
            }
        }
        for(day in 1..month.lengthOfMonth()) compose.onNodeWithContentDescription(month.atDay(day).toString(),substring=true).assertIsDisplayed()
        compose.onNodeWithContentDescription("السابق").assertIsDisplayed()
        compose.onNodeWithContentDescription("التالي").assertIsDisplayed()
    }
    @Test fun fourRowsRemainFullyVisible()=calendar(YearMonth.of(2026,2),4)
    @Test fun fiveRowsRemainFullyVisible()=calendar(YearMonth.of(2026,11),5)
    @Test fun sixRowsRemainFullyVisible()=calendar(YearMonth.of(2026,8),6)
    @Test fun smallLargeTextTaskListKeepsFiltersAndActionsAccessible() {
        compose.setContent {
            NeoEnvironment(NeoAppearance(language=NeoLanguage.ENGLISH,fontScale=2f,reduceMotion=true),true,{}) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT,V11Experience.PREMIUM) {
                    CompositionLocalProvider(LocalV11NeoEnabled provides true) {
                        Box(Modifier.width(320.dp).height(430.dp)) {NeoPremiumTasksScreen(V11Database(),emptyList(),{},null,null,{})}
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Add task").assertIsDisplayed()
        compose.onNodeWithContentDescription("Filters").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithText("Task filters and view").assertIsDisplayed()
    }
    @Test fun narrowWorkplaceStripDoesNotPutPaintActionOverWorkplaceChips() {
        val context=org.robolectric.RuntimeEnvironment.getApplication()
        V11Runtime(context).start()
        val legacy=com.todoshift.app.data.AppStorage(context)
        legacy.save(legacy.load().copy(workplaces=listOf(com.todoshift.app.data.Workplace("clinic","Clinic long workplace name","#88BB99"))))
        NeoAppearanceStore(context).save(NeoAppearance(language=NeoLanguage.ENGLISH,fontScale=1.3f,reduceMotion=true))
        compose.setContent {Box(Modifier.width(320.dp).height(600.dp)) {V11Home()}}
        compose.waitUntil(10_000) {compose.onAllNodesWithContentDescription("Paint shifts").fetchSemanticsNodes().isNotEmpty()}
        val action=compose.onNodeWithContentDescription("Paint shifts").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val chip=compose.onNodeWithText("Clinic long workplace name").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertFalse("Paint control must not obscure the scrollable workplace selector",action.overlaps(chip))
        compose.onNodeWithContentDescription("Paint shifts").performClick()
        compose.onNodeWithContentDescription("Eraser").assertExists()
    }

}
