package com.todoshift.cashbook.core;


/** Executable integrity test: java ...SalaryIncomeIdempotencyTest */
public final class SalaryIncomeIdempotencyTest {
    private static final class Memory implements Ledger.Store {
        byte[] bytes;
        boolean failNextWrite;
        public byte[] read() { return bytes == null ? null : bytes.clone(); }
        public void write(byte[] value) throws java.io.IOException { if (failNextWrite) { failNextWrite=false; throw new java.io.IOException("Simulated crash before commit"); } bytes = value.clone(); }
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
        expectFailure(() -> restarted.setDeleted(first, true));
        expectFailure(() -> restarted.edit(first, Ledger.Kind.EXPENSE, account, null, 12500, "modified", "2026-11-14", ""));
        expectFailure(() -> restarted.post(Ledger.Kind.INCOME, account, null, 12500, "[salary-receipt:payment-2]", "2026-11-14"));
        byte[] protectedSnapshot = restarted.backup();
        Ledger empty = new Ledger(new Memory());
        empty.load();
        byte[] emptyBackup = empty.backup();
        expectFailure(() -> restarted.restore(emptyBackup));
        if (restarted.snapshot().entries.size() != 1) throw new AssertionError("Protected entry lost");
        restarted.restore(protectedSnapshot);
        if (!first.equals(restarted.postSalaryIncomeOnce("payment-1", account, 12500, "2026-11-14")))
            throw new AssertionError("Backup restore changed receipt");
        if (restarted.snapshot().entries.size() != 1) throw new AssertionError("Duplicate receipt after restore");
        // Simulate failure before the atomic snapshot becomes durable.
        Memory failedStore = new Memory();
        Ledger failing = new Ledger(failedStore);
        failing.load();
        String account2 = failing.addAccount("Cash", "EGP", 0);
        failedStore.failNextWrite = true;
        try {
            failing.postSalaryIncomeOnce("payment-crash", account2, 500, "2026-11-14");
            throw new AssertionError("Expected write failure");
        } catch (java.io.IOException expected) { }
        Ledger recovered = new Ledger(failedStore);
        String recoveredId = recovered.postSalaryIncomeOnce("payment-crash", account2, 500, "2026-11-14");
        if (!recoveredId.equals(recovered.postSalaryIncomeOnce("payment-crash", account2, 500, "2026-11-14")))
            throw new AssertionError("Crash retry duplicated income");
        if (recovered.snapshot().entries.size() != 1) throw new AssertionError("Crash recovery duplicated income");

        // Two independent Ledger instances must serialize salary posting by payment ID.
        Memory concurrentStore = new Memory();
        Ledger setup = new Ledger(concurrentStore);
        setup.load();
        String concurrentAccount = setup.addAccount("Bank", "EGP", 0);
        java.util.concurrent.atomic.AtomicReference<Throwable> failure = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.List<String> posted = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        java.util.List<Thread> workers = new java.util.ArrayList<>();
        for(int i=0;i<8;i++) {
            Thread thread = new Thread(() -> {
                try {
                    Ledger writer = new Ledger(concurrentStore);
                    posted.add(writer.postSalaryIncomeOnce("payment-parallel", concurrentAccount, 700, "2026-11-14"));
                } catch(Throwable t) { failure.compareAndSet(null,t); }
            });
            workers.add(thread);
            thread.start();
        }
        for(Thread thread:workers)thread.join();
        if(failure.get()!=null)throw new AssertionError("Parallel posting failed",failure.get());
        if(posted.size()!=8 || new java.util.HashSet<>(posted).size()!=1)
            throw new AssertionError("Parallel posting produced different entries");
        Ledger finalLedger = new Ledger(concurrentStore);
        finalLedger.load();
        if(finalLedger.snapshot().entries.size()!=1)throw new AssertionError("Parallel posting duplicated income");
        System.out.println("PASS: salary retries, restart, conflicts, protected restore, failed write and parallel writers");
    }
}
