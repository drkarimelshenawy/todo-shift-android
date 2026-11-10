package com.todoshift.app.v11.ui

import androidx.compose.runtime.*
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
class PremiumSettingsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun directionControlsShareOnePageAndPersistIndependently() {
        val store = NeoAppearanceStore(RuntimeEnvironment.getApplication())
        val state =
            mutableStateOf(
                NeoAppearance(
                    language = NeoLanguage.ENGLISH,
                    uiDirection = NeoDirection.RTL,
                    navigationDirection = NeoDirection.LTR,
                    gridDirection = NeoDirection.LTR,
                    weekdayDirection = NeoDirection.LTR,
                )
            )
        compose.setContent {
            NeoEnvironment(
                state.value,
                true,
                {
                    store.save(it)
                    state.value = it
                },
            ) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
                    NeoLanguageSettings("UI_DIRECTION", {})
                }
            }
        }
        listOf(
                "App language",
                "UI direction",
                "Navigation direction",
                "Calendar grid direction",
                "Weekday direction",
                "First weekday",
            )
            .forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithText("Right to left").performScrollTo().performClick()
        compose.onNodeWithText("Auto").performClick()
        val p = store.load()
        assertEquals(NeoDirection.AUTO, p.uiDirection)
        assertEquals(NeoDirection.LTR, p.gridDirection)
        assertEquals(NeoDirection.LTR, p.weekdayDirection)
        assertEquals(NeoDirection.LTR, p.navigationDirection)
        assertEquals(NeoLanguage.ENGLISH, p.language)
    }

    @Test
    fun settingsSearchDoesNotMixCalendarOperationsIntoConfiguration() {
        val c = RuntimeEnvironment.getApplication()
        compose.setContent {
            NeoEnvironment(NeoAppearance(language = NeoLanguage.ENGLISH), true, {}) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
                    NeoRescueSettings(
                        V11Database(),
                        V11UiSettings(),
                        V11SecuritySettings(),
                        V11SecurityStore(c),
                        false,
                        {},
                        {},
                        {},
                        {},
                    )
                }
            }
        }
        compose.onNodeWithText("Search settings").performTextInput("copy")
        compose.onNodeWithText("Copy shifts").assertDoesNotExist()
    }

    @Test
    fun livePreviewUsesProductionCellsHeaderAndNavigationWithoutWritingData() {
        val c = RuntimeEnvironment.getApplication()
        val runtime = V11Runtime(c)
        val before = runtime.start()
        compose.setContent {
            NeoEnvironment(
                NeoAppearance(language = NeoLanguage.ENGLISH, reduceMotion = true),
                true,
                { fail("Read-only preview") },
            ) {
                Neo4ThemePreview(
                    NeoAppearance(language = NeoLanguage.ENGLISH, reduceMotion = true),
                    1f,
                )
            }
        }
        compose.onNodeWithText("Live preview").assertExists()
        compose
            .onNodeWithContentDescription(java.time.LocalDate.now().toString(), substring = true)
            .assertExists()
        compose.onNodeWithText("Calendar").performClick()
        compose.onNodeWithText("Task").performClick()
        compose.onNodeWithText("Review schedule").assertExists()
        compose.onNodeWithText("Task").performClick()
        compose.onNodeWithText("Header").performClick()
        compose.onNodeWithContentDescription("Manage calendars").performClick()
        compose.onNodeWithText("Header").performClick()
        compose.onNodeWithText("Navigation").performClick()
        compose.onNodeWithText("Tasks").performClick()
        compose.onNode(hasText("Calendar") and isSelected()).assertExists()
        assertEquals(before, runtime.start())
    }

    @Test
    fun rejectedAppearanceCommitCannotCompleteDeferredNavigationOrLoseDraft() {
        val c = RuntimeEnvironment.getApplication()
        val store = NeoAppearanceStore(c)
        val p = mutableStateOf(NeoAppearance(language = NeoLanguage.ENGLISH, reduceMotion = true))
        store.save(p.value)
        val gate = NeoLeaveGate()
        var done: (() -> Unit)? = null
        var requested: NeoAppearance? = null
        var navigated = 0
        compose.setContent {
            NeoEnvironment(p.value, true, { p.value = it }) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
                    CompositionLocalProvider(
                        LocalNeoLeaveGate provides gate,
                        LocalNeoAppearanceCommit provides
                            { next, ack ->
                                requested = next
                                done = ack
                            },
                    ) {
                        NeoAppearanceStudio({}, {})
                    }
                }
            }
        }
        compose.onNodeWithText("Colors and background").performClick()
        compose
            .onNodeWithText("Custom accent #RRGGBB")
            .performScrollTo()
            .performTextInput("#123456")
        compose.runOnIdle { gate.request { navigated++ } }
        compose.onNodeWithText("Apply and leave").performClick()
        compose.runOnIdle {
            assertEquals(0, navigated)
            assertEquals("", store.load().accentHex)
        }
        compose.onNodeWithText("#123456").assertExists()
        compose.runOnIdle {
            store.save(requested!!)
            p.value = requested!!
            done!!()
        }
        assertEquals(1, navigated)
        assertEquals("#123456", store.load().accentHex)
    }

    @Test
    fun productionCalendarPreviewKeepsLargeTypePinDateAndBadgesSeparate() {
        compose.setContent {
            NeoEnvironment(NeoAppearance(language = NeoLanguage.ENGLISH), true, {}) {
                Neo4ThemePreview(
                    NeoAppearance(
                        language = NeoLanguage.ENGLISH,
                        fontScale = 2f,
                        showShiftNames = true,
                        reduceMotion = true,
                    ),
                    1f,
                )
            }
        }
        val date =
            compose
                .onNodeWithText(
                    java.time.LocalDate.now().dayOfMonth.toString(),
                    useUnmergedTree = true,
                )
                .fetchSemanticsNode()
                .boundsInRoot
        val pin =
            compose.onNodeWithText("📌", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val note =
            compose
                .onNodeWithText("Review", useUnmergedTree = true)
                .fetchSemanticsNode()
                .boundsInRoot
        val shift =
            compose.onNodeWithText("24h", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val task =
            compose.onNodeWithText("☑1", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("Date and pin need distinct lines: $date $pin", pin.top >= date.bottom)
        assertTrue("Pin and note need distinct lines: $pin $note", note.top >= pin.bottom)
        assertTrue("Note and shift need distinct lines: $note $shift", shift.top >= note.bottom)
        assertTrue("Content and count must not overlap: $shift $task", task.top >= shift.bottom)
    }
}
