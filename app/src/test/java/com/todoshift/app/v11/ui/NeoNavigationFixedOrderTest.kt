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
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28],qualifiers="ar-rEG-w420dp-h840dp-mdpi")
@LooperMode(LooperMode.Mode.PAUSED)
class NeoNavigationFixedOrderTest {
    @get:Rule val compose = createComposeRule()

    @Test fun languageDoesNotMirrorNavigationOrderUnlessManuallyOverridden() {
        var appearance by mutableStateOf(NeoAppearance(
            language = NeoLanguage.ARABIC, reduceMotion = true,
            navigationDirection = NeoDirection.AUTO
        ))
        compose.setContent {
            NeoEnvironment(appearance, true, { appearance = it }) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT,V11Experience.PREMIUM) {
                    V11PremiumBottomBar(selectedTab=0, onSelectTab={})
                }
            }
        }
        fun left(label: String) = compose.onNodeWithText(label)
            .fetchSemanticsNode().boundsInRoot.left
        assertTrue("Arabic must retain Calendar on the left",
            left("التقويم") < left("الإعدادات"))
        compose.runOnIdle { appearance = appearance.copy(language=NeoLanguage.ENGLISH) }
        assertTrue("English must retain the same positions",
            left("Calendar") < left("Settings"))
        compose.runOnIdle {
            appearance = appearance.copy(language=NeoLanguage.ARABIC,
                navigationDirection=NeoDirection.RTL)
        }
        assertTrue("Only an explicit RTL navigation preference may reverse them",
            left("التقويم") > left("الإعدادات"))
    }
}
