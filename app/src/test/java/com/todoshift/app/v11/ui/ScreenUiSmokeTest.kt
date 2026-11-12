package com.todoshift.app.v11.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
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
class ScreenUiSmokeTest {
    @get:Rule val compose = createComposeRule()
    private fun render(p: NeoAppearance = NeoAppearance(language = NeoLanguage.ENGLISH, reduceMotion = true), content: @Composable () -> Unit) {
        compose.setContent {
            NeoEnvironment(p, true, {}) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
                    CompositionLocalProvider(LocalV11NeoEnabled provides true) {
                        Box(Modifier.width(320.dp).height(600.dp)) { content() }
                    }
                }
            }
        }
    }
    @Test fun tasksAndTodayAreIntegratedAndScrollable() {
        render { V11TasksScreen(V11Database(), emptyList(), {}) }
        compose.onNodeWithContentDescription("Expand or collapse today's dashboard").assertDoesNotExist()
        compose.onNodeWithText("Search tasks").assertExists()
        compose.onNodeWithContentDescription("Add task").assertExists()
        assertTrue(compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().isNotEmpty())
    }
    @Test fun protectedTodayDoesNotExposeFinance() {
        render(NeoAppearance(language = NeoLanguage.ENGLISH, reduceMotion = true, dashboardExpanded = true, showFinanceSummary = true)) {
            V11TasksScreen(V11Database(), emptyList(), {}, financeAuthorized = false)
        }
        compose.onNodeWithText("Unlock Finance before viewing protected amounts").assertDoesNotExist()
        compose.onNodeWithContentDescription("Add task").assertExists()
    }
    @Test fun studioPreviewAndDiscardDoNotPersist() {
        val context = RuntimeEnvironment.getApplication()
        val store = NeoAppearanceStore(context)
        val before = store.load()
        render { NeoAppearanceStudio({}, {}) }
        compose.onNodeWithText("Preview default appearance").performScrollTo().performClick()
        compose.onNodeWithText("Discard").performClick()
        assertEquals(before, store.load())
    }
    @Test fun amoledAndLightSchemesMeetBaseTextContrastAcrossPalettes() {
        fun contrast(a: androidx.compose.ui.graphics.Color, b: androidx.compose.ui.graphics.Color): Float {
            val x = a.luminance(); val y = b.luminance()
            return (maxOf(x, y) + .05f) / (minOf(x, y) + .05f)
        }
        for (brightness in listOf(NeoBrightness.LIGHT, NeoBrightness.DARK, NeoBrightness.AMOLED)) {
            for (palette in NeoPalette.entries) {
                val c = neoColorScheme(NeoAppearance(brightness = brightness, palette = palette), false)
                assertTrue("$brightness $palette primary", contrast(c.primary, c.surface) >= 4.5f)
                assertTrue("$brightness $palette text", contrast(c.onSurface, c.surface) >= 4.5f)
                assertTrue("$brightness $palette muted", contrast(c.onSurfaceVariant, c.surface) >= 4.5f)
                if (brightness == NeoBrightness.AMOLED) assertEquals(0f, c.background.luminance(), 0f)
            }
        }
    }
    @Test fun invalidSecurityModeCannotBecomeUnlockedDefaults() {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("todo_shift_security_v11", 0).edit().putString("mode", "UNSUPPORTED").commit()
        var rejected = false
        try { V11SecurityStore(context).load() } catch (_: Exception) { rejected = true }
        assertTrue(rejected)
    }
    @Test fun pinAndProtectionSurviveSettingsCommit() {
        val store = V11SecurityStore(RuntimeEnvironment.getApplication())
        assertTrue(store.setPin("4826"))
        store.save(V11SecuritySettings(mode = V11LockMode.PIN, protectAccounts = true))
        assertTrue(store.verifyPin("4826")); assertFalse(store.verifyPin("0000"))
        assertTrue(store.load().protectAccounts)
    }
    @Test fun failedBootBlocksEditingAndPreservesTheRawPayload() {
        val context = RuntimeEnvironment.getApplication()
        val prefs = context.getSharedPreferences("todo_shift_v11", 0)
        prefs.edit().putString("database", "{synthetic-corrupt-data").commit()
        compose.setContent { V11Home() }
        compose.waitUntil(timeoutMillis = 10000) {
            compose.onAllNodesWithText("Unable to open data safely", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("No automatic data reset", substring = true).assertExists()
        assertEquals("{synthetic-corrupt-data", prefs.getString("database", null))
    }
}
