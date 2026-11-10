# P01 findings beyond the supplied defect register

These are source/fault-model findings. They do not reproduce the original phone incident. Final source/test SHA and results are in P01_HANDOFF.md and QA.md. No device-only claim is accepted here.

| ID | Evidence / prior failure | Correction / focused regression | Status |
|---|---|---|---|
| P01-A01 | Home's Unit callbacks dismissed/reset editors when a write was only queued; storage re-read could reflect failed SharedPreferences disk commit in memory. | V11Home.kt:329, NeoCommitRequest.kt; completion only after journaled checked write. NeoTaskCommit requires positive write acknowledgement for ambiguous/repeated stable IDs. PremiumP01UiTest.completionContractNeverDismissesOnEnqueueOrRejectedWrite; NeoTaskCommitTest.visibleMemoryWithoutDurableAcknowledgementIsNeverReportedCommitted. | Implemented, targeted simulation tested; whole-editor matrix pending. |
| P01-A02 | Restored editor could adopt a newly loaded record as its original revision, overwriting a concurrent change. | rememberNeoDraftUnchanged saves the original record digest. PremiumP01UiTest.restoredDraftDoesNotAdoptAChangedRecordRevision. | Implemented/tested for covered forms; full Activity matrix pending. |
| P01-A03 | Retrying creation could generate a fresh payment/allocation/source/obligation ID. | Stable saveable request IDs, backend collision rejection before ledger/audit append. PremiumP01UiTest.paymentRequestAndAllocationIdsSurviveUnknownAcknowledgementAndRecreation and FinancialRequestIdentityTest. | Implemented/targeted tests; no business formula change. |
| P01-A04 | New task/note could reference a deleted destination; workplace deletion backend omitted committed task references. Red proof: NewReferenceSafetyTest, 4 tests, 3 failures before guard, preserved outside Git. | NeoNewReferenceGuard and journaled mutation callers reject new missing targets; historical unchanged orphan references remain untouched and hidden calendars remain valid. All 4 regressions. | Fixed in tested synthetic store scenarios. |
| P01-A05 | Unchanged alarms were included in cancellation intent; an aborted proposed write could leave cancellation state. | NeoCommittedEffects compares record identity/content; all four data transactions snapshot service intent in rollback journal. rolledBackDataWriteDoesNotCancelAnExistingUnchangedAlarm and unrelatedDataSaveDoesNotCancelAnUnchangedReminder. | Implemented/simulated alarm checks; physical scheduling pending. |
| P01-A06 | Read of confirmed DB and service replay could be separated by another writer. | reconcileCurrent reads and replays under Neo4DataGate; production callers use it. retryReplaysNewestCommittedTaskRatherThanAnEarlierQueuedSnapshot. | Implemented/targeted test; OS delivery timing pending. |
| P01-A07 | Deferred service replay depended on UI/boot reopening alone. | Durable marker plus persisted internal JobService retry, backoff; queue remains if OS rejects job. retryJobIsPersistedAndAcknowledgedEffectsCancelIt. | Config/model tested; Doze/Honor/background restrictions unverified. |
| P01-A08 | Classic backup used a separate synchronous import/export path and did not adopt full restored state; finance access needed a live check on provider return. | V11BackupRestoreUi uses common preview/token/safety archive/journal; Root adopts full state and re-locks; legacy JSON export retained. Live finance authorization checked before I/O. ClassicBackupAuthorizationTest verifies stale true prop cannot bypass relock. | Implemented/component test; real providers and interrupted Activity pending. |
| P01-A09 | Widget completion bypassed the common rollback/effects transaction. | TasksWidgetProvider uses commitDatabase; actual receiver broadcast regression preserves unrelated tasks. | Robolectric integration tested; actual Launcher pending. |
| P01-A10 | Notification had no neutral public-version model for locked OS surfaces. | PRIVATE visibility and neutral publicVersion retaining click identity; ReminderPublicVersionTest. | Independent P05 safety preparation only; physical lock-screen acceptance blocked. |

## Original P01 defects

D01/D23: grouped/localized raw money parsing and blank/invalid distinctions repaired; reopened-cent fixtures cover source/payment/obligation cases, not every MainActivity numeric route.
D07/D16: checked completion, pending/error draft retention, stable IDs, stale-revision rejection; whole-Activity/process-loss/editor acceptance not complete.
D17: note text/style share a rollback journal; tests interrupt journal/effects/text/metadata boundaries and reject concurrent metadata writes.
D18: effects recorded transactionally, failures distinct from committed data, durable replay; OS-specific behavior pending.
D24: restore journal precedes new asset writes, tracks paths/hashes, recovers old preferences before cleaning only new unreferenced matching content. Existing linked/unknown files preserved in tests; provider/device matrix pending.

## Intermediate failures retained

The intentionally red reference tests established an actual pre-fix defect. An optional native probe idling failure was repaired without disabling application tests. New alarm assertions initially used Robolectric's consuming getNextScheduledAlarm; bytecode confirmed it polls/deschedules, and the test now uses peekNextScheduledAlarm. A test nullability compile error was fixed. See raw evidence history; intermediate failures are not reported as environmental blockers or final successes.
