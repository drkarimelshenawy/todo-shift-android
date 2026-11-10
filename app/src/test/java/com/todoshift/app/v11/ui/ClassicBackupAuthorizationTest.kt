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
class ClassicBackupAuthorizationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun currentRootAuthorizationOutranksAStaleUnlockedPropAndBothExportFormatsRemainAvailable() {
        val authorized = mutableStateOf(false)
        val data =
            V11Database(
                payments = listOf(PaymentV11("private-fixture", 9000.0, "2026-11-10", "2026-11"))
            )
        compose.setContent {
            NeoEnvironment(NeoAppearance(language = NeoLanguage.ENGLISH), true, {}) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
                    CompositionLocalProvider(LocalNeoFinanceAccess provides { authorized.value }) {
                        V11BackupRestoreDialog(
                            RuntimeEnvironment.getApplication(),
                            data,
                            { fail("No restore was confirmed") },
                            {},
                            financeAuthorized = true,
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("Financial verification required").assertExists()
        compose.onAllNodesWithText("Export legacy JSON").assertCountEquals(0)
        compose.onAllNodesWithText("Export full ZIP").assertCountEquals(0)
        compose.onAllNodesWithText("9000", substring = true).assertCountEquals(0)
        compose.runOnIdle { authorized.value = true }
        compose.onNodeWithText("Export legacy JSON").assertExists()
        compose.onNodeWithText("Export full ZIP").assertExists()
        compose.runOnIdle { authorized.value = false }
        compose.onAllNodesWithText("Export legacy JSON").assertCountEquals(0)
        compose.onNodeWithText("Financial verification required").assertExists()
    }
}
