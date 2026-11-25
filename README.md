# ExMore

Official identity release candidate: **4.4.2 / versionCode 81**, package **com.exmore.app**, on `feat/exmore-official-identity-migration`.

- [Validated APK and Android update evidence](https://github.com/drkarimelshenawy/todo-shift-android/actions/workflows/premium-device-validation.yml): choose the latest successful run, download **Premium-UX-vc38-VALIDATED**, extract it, then open **candidate-apk/ToDoShift-4.4.0-premium-ux-test-vc38.apk**.
- [Source tests, lint and signed builds](https://github.com/drkarimelshenawy/todo-shift-android/actions/workflows/financial-restore-validation.yml?query=branch%3Afix%2Fpremium-financial-restore)
- [Changes and phone test checklist](docs/premium/UX_4_4_ACCEPTANCE.md)
- [Installation and identity](docs/premium/INSTALL.md)

Financial restore validation checks tests and Android lint before building the APK and verifying its actual package/version/certificate/source and alignment. Premium UX device validation then tests the exact immutable vc38 APK against vc37 on Android 35. It checks the earlier source gates, the APK checksum/identity and application-tree equality before testing, so automation-only changes can be checked without replacing the tested binary. Its validated artifact is uploaded only after the smoke passes and includes the same APK, identity/checksums, screenshots/logs and both source commits. An uploaded signing artifact alone does not imply device validation passed.

Calendar cell sizing and day taps, calendar widget layout/opacity, completed-task styling, task filters/order/density, shared task editing, and template management/use are covered by this candidate. Existing Classic/Neo preference is retained; select **Neo Luxury** in Appearance & Design if Classic is selected. Today lives in Tasks; calendar Today navigates to the date. Cash Book remains separate.

This is the independent ExMore package. The former `com.todoshift.app` installation is intentionally retained during migration. Export a complete encrypted backup from the former app, inspect it in ExMore, restore with Replace on the fresh ExMore installation, verify all data and attachments, and only then decide whether to remove the former app.

## Build and verify

Use Java 17, Gradle 8.9 and Android SDK/build tools 35. This repository has no Gradle wrapper. Signed debug builds use the existing secure `TODOSHIFT_SIGNING_FILE` and `TODOSHIFT_SIGNING_PASSWORD` inputs with alias `todoshift`; no credentials are bundled.

```sh
gradle --no-daemon -I tools/premium_qa.init.gradle testDebugUnitTest lintDebug
gradle --no-daemon assembleDebug
```

The older source-hash verifiers pin historical versions and are not current runtime acceptance gates.
