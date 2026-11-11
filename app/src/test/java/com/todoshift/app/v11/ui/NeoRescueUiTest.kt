package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
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
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@LooperMode(LooperMode.Mode.PAUSED)
class NeoRescueUiTest {
    @get:Rule val compose = createComposeRule()

    private fun render(
        onNavigate: (String, String?) -> Unit = { _, _ -> },
        content: @Composable () -> Unit,
    ) {
        compose.setContent {
            NeoEnvironment(
                NeoAppearance(language = NeoLanguage.ENGLISH, reduceMotion = true),
                true,
                {},
            ) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
                    CompositionLocalProvider(
                        LocalV11NeoEnabled provides true,
                        LocalNeoFeatures provides NeoFeatureNavigator(onNavigate),
                    ) {
                        Box(Modifier.width(360.dp).height(700.dp)) { content() }
                    }
                }
            }
        }
    }

    @Test
    fun appearanceDraftSurvivesRestorationUntilExplicitApply() {
        val restoration = StateRestorationTester(compose)
        var applied: NeoAppearance? = null
        restoration.setContent {
            NeoEnvironment(
                NeoAppearance(language = NeoLanguage.ENGLISH, reduceMotion = true),
                true,
                { applied = it },
            ) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
                    CompositionLocalProvider(LocalV11NeoEnabled provides true) {
                        NeoAppearanceStudio({}, {})
                    }
                }
            }
        }
        compose.onNodeWithText("Theme and identity").performClick()
        compose.onNodeWithText("Follow system").performScrollTo().performClick()
        compose.onNodeWithText("Dark").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle { assertNull(applied) }
        compose.onNodeWithText("Apply").performClick()
        compose.runOnIdle {
            assertEquals(NeoBrightness.DARK, applied!!.brightness)
            assertEquals(NeoLanguage.ENGLISH, applied!!.language)
        }
    }

    @Test
    fun paintingAndUndoCommitThroughRootWithoutChangingCalendarIds() {
        val context = RuntimeEnvironment.getApplication()
        val runtime = V11Runtime(context)
        val before = runtime.start()
        NeoAppearanceStore(context)
            .save(NeoAppearance(language = NeoLanguage.ENGLISH, reduceMotion = true))
        V11UiSettingsStore(context).save(V11UiSettings(experience = V11Experience.PREMIUM))
        compose.setContent { V11Home(initialDate = LocalDate.parse("2026-11-10")) }
        compose.waitUntil(10000) {
            compose
                .onAllNodesWithContentDescription("Paint shifts")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithContentDescription("Paint shifts").performClick()
        compose.onNodeWithContentDescription("Eraser").assertExists()
        compose.onNodeWithContentDescription("2026-11-10", substring = true).performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithContentDescription("Eraser").fetchSemanticsNodes()
            V11Storage(context).load().days.any {
                it.date == "2026-11-10" && it.segments.isNotEmpty()
            }
        }
        compose.onNodeWithContentDescription("Calendar actions").performClick()
        compose.waitUntil(10000) {
            compose
                .onAllNodes(hasText("Undo last change") and isEnabled())
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithText("Undo last change").performClick()
        compose.waitUntil(10000) {
            compose.onAllNodesWithContentDescription("Eraser").fetchSemanticsNodes()
            V11Storage(context).load().days.none { it.segments.isNotEmpty() }
        }
        compose.runOnIdle {
            val after = V11Storage(context).load()
            assertEquals(before.calendars, after.calendars)
            assertEquals(before.tasks, after.tasks)
            assertTrue(after.auditLog.any { it.action == "UNDO" })
        }
    }

    @Test
    fun darkThemePublishesReadableInheritedContentColor() {
        var observed: androidx.compose.ui.graphics.Color? = null
        var expected: androidx.compose.ui.graphics.Color? = null
        compose.setContent {
            NeoEnvironment(
                NeoAppearance(language = NeoLanguage.ENGLISH, brightness = NeoBrightness.AMOLED),
                true,
                {},
            ) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
                    androidx.compose.material3.Surface(
                        color =
                            androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(
                                alpha = .92f
                            )
                    ) {
                        observed = androidx.compose.material3.LocalContentColor.current
                        expected = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                        androidx.compose.material3.Text("Synthetic inherited content")
                    }
                }
            }
        }
        compose.runOnIdle { assertEquals(expected, observed) }
    }

    @Test
    fun settingsHomeHasExactlySixGroupsAndNoToolsDump() {
        val context = RuntimeEnvironment.getApplication()
        render {
            NeoRescueSettings(
                V11Database(),
                V11UiSettings(),
                V11SecuritySettings(),
                V11SecurityStore(context),
                false,
                {},
                {},
                {},
                {},
            )
        }
        for (title in
            listOf(
                "Appearance & Design",
                "Language & Direction",
                "Calendar & Shifts",
                "Notifications & Widgets",
                "Privacy & Data",
                "Advanced",
            )) compose.onNodeWithText(title).assertExists()
        compose.onNodeWithText("Smart tools and search").assertDoesNotExist()
    }

    @Test
    fun settingsSearchRoutesToExactDirectionControl() {
        var route = ""
        val context = RuntimeEnvironment.getApplication()
        render({ id, _ -> route = id }) {
            NeoRescueSettings(
                V11Database(),
                V11UiSettings(),
                V11SecuritySettings(),
                V11SecurityStore(context),
                false,
                {},
                {},
                {},
                {},
            )
        }
        compose.onNode(hasSetTextAction()).performTextInput("weekday direction")
        compose.onNodeWithText("Weekday direction").performClick()
        compose.runOnIdle { assertEquals("WEEKDAY_DIRECTION", route) }
    }

    @Test
    fun focusedOperationDoesNotRequireToolSelection() {
        render {
            NeoModernHub(
                V11Database(),
                emptyList(),
                false,
                false,
                {},
                {},
                {},
                initialSection = "COPY",
                focused = true,
            )
        }
        compose.onNodeWithText("Tool").assertDoesNotExist()
        compose.onNodeWithText("Copy shifts").assertExists()
        compose.onNodeWithText("From", substring = true).assertExists()
    }

    @Test
    fun brushActionEntersPainterImmediatelyAndDoneExits() {
        val date = LocalDate.parse("2026-11-10")
        val db = V11Database(calendars = listOf(AppCalendarV11("c", "Work", "#00A4B8")))
        render {
            V11CalendarScreen(
                month = YearMonth.from(date),
                initialDate = date,
                db = db,
                workplaces = listOf(Workplace("w", "Clinic", "#00A4B8")),
                uiSettings = V11UiSettings(experience = V11Experience.PREMIUM),
                activeCalendarId = "c",
                onActiveCalendarChange = {},
                onMonthChange = {},
                onDatabaseChange = {},
                onUiSettingsChange = {},
                quickAction = NeoQuickAction.ADD_SHIFT,
                quickActionId = 1,
            )
        }
        compose.onNodeWithContentDescription("Eraser").assertExists()
        compose.onNodeWithContentDescription("Done").assertExists().performClick()
        compose.onNodeWithContentDescription("Eraser").assertDoesNotExist()
    }

    @Test
    fun schedulesExposeThreeVisibleSectionsAndUsefulSummary() {
        render {
            NeoRescueSchedules(
                V11Database(),
                emptyList(),
                YearMonth.of(2026, 11),
                {},
                {},
                { _, _ -> },
            )
        }
        compose.onNodeWithText("Calendars").assertExists()
        compose.onNodeWithText("Summary").performClick()
        compose.onNodeWithText("Worked days").assertExists()
        compose.onNodeWithText("Compare months").assertExists()
    }

    @Test
    fun appearanceHasVerticalSectionsAndCompactRealPreview() {
        render { NeoAppearanceStudio({}, {}) }
        compose.onNodeWithText("Live preview").assertExists()
        compose.onNodeWithText("Fonts and density").assertExists()
        compose.onNodeWithText("Only what you need").assertDoesNotExist()
        compose.onNodeWithText("Theme and identity").performClick()
        compose.onNodeWithText("Identity").assertExists()
    }

    @Test
    fun quickTaskDraftRestoresWithoutCreatingARecord() {
        val restoration = StateRestorationTester(compose)
        var saved: TaskV11? = null
        restoration.setContent {
            NeoEnvironment(NeoAppearance(language = NeoLanguage.ENGLISH), true, {}) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
                    TaskEditorDialogV11(null, V11Database(), emptyList(), {}, { saved = it })
                }
            }
        }
        compose.onNode(hasSetTextAction()).performTextInput("Synthetic restored draft")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Synthetic restored draft").assertExists()
        compose.runOnIdle { assertNull(saved) }
        compose.onNodeWithText("Save").performClick()
        compose.runOnIdle { assertEquals("Synthetic restored draft", saved!!.title) }
    }
}
