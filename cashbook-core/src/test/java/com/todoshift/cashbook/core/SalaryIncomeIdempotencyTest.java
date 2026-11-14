package com.todoshift.cashbook.core;

import java.io.IOException;
import java.util.Arrays;

/** Executable integrity test: java ...SalaryIncomeIdempotencyTest */
public final class SalaryIncomeIdempotencyTest {
    private static final class Memory implements Ledger.Store {
        byte[] bytes;
        public byte[] read() { return bytes == null ? null : bytes.clone(); }
        public void write(byte[] value) { bytes = value.clone(); }
    }
    private static void expectFailure(Checked action) throws Exception {
        try { action.run(); } catch (IllegalStateException | IllegalArgumentException expected) { return; }
        throw new AssertionError("Expected rejected conflicting receipt");
    }
    interface Checked { void run() throws Exception; }
    public static void main(String[] args) throws Exception {
        Memory store = new Memory();
        Ledger ledger = new Ledger(store);
        ledger.load();
        String account = ledger.addAccount("Salary", "EGP", 0);
        String first = ledger.postSalaryIncomeOnce("payment-1", account, 12500, "2026-11-14");
        String again = ledger.postSalaryIncomeOnce("payment-1", account, 12500, "2026-11-14");
        if (!first.equals(again)) throw new AssertionError("Duplicate receipt");
        Ledger restarted = new Ledger(store);
        String afterRestart = restarted.postSalaryIncomeOnce("payment-1", account, 12500, "2026-11-14");
        if (!first.equals(afterRestart)) throw new AssertionError("Restart duplicated receipt");
        if (restarted.snapshot().entries.size() != 1) throw new AssertionError("Unexpected extra entry");
        expectFailure(() -> restarted.postSalaryIncomeOnce("payment-1", account, 12000, "2026-11-14"));
        expectFailure(() -> restarted.postSalaryIncomeOnce("payment-1", account, 12500, "2026-11-15"));
        expectFailure(() -> restarted.postSalaryIncomeOnce("bad]id", account, 12500, "2026-11-14"));
        restarted.setDeleted(first, true);
        expectFailure(() -> restarted.postSalaryIncomeOnce("payment-1", account, 12500, "2026-11-14"));
        if (restarted.snapshot().entries.size() != 1) throw new AssertionError("Deleted entry duplicated");
        System.out.println("PASS: salary income retry, restart, conflict and deleted-entry protection");
    }
}
