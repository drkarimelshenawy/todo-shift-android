# Neo Luxury 4.1 P0 — cumulative implementation report

This is the single cumulative progress report. The other three requested files are evidence and scope indexes, not parallel progress logs.

## Baseline and delivery

- Repository: drkarimelshenawy/todo-shift-android.
- Verified remote Luxury-v4 baseline: e7ca3ba8d9b72f1d2c570d24a334117e5543107a, including the NeoCalendar maxWidth fix after 592be8c.
- Working branch: fix/neo-luxury-4.1-data-integrity, created directly from that commit in the existing isolated cloud checkout.
- Reference branches are not checked out, reset, merged or pushed. Only the development branch is eligible for push under the user's latest delivery policy.
- No actual user data was opened or changed. Tests use synthetic JSON and an in-memory SharedPreferences double.

## Delivery blocker

Implementation and JVM/debug-build verification are complete locally. GitHub delivery is incomplete: native HTTPS push failed without a credential helper; a focused retry using the existing GH_TOKEN binding through a temporary askpass helper was rejected as invalid username/token. Neither the token value nor credential helper was saved in the repository. A final remote read confirmed that the target development branch was not created and Luxury-v4/main stayed at their original tips. GitHub write authentication for this repository must be repaired before pushing this same development branch. Do not merge or push either reference branch. No additional build is required merely to retry delivery.

## Implemented

1. Both stores expose explicit Missing, Loaded and Failed outcomes. Existing load() callers receive an exception for missing/failed reads rather than fabricated writable defaults. Reads never initialize or write.
2. Normal V11 loading validates schema and collection structure before the existing parser runs. Schemas 11, 12 and 13 are supported: base collections are required for all; dayItems/obligations/obligationPayments are required from 12; tasks from 13. Older absent additive collections retain their historical empty meaning. Present malformed collections fail regardless of version. Shift day IDs, dates, calendar references, segment IDs/kinds and nested arrays are checked. Unknown schemas are rejected, not reinterpreted as 13.
3. Legacy loading supports the repository's unversioned original format and schema 9. Required collections, shift dates/types, nested ID arrays and existing entity IDs are checked. An intentionally empty workplaces collection is retained rather than replaced with random defaults.
4. Runtime startup blocks initialization/migration after failed reads. A missing previously migrated V11 store is an error. Initial defaults are created only when both databases and preference stores are pristine; an existing valid legacy database can be migrated normally. Initial workplace IDs are committed before migration. The V11 payload and existing migration marker are committed in one preference edit. Commit failure is surfaced.
5. Ordinary saves revalidate the current backing payload and proposed payload, preventing a failed read from being replaced by an unrelated empty save. Valid intentionally empty databases remain writable. Explicit legacy user import remains a validated replacement operation; it is not an automatic fallback.
6. V11Home's existing boot error screen now receives parser failures before database state and synchronization are created. Automatic workplace/rate synchronization cannot run after boot failure. Database state is published only after save returns successfully.
7. Synthetic regression tests exercise production storage parsers, migration and runtime with injectable preference stores and recovery/refresh callbacks. Dates 2026-11-07, 08, 11, 17, 23 and 28 are checked, including exact V11 day/segment identity, calendar association, workplace, note, exceptional amount and custom times across save/reload.

## Compatibility and limits

Application ID, signing configuration, preference filenames/keys, database output schemas, backup archive format, calendar/theme/navigation and paint rules are unchanged. Historical schema support was checked against repository revisions 6f98924 (V11 schema 11), 41d55ef (schema 12), 7953274 (unversioned legacy) and 85fe86f (legacy schema 9).

Legacy shifts have no persistent shift IDs in their input model; migration retains dates/content/workplace references and uses the existing fixed default calendar and generated V11 IDs. Restart tests ensure those generated IDs do not change through remigration. No new schema migration or record repair was introduced.

Synchronous commit now reports persistence failures, but is not a cross-file transaction or a device durability proof. SharedPreferences can update its in-process cache even when disk commit fails; tests do not claim rollback guarantees. Existing UI save callbacks can still perform blocking IO. See KNOWN_ISSUES_4.1_P0.md.

## Status

| Area | Status |
|---|---|
| Explicit reads and failed-load write protection | Implemented; targeted JVM verification recorded in QA |
| Legacy initialization and migration guards | Implemented; targeted JVM verification recorded in QA |
| Supported historical schema loading | Implemented; synthetic compatibility tests |
| Commit failure reporting | Implemented; simulated failures only |
| Android device/process-death/disk verification | Untested |
| Recovery UX, cross-writer revision control, unrelated Task 01 risks | Unimplemented, outside this task |
| GitHub development-branch delivery | Incomplete; authentication rejected, local commits preserved |
| Original November incident | Not reproduced; resolution not claimed |

Verification outcome: 31 targeted JVM tests passed; debug packaging and APK signature verification passed using a disposable synthetic signing key after an initial missing-production-keystore failure. Code commit: f77e4e7. Production upgrade and device persistence remain untested.

Final commands, counts and artifact status: QA_RESULTS_4.1_P0.md. Changed-file index: FILES_CHANGED_4.1_P0.md. Remaining risks: KNOWN_ISSUES_4.1_P0.md.
