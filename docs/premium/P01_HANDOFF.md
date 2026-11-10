# P01 resumed implementation checkpoint — HOLD

This checkpoint contains substantive P01 implementation and regression work. It does **not** accept P01, complete the Premium rescue or enter the dependent P02–P06 stages.

Branch: `work/todo-shift-premium-rescue`.
Original baseline/source ref: `e178b134658afad04d645b044f24d236594cbe33`.
Resume input: `faf1c93dc7dc9eef9c9aab3ffe374e6bcc03c784` (preserved).
Local P01 source checkpoints:

1. `7a747bdbc78b33975a63bfa6646c9e64777d7107` — journaled notes/restore, checked editor saves and service intent.
2. `3f3b1c37f21e91fed3184ca9043c1d4f0333dc2a` — positive durable acknowledgement, stable request IDs and creation-reference validation.
3. `4c78f0bddfa134d593ce891e991456ccce0fd4e5` — transactional effects rollback, persisted replay job/current-snapshot locking, widget completion and live-authorized Classic backup.

App tree tested/built: `9f2edbcb390ccaf2db74de831e128574229db754`. A later documentation/probe-only commit may follow; final local SHA is in the external delivery manifest and Git history. **Accepted P01 exit SHA: none.**

## Package and safety

All14 Blueprint Markdown files are preserved verbatim, including P01–P06, with hashes in BLUEPRINT_MANIFEST.json. ACCEPTANCE.json retains all147 protected IDs (130 original +12UX +5PM); DEFECT_STATUS.json keeps D01–D29. No row is fully accepted by the existence of a test or file. The referenced original phone visual materials are unavailable and no original incident is claimed reproduced.

Only the isolated work branch was committed. The local source ref remains e178b13; main/Luxury-v4/Premium-V-byWork are not present locally and were neither created nor updated. No remote push/fetch mutation, merge, force-push, tag, PR or release. No install/uninstall/storage clear or real user-data migration. Application/signing Gradle configuration, applicationId, launcher identity and version4.2.0/31 remain baseline; the sole Android entry addition is an internal signature-protected retry JobService. Private isolated test signing files are outside Git/delivery.

## Implemented P01 changes

- **D01/D23 money**: unified raw-input classification for blank/invalid/value; removed grouping-comma rewrite from source/obligation routes, prohibited silently nulling invalid nonblank obligation amounts, rejected invalid dates/zero Classic receipts with visible inline errors. Cent-based validation/reopen tests cover grouped ASCII/Arabic/Persian and Arabic decimal cases without rewriting historical records or changing existing clamping/formulas.
- **D07/D16 editor contract**: Home has explicit async committed/rejected acknowledgement, shared pending/error state, retryable error modal and undo only after commit. Migrated task/finance/day/paint/calendar/workplace paths close/reset only on committed acknowledgement. Draft record IDs and original-record digests survive covered saved-state restorations, rejecting stale targets instead of overwriting. Notification permission is requested after task commit. Inventories/remaining unproven routes are explicit.
- **D17 atomic notes**: `Neo4LocalBackup.saveNote` coordinates existing V11 text and SmartStore style with the shared rollback journal/gate and both expected snapshots. Note dialog keeps input pending/failure and retains intended calendar/record revision. Failure/injected process interruption recovers consistent prior state in tests.
- **D18 write/service recovery**: all journaled DB/note/workplace/restore transactions snapshot pending side effects before preparing new intent; rollback restores that intent too. Production replay reads current confirmed data under the same gate, cancels only changed/removed prior alarms and positively acknowledges completed services. Persisted JobScheduler retries are best effort with boot/app-open fallback. OS delivery failure never converts a data commit into false data loss or duplicate records.
- **D24 restore/assets**: journal before copying absent assets; records created path/hash; restores old preferences before cleanup and removes only new hash-matching unreferenced content. Existing linked/unknown/modified files are preserved. Root adopts full restored state, clears stale undo and re-locks finance rather than enqueueing a second save.
- **Additional source findings**: new missing calendar/workplace/shift references rejected, hidden calendars remain valid and historical unchanged orphan references remain intact; referenced workplace removal blocked at backend; widget completion uses shared journal; Classic retains legacyJSON plus fullZIP and now shares bounded preview/token/validated safety archive and live finance authorization.
- **Independent safety work**: PRIVATE/neutral-publicVersion notification model regression; does not mean P05 lock-screen/device acceptance. Experimental signing/build/evidence preparation does not mean P06 entry.

Source locations: `V11Home.kt` saveDb/saveWorkplaces/adoptRestore, `NeoCommitRequest.kt`, `NeoTaskCommit.kt`, `MoneyInput.kt`, `V11FinanceUi.kt`, `NeoFinanceForms.kt`, `V11AccountsController.kt`, `NeoDayNoteDialog.kt`, `Neo4LocalBackup.kt`, `NeoCommittedEffects.kt`, `NeoEffectsJobService.kt`, `NeoNewReferenceGuard.kt`, `V11BackupRestoreUi.kt`, `TasksWidgetProvider.kt`. Exact current lines/callsites are in WRITE_CALLSITES.txt; changed-file status is in the external delivery report. No protected feature was intentionally removed or replaced by a new project/schema.

## Verification

Final serial full run: **168 tests passed /0 failed /0 errors /0 skipped**,27 classes; lint **0 errors /45 warnings**; debug build PASS; protected11-file/source and standalone Kotlin checks PASS. Optional native probe separately: **2 passed /0 failed /0 skipped**,28 synthetic PNGs. APK signature/package/version/alignment inspected. QA.md contains exact commands, named tests, limitations and intermediate red/repair evidence. Tests do not prove physical power-loss/full-disk semantics or product acceptance.

## Gate and dependencies

P01 **HOLD**: Q01/Q02 full editor/Activity adversarial matrix remains incomplete; Q04/Q05/Q15/Q18 real providers, lifecycle, privacy, launcher, Android/Honor acceptance lack required evidence. These are separated into unperformed software checks and **BLOCKED — ENVIRONMENT** cases in QA_STATUS.json/REMAINING.md. ADB now enumerates normally with no devices; SDK has no emulator and /dev/kvm is absent. Installed certificate is unavailable. The isolated test certificate differs from documented CI certificate; this same-code31 checkpoint is **not a safe in-place upgrade**.

P02–P06 remain **NOT RUN** because their prerequisite gate has no accepted exit SHA. Their downstream UX defects are not erased from the defect matrix. Continue independent diagnosis/coverage but do not mark the main sequence advanced. Resume and resources required are in REMAINING.md. No Production Ready or zero-bug claim.

## Rollback / artifacts

Use a separate code checkout/worktree at one of the recorded checkpoints; never reset the user's app data or force-push. No actual user upgrade happened. Any future data rollback requires a verified archive and explicit reviewed recovery; older APK installation is not a data rollback strategy.

Actual artifacts: `/workspace/output/premium-p01-resume/`. Experimental APK name/certificate/checksum and retrieval limits: INSTALL.md and QA.md. Raw XML/logs/screenshots are outside Git; generated archives/APKs/private credentials are not committed. Final source ZIP is a Git archive of the final local checkpoint. External delivery manifest/checksums bind it to the exact code and evidence without a self-referential Git hash.
