# v45 specification execution

Baseline: 7887df97526ee73b8829be5b65a9de1227d8693e on the preserved v44 branch.
Implementation: feat/v45-complete-specification. main and v44 are not modified.
Approved later decisions override older handoff text: receipts automatically link to a mandatory selected account; quiet hours deliver silently; independent widget settings; attachments/PDF and Drive authentication remain deferred.

| Area | Implementation evidence | Acceptance evidence |
|---|---|---|
| Five destinations / shared Salary | V11Home, EmbeddedCashBookScreen | Compilation; device navigation review remains broader than smoke |
| Calendar month/week/day, seven rows/columns, coverage | NeoCalendar, NeoCalendarPresentation | Existing calendar presentation suite |
| Work rates and fixed salaries | NeoRatePolicy, V11FinanceEngine | FixedSalaryIntegrityTest, NeoRatePolicyTest |
| Tasks history and completed styling | TaskCompletionV11, TaskCardV11, widget projection | Existing completion/widget suites |
| Rich note formatting / checklists / undo-redo | NeoRichNote, NeoDayNoteDialog | Compilation; emulator exercises editor autosave, not every formatting gesture |
| Note automatic committed save, draft recovery | NeoDayNoteDialog, NeoNoteDraftStore | NoteTransactionTest; emulator autosave and process restart |
| Locked note/linked-task contents at rest | NoteContentCipher, V11Storage | Existing NoteTransactionTest extended; emulator Keystore migration |
| Hide independent from lock | NeoDayNoteDialog, NeoPremiumTasks | Code inspection; device privacy acceptance remains necessary |
| Note colors/default, tags, pinned/archive/trash/bulk | NeoPremiumTasks, NeoLaunchPreferencesUi | Compilation; not an exhaustive gesture acceptance |
| Optional note calendar/date link | DayItemV11.noteCalendarLinked, NeoDayNoteDialog, calendar projections | NoteTransactionTest: unlink retains ID, text and reminder after reload |
| Stable checklist task identity, existing-task link/unlink | NeoDayNoteDialog, Neo4LocalBackup.saveNote | NoteTransactionTest extended |
| Full/partial/multi-month receipts and actual date | NeoReceiveEntitlements, NeoPaymentForm | SalaryReceiptOperationsTest |
| Workplace statement receive above CSV | V11FinanceUi, NeoModernHub | Compilation; financial remaining calculation test |
| Cash Book idempotency/corrections/reversals | SalaryCashSync, Ledger, SalaryIncomeLedgerBridge | Core suites and SalaryReceiptOperationsTest |
| Accounts vs debts | CashBookRoute | Preserved v44 behavior; not device-wide acceptance |
| Theme Studio / language / directions | NeoAppearanceStudio, NeoLanguageSettings | Existing code preserved; emulator Arabic small-screen capture |
| Optional independent section security, including Tasks-only / relock | V11SecurityStore, V11Home, note/task gates | Compilation; biometrics and all entry paths not physically accepted |
| Weekly selected days / quiet interval / category controls | ReminderPolicy, NeoNotificationPolicy, NeoRuntimeControls | ReminderDeliveryPolicyTest extended |
| Independent widgets, finance destination, live refresh | Neo4WidgetSettings, Neo4SummaryWidgets, EmbeddedCashBookScreen | Existing widget suites; launcher resizing remains device acceptance |
| Contextual search / default Tasks-Notes / launcher requests | NeoModernHub, NeoNavigation, NeoLaunchPreferencesUi, NeoShortcutControls | Compilation; launcher pinning depends on launcher |
| Encrypted full/selective merge/replace and local safety copies | Neo4LocalBackup, EncryptedBackup, Neo4SafetyCopy | Existing restore/recovery suites; safety test now opens sealed copies |
| Update without clearing data | tools/v45_device_smoke.py | GitHub Android 35 emulator job when successful |
| Signed release APK | .github/workflows/v45-complete.yml | Actual signature, package/version/alignment checks |

Code presence, passing unit tests, emulator acceptance, and physical-device acceptance are different states. Do not present this document or an APK build as proof that every gesture, launcher, biometric sensor or Android version has been validated. No new features outside the supplied specification are introduced.
