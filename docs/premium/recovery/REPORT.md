# P01–P06 software recovery — delivery record

Tested code/source commit: `eeb1af78251e20c15c47f989bdaea80419104563`. Branch `work/todo-shift-premium-rescue`; input14df509; original baselinee178b13. The final docs-only checkpoint is recorded in delivery/CHECKPOINT.json after commit. Its app Git tree must match this tested tree. No push, merge, PR, publication, install, storage clear or user-data operation.

**Development P01–P06 PASS. Device/Release HOLD.** Original GATES.json/ACCEPTANCE.json/Blueprint are untouched historical results. The user authorized independent Development Gates; this record does not promote original acceptance. Not Production Ready.

| Phase | Tested source SHA | Regular tests | Lint errors / warnings | Build / Development |
|---|---|---:|---:|---|
| P01 | 859f41266366a675051fbfa82beae4ae2a1811f1 | 170 (0 fail/error/skip) | 0 / 45 | PASS / PASS |
| P02 | bfab8d97f63be559dc1b9f40a1151abdfa31b6da | 180 (0 fail/error/skip) | 0 / 46 | PASS / PASS |
| P03 | 8eb3c7a839141708c03544f5d062ed70b85222ac | 189 (0 fail/error/skip) | 0 / 46 | PASS / PASS |
| P04 | 7d05c61c254d7c45c4e39ce78a3dbf1b1eff4da8 | 196 (0 fail/error/skip) | 0 / 46 | PASS / PASS |
| P05 | 37723d1fb9825ff6bac9c83c8e1051e9c453d527 | 208 (0 fail/error/skip) | 0 / 46 | PASS / PASS |
| P06 | eeb1af78251e20c15c47f989bdaea80419104563 | 211 (0 fail/error/skip) | 0 / 46 | PASS / PASS |

Counts are cumulative suites per phase, not additive. Final regular suite211; separate optional native suite2. Do not sum earlier phase totals. Native diagnostics are Robolectric SDK28 on Linux, not emulator/device tests.

## Implemented software

- P01: actual Activity I/O/retry/CAS journeys; rollback-journal bytes verified before business writes. Prior exact localized-money contracts, stable editor IDs/raw drafts, joint note-text/style transaction, interrupted restore/asset recovery and durable effects retained and exercised.
- P02: one saved navigator/history for body/title/tab, dirty-route guard, cold/warm/deleted/private intents, consume-once and ordinary launcher resume; no CLEAR_TASK widget route. Correct saved origin on Back, original branding and Classic identity retained.
- P03: direct Premium floating + per Blueprint, compact persistent workplace/type controls, layered Back/Done, named whole-segment conflict/deletion reviews, note confirmation, distinct task/event/shift indicators and measured cell text. No duration/financial engine inference changes.
- P04: task empty/search states, saved task/schedule filters, compact month/filter summaries, explicit OFF/unknown legacy clocks. Root reload/retry keeps raw draft and unrelated external records. CSV/provider return/pending financial confirmation recheck live authorization. Session-only undo labelled.
- P05: six settings groups, combined language/direction page, settings-only search and reachable layout preset. Read-only calendar/header/navigation/task preview uses real components/atmosphere. Four widget kinds share real projections/privacy/painter; cached adapters recheck hiding, OFF distinct, workplace color retained when decorative paint disabled. Checked widget-only refresh journal avoids alarm churn and acknowledges after RemoteViews request. Inactive cloud shown honestly, stored values retained.
- P06: full-allowance large backup regression, isolated manual QA workflow with metadata/signature/checksums (stable workflow unchanged), candidate32. Real native diagnostics exposed duplicate feature headers, stale screenshot output, hidden seventh column at320dp/system200%, and pin/date overlap. Unified guarded shell Back/header; clean capture output; full-width week, full spoken weekday names, measured stacked badges/marker and matching production preview. Global corrupt-type widget privacy fails closed without erasing raw value.

## Commands and evidence

Actual local Java17/Gradle8.9/SDK35 toolchain is under `/workspace/work/toolchain`. Signing environment was supplied privately; no credential values are in commands/reports. The wrapper `/workspace/work/premium_phase_validation.py` logs these exact Gradle arguments:

```text
gradle --no-daemon --continue -Pkotlin.compiler.execution.strategy=in-process -I /workspace/work/toolchain/test-proxy.init.gradle testDebugUnitTest lintDebug assembleDebug
gradle --no-daemon --continue -Pkotlin.compiler.execution.strategy=in-process -I /workspace/work/toolchain/test-proxy.init.gradle -I tools/visual_capture.init.gradle -I /workspace/work/premium-test-events.gradle testDebugUnitTest --tests com.todoshift.app.v11.ui.NeoVisualProbeTest
python tools/verify_neo41_source.py
JAVA_HOME=/workspace/work/toolchain/jdk17 PATH=<jdk17/bin>:<kotlinc/bin>:$PATH bash tools/run_neo_checks.sh
git diff --check
```

Final results: regular211/0/0/0, native2/0/0/0, source11 protected files byte-identical to their guarded baseline, Kotlin golden checks PASS, lint0 Error/Fatal and46 warnings. Lint ID counts match recorded P02 XML exactly; no suppressed checks, test disabling or continue-on-error visual gate. Compiler Android deprecation/analytics-directory warnings remain in logs. Workflow YAML structural checks passed; Actions execution **NOT RUN**, no URL/artifact on GitHub (latest instruction forbids push; new workflow registration may require separate authorization).

Phase JUnit/lint/logs: `/workspace/output/premium-recovery/P01/` through `P06/`. P06 regular JUnit is saved before optional filtered native execution. BUILD-INPUTS.json hashes the actual pre-build app/Gradle/test files and records matching committed app tree. Checksum/signature/badging/alignment and final checkpoint are staged outside Git in delivery. Intermediate failed/aborted diagnostics are not final results: notably the strengthened large-row assertion first queried clipped offscreen bounds; it now scrolls the target cell and retains all date/pin/note/name/count separation assertions. Historical failure XML/log is labelled separately in QA.

## Large synthetic fixture

{"fixtureSeed":147,"environment":"Robolectric SDK28 desktop JVM; no physical p95 or peak-memory claim","tasks":10000,"segments":3000,"notes":1000,"receipts":2000,"attachmentBytes":25165824,"zipBytes":25364380,"exportMs":927,"previewMs":402,"restoreMs":839,"heapBeforeBytes":185797304,"heapAfterBytes":293556752}

Seed147; five-year task dates include2024-02-29; visible/hidden calendars and actual overnight clocks retained.6×4MiB referenced attachments use deterministic synthetic bytes, IDs and SHA256;2000 receipts preserve exact aggregate cents/allocations after preview/restore. Measured export/preview/restore times and before/after JVM heap are a desktop sample, not peak memory, physical p95, comparative performance, or a user upgrade. D21/D28/D29 remain physical qualification risks.

## Traceability and acceptance

TRACEABILITY.json/MD preserve all147 original IDs and criteria with corrected implementation, primary location, persistence and named regression subsets. QA_DEVELOPMENT.json covers Q01–Q28 without promoting their full/manual clauses. DEFECTS.json reconciles D01–D29; ADDITIONAL_DEFECTS.json records four new evidence-backed corrections. EDITOR_INVENTORY.json spans Premium/Classic source callsites; APPEARANCE_CONTROLS.json inventories67 fields and indirect/legacy mappings. These static inventories and associated tests do not prove every editor/interleaving/toggle/visual combination. No requirement row is marked physically accepted.

NAVIGATION_MAP.md gives natural entry points and source-derived tap counts. REMAINING.md records pending manual/device cases; INSTALL.md gives certificate/backup safeguards and actual retrieval limits.

## Visual inspection

28 fresh PNGs from2 actual MainActivity native diagnostic tests; two labelled component-p01-* images explicitly substitute production component content/synthetic rejection and are not root I/O reproductions. Includes English Light360dp, Arabic RTL Dark/AMOLED at1.3 app scale, narrow320dp confirmed **system**200% empty and populated calendar; populated image is vertically scrolled to show the entire target cell. The old UI-direction image is absent from the fresh manifest. Contact sheet and key individual screens were visually inspected; all seven columns, date/pin separation and distinct count rows verified in the native samples. Short note/shift text and navigation labels intentionally truncate at200%; complete labels remain in semantics/details. No real IME, Launcher, TalkBack, Honor, notification timing or full pairwise acceptance is established. These are diagnostics, not user visual approval.

## Identity and remaining release gate

Candidate `com.todoshift.app`, `4.3.0-premium-dev`, code32; min26/target35. applicationId, launcher component, signing configuration and protected storage/business formats retained; no speculative schema migration or historical money/clock rewrite. Version32 exceeds known baseline31, not an assertion about an unavailable installed version. Isolated public certificate `3a969f25d41d3f07a1f5ea352005ecd02a3afe1e186e82b5f631c227418214d4`; it differs from documented prior CI certificate. Installed certificate/lineage unknown; **do not install this as an update over the existing app**. Final public signature/hash/size are in delivery/identity, verified again after the docs-only checkpoint without rebuilding APK.

BLOCKED — ENVIRONMENT: authorized Android and Honor devices, installed matching certificate/signing job, real Launcher/notifications/Doze/reboot/providers/keyboard/TalkBack, non-destructive fixture upgrade and physical baseline/candidate p95/memory/jank/battery. `adb devices -l` empty, emulator executable and/dev/kvm absent. Full manual combinations not executed are NOT RUN/acceptance pending rather than invented PASS or a blanket environment excuse. Continue those release cases on authorized fixtures; keep real data untouched.
