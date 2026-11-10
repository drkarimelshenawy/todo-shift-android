# Changed files since 4.1.1 CI baseline

Source stages plus delivery documentation/workflow. No APKs, keys or generated archives tracked.

```text
M	app/build.gradle.kts
M	app/src/main/java/com/todoshift/app/backup/Neo4LocalBackup.kt
M	app/src/main/java/com/todoshift/app/reminders/ToDoShiftReminder.kt
M	app/src/main/java/com/todoshift/app/v11/NeoAppearance.kt
M	app/src/main/java/com/todoshift/app/v11/NeoAppearanceStore.kt
M	app/src/main/java/com/todoshift/app/v11/NeoAttachmentStore.kt
A	app/src/main/java/com/todoshift/app/v11/NeoWorkplaceSources.kt
M	app/src/main/java/com/todoshift/app/v11/V11UiSettingsStore.kt
M	app/src/main/java/com/todoshift/app/v11/V11UiState.kt
M	app/src/main/java/com/todoshift/app/v11/ui/Neo4Shell.kt
M	app/src/main/java/com/todoshift/app/v11/ui/Neo4StudioUi.kt
M	app/src/main/java/com/todoshift/app/v11/ui/NeoAppearanceStudio.kt
M	app/src/main/java/com/todoshift/app/v11/ui/NeoCalendar.kt
A	app/src/main/java/com/todoshift/app/v11/ui/NeoCalendarManager.kt
M	app/src/main/java/com/todoshift/app/v11/ui/NeoDayNoteDialog.kt
M	app/src/main/java/com/todoshift/app/v11/ui/NeoDesignSystem.kt
M	app/src/main/java/com/todoshift/app/v11/ui/NeoFinanceForms.kt
M	app/src/main/java/com/todoshift/app/v11/ui/NeoModernHub.kt
M	app/src/main/java/com/todoshift/app/v11/ui/NeoModernPreferences.kt
M	app/src/main/java/com/todoshift/app/v11/ui/NeoMotion.kt
M	app/src/main/java/com/todoshift/app/v11/ui/NeoPainterBar.kt
A	app/src/main/java/com/todoshift/app/v11/ui/NeoRescueComponents.kt
A	app/src/main/java/com/todoshift/app/v11/ui/NeoRescueSchedules.kt
A	app/src/main/java/com/todoshift/app/v11/ui/NeoRescueSettings.kt
M	app/src/main/java/com/todoshift/app/v11/ui/NeoRuntimeControls.kt
M	app/src/main/java/com/todoshift/app/v11/ui/NeoSections.kt
M	app/src/main/java/com/todoshift/app/v11/ui/NeoTodayDecoration.kt
M	app/src/main/java/com/todoshift/app/v11/ui/V11CalendarUi.kt
M	app/src/main/java/com/todoshift/app/v11/ui/V11DayDetailsUi.kt
M	app/src/main/java/com/todoshift/app/v11/ui/V11FinanceUi.kt
M	app/src/main/java/com/todoshift/app/v11/ui/V11Home.kt
M	app/src/main/java/com/todoshift/app/v11/ui/V11MoreUi.kt
M	app/src/main/java/com/todoshift/app/v11/ui/V11SchedulesUi.kt
M	app/src/main/java/com/todoshift/app/v11/ui/V11TasksUi.kt
M	app/src/main/java/com/todoshift/app/v11/ui/V11Theme.kt
M	app/src/main/java/com/todoshift/app/v11/ui/V11WorkplacesUi.kt
M	app/src/main/res/values-en/strings.xml
M	app/src/main/res/values/strings.xml
M	app/src/test/java/com/todoshift/app/v11/ModernSafetyTest.kt
A	app/src/test/java/com/todoshift/app/v11/NeoRescueIntegrityTest.kt
A	app/src/test/java/com/todoshift/app/v11/NeoWidgetIntegrationTest.kt
M	app/src/test/java/com/todoshift/app/v11/ui/ModernUiIntegrationTest.kt
A	app/src/test/java/com/todoshift/app/v11/ui/NeoRescueUiTest.kt
A	docs/ux-rescue/ARCHITECTURE.md
M	tools/verify_neo41_source.py
A	tools/visual-tests/NeoVisualProbeTest.kt
A	tools/visual_capture.init.gradle
```

Delivery stage adds the reports in this directory and updates README and the existing manual CI workflow.
