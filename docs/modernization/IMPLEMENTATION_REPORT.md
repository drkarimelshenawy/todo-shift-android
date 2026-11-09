# ToDo Shift 4.1.1 modernization test candidate

Branch: feat/neo-luxury-4.1-ultimate. Starting commit: dc882aec8fd2ccfeaf87d9533fdd7f11ca6b15c1 (4.1.0/29). Candidate: 4.1.1/30, applicationId com.todoshift.app. This work is committed locally only. Full closed-scope acceptance is not claimed; see TRACEABILITY and REMAINING.

## Evidence-based fixes

- At baseline dc882ae, NeoCalendar.kt gated the entire Neo4ShiftFill call on showShiftPaint (baseline line 163). Disabling decorative paint therefore removed workplace coloring, even when entries existed. The new cell always invokes the fill renderer; disabling customized styles now restores inherited workplace colors. That is a verified code-path defect, not proof of reproducing the original user's device incident. Original missing-calendar/workplace possibilities are not asserted as the device's root cause.
- Relative visual geometry changes Morning to top quarter and Evening to second quarter. Real timeRange/rates stay separate. OFF remains duration-free. Multiple owners are preserved in bands rather than hidden by a single owner.
- V11FinanceEngine previously counted payment.amount by the legacy accrual month; explicit multi-month allocations now take precedence, avoiding double counting. Tests cover cent-exact distribution, old unallocated payments and allocation resizing. Stored Double/schema compatibility is retained.
- Dated-rate tools could otherwise be undone by the old workplace-to-rate synchronization. NeoRatePolicy records ownership in the atomic database audit log, preserves prior dates and prevents reconciliation from replacing deliberate dated rates. It rejects duplicate effective dates. Legacy workplace synchronization and migration files remain unchanged.
- The inherited resolver can infer paid shift kinds while splitting segments. The new painter previews/replaces whole overlapping records after confirmation and preserves unrelated records, avoiding implicit partial-shift price assumptions. Classic behavior is preserved.

## Calendar, painting and presentation

Retained month/week/day modes, calendar management, selection and visibility. Added independent grid/header direction and first weekday, cell sizing, outside-date visibility, optional tomorrow marker, Today style/intensity/speed, configurable transitions, compact normal-mode long-press preview and day navigation. Tiny workplace/type painter supports favorite/remembered selection, eraser, single-date direct add, reviewed multi-date/overlap changes and Done. Removing shift segments does not remove independent notes. Schedules distinguish shifts, worked dates and OFF, include all visible calendars, filters, and explicit overnight labels.

Brand palette aligns with the existing approved references. Adaptive/monochrome/legacy launcher, header and splash assets from the baseline are retained; both brand ZIPs are unchanged. Light/Dark/AMOLED contrast and saved appearance workflows remain available. Direction/language/animation preferences are additive; old explicit direction is preserved when decoding old language settings. Profile switching preserves active language. Runtime low-memory/focus reduction does not rewrite saved appearance preferences.

## Tasks, notes and widgets

Compact task creation starts with title/date/save and expands to the retained advanced editor, including recurrence, priority, reminder and links. New external dialog-themed QuickTaskActivity provides widget and launcher quick add, fail-closed load and compare-and-save under the restore gate. It does not launch MainActivity; runtime widget refresh follows successful save. Added notification-permission request when a task reminder requires it. Advanced task and independent note editors guard unsaved changes.

Independent NOTE day items support multiple records, actual truncated calendar text, pin/star/warning, pinned priority, cosmetic per-note dot/border color and separate notes/icons/shift-name/task-indicator switches. Notes are searchable and not copied by bulk shift copying. Attachments are available from Tools for task/note owners through SAF, bounded content recognition and a private, nonexported FileProvider.

## Finance and smart tools

Compact financial summary and conditional payment/obligation forms preserve privacy gating. All reachable V11 monetary editors use strict localized parsing; retained legacy MainActivity editor paths also accept the approved digit formats. Payment edits resize allocations in cents while preserving source/month IDs. Savings/subscription compatibility is explicit, not a new ledger schema.

Tools is reachable from Settings and header search. Its sections implement global search, shift copying, shift/task templates, dated rates, month comparison, per-workplace CSV statements, payment distribution, audited close/reopen review, health/status repair, notification preferences, attachment management, temporary focus and opt-in local usage counts. Financial tool content respects unlock and amount-visibility state. Major reviewed changes require a validated pre-change safety archive and an unchanged database snapshot. Safety copies are inspectable through Backup. Undo covers successful shift/task/finance snapshots, with audited entries, financial-unlock enforcement and no silent closed-month reversal.

## Integrity and compatibility

The existing fail-closed parsers, storage, restore gate and compare-before-save remain intact. Eleven protected persistence/model/migration files are byte-checked against the original baseline. No database reset or speculative migration was added. Backup metadata is additive/optional for older archives, checksum checked and rollback journal aware. Attachment restoration validates private paths and stages before preference commit. No data from a real installed app was loaded, replaced, uninstalled or cleared.

## Validation scope

Automated checks include storage failures and stale writes, backup rollback, localized parsing and allocations, date/visibility/calendar regressions, template idempotency, attachment round-trip, rate history, reviewed painting, safety archives, quiet-hour policy and selected Compose interactions/tool screens. Gradle/lint/APK checks and exact outcomes are in QA_REPORT.md. This is not a guarantee of zero bugs. Every physical/device-only acceptance remains explicitly open.
