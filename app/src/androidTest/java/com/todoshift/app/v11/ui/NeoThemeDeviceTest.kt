package com.todoshift.app.v11.ui

import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
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
        } finally {
            store.save(previous)
        }
    }

    private fun capture(name: String) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val command = "sh -c 'mkdir -p /sdcard/Download/nile-jade && screencap -p /sdcard/Download/nile-jade/$name.png'"
        ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command)).use { it.readBytes() }
    }
}
