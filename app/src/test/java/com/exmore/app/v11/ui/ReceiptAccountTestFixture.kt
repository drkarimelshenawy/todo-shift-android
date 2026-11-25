package com.exmore.app.v11.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import com.exmore.cashbook.core.Ledger
import com.exmore.cashbook.ui.AndroidLedgerStore
import org.robolectric.RuntimeEnvironment

/** Real persisted test account: receipt tests must not bypass Cash Book validation. */
internal fun seedReceiptAccount(name: String) {
    val ledger = Ledger(AndroidLedgerStore(RuntimeEnvironment.getApplication()))
    ledger.load()
    ledger.addAccount(name, "EGP", 0L)
}

internal fun ComposeContentTestRule.waitUntilReceiptAccountLoaded() {
    waitUntil(timeoutMillis = 10_000) {
        onAllNodesWithText("Create a Cash Book account to receive payment")
            .fetchSemanticsNodes().isEmpty()
    }
    onNodeWithText("Could not load Cash Book accounts; reopen this form").assertDoesNotExist()
}

internal fun ComposeContentTestRule.selectReceiptAccount(name: String) {
    waitUntilReceiptAccountLoaded()
    onNodeWithText("Select an account").performClick()
    onNodeWithText(name).performClick()
}
