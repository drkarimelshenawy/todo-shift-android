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
        compose.onNodeWithContentDescription(date.toString() + "، 1 شيفت", substring = true).assertExists()
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
        // Full-month fit is intentional: the calendar MUST NOT expose a scroll action,
        // including at 200% font scale. Complete day data remains in accessibility
        // semantics even when visible captions have to ellipsize within fixed rows.
        val cell = compose.onNodeWithContentDescription(date.toString(), substring = true)
        cell.assertExists()
        val description = cell.fetchSemanticsNode().config[
            androidx.compose.ui.semantics.SemanticsProperties.ContentDescription
        ].joinToString(" ")
        org.junit.Assert.assertTrue(description.contains("Morning"))
        org.junit.Assert.assertTrue(description.contains("Fixture"))
        compose.onNodeWithText("×1").assertDoesNotExist()
        compose.onNodeWithText("9", useUnmergedTree = true).assertExists()
        org.junit.Assert.assertFalse(
            "The month must not become vertically scrollable",
            compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().isNotEmpty(),
        )
    }
}
