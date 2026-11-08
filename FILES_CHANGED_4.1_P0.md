# Files changed — 4.1 P0

Status: implementation files complete; execution evidence is in QA_RESULTS_4.1_P0.md. Device behavior is untested. No unrelated feature work was implemented.

| File | Focus |
|---|---|
| app/src/main/java/com/todoshift/app/storage/StorageRead.kt | Explicit missing/loaded/failed outcomes and throwing compatibility adapter |
| app/src/main/java/com/todoshift/app/storage/StorageValidation.kt | JSON structure/schema helpers |
| app/src/main/java/com/todoshift/app/data/AppStorage.kt | Safe legacy reads, initialization, guarded writes and validation |
| app/src/main/java/com/todoshift/app/v11/V11Storage.kt | Version-aware load validation, guarded saves and durable migration marker |
| app/src/main/java/com/todoshift/app/v11/V11Runtime.kt | Stop failed startup/migration; injectable IO boundaries for tests |
| app/src/main/java/com/todoshift/app/v11/ui/V11Home.kt | Successful-save-before-state ordering; boot guard documentation |
| app/src/test/java/com/todoshift/app/storage/SafePersistenceTest.kt | Synthetic parser, startup, migration, restart and failure regression tests |
| app/build.gradle.kts | JVM test dependencies only: JUnit 4.13.2 and org.json 20240303 |
| IMPLEMENTATION_REPORT_4.1_P0.md | Single cumulative report |
| QA_RESULTS_4.1_P0.md | Verification evidence |
| FILES_CHANGED_4.1_P0.md | This scope index |
| KNOWN_ISSUES_4.1_P0.md | Limitations and deferred work |

Application ID, signing configuration, database models/output keys, backup formats, calendar rendering and paint logic are unchanged. No ZIP archives generated.
