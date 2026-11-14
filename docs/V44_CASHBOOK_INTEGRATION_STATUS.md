# v44 execution checkpoint

Branch: `feat/v44-cashbook-integration`. Implementation commit: `1c25a59` (current HEAD also includes this checkpoint).

Completed: five destinations; Calendar/work locations/history; Tasks and rich Markdown Notes with encrypted autosaved drafts, explicit record Save and protection; unified Salary and idempotent Cash Book receipts/corrections/reversals; ordinary accounts/debt separation; Theme Studio; Arabic/English/RTL; reminders, widgets and shortcuts; optional security; encrypted selective/full backup, inspected merge/replace, safety snapshots and rollback.

Last successful checks: Actions [43628815185](https://github.com/drkarimelshenawy/todo-shift-android/actions/runs/43628815185) on `6d7b659` passed all nine core executable suites, selected Android unit suites, release build, signing/package/version/alignment verification and direct APK publication. Application and test Kotlin compilation also passed locally. No emulator used.

Next/final execution: build the final HEAD containing the Cash Book toolbar correction, verify its APK and download it. Exact source SHA and signature are recorded in each run's `BUILD-IDENTITY.json` artifact; use the newest passing branch run.

Blockers: none for CI/signing. Physical-device acceptance is unverified: update installation/certificate of the user's installed app, RTL/editor behavior, receipt lifecycle UI, biometrics/relock, alarms/snooze, widget resize/instances, launcher pinning, Keystore draft recovery and encrypted restore. No Android-version/device compatibility claim is made.

Tools: `/workspace/android-tools` (JDK17/Gradle8.9/SDK35); local compile log `/tmp/v44-compile5.log`.
