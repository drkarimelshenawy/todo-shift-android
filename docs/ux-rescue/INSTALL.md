# Download and installation safety — 4.2.0 (31)

Package com.todoshift.app; test candidate only. The final report identifies the exact feature-branch CI run and APK/QA/visual-diagnostic artifact names. GitHub Actions artifacts are downloadable after signing in with repository access: open that run, scroll to Artifacts, select the APK artifact, and extract its ZIP in Android Files. Chrome's Desktop site may help if the artifact list is not visible. Do not use historical 4.1.0/4.1.1 local delivery files as this candidate.

The CI APK must verify against SHA-256 certificate aecf41e372d7204dacf6ebb93f02414b265c8d97fd5508023287aa2fc6da6ea9; the workflow fails if it does not. The locally built test APK uses a257d6f5be339f6f317d58d641539ce605d61d9a8d822e76d4d58f974e98ba02. These keys are different. Neither is assumed to identify the app installed on the user's phone.

Before an in-place update, inspect the installed com.todoshift.app signing-certificate SHA-256 using a trusted installed-APK inspector, or read/pull its base APK with adb and run apksigner verify --print-certs on that copy. This is read-only; keep the application installed and its storage intact. Compare the full certificate digest, not package name, version label, key alias or a production-labelled key. An inspected backup should be kept before an actual upgrade.

If the installed certificate matches the CI certificate, the higher versionCode permits the normal Android Update path, subject to device policy and actual upgrade testing. If it differs, stop and obtain a build signed by the installed key. Do not uninstall, clear storage or bypass signature checks. No installation or real-data restore was performed in this task.

The APK artifact contains SOURCE-COMMIT.txt, package/signature evidence and SHA256SUMS. Verify checksums before installation. QA/visual artifacts document verification limits; rendered diagnostics are not device acceptance.

If an explicit Classic choice was saved, it remains selected. Use Settings → Appearance and customization → App experience → Neo Luxury to enter the new experience; no data or preferences need to be reset.
