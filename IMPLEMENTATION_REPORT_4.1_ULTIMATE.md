# ToDo Shift — Neo Luxury 4.1 Ultimate Fixed

## Delivery scope and baseline

Work is confined to `feat/neo-luxury-4.1-ultimate` in `/workspace/todo-shift-android`, based on `f71bf40a87aa5df7cc10da7085b3e44ac196dad0`. The implementation was initially packaged uncommitted; it is now preserved by a local commit on this feature branch. Existing delivery archives remain the earlier packaging snapshot. No push, merge, pull request, uninstall, storage clear, real-device install or replacement of real user records was performed.

Version: **4.1.0 / code 29**, increased from code 28. Application ID remains **com.todoshift.app**. The production signing configuration, launcher component, manifest permissions, database schemas, parser protections and financial calculation engines are preserved. The locally delivered APK uses a separately generated **local test certificate**, not the production certificate. Read INSTALL_4.1.md before attempting installation.

## Audit coverage and practical limits

The audit covered the app module and its source/resource inventory, the actual activity entry point, Classic/Neo routing, calendar/day editing, schedules, finance/security boundaries, task editing/filtering/deep links, settings subpages, Appearance/Widget Studios, local/cloud-readiness backup flows, image import, legal views, widgets, reminder scheduling and boot/package-replaced receivers. Searches traced storage writes and side effects across the source tree. Compilation and Android lint analyze the full app, including historical code still present in MainActivity.

This is an evidence-based source audit plus JVM/Robolectric and Compose checks, **not** a claim that every screen has been manually verified on physical hardware. No available device/emulator session was established. Biometrics, launcher masks, actual notifications, OEM layouts, power-loss durability and an upgrade over a real production installation remain device acceptance items.

## Calendar diagnosis and repair

The original affected-device incident was not reproduced. The following code behaviors were evidenced at baseline:

- `v11/ui/NeoCalendar.kt:53` filters day entries by visibility before rendering.
- `NeoCalendar.kt:160` draws fill only when `showShiftPaint` is enabled.
- `NeoCalendar.kt:165–168` provides task/event dots and a segment count only for more than one shift. A single saved shift could therefore become visually unmarked when fill was disabled.
- `v11/Neo4ShiftPaint.kt:11` intentionally excludes OFF from paint geometry. A single OFF segment also lacked a fallback count.
- `NeoCalendar.kt:134` omitted visibility from the sheet occurrence cache key.
- `NeoCalendar.kt:164` muted only the date number outside the month.

The upgrade adds a pure `NeoCalendarPresentation` projection, permanent compact shift markers independent of decorative options, an explicit OFF marker, visibility-dependent sheet invalidation, outside-month fill muting, distinguishable selection/today borders, localized textual month titles, clamped date navigation and density-aware RTL/LTR swipe interpretation. Month swiping is disabled during paint selection and when horizontal overflow is needed for large text; navigation arrows remain available.

The Neo renderer changed; Classic calendar geometry and shift/financial business rules did not. The toolbar now gives month/year and view controls separate readable rows. Day semantics include shift/task/event counts. Existing multi-owner paint bands are retained.

## Persistence and ordering

Existing P0 parsers, storage structure checks, Missing/Failed/Loaded distinctions, migration guards and checked database commits remain byte-identical to baseline. The new static safety check pins these and financial engines to the authorized baseline.

`V11Runtime.saveIfCurrent` compares the complete expected persisted database under the same monitor used by widget writes and restore. A stale UI snapshot is rejected rather than overwriting a background change. Foreground database writes run on IO, publish UI state only after success, and block further editing on a failed confirmation. A failed save is not presented as successful. Close/reopen is required to reassess uncertain storage; no empty fallback or automatic reset is introduced.

Workplace edits update legacy storage and synchronized V11 data under the existing crash-recovery journal. Both expected snapshots are checked; failure attempts rollback and retains the journal when rollback cannot complete. Existing ordinal workplace repair is unchanged, not expanded speculatively.

Reminder cancellation/scheduling follows confirmed task/event persistence. Background widget completion uses the same snapshot check and no longer cancels a reminder before save succeeds. Widget refresh failure is isolated from successful database persistence.

Limits: compare-before-save protects the in-process writers using this gate, not arbitrary external modification or another application process. Android SharedPreferences cache/disk behavior during real I/O failure and power loss needs device fault testing. No new database revision system or speculative schema migration was introduced.

## Multi-calendar behavior

Visibility and primary creation selection remain separate stored concepts. UI labels now say “Primary for new entries,” and the calendar shows its creation destination with a warning if it is hidden. Selecting primary does not toggle visibility. Cells aggregate all visible calendars; global tasks/events remain visible. Task lists, summaries and task widgets apply the same visibility policy.

Referenced calendars cannot be deleted. The manager now disables that delete control rather than silently ignoring it. Calendar IDs, ownership and historical records are not reassigned.

## Today, Tasks, settings and navigation

Today is hosted in the Tasks experience using one reusable dashboard. The obsolete duplicate overview sheet implementation was removed; header overview navigation goes to Tasks. Calendar “go to today” remains a date-navigation action.

Dashboard ordering, hidden cards, quick actions and optional finance summary are retained. Protected finance requires the existing authorization state; hidden amounts also hide the chart. Dashboard preferences no longer become overridden merely because the panel is expanded.

Task controls and rows share a scrollable list so larger fonts and smaller windows can reach the content. Task deletion requires confirmation. Notification/widget task actions are consumed once, avoiding repeated editor opening when returning to Tasks. Existing task editor, completion, search, priority/category and date filters remain.

Settings consolidate duplicate Appearance Studio deep links and add clearer work/data/privacy hierarchy. About reads generated version/application metadata. Local backup actions appear before an expandable, explicitly unavailable Google-services section. No working cloud service was removed; it was already unconfigured.

Appearance Studio keeps draft/apply/discard/reset semantics and warns before abandoning unsaved preview changes. Preview language, direction and scaling follow the draft without saving it. Appearance, primary calendar, UI settings, layout profile and widget-setting writes check commit results where changed. Widget/profile failures are displayed.

## Theme, language and accessibility

Custom accents are bounded by measured luminance rather than one fixed interpolation. Base text, muted text and accent-on-surface contrast are tested across all palettes in Light, Dark and AMOLED. AMOLED retains a black background. This does not claim pixel-level contrast verification over every arbitrary personal image and transparency combination.

Month titles use Arabic/English month names at narrow widths. Directional active-screen back icons are auto-mirrored. English launcher/widget/notification metadata and widget empty text were completed. Small widget text was increased to at least 11sp. Cells preserve accessible data counts even without decorative paint. Protected-account screens use Android secure-window flags.

## Brand resources

Both brand archives remain unchanged. Their README files explicitly describe draft/reference PNGs, including a phone-framed splash and a prototype foreground containing background pixels. Those crops were not installed as production assets.

Clean calendar/check/clock artwork was independently redrawn as VectorDrawable resources with navy/blue/gold styling. Deliverables include adaptive foreground/background, a monochrome silhouette, density-specific legacy PNGs, an SVG design source, header emblem and navigation vectors. Android 12+ uses a real splash icon; earlier supported versions use a centered emblem starting window. Existing launcher component identity is retained. Classic appearance choice and saved branding preferences remain available.

## Additional concrete audit fixes

- Valid personal photos previously failed: bounds-only Bitmap decoding returns null by design, but the old importer treated that as an unreadable stream. Bounds loading now validates the stream and dimensions separately; a synthetic image regression test exercises import/load.
- Image orientation reading now uses AndroidX ExifInterface instead of the older framework implementation.
- Unknown security-mode values fail closed rather than mapping to unlocked NONE. Settings and obsolete credential cleanup commit together; PIN/pattern setup returns actual commit success. Hashing and credential formats are unchanged.
- Boot reminder restoration runs off the broadcast main thread. Background task completion errors do not crash the receiver coroutine.
- Summary widgets show an explicit read-failure state rather than retaining an old finance summary when loading fails; finance stays hidden if security cannot be read.
- Legal-document/personal-image/boot loading use explicit remembered state plus lifecycle effects, resolving Compose lint producer-analysis findings without disabling lint.

## Backup and explicit recovery

The existing format, allowlist, checksums, confirmation token and rollback journal are retained. Exports reject a stale UI database snapshot. Known preference keys are checked for the types their readers expect, preventing a checksummed archive from committing incompatible settings types.

Ordinary restore refuses a failed current database. The boot error screen now offers a separate recovery flow: existing security must be readable and satisfied, the selected backup is inspected and validated, replacement requires explicit confirmation, and exact raw preferences are preserved in a private `neo41-before-recovery-<UUID>.json` file before replacement. Failed quarantine creation blocks replacement. A JSON-only recovery cannot retain an unreadable legacy store and claim full recovery.

Security credentials, device keys and OAuth tokens remain excluded from portable backups. The original archive format remains unencrypted and is labeled accordingly. Quarantine files remain private and are not automatically deleted. Process-death/power-loss and recovery UX still require physical-device acceptance.

## Verification and handoff

See QA_REPORT_4.1.md for exact final commands/results, historical failed attempts and unperformed checks; CHANGED_FILES_4.1.json for paths/status/line counts/hashes; INSTALL_4.1.md for signing and data-preserving upgrade instructions; REMAINING_ISSUES_4.1.md for limitations. No zero-bug or production-install compatibility claim is made.
