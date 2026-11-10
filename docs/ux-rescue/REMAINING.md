# Open acceptance and compatibility limits

Full user/product acceptance is not claimed. Passing automated tests and simulated renders do not reverse the user's rejection of 4.1.1 without a new review of this candidate.

- No emulator binary, KVM or attached USB device was available. Physical Android/Honor behavior, reboot/doze/battery-policy notification delivery, external document providers/viewers, launcher resizing, actual keyboard insets, biometric hardware, TalkBack and real upgrade are unperformed.
- Native-render diagnostics launch MainActivity in an isolated Robolectric application. They establish only the inspected rendered states. They do not prove all language/direction/theme/font/screen-size combinations or animation timing. Modal capture may be unavailable and is explicitly reported, never substituted with the main window and called a modal screenshot.
- Existing explicit Classic/custom appearance choices are retained. New-install defaults are Neo Luxury; existing users can select Neo in Appearance → Theme and identity. Profile switching deliberately preserves independent language/direction/first weekday.
- Stored database schemas/IDs/Double fields remain compatible. No speculative migration or new interest/savings ledger was introduced. Savings/subscription UI still uses compatible OTHER obligation subtype notes; existing calculations apply.
- Undo holds up to 20 successful snapshots in memory, with financial unlock and closed-month protection. It does not survive process death. Attachments/appearance are outside shift/task/finance undo.
- Drafts use Android saved state and discard guards, not a new cross-device draft database. Task restoration is tested. All process-death/interrupted-editor/provider combinations and inherited Classic-only paths need manual acceptance. Saved-state restoration does not create a saved task automatically.
- Note cosmetic metadata and database text use separate checked commits; a failed text save may leave an orphan cosmetic style. Existing records are not reset. This cross-store cosmetic atomicity limit is retained.
- Attachment support remains PNG/JPEG/PDF/UTF-8 text, 4 MiB/file and 24 attached files/24 MiB attached payload. Explicit cleanup removes only recognized, currently unreferenced private files after fresh-set confirmation. Safety archives and unknown files are retained; their storage use still needs user management.
- Copy skips conflicts after preview and never copies independent notes. New painting replaces whole conflicting segments only after confirmation; inferred partial-segment pricing is not introduced. Classic's original resolver remains available.
- Notification quiet hours are opt-in 22:00–07:00 deferral. Android ultimately controls exact alarms, notification permission, reboot/doze/battery behavior. No delivery guarantee is claimed.
- No installed-app certificate was supplied or read from a device. CI certificate identity alone does not prove upgrade compatibility. The old saved local 4.1.0 test APK has a different certificate from CI. Never uninstall/clear data to bypass a mismatch.
- No sustained real-device performance/memory benchmark or hardware power-loss/full-storage test was run. Bounded lists/attachments and runtime reduction are not proof of all workload behavior.

The complete 130-item matrix preserves open per-item manual acceptance. Settings/feature routes and functional checks are evidence of discoverability/behavior, not a blanket accepted status.
