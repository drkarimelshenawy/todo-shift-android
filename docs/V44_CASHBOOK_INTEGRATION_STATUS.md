# v44 Cash Book / Salary integration checkpoint

Branch: `feat/v44-cashbook-integration` (do not merge into main until verified).

## Implemented in this branch
- Cash Book v0.13 core and Compose feature imported as Gradle modules.
- Cash Book tab wired to host navigation, protected by the existing finance unlock gate.
- `SalaryIncomeLedgerBridge.accounts()` returns non-archived EGP Cash Book accounts.
- `SalaryIncomeLedgerBridge.post(paymentId, accountId, amountMinor, date)` delegates to an idempotent, ledger-atomic receipt entry.
- The persisted marker `[salary-receipt:<paymentId>]` is embedded in the income entry, so the marker and financial entry share one atomic ledger snapshot.
- Normal ledger posting cannot create that reserved marker. Editing, deleting or destructive backup restoration of salary-linked entries is rejected.
- Core regression suite includes retries, restart, amount/date conflict, failed snapshot write and parallel Ledger instances.

## Critical integration contract — not yet implemented
1. The **Salary subsystem must first durably record the receipt** with a stable payment ID and selected Cash Book account.
2. Mark the receipt as pending and call `SalaryIncomeLedgerBridge.post` on an IO dispatcher.
3. After the ledger write succeeds, mark the Salary receipt as linked with the returned transaction ID.
4. On startup, retry pending receipts with the **same payment ID, account, amount and date**.
5. Do not allow changing or deleting a linked receipt without a defined reversal/adjustment workflow.
6. Ensure full-app backup/restore reconciles Salary and Cash Book together. Cash Book-only restore currently rejects removal of recorded salary entries, but full-app restore coordination is still missing.

## Verification required
- Confirm GitHub Actions workflow `v44-cashbook-checks.yml` actually passes. A workflow file alone is not a passing build.
- Run the Android app on a device: finance lock, Cash Book CRUD, backup/restore, process death, navigation, and RTL.
- Verify existing host calendar/shift features and migrations remain intact.
- Produce and verify an APK with a valid signing key compatible with intended update installations.

**Do not enable automatic Salary-to-Cash posting or distribute a production APK until the pending-receipt recovery contract and end-to-end tests are complete.**
