# Install the validated UX test candidate

Candidate **4.4.0-premium-ux-test**, Android versionCode **38**, package **com.todoshift.app**.

1. Open [Premium UX device validation](https://github.com/drkarimelshenawy/todo-shift-android/actions/workflows/premium-device-validation.yml).
2. Choose the latest successful run on `fix/premium-financial-restore`.
3. Download **Premium-UX-vc38-VALIDATED** from Artifacts. GitHub may require sign-in.
4. Extract the ZIP. In **candidate-apk**, open **ToDoShift-4.4.0-premium-ux-test-vc38.apk** and install as an update, retaining app data. Keep an exported backup before phone testing.
5. Confirm the installed version is **4.4.0-premium-ux-test (38)**. Follow [the phone checklist](UX_4_4_ACCEPTANCE.md).

This archive is uploaded only after successful Android 35 smoke. It retains the exact signed APK from source `66453702296db1a650fb2de94ed3250be69d20f5` with passing tests/lint and signed-build verification. The independent device workflow verifies the prior successful source jobs, checksums, identity and equality of current application/build sources before reuse. Test-automation source may be newer and is recorded separately in the evidence. No APK identity is rewritten.

**candidate-apk/SHA256SUMS** and **candidate-apk/identity/BUILD-IDENTITY.json** identify the APK. **device-smoke** contains screenshots, XML, logcat, result and APK/test-script commits. Select vc38 rather than an older vc35 artifact.

Persistent CI certificate SHA-256:

`aecf41e372d7204dacf6ebb93f02414b265c8d97fd5508023287aa2fc6da6ea9`

The signing verification fails if the certificate, package, version or source identity differs. Android also checks the installed APK: if it rejects the update, retain the existing installation/data and report the installer message; do not uninstall or clear storage.

The Android 35 smoke installs vc37, creates a task through the UI, updates in place to vc38, checks fixture data/IDs, opens the editor, completes/reopens the task and restarts three times. This is emulator evidence; the actual phone/launcher and notification behavior still need the checklist.

Raw signed builds and unit/lint reports remain in [Financial restore validation](https://github.com/drkarimelshenawy/todo-shift-android/actions/workflows/financial-restore-validation.yml?query=branch%3Afix%2Fpremium-financial-restore).
