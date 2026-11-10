# Storage ownership / write contract — P01 checkpoint

| Owner | Store | Commit and recovery boundary |
|---|---|---|
| V11Storage / V11Runtime | todo_shift_v11: database, migrated_from_rc1 | Existing strict parse/checked commit/migration retained. Root/QuickTask/widget mutations now go through Neo4LocalBackup.commitDatabase, full-snapshot CAS under Neo4DataGate and rollback journal. Runtime.start recovers before loading. |
| AppStorage | todo_shift_data: legacy database/workplaces | Existing format/strict writes retained; workplace changes coordinate legacy and V11 stores with the same journal. Referenced workplace removal is rejected. |
| NeoSmartStore | todo_shift_neo41_smart: metadata JSON | Existing templates/notes/attachment refs retained. saveNote journals V11 text and SmartStore decoration together, checks both original snapshots and retains prior state on failure. |
| NeoCommittedEffects | todo_shift_pending_effects: pending, cancelIds | Private additive service-intent store, snapshotted in the same internal journal as business writes. Rollback restores it. Replay only from confirmed data, checked acknowledgement, persisted best-effort JobScheduler plus app/boot fallback. Not included in public backup preferences. |
| Neo4LocalBackup | ZIP / private AtomicFile journal | Bounded preview, token confirmation/checksums, rollback preferences before fresh read. Journal records newly created asset paths/hashes before copying; recovery only cleans hash-matching newly-created unreferenced assets. Existing/unknown/changed files remain. |
| NeoAttachmentStore | private filesDir/attachments + SmartStore refs | Existing type/size/path checks and fresh-reference explicit cleanup retained. Full OS provider/permission/lifecycle matrix not yet accepted. |
| NeoAppearanceStore / V11UiSettingsStore / Neo4Profiles | existing appearance/UI/profile preferences | Existing identity and formats retained. Full restored state adopted in Root, not written twice. No implicit language/direction migration. |
| V11ActiveCalendarStore | existing primary-calendar preferences | Independent creation destination retained; hidden calendars remain valid. New references to deleted destinations are rejected. |
| V11SecurityStore | existing private local security preferences | Not portable backup content; Classic and Neo backup entrances require finance authorization, Classic rechecks live Root access on provider return and before I/O. Existing PIN/biometric configuration unchanged. |
| Widget settings/privacy | existing Neo4 widget stores | Rendering/refresh is a retryable side effect, not proof of committed data. Completion uses the journaled CAS store; actual launcher/privacy timing unverified. |
| Editors / Home | existing records and saveable draft state | Checked async acknowledgement for migrated entry points; pending/rejected operations do not close/reset; undo after commit. Stable IDs and original-record digests protect covered restored drafts. See remaining editor matrix in REMAINING.md. |

WRITE_CALLSITES.txt is a source search inventory, not an assertion that every callsite has executed-case coverage. No persisted business schema or historical financial/shift record was rewritten. The additive rollback/effects journal is private and recovered before normal reads.
