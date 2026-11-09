# Known issues — 4.1 P0

Implemented: failed-load blocking and migration guards; see the cumulative implementation report.

Partially verified: parser/runtime persistence behavior is covered with synthetic in-memory preferences. Actual Android filesystem commits, process death, power loss, startup error UI and background restoration are untested. Simulated commit failure is not evidence of Android cache rollback or crash recovery.

Unimplemented/out of scope:

- The original November disappearance has not been reproduced or attributed to a specific path.
- Whole-database stale-writer/lost-update risk remains; this change adds no revision/transaction system.
- Existing automatic ordinal workplace-ID repair remains; no speculative remapping change was made.
- Theme paint visibility, hidden calendars, day-sheet remember keys and rendering behavior are unchanged.
- Existing save callbacks may block the main thread or surface an exception on commit failure; a nonblocking retry/error UX is separate work.
- Existing backup restore reads the current V11 database to cancel old reminders. It now fails safely if that current database is corrupt instead of receiving fabricated empty data. A separately tested corrupt-store recovery flow is still needed; no archive format was changed.
- First-install checks cannot distinguish genuinely new installation from external deletion of BOTH complete preference stores. The app does not delete either store here.
- Supported schemas are those evidenced in repository history (V11 11/12/13, legacy absent/9); malformed or unknown formats fail closed without modifying their payload. Real-device historical exports were not supplied.

Production signing credentials are not present in this cloud environment. The signing configuration is preserved; APK status is documented in QA_RESULTS_4.1_P0.md. No installable production-signature claim is made.

Delivery incomplete: GitHub rejected the existing authentication binding for the authorized development-branch push. Local source and reports are committed on fix/neo-luxury-4.1-data-integrity; enable repository write authentication securely before retrying. Do not provide token values in chat. Luxury-v4/main remain unchanged.
