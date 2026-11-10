# P01 current storage ownership and write boundary inventory

| Owner | Current storage | Boundary / recovery | Status |
|---|---|---|---|
| V11Storage / V11Runtime | todo_shift_v11: database, migrated_from_rc1 | strict parser, checked commit; saveIfCurrent full-snapshot CAS under Neo4DataGate; start invokes backup recovery | unchanged; SafePersistenceTest regression |
| AppStorage | todo_shift_data: database | legacy strict reads and checked writes; workplace change coordinated via Neo4LocalBackup journal | unchanged |
| NeoSmartStore | todo_shift_neo41_smart: data JSON | metadata/templates/note styles/attachment references; checked commits and shared gate | unchanged; note cross-store gap remains |
| NeoAppearanceStore | todo_shift_neo3_appearance; legacy UI fallback | appearance preferences separate from business data | unchanged |
| V11UiSettingsStore | todo_shift_v11_ui | experience/Classic compatibility | unchanged |
| Neo4Profiles | todo_shift_neo4_profiles | saved appearance profiles | unchanged |
| V11ActiveCalendarStore | see constructor / WRITE_CALLSITES | independent creation destination | unchanged |
| V11SecurityStore | private local security preferences | PIN/biometric boundaries; not exported as generic backup UI prefs | unchanged; hardware acceptance blocked |
| NeoAttachmentStore | private filesDir/attachments plus SmartStore reference metadata | type/size/path limits, shared restore gate and explicit fresh-reference cleanup | unchanged; lifecycle/fault matrix incomplete |
| Neo4LocalBackup | ZIP v11/legacy/preferences/assets plus private restore journal | confirmed preview token; checked preference rollback and boot recovery | unchanged; staged-asset D24 remains open |
| Neo4WidgetSettings / privacy | todo_shift_neo4_widgets / todo_shift_neo4_widget_privacy | RemoteViews refresh is a side effect, not data commit proof | unchanged; real launcher timing blocked |
| QuickTaskActivity / NeoTaskCommit | existing V11 task records | fresh read/CAS, stable draft ID, result-based retry/side-effect pending | changed, partial tests; full Activity faults outstanding |
| V11Home | V11Runtime + UI/appearance/legacy stores | Unit save callbacks, async UI-owned coroutine | D07 remains open; do not mark checked contract complete |

Exact source callsites are in WRITE_CALLSITES.txt, generated from current production files. The inventory includes declarations and callbacks; it is not an executed-case list. See P01_HANDOFF.md for required tests and unsatisfied gates. No schema, IDs, backups, historical money or preferences were reset.
