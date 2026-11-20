package com.todoshift.app.v11.ui

import android.os.ParcelFileDescriptor
import androidx.core.view.WindowCompat
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.todoshift.app.v11.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NeoThemeDeviceTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun applyLightThemeAndVisitEnglishTabs() = review(false, NeoBrightness.LIGHT)
    @Test fun applyDarkThemeAndVisitArabicTabs() = review(true, NeoBrightness.DARK)

    @Test fun previewKeepsSystemBarsOnTheAppliedTheme() {
        val light = NeoAppearance(palette = NeoPalette.NILE_JADE, brightness = NeoBrightness.LIGHT)
        val dark = light.copy(brightness = NeoBrightness.DARK)
        val applied = mutableStateOf(light)
        rule.setContent {
            NeoEnvironment(applied.value, true, {}) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
                    Neo4ThemePreview(if (applied.value == light) dark else light, 1f)
                }
            }
        }
        rule.runOnIdle {
            val window = rule.activity.window
            val bars = WindowCompat.getInsetsController(window, window.decorView)
            assertTrue(bars.isAppearanceLightStatusBars)
            assertTrue(bars.isAppearanceLightNavigationBars)
            applied.value = dark
        }
        rule.runOnIdle {
            val window = rule.activity.window
            val bars = WindowCompat.getInsetsController(window, window.decorView)
            assertFalse(bars.isAppearanceLightStatusBars)
            assertFalse(bars.isAppearanceLightNavigationBars)
        }
    }

    private fun review(arabic: Boolean, brightness: NeoBrightness) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = NeoAppearanceStore(context)
        val previous = store.load()
        val initial = previous.copy(
            palette = NeoPalette.CHAMPAGNE_GOLD, accentHex = "", brightness = brightness,
            language = if (arabic) NeoLanguage.ARABIC else NeoLanguage.ENGLISH,
            uiDirection = if (arabic) NeoDirection.RTL else NeoDirection.LTR,
            reduceMotion = true
        )
        val name = if (arabic) "يشم النيل" else "Nile Jade"
        val settings = if (arabic) "الإعدادات" else "Settings"
        val language = if (arabic) "ar" else "en"
        fun tab(title: String) = rule.onNode(hasText(title) and
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
        try {
            store.save(initial)
            rule.setContent { V11Home() }
            rule.waitUntil(30_000) { rule.onAllNodesWithText(settings).fetchSemanticsNodes().isNotEmpty() }
            tab(settings).performClick()
            rule.onNodeWithText(if (arabic) "المظهر والتصميم" else "Appearance & Design").performScrollTo().performClick()
            rule.onNodeWithText(if (arabic) "إخفاء المعاينة" else "Hide preview").performScrollTo().performClick()
            val selector = if (brightness == NeoBrightness.DARK) {
                if (arabic) "داكن" else "Dark"
            } else "Neo Luxury"
            rule.onAllNodesWithText(selector).onFirst().performScrollTo().performClick()
            rule.onNodeWithText(name).performClick()
            assertEquals(initial, store.load())
            rule.onNodeWithText(if (arabic) "تطبيق" else "Apply").performClick()
            rule.waitUntil(20_000) { store.load().palette == NeoPalette.NILE_JADE }
            assertEquals(brightness, store.load().brightness)
            rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            val pages = if (arabic) listOf("التقويم", "المهام والملاحظات", "المستحقات", "الإدارة المالية", settings)
                        else listOf("Calendar", "Tasks & Notes", "Salary", "Cash Book", settings)
            for ((index, page) in pages.withIndex()) {
                tab(page).performClick().assertIsSelected()
                if (index == 1) {
                    val notes = if (arabic) "الملاحظات" else "Notes"
                    rule.onAllNodesWithText(notes).onFirst().performClick()
                }
                rule.waitForIdle()
                capture("$language-$index")
            }
        } catch (failure: Throwable) {
            android.util.Log.e("NileJadeReview", rule.onRoot().printToString(), failure)
            capture("$language-failure")
            throw failure
        } finally {
            store.save(previous)
        }
    }

    private fun capture(name: String) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        fun shell(command: String): String =
            ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).bufferedReader().use { it.readText() }
        val directory = "/sdcard/Download/nile-jade"
        val path = "$directory/$name.png"
        shell("mkdir -p $directory")
        shell("screencap -p $path")
        assertTrue("Missing screenshot $path", (shell("stat -c %s $path").trim().toLongOrNull() ?: 0L) > 0L)
    }
}
