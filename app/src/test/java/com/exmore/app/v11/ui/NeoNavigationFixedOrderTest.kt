package com.exmore.app.v11.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.exmore.app.v11.*
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

    @Test fun fiveDestinationsKeepApprovedPhysicalPositionsInBothLanguages() {
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
        fun x(label: String) = compose.onNodeWithText(label)
            .fetchSemanticsNode().boundsInRoot.left
        assertTrue(x("الإدارة المالية") < x("المهام"))
        assertTrue(x("المهام") < x("التقويم"))
        assertTrue(x("التقويم") < x("الملاحظات"))
        assertTrue(x("الملاحظات") < x("الإعدادات"))
        compose.runOnIdle { appearance = appearance.copy(language=NeoLanguage.ENGLISH) }
        assertTrue(x("Finance") < x("Tasks"))
        assertTrue(x("Tasks") < x("Calendar"))
        assertTrue(x("Calendar") < x("Notes"))
        assertTrue(x("Notes") < x("Settings"))
        // A previously saved navigation-direction override must not move Settings
        // away from the absolute right edge after upgrading to v70.
        compose.runOnIdle {
            appearance = appearance.copy(language=NeoLanguage.ARABIC,
                navigationDirection=NeoDirection.RTL)
        }
        assertTrue(x("الإدارة المالية") < x("المهام"))
        assertTrue(x("المهام") < x("التقويم"))
        assertTrue(x("التقويم") < x("الملاحظات"))
        assertTrue(x("الملاحظات") < x("الإعدادات"))
    }
}
