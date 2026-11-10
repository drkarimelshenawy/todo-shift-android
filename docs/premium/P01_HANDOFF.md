# P01 checkpoint — HOLD, not a passed phase

Input SHA: e178b134658afad04d645b044f24d236594cbe33.
Source checkpoint SHA: 2604aa8a7d5a6b68c039d03ebff0cb07fe21a3cd.
Branch: work/todo-shift-premium-rescue, created locally from the exact supplied baseline.
Source branch remains at e178b13. No push, merge, release, PR, app installation, storage clear, data migration or version/signing change.

## Package and inventory

All 13 supplied Markdown documents are retained verbatim in blueprint/ with SHA256 in BLUEPRINT_MANIFEST.json. The package contains no phone videos/images referenced by the defect register. Those original visual materials are not available here; they are not claimed reproduced. ACCEPTANCE.json retains 147 protected IDs (130 original +12 UX +5 PM); no blanket implementation or acceptance claim. WRITE_CALLSITES.txt is a production-source search inventory, not proof each call was executed.

## Changes and specific evidence

D01/D23: V11FinanceUi source create/edit and obligation payment no longer rewrite grouping commas to decimal dots. MoneyInput preserves original input and returns blank/invalid/cent-based value; the same result drives error/enablement/save in these paths. Classic obligation create/edit now prevent nonblank invalid total/installment being saved as null. Existing parser, records, schema and historical amounts are unchanged. MoneyInputTest covers ASCII/Arabic/Persian/mixed, grouped, decimal, rounding, malformed, zero and range cases. PremiumP01UiTest edits an income source with 9,00 (disabled/raw retained) then 9,000 and reopens actual V11 storage to check 900000 cents. This does NOT yet cover every financial editor from MainActivity.

D16/D18 (external entry only): QuickTaskActivity keeps its editor composed while pending/failure; shared write state disables submit, keeps the raw draft and enables retry after an error. NeoTaskCommit submits under Neo4DataGate, reads fresh storage, uses CAS, preserves unrelated records, resolves ambiguous write exceptions by rereading the stable ID, distinguishes conflicts and data-committed/services-pending. A retry resynchronizes services without repeating the record. NeoTaskCommitTest injects before/after write, ID conflict and service failure. PremiumP01UiTest verifies pending/error draft retention and same-ID retry in the actual editor component. This is Robolectric component evidence, not full Launcher/Activity/process-death acceptance. Reminder permission remains after confirmed save.

## Open software work — not environment blockers

- D07: Home still has Unit fire-and-forget save callbacks; all child editors need a checked asynchronous result before dismissal. Inventory every save/delete/restore entrance in Premium and Classic; explicitly test busy/CAS/I/O before/after commit and stable operation IDs. Do not label generic failure messages a complete contract.
- D17: note text and NeoSmartStore style writes still have the baseline cross-store atomicity gap. Implement reviewed journal/recovery coordinated with restore and fault-test every write boundary before accepting Q03. Do not merely reorder style/text or delete orphan files as a substitute.
- D18: root UI coroutine cancellation and side-effect recovery still require durable/idempotent reconciliation from confirmed storage. External-task result handling is only one path.
- D24: restore asset staging before the preference journal remains to be fault-tested and safely tracked. Preserve existing linked/unknown files and rollback behavior.
- Q01 all editor create/edit/pay/allocation/rate paths, Q02 all callsites, Q03 cross-store faults, Q04 every restore/provider interruption, Q05 finance privacy, Q15 full Activity faults and Q18 attachment lifecycle still need expanded tests. Current passing regressions are partial evidence only.

## Environmental blockers

APK packaging was attempted with the existing Gradle configuration and failed: SigningConfig persistentDebug is missing required property storeFile. Signing inputs were not configured for this invocation; credentials/configuration were not changed. Supply an authorized signing file/password through protected runtime variables (never chat/source) or an authorized isolated test-signing invocation; compare installed certificate before any upgrade. No newly signed APK is delivered from this checkpoint.

ADB devices failed before enumeration because /home/agent/.android is read-only, including attempts using ANDROID_USER_HOME and ANDROID_SDK_HOME. SDK has no emulator executable, /dev/kvm is absent, and no physical Android/Honor is available. Q04/Q15/Q18 device/provider/Launcher and Q26 upgrade acceptance are BLOCKED. Required: usable emulator for A tests and isolated physical Android plus Honor for D/H, with synthetic fixtures and verified matching certificate/lineage. Do not substitute Robolectric for these environments.

## Resume and rollback

Resume P01 here, not P02 and not the old modernization. Keep the source checkpoint and raw QA outside Git. Address the open software list, rerun focused tests plus complete unit/lint/build, update all affected protected/QA rows individually. Advance only when the required gate evidence passes; environmental cases remain BLOCKED and independent P01 work can proceed. P02–P06 have no exit SHA or passed gates yet.

Code rollback is an isolated checkout/worktree at the recorded input/checkpoint, without reset of app data. No user-data upgrade occurred, so no data rollback is authorized or needed. Never force-push/rebase protected refs or imply this is a release-ready build.
