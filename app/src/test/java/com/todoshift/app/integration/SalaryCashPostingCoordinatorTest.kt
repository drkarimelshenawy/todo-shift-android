package com.todoshift.app.integration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SalaryCashPostingCoordinatorTest {
    private class FakeIncome : CashIncomePort {
        val entries = mutableMapOf<String, CashIncomeRecord>()
        var created = 0
        override fun findByExternalKey(externalKey: String) = entries[externalKey]
        override fun createIncome(externalKey: String, accountId: String, amountMinor: Long, currencyCode: String): CashIncomeRecord {
            entries[externalKey]?.let { return it }
            created++
            return CashIncomeRecord("cash-$created", externalKey, accountId, amountMinor, currencyCode)
                .also { entries[externalKey] = it }
        }
    }
    private class FakeLinks : SalaryCashLinkStore {
        val links = mutableMapOf<String, SalaryCashLink>()
        var failNextSave = false
        override fun find(paymentId: String) = links[paymentId]
        override fun saveIfAbsent(link: SalaryCashLink): SalaryCashLink {
            if (failNextSave) {
                failNextSave = false
                error("simulated crash after cash income was saved")
            }
            return links.getOrPut(link.paymentId) { link }
        }
    }

    @Test fun retryDoesNotCreateDuplicateIncome() {
        val incomes = FakeIncome()
        val links = FakeLinks()
        val gateway = SalaryCashPostingCoordinator(incomes, links)
        val first = gateway.postReceipt("p1", "a1", 12500, "EGP")
        val second = gateway.postReceipt("p1", "a1", 12500, "EGP")
        assertEquals(first, second)
        assertEquals(1, incomes.created)
    }

    @Test fun retryAfterLinkSaveFailureRecoversExistingIncome() {
        val incomes = FakeIncome()
        val links = FakeLinks().apply { failNextSave = true }
        val gateway = SalaryCashPostingCoordinator(incomes, links)
        assertThrows(IllegalStateException::class.java) {
            gateway.postReceipt("p1", "a1", 12500, "EGP")
        }
        val recovered = gateway.postReceipt("p1", "a1", 12500, "EGP")
        assertEquals("cash-1", recovered.cashTransactionId)
        assertEquals(1, incomes.created)
    }

    @Test fun conflictingRetryIsRejected() {
        val gateway = SalaryCashPostingCoordinator(FakeIncome(), FakeLinks())
        gateway.postReceipt("p1", "a1", 12500, "EGP")
        assertThrows(IllegalArgumentException::class.java) {
            gateway.postReceipt("p1", "a2", 12500, "EGP")
        }
    }

    @Test fun conflictingOrphanIncomeIsRejected() {
        val incomes = FakeIncome()
        incomes.entries["salary-receipt:p1"] =
            CashIncomeRecord("cash-old", "salary-receipt:p1", "a1", 9000, "EGP")
        val gateway = SalaryCashPostingCoordinator(incomes, FakeLinks())
        assertThrows(IllegalArgumentException::class.java) {
            gateway.postReceipt("p1", "a1", 12500, "EGP")
        }
        assertEquals(0, incomes.created)
    }
}
