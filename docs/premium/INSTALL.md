# Install the current repair candidate

Candidate **4.4.0-premium-ux-test**, Android versionCode **38**, package **com.todoshift.app**.

1. Open [Financial restore validation](https://github.com/drkarimelshenawy/todo-shift-android/actions/workflows/financial-restore-validation.yml?query=branch%3Afix%2Fpremium-financial-restore).
2. Choose the latest successful **push** run on `fix/premium-financial-restore`; check verify, signed-test-apk and emulator-smoke.
3. In Artifacts, download **Premium-Repair-PERSISTENT-SIGNED-vc38**. GitHub may require sign-in.
4. Extract the ZIP and install **ToDoShift-4.4.0-premium-ux-test-vc38.apk** as an update, retaining app data. Keep an exported backup before phone testing.
5. Confirm the installed version is **4.4.0-premium-ux-test (38)**. Use [the checklist](UX_4_4_ACCEPTANCE.md) for the changed screens.

The workflow derives the APK filename and artifact name from the **verified APK**, rather than a static old label. The archive includes `SHA256SUMS` and `identity/BUILD-IDENTITY.json` with the exact source SHA and Android package/version. Select vc38 rather than an older vc35 artifact.

Persistent CI certificate SHA-256:

`aecf41e372d7204dacf6ebb93f02414b265c8d97fd5508023287aa2fc6da6ea9`

The signing verification fails before upload if that certificate, package, version or source identity differs. Android still checks compatibility with the installed APK: if it rejects the update, retain the existing installation/data and report the installer message; do not uninstall or clear storage.

The Android 35 smoke installs vc37, creates a real task through the UI, updates in place to vc38, checks fixture data/IDs, opens the editor, completes/reopens the task and restarts three times. Its screenshots, XML, result and logcat are in **Android-35-update-startup-smoke**. This is emulator evidence; the user's actual phone and launcher still need the checklist.
