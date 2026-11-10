# ToDo Shift UX rescue — 4.2.0 (31)

Baseline: 6a0acb77720290a305703df578f4d1646f8976c8. Work is confined to feat/neo-luxury-4.1-ultimate. Package remains com.todoshift.app; minSdk 26/targetSdk 35 and signing configuration remain unchanged. The previous 4.1.1 product failed user review. This report supersedes its UX acceptance claims; the earlier implementation/QA/traceability/remaining/install files are historical evidence, not acceptance of this candidate.

## Architecture and discoverability

V11Home retains state, checked CAS saves, restore coordination, financial unlock boundaries and undo. Stable tab IDs are preserved; presentation order changes to Calendar, Schedules, Tasks, Finances, Settings. NeoFeatureCatalog defines bilingual primary destinations and focused routes, used by contextual action menus and search. Each focused screen reuses the existing reviewed operation implementation without requiring a generic tool selector. Existing tools remain internal reusable code and compatibility entry points; Neo navigation does not expose a Tools dumping ground.

Settings is rebuilt into six groups. Appearance opens its Studio directly. Language/direction and first weekday have individually addressable authoritative pages. Calendar display, notes, Today and paint styling belong to Calendar & Shifts. Notifications/widgets, privacy/data and Advanced retain separate ownership. Classic controls remain available through the explicit Classic experience; stored preferences are not deleted.

Studio is vertical with five expandable sections and a small real calendar-cell preview. Apply/discard/reset affect appearance only. Visual identities and saved profiles preserve language, all independent direction preferences and first weekday. Existing customized appearance values are retained; new installations default to flat, calm Neo Luxury with a plain background. Users can explicitly select the new Neo identity in Studio without resetting data or language.

## Calendar, painting, schedules and tasks

The header brush enters painting immediately. The unwanted quick-add dialog is removed from the Neo brush path. Workplace controls are colored and scroll independently; eraser and Done stay fixed. Type labels/dots reflect workplace/custom colors. Favorite selection persists; single nonconflicting paints commit directly, while overlaps/bulk changes retain review/cancel, fresh-snapshot checks and undo. Full/half/quarter geometry is unchanged and separate from actual clock time.

Calendar date/selection/Today/shift fill/note layers remain separate. Jump-to-today is visible beside month/week/day controls. Notes show truncated actual text; marker icons are separate so the icon prefix cannot consume the cell's entire text width. OFF is not labeled twice. Outside-month and density preferences remain available. RTL/English month labels and large-font behavior have simulated-render evidence with explicit limitations.

Schedules has three visible sections: shifts, summary, calendars. Statistics include only visible calendars and distinguish segments, distinct worked dates and OFF. Templates/comparison have contextual links. Existing actual clock ranges and next-day labels remain separate from geometry. Day-detail time editing formats overnight ends as clock values (09:00 rather than invalid 33:00) and retains the existing normalization/overlap checks.

Tasks retains quick title/date/save, expandable advanced fields and reminders. Per-task actions include edit, attachments and delete. Task IDs and dialog visibility are saveable so restoring a draft does not create a record. Independent note editing and attachments are available from day details; deleting shifts does not delete notes. Shift copying still excludes independent notes.

## Finance and integrity improvements

Finance has summary, receipts and obligations sections with concise expected/received/remaining/upcoming figures, retained privacy eye, compact payment/obligation forms and managers. Rates, allocations, statements and month review have focused natural routes. Existing money parsing, cent arithmetic, history, allocations, exceptional rates and audited month closure remain intact.

Workplace creation now adds a missing compatible income-source link inside the existing journaled workplace transaction. No existing source is renamed, reassigned or deleted. Previously missing links are offered as a user-reviewed Data health operation with pre-change safety copy. A pure regression checks additivity/idempotency and preserved payments.

Attachment import and reference commits share the restore gate. Storage review lists recognized detached files and requires explicit confirmation of the exact set before removal; stale reviews are refused. Referenced files, unknown private files, records and safety archives are preserved. No automatic cleanup or retention deletion was added.

Inherited day-item, workplace, source/method and financial editors have unsaved-change guards and primitive saved state. Notes and advanced task fields restore stable draft values. Security credentials are not persisted into drafts. Process-death, interrupted document-provider and all inherited modal combinations still require device acceptance.

Quiet-hour rescheduling now checks deferred delivery time, preventing a reboot after the original timestamp from dropping a still-pending deferred reminder. A supplied clock value makes that boundary deterministic in tests; production defaults to the system clock. Exact/approximate alarm state and its Android permission screen are exposed. Essential reminders remain independent of focus/performance state.

## Visual findings and corrections

Simulated native activity renders exposed a real inherited-content-color defect: Dark/AMOLED header text/icons could remain black from the outer light theme despite valid color tokens. The theme now provides its on-surface content color; a regression covers nested translucent surfaces. The six transitions apply to main navigation as well as calendar/preview; Reduce Motion/runtime reduction yields identity transforms.

Initial capture methods timed out or lagged a frame. The final harness explicitly advances/synchronizes native window drawing and records its limits. It uses the actual MainActivity with synthetic isolated fixtures, not a hand-drawn reconstruction. These renders are not emulator, Honor, physical keyboard or user acceptance. A modal image is omitted if a distinct dialog window cannot be identified. QA_REPORT and VISUAL_EVIDENCE distinguish verified captures from unsupported checks.

## Preservation

The eleven protected storage/model/migration/business-engine files and both brand archives remain byte-identical to the protected baseline. No production signing key was changed or exposed. No database reset, uninstall, user-data clearing, merge or release was performed. All source and delivery reports are committed in internal stages. Final source/CI SHA and artifact identity are recorded separately to avoid self-referential commit hashes in this file.
