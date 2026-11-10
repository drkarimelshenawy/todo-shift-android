package com.todoshift.app.v11.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.todoshift.app.v11.*
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
class ClassicReceiptSafetyTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun classicCreateAndEditRejectZeroAndInvalidDatesWithoutDiscardingLocalizedAmounts() {
        val runtime = V11Runtime(RuntimeEnvironment.getApplication())
        val current = mutableStateOf(runtime.start())
        val edit = mutableStateOf(false)
        compose.setContent {
            NeoEnvironment(NeoAppearance(language = NeoLanguage.ENGLISH), true, {}) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT, V11Experience.PREMIUM) {
                    CompositionLocalProvider(LocalV11NeoEnabled provides false) {
                        if (!edit.value)
                            V11PaymentDialog(YearMonth.of(2026, 11), current.value, {}) { next ->
                                runtime.saveIfCurrent(current.value, next)
                                current.value = next
                                edit.value = true
                            }
                        else
                            V11EditPaymentDialog(
                                current.value.payments.last(),
                                current.value,
                                {},
                            ) { payment ->
                                val next =
                                    current.value.copy(
                                        payments =
                                            current.value.payments.map {
                                                if (it.id == payment.id) payment else it
                                            }
                                    )
                                runtime.saveIfCurrent(current.value, next)
                                current.value = next
                            }
                    }
                }
            }
        }
        compose.onNodeWithText("Amount").performTextReplacement("0")
        compose.onNodeWithText("Save").assertIsNotEnabled()
        compose.onNodeWithText("0").assertExists()
        compose.onNodeWithText("Amount").performTextReplacement("9,000")
        compose.onNodeWithText("Save").assertIsEnabled().performClick()
        assertEquals(900000L, LocalizedNumbers.cents(runtime.start().payments.last().amount))
        compose.onNodeWithText("Amount").performTextReplacement("0")
        compose.onNodeWithText("Save change").assertIsNotEnabled()
        compose.onNodeWithText("Amount").performTextReplacement("٩٠٠٠٫٥٠")
        compose.onNodeWithText("Received date").performTextReplacement("invalid")
        compose.onNodeWithText("Save change").assertIsNotEnabled()
        assertEquals(900000L, LocalizedNumbers.cents(runtime.start().payments.last().amount))
        compose.onNodeWithText("Received date").performTextReplacement("2026-11-10")
        compose.onNodeWithText("Save change").assertIsEnabled().performClick()
        assertEquals(900050L, LocalizedNumbers.cents(runtime.start().payments.last().amount))
    }
}
