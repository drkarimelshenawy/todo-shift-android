# ToDo Shift — Neo Luxury 4.1 Ultimate Fixed

Android application, test candidate **4.2.0 (31)**, package **com.todoshift.app**.

Current delivery: [implementation](docs/ux-rescue/IMPLEMENTATION_REPORT.md), [130-item scope matrix](docs/ux-rescue/TRACEABILITY.md), [QA](docs/ux-rescue/QA_REPORT.md), [installation/signing](docs/ux-rescue/INSTALL.md), and [remaining acceptance](docs/ux-rescue/REMAINING.md). Full closed-scope acceptance is not claimed. Physical-device verification remains open. Root-level 4.0/P0/4.1.0 reports are historical and do not describe the current candidate. The rescue is delivered only on the feature branch; no release or merge is performed.

The existing Classic/Neo experience preference is preserved. Select **Neo Luxury** in Settings → Appearance & Design to use the upgraded Neo interface if Classic is currently selected. Today now lives in Tasks; calendar “Today” still navigates to today's date.

## Build and verify

Use Java 17, Gradle 8.9, Android SDK platform 35 and Android build tools. This repository does not contain a Gradle wrapper. Configure `ANDROID_HOME` for your SDK. For the signed debug APK, supply the existing `TODOSHIFT_SIGNING_FILE` and `TODOSHIFT_SIGNING_PASSWORD` environment variables securely; the keystore alias remains `todoshift`. No credentials are bundled.

```sh
python3 tools/verify_neo41_source.py
gradle testDebugUnitTest lintDebug assembleDebug
bash tools/run_neo_checks.sh  # requires Kotlin compiler 2.0.21 and Java
```

The 4.1 source verifier uses checked-in hashes from authorized baseline `f71bf40`, so it also works from a source ZIP. The older `verify_neoluxury4_source.py` pins an earlier 4.0 inventory/build and intentionally reports authorized 4.1 differences; do not interpret that historical hash check as current runtime verification.

**Signing matters:** a newly signed local test build cannot replace an existing differently signed installation. Do not uninstall or clear storage to bypass that incompatibility. Read the installation guide before using the APK on a device containing real data.
