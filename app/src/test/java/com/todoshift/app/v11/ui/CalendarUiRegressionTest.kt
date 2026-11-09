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
    private fun render(kind: ShiftKind, paint: Boolean, visible: State<Set<String>> = mutableStateOf(setOf("a")), language: NeoLanguage = NeoLanguage.ENGLISH, fontScale: Float = 1f) {
        val db = V11Database(calendars = listOf(AppCalendarV11("a", "Work", "#123456")),
            days = listOf(DayEntryV11("day", date.toString(), "a", listOf(ShiftSegmentV11("shift", kind)))))
        compose.setContent {
            NeoEnvironment(NeoAppearance(language = language, uiDirection = if(language == NeoLanguage.ARABIC) NeoDirection.RTL else NeoDirection.LTR, fontScale = fontScale, showShiftPaint = paint, showSecondaryIndicators = false, reduceMotion = true), true, {}) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
                    CompositionLocalProvider(LocalV11NeoEnabled provides true) {
                        Box(Modifier.width(360.dp).height(600.dp)) {
                            NeoCalendarArea(YearMonth.from(date), db, emptyList(), visible.value, date,
                                false, emptySet(), {}, {}, {}, {})
                        }
                    }
                }
            }
        }
    }
    @Test fun singleShiftRemainsVisibleWithBothDecorativeOptionsDisabled() {
        render(ShiftKind.MORNING, false)
        compose.onNodeWithText("• 1").assertExists()
        compose.onNodeWithText("November 2026").assertExists()
        compose.onNodeWithContentDescription("2026-11-09, 1 shifts, 0 tasks, 0 events").assertExists()
    }
    @Test fun offDayHasAnExplicitMarker() {
        render(ShiftKind.OFF, true)
        compose.onNodeWithText("Off").assertExists()
    }
    @Test fun visibilityChangesInvalidateCellsWithoutChangingRecords() {
        val visible = mutableStateOf(setOf("a"))
        render(ShiftKind.NIGHT, false, visible)
        compose.onNodeWithText("• 1").assertExists()
        compose.runOnIdle { visible.value = emptySet() }
        compose.onNodeWithText("• 1").assertDoesNotExist()
        compose.runOnIdle { visible.value = setOf("a") }
        compose.onNodeWithText("• 1").assertExists()
    }
    @Test fun arabicMonthAndLayoutDirectionRemainConsistent() {
        render(ShiftKind.MORNING, false, language = NeoLanguage.ARABIC)
        val month = compose.onNodeWithText(neoMonthTitle(YearMonth.from(date), false))
        month.assertExists()
        org.junit.Assert.assertEquals(androidx.compose.ui.unit.LayoutDirection.Rtl, month.fetchSemanticsNode().layoutInfo.layoutDirection)
        compose.onNodeWithText("• 1").assertExists()
    }
    @Test fun largeTextRetainsReadableMonthAndShiftMarker() {
        render(ShiftKind.MORNING, false, fontScale = 1.8f)
        compose.onNodeWithText("November 2026").assertExists()
        compose.onNodeWithText("• 1").assertExists()
    }
}
