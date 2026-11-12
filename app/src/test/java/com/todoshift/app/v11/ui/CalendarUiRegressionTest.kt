package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.todoshift.app.v11.*
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@LooperMode(LooperMode.Mode.PAUSED)
class CalendarUiRegressionTest {
    @get:Rule val compose = createComposeRule()
    private val date = LocalDate.parse("2026-11-09")

    private fun render(
        kind: ShiftKind,
        paint: Boolean,
        visible: State<Set<String>> = mutableStateOf(setOf("a")),
        language: NeoLanguage = NeoLanguage.ENGLISH,
        fontScale: Float = 1f,
        showNames: Boolean = false,
        width: Int = 360,
        withTasks: Boolean = false,
    ) {
        val db =
            V11Database(
                calendars = listOf(AppCalendarV11("a", "Work", "#123456")),
                tasks =
                    if (withTasks) listOf(TaskV11("task", "Fixture", date = date.toString()))
                    else emptyList(),
                dayItems =
                    if (withTasks)
                        listOf(
                            DayItemV11(
                                "pin",
                                date.toString(),
                                type = DayItemTypeV11.NOTE,
                                title = "📌 Fixture",
                            )
                        )
                    else emptyList(),
                days =
                    listOf(
                        DayEntryV11(
                            "day",
                            date.toString(),
                            "a",
                            listOf(ShiftSegmentV11("shift", kind)),
                        )
                    ),
            )
        compose.setContent {
            NeoEnvironment(
                NeoAppearance(
                    language = language,
                    uiDirection =
                        if (language == NeoLanguage.ARABIC) NeoDirection.RTL else NeoDirection.LTR,
                    fontScale = fontScale,
                    showShiftNames = showNames,
                    showShiftPaint = paint,
                    showSecondaryIndicators = false,
                    reduceMotion = true,
                ),
                true,
                {},
            ) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
                    CompositionLocalProvider(LocalV11NeoEnabled provides true) {
                        Box(Modifier.width(width.dp).height(600.dp)) {
                            NeoCalendarArea(
                                YearMonth.from(date),
                                db,
                                emptyList(),
                                visible.value,
                                date,
                                false,
                                emptySet(),
                                {},
                                {},
                                {},
                                {},
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun singleShiftRemainsVisibleWithBothDecorativeOptionsDisabled() {
        render(ShiftKind.MORNING, false)
        compose.onNodeWithContentDescription(date.toString() + ", 1 shifts", substring = true).assertExists()
        compose.onNodeWithText("November 2026").assertExists()
        compose
            .onNodeWithContentDescription(
                "2026-11-09, 1 shifts, 0 tasks, 0 events",
                substring = true,
            )
            .assertExists()
    }

    @Test
    fun offDayHasAnExplicitMarker() {
        render(ShiftKind.OFF, true)
        compose.onNodeWithText("Off").assertExists()
    }

    @Test
    fun visibilityChangesInvalidateCellsWithoutChangingRecords() {
        val visible = mutableStateOf(setOf("a"))
        render(ShiftKind.NIGHT, false, visible)
        compose.onNodeWithContentDescription(date.toString() + ", 1 shifts", substring = true).assertExists()
        compose.runOnIdle { visible.value = emptySet() }
        compose.onNodeWithContentDescription(date.toString() + ", 1 shifts", substring = true).assertDoesNotExist()
        compose.runOnIdle { visible.value = setOf("a") }
        compose.onNodeWithContentDescription(date.toString() + ", 1 shifts", substring = true).assertExists()
    }

    @Test
    fun arabicMonthAndLayoutDirectionRemainConsistent() {
        render(ShiftKind.MORNING, false, language = NeoLanguage.ARABIC)
        val month = compose.onNodeWithText(neoMonthTitle(YearMonth.from(date), false))
        month.assertExists()
        org.junit.Assert.assertEquals(
            androidx.compose.ui.unit.LayoutDirection.Rtl,
            month.fetchSemanticsNode().layoutInfo.layoutDirection,
        )
        compose.onNodeWithContentDescription(date.toString() + ", 1 shifts", substring = true).assertExists()
    }

    @Test
    fun largeTextRetainsReadableMonthAndShiftMarker() {
        render(ShiftKind.MORNING, false, fontScale = 1.8f)
        compose.onNodeWithText("November 2026").assertExists()
        compose.onNodeWithContentDescription(date.toString() + ", 1 shifts", substring = true).assertExists()
    }

    @Test
    fun largeTextDateAndShiftLabelDoNotOverlapWithoutCounts() {
        render(ShiftKind.MORNING, false, fontScale = 1.8f, showNames = true)
        val dateBounds =
            compose.onNodeWithText("9", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val nameBounds =
            compose
                .onNodeWithText("Morning", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot
        org.junit.Assert.assertTrue("Date and shift name need separate lines",nameBounds.top >= dateBounds.bottom)
        compose.onNodeWithText("×1").assertDoesNotExist()

    }

    @Test
    fun narrowTwoHundredPercentWeekShowsAllColumnsAndStacksIndependentBadges() {
        render(
            ShiftKind.MORNING,
            false,
            fontScale = 2f,
            showNames = true,
            width = 320,
            withTasks = true,
        )
        val friday =
            compose.onNodeWithText("Fr").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue(
            "Seventh column must fit: $friday",
            friday.left >= 0f && friday.right <= with(compose.density) { 320.dp.toPx() },
        )
        // Large rows scroll vertically; inspect the complete target cell, not clipped viewport
        // bounds.
        compose.onNodeWithContentDescription(date.toString(), substring = true).performScrollTo()
        val task =
            compose.onNodeWithText("☑1", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val name =
            compose
                .onNodeWithText("Morning", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot
        val dateBounds =
            compose.onNodeWithText("9", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val pinBounds =
            compose.onNodeWithText("📌", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val noteBounds =
            compose
                .onNodeWithText("Fixture", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot
        org.junit.Assert.assertTrue(
            "Pin must not obscure date: $dateBounds $pinBounds",
            pinBounds.top >= dateBounds.bottom,
        )
        org.junit.Assert.assertTrue(
            "Note must not obscure pin: $pinBounds $noteBounds",
            noteBounds.top >= pinBounds.bottom,
        )
        org.junit.Assert.assertTrue(
            "Shift must not obscure note: $noteBounds $name",
            name.top >= noteBounds.bottom,
        )
        compose.onNodeWithText("×1").assertDoesNotExist()
        org.junit.Assert.assertTrue(
            "Labels and counts must not overlap: $name $task",
            name.bottom <= task.top,
        )
    }
}
