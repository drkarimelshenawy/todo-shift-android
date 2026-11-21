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
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
@LooperMode(LooperMode.Mode.PAUSED)
class NeoSalaryDashboardUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun fourTabsShareTheSameMonthWithoutLeakingObligationPanelIntoSalary() {
        val month=YearMonth.of(2026,11)
        val db=V11Database(
            incomeSources=listOf(IncomeSourceV11(
                "s","Fixed",IncomeSourceTypeV11.FIXED_SALARY,
                workplaceId="w",fixedMonthlyAmount=14000.0
            ))
        )
        compose.setContent {
            NeoEnvironment(NeoAppearance(language=NeoLanguage.ENGLISH,reduceMotion=true),
                true,{}) {
                V11AppThemeProvider(V11AppTheme.TODO_SHIFT,V11Experience.PREMIUM) {
                    NeoSalaryDashboard(
                        month=month, db=db,
                        workplaces=listOf(com.todoshift.app.data.Workplace("w","Work","#123456")),
                        moneyVisible=true,onPrevious={},onNext={},onReceive={},
                        onRecordPayment={},onOpenWorkplace={},onPaymentDetails={},
                        onIncomeSources={},onPaymentMethods={},onRates={},
                        onStatement={},onReview={}
                    )
                }
            }
        }
        compose.onNodeWithText("Work entitlements").assertExists()
        compose.onNodeWithText("Summary").assertExists()
        compose.onNodeWithText("Payments").performClick()
        compose.onNodeWithText("Receipt history").assertExists()
        compose.onNodeWithText("Statement").performClick()
        compose.onNodeWithText("Workplace statement").assertExists()
        compose.onNodeWithText("Review").performClick()
        compose.onNodeWithText("Month review & close").assertExists()
        compose.onNodeWithText("Obligations").assertDoesNotExist()
    }
}
