package com.todoshift.app.v11.ui

import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.todoshift.app.MainActivity
import com.todoshift.app.data.AppStorage
import com.todoshift.app.data.Workplace
import com.todoshift.app.v11.*
import java.io.File
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

/** Actual MainActivity under Robolectric native graphics; never presented as device acceptance. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rUS-w360dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class NeoVisualProbeTest {
    @get:Rule(order = 0)
    val fixtures =
        object : ExternalResource() {
            override fun before() {
                val c = RuntimeEnvironment.getApplication()
                val runtime = V11Runtime(c)
                val first = runtime.start()
                val legacy = AppStorage(c)
                legacy.save(
                    legacy
                        .load()
                        .copy(
                            workplaces =
                                listOf(
                                    Workplace("clinic", "Clinic", "#00A4B8"),
                                    Workplace("hospital", "Hospital", "#6C3A9B"),
                                )
                        )
                )
                val cal = first.calendars.first().id
                val today = LocalDate.now()
                val days =
                    listOf(
                        DayEntryV11(
                            "one",
                            today.toString(),
                            cal,
                            listOf(
                                ShiftSegmentV11(
                                    "full",
                                    ShiftKind.FULL_DAY,
                                    "clinic",
                                    startMinute = 540,
                                    endMinute = 1980,
                                )
                            ),
                        ),
                        DayEntryV11(
                            "multi",
                            today.plusDays(1).toString(),
                            cal,
                            listOf(
                                ShiftSegmentV11(
                                    "long",
                                    ShiftKind.LONG,
                                    "clinic",
                                    startMinute = 540,
                                    endMinute = 1260,
                                ),
                                ShiftSegmentV11(
                                    "night",
                                    ShiftKind.NIGHT,
                                    "hospital",
                                    startMinute = 1260,
                                    endMinute = 1980,
                                ),
                            ),
                        ),
                        DayEntryV11(
                            "off",
                            today.plusDays(2).toString(),
                            cal,
                            listOf(ShiftSegmentV11("off-shift", ShiftKind.OFF)),
                        ),
                    )
                runtime.saveIfCurrent(
                    first,
                    first.copy(
                        days = days,
                        tasks =
                            listOf(
                                TaskV11("task", "Prepare tomorrow's shift", date = today.toString())
                            ),
                        dayItems =
                            listOf(
                                DayItemV11(
                                    "note",
                                    today.toString(),
                                    type = DayItemTypeV11.NOTE,
                                    title = "📌 Bring documents",
                                )
                            ),
                    ),
                )
                V11UiSettingsStore(c).save(V11UiSettings(experience = V11Experience.PREMIUM))
                NeoAppearanceStore(c)
                    .save(
                        NeoAppearance(
                            language = NeoLanguage.ENGLISH,
                            brightness = NeoBrightness.LIGHT,
                            reduceMotion = true,
                            showShiftNames = true,
                        )
                    )
            }
        }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    private fun ready() {
        compose.waitUntil(timeoutMillis = 20000) {
            compose
                .onAllNodesWithContentDescription("Paint shifts")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun shot(name: String, settle: Boolean = true) {
        compose.mainClock.advanceTimeBy(48)
        if (settle) {
            compose.waitForIdle()
        }
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
            .idleFor(java.time.Duration.ofMillis(64))
        var captured: Bitmap? = null
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val global =
                org.robolectric.util.ReflectionHelpers.callStaticMethod<Any>(
                    Class.forName("android.view.WindowManagerGlobal"),
                    "getInstance",
                )
            val views =
                org.robolectric.util.ReflectionHelpers.getField<List<android.view.View>>(
                    global,
                    "mViews",
                )
            val dialog =
                org.robolectric.shadows.ShadowDialog.getShownDialogs().lastOrNull { it.isShowing }
            if (!settle && dialog == null) {
                val out = File(requireNotNull(System.getProperty("neo.capture.dir")))
                out.mkdirs()
                File(out, "modal-capture-unavailable.txt")
                    .writeText(
                        "Compose modal window was not available through this native window probe. No modal screenshot is claimed. Modal functionality is covered separately by regular Compose tests; keyboard/device acceptance remains open."
                    )
                File(out, "$name.png").delete()
                return@runOnMainSync
            }
            val view = if (!settle) dialog!!.window!!.decorView else views.last()

            captured =
                Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also {
                    view.draw(android.graphics.Canvas(it))
                }
        }
        val bitmap = captured ?: return
        val colors = mutableSetOf<Int>()
        for (y in 0 until bitmap.height step 8) for (x in 0 until bitmap.width step 8) colors +=
            bitmap.getPixel(x, y)
        assertTrue("Reject blank capture: $name", colors.size > 12)
        val dir = File(requireNotNull(System.getProperty("neo.capture.dir")))
        dir.mkdirs()
        File(dir, "$name.png").outputStream().use {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }

    @Test
    @Config(qualifiers = "en-rUS-w320dp-h640dp-mdpi")
    fun captureNarrowEmptyCalendarWithLargeText() {
        val c = RuntimeEnvironment.getApplication()
        val runtime = V11Runtime(c)
        val db = runtime.start()
        runtime.saveIfCurrent(
            db,
            db.copy(days = emptyList(), tasks = emptyList(), dayItems = emptyList()),
        )
        NeoAppearanceStore(c)
            .save(
                NeoAppearance(
                    language = NeoLanguage.ENGLISH,
                    fontScale = 1f,
                    showShiftNames = true,
                    reduceMotion = true,
                )
            )
        RuntimeEnvironment.setFontScale(2f)
        assertEquals(2f, RuntimeEnvironment.getFontScale(), 0f)
        compose.activityRule.scenario.recreate()
        ready()
        shot("12-calendar-en-narrow-empty-large-text")
        runtime.saveIfCurrent(runtime.start(), db)
        compose.activityRule.scenario.recreate()
        ready()
        compose
            .onNodeWithContentDescription(LocalDate.now().toString(), substring = true)
            .performScrollTo()
        shot("13-calendar-en-narrow-filled-200")
    }

    @Test
    fun captureActualMainActivityWorkflows() {
        ready()
        shot("01-calendar-en-light")
        compose.onNodeWithContentDescription("Paint shifts").performClick()
        shot("02-painter")
        compose.onNodeWithText("Clinic").performClick()
        shot("03-painter-types")
        compose.onNodeWithText("✓ Done").performClick()
        compose.onNodeWithText("Schedules").performClick()
        shot("04-schedules")
        compose.onNodeWithText("Summary").performClick()
        shot("05-schedule-summary")
        compose.onNodeWithText("Tasks").performClick()
        shot("06-tasks")
        compose.onNodeWithText("Finance").performClick()
        shot("08-finance")
        compose.onNodeWithText("Settings").performClick()
        shot("09-settings")
        compose.onNodeWithText("Appearance & Design").performClick()
        shot("10-appearance-studio")
        compose.onNodeWithContentDescription("Back").performClick()
        for (group in
            listOf(
                "Language & Direction",
                "Calendar & Shifts",
                "Notifications & Widgets",
                "Privacy & Data",
                "Advanced",
            )) {
            compose.onNodeWithText(group).performClick()
            shot("settings-" + group.replace(" & ", "-").replace(" ", "-"))
            compose.onNodeWithContentDescription("Back").performClick()
        }
        for ((group, option) in
            listOf(
                "Calendar & Shifts" to "Calendar and note display",
                "Calendar & Shifts" to "Today highlight",
                "Notifications & Widgets" to "Notification center",
                "Notifications & Widgets" to "Widgets and shortcuts",
                "Privacy & Data" to "Backup, restore and account",
                "Privacy & Data" to "Attachment storage",
                "Advanced" to "Performance mode",
            )) {
            compose.onNodeWithText(group).performClick()
            compose.onNodeWithText(option).performClick()
            shot("option-" + option.replace(",", "").replace(" ", "-"))
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
                compose.activity.onBackPressedDispatcher.onBackPressed()
            }
            compose.waitForIdle()
            // Child returns to its originating group; second Back returns to Settings home.
            compose.onNodeWithContentDescription("Back").performClick()
        }
        compose.onNode(hasText("Calendar") and hasClickAction()).performClick()
        for ((label, brightness) in
            listOf("dark" to NeoBrightness.DARK, "amoled" to NeoBrightness.AMOLED)) {
            NeoAppearanceStore(RuntimeEnvironment.getApplication())
                .save(
                    NeoAppearance(
                        language = NeoLanguage.ARABIC,
                        uiDirection = NeoDirection.RTL,
                        brightness = brightness,
                        fontScale = 1.3f,
                        reduceMotion = true,
                    )
                )
            compose.activityRule.scenario.recreate()
            compose.waitUntil(timeoutMillis = 20000) {
                compose
                    .onAllNodesWithContentDescription("رسم الورديات")
                    .fetchSemanticsNodes()
                    .isNotEmpty()
            }
            shot("11-calendar-ar-$label-large-text")
        }
        NeoAppearanceStore(RuntimeEnvironment.getApplication())
            .save(
                NeoAppearance(
                    language = NeoLanguage.ENGLISH,
                    brightness = NeoBrightness.LIGHT,
                    reduceMotion = true,
                )
            )
        compose.activityRule.scenario.recreate()
        ready()
        compose.onNodeWithText("Tasks").performClick()
        compose.onNodeWithContentDescription("Add task").performClick()
        shot("07-task-dialog", settle = false)
        // Separate production-component diagnostics; these do not claim root/IO fault reproduction.
        val app = RuntimeEnvironment.getApplication()
        val fixture = V11Runtime(app).start()
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            compose.activity.setContent {
                NeoEnvironment(
                    NeoAppearance(language = NeoLanguage.ENGLISH, reduceMotion = true),
                    true,
                    {},
                ) {
                    V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
                        V11BackupRestoreDialog(app, fixture, {}, {}, financeAuthorized = true)
                    }
                }
            }
        }
        shot("component-p01-classic-backup", settle = false)
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            compose.activity.setContent {
                NeoEnvironment(
                    NeoAppearance(language = NeoLanguage.ENGLISH, reduceMotion = true),
                    true,
                    {},
                ) {
                    V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
                        androidx.compose.runtime.CompositionLocalProvider(
                            LocalNeoEditorWriteState provides
                                NeoEditorWriteState(
                                    error = "Synthetic write rejection; draft retained"
                                )
                        ) {
                            TaskEditorDialogV11(
                                TaskV11(
                                    "retry-model",
                                    "Retained synthetic draft",
                                    date = LocalDate.now().toString(),
                                ),
                                fixture,
                                emptyList(),
                                {},
                                {},
                            )
                        }
                    }
                }
            }
        }
        shot("component-p01-task-editor-error-model", settle = false)
        compose.activityRule.scenario.close()
        File(requireNotNull(System.getProperty("neo.capture.dir")), "EVIDENCE-LIMITS.txt")
            .writeText(
                "Main workflow captures use actual MainActivity. component-p01-* replace Activity content with production components and a synthetic rejection model; they are not root/IO incident reproductions. Synthetic isolated fixtures, Robolectric native window drawing. Not an emulator or physical-device capture. No product acceptance or Honor/keyboard/doze/upgrade behavior is established. Modal captures are omitted when the window cannot be located. Colors and initial English/Arabic layouts were inspected separately; full manual matrix remains pending."
            )
    }
}
