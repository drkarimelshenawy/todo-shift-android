package com.exmore.app.v11.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.exmore.app.v11.*
import com.exmore.app.data.Workplace
import com.exmore.cashbook.ui.CashBookSectionPreference
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@LooperMode(LooperMode.Mode.PAUSED)
class V67BehaviorTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cashbookLandingChoicePersistsIndependentlyOfSelectedTab() {
        val c = RuntimeEnvironment.getApplication()
        val prefs = c.getSharedPreferences("cashbook_section_preferences", 0)
        prefs.edit().clear().commit()
        assertEquals(CashBookSectionPreference.ACCOUNTS, CashBookSectionPreference.load(c))
        CashBookSectionPreference.validSections.forEach { section ->
            assertTrue(CashBookSectionPreference.save(c, section))
            assertEquals(section, CashBookSectionPreference.load(c))
        }
        assertEquals(CashBookSectionPreference.ACCOUNTS, CashBookSectionPreference.normalize("unknown"))
        assertEquals(CashBookSectionPreference.ACCOUNTS, CashBookSectionPreference.normalize(null))
        prefs.edit().clear().commit()
    }

    @Test fun calendarHeaderReplacesWorkSummaryWithSavedMonthWeekDayChoice() {
        val appearance = mutableStateOf(NeoAppearance(language=NeoLanguage.ENGLISH, reduceMotion=true))
        compose.setContent {
            NeoEnvironment(appearance.value, true, { appearance.value = it }) {
                NeoContextActions("Calendar")
            }
        }
        compose.onNodeWithContentDescription("Calendar actions").performClick()
        compose.onNodeWithText("Work summary").assertDoesNotExist()
        compose.onNodeWithText("Default calendar view").assertExists()
        compose.onNodeWithText("Week").performClick()
        compose.runOnIdle { assertEquals(NeoCalendarView.WEEK, appearance.value.calendarView) }
    }

    @Test fun changingWorkplaceReopensShiftTypesAndToolsRemainNearPainterBar() {
        val places = listOf(
            Workplace("clinic_a", "Clinic A", "#00A4B8"),
            Workplace("clinic_b", "Clinic B", "#8052A5"),
        )
        compose.setContent {
            NeoEnvironment(NeoAppearance(language=NeoLanguage.ENGLISH, reduceMotion=true), true, {}) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
                    NeoPainterBar(
                        db=V11Database(calendars=listOf(AppCalendarV11("calendar","Calendar","#00A4B8"))),
                        workplaces=places, calendar="calendar", dates=emptySet(),
                        clicked=LocalDate.of(2026,11,21), request=0, bulk=false,
                        onBulk={}, onClear={}, onCommit={}, onExit={},
                    )
                }
            }
        }
        compose.onNodeWithText("Long").performClick()
        compose.onNodeWithText("Clinic B").performClick()
        compose.onNodeWithText("Long").assertExists()
        compose.onNodeWithContentDescription("Painting tools").performClick()
        compose.onNodeWithTag("painter_tools_menu").assertExists()
        compose.onNodeWithText("Multiple dates").assertExists()
    }
}
