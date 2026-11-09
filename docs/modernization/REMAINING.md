# Acceptance gaps and limits

This is a local 4.1.1 (30) test candidate, not a claim that the entire closed scope has passed acceptance. The implementation spans all 16 groups; the 130-item matrix distinguishes implementation from verification. No release or publication was performed.

## Device-dependent acceptance still required

No emulator or attached physical device was available. Honor behavior, keyboard/insets on actual devices, widget launcher sizing and lifecycle, notification permissions/exact-alarm restrictions, delivery after reboot/doze/battery optimization, biometric/PIN interactions, system restore interruption, real upgrade from the installed signing certificate, and actual image/PDF viewer interoperability remain unperformed. Robolectric tests do not establish these results.

Exercise Arabic and English with every independent direction, theme, font size, and supported screen width. Review tap targets, colored-cell legibility, animation gestures and accessibility using TalkBack. There is no exhaustive visual acceptance or timing/memory benchmark. The settings audit changed reachable code and removed specific duplicate controls; it does not prove every inherited combination works.

## Intentional compatibility choices

- Existing JSON database format, IDs, parsers, migration and fail-closed storage remain intact. New operational metadata is an additive preference section; attachments use a narrow private directory. Existing business models were not replaced.
- Savings and subscription UI use compatible OTHER obligations and subtype note markers. Existing monthly/remaining-balance calculations apply; there is no new savings ledger, interest engine or speculative migration.
- New painting uses explicit 09:00-anchored defaults. Historical stored times are unchanged. Whole conflicting segments are replaced only after preview/confirmation; no inferred partial-segment pricing is introduced. The original Classic resolver is retained.
- Explicit payment allocations now determine monthly received totals instead of also counting the legacy month. Legacy payments without allocations still belong to exactly their saved accrual month. Currency arithmetic rounds HALF_UP to cents; stored Double fields remain backward compatible.
- Comma is a grouping separator; decimal dot and Arabic decimal separator are accepted. Ambiguous decimal comma input is rejected with an error, never reinterpreted silently. Amount input is capped to safe precision/range.
- Dated-rate ownership is stored in an audit record in the same database snapshot. The legacy workplace editor locks those kinds and points to the dated-rate tool. Existing older rates remain; duplicate dates must be corrected through a separately reviewed workflow rather than an ambiguous duplicate append.
- Copy skips conflicts with a visible preview and never copies independent notes. Task-template edits are nonretroactive; repeat creation on a date is idempotent and does not resurrect individually deleted tasks.
- Undo keeps up to 20 successful snapshots in memory; it does not survive process death. Month closing/reopening uses explicit audited actions rather than generic undo. Attachments and appearance preferences are outside shift/task/finance undo.
- Draft preservation and discard guards cover new quick task, advanced task, note and compact finance interactions, but are not a complete persisted draft system for every inherited editor. Process-death restoration across all editors remains unverified.
- Attachment types: PNG, JPEG, PDF and UTF-8 text only, 4 MiB/file, 24 attachments/24 MiB attached payload. Detach removes metadata without silently deleting private files. Detached files and safety copies are retained; there is no automatic retention cleanup. Monitor private storage use.
- Note style metadata and note text use separate checked commits. A style may remain after a note-text save failure; existing note records are not reset. Full cross-store atomicity for this cosmetic metadata is not claimed.
- Notification quiet hours are a fixed 22:00–07:00 opt-in deferral. Essential reminder policy does not depend on performance/focus preferences. Actual scheduling/delivery remains subject to Android permission and battery policies.
- Optional local usage statistics count tool visits, not private task or financial content. No telemetry service was added.

## Delivery limits

The local test certificate differs from the expected production certificate. This APK cannot upgrade an installed app signed by another certificate. Do not uninstall or clear storage to bypass that mismatch. A build signed by the installed certificate is required for a data-preserving upgrade. Production signing keys were unavailable and were not changed.

No session file-browser/download control or authenticated phone-accessible file service was verified. Artifacts exist at the recorded workspace paths. Source can be retrieved from a local checkout or an explicitly authorized future source push; APK retrieval requires a verified session export facility or separately authorized artifact hosting. No download URLs are invented.
