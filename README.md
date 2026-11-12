# ToDo Shift

Current repair candidate: **4.4.0-premium-ux-test / versionCode 38**, package **com.todoshift.app**, on `fix/premium-financial-restore`.

- [Validated APK and Android update evidence](https://github.com/drkarimelshenawy/todo-shift-android/actions/workflows/premium-device-validation.yml): choose the latest successful run, download **Premium-UX-vc38-VALIDATED**, extract it, then open **candidate-apk/ToDoShift-4.4.0-premium-ux-test-vc38.apk**.
- [Source tests, lint and signed builds](https://github.com/drkarimelshenawy/todo-shift-android/actions/workflows/financial-restore-validation.yml?query=branch%3Afix%2Fpremium-financial-restore)
- [Changes and phone test checklist](docs/premium/UX_4_4_ACCEPTANCE.md)
- [Installation and identity](docs/premium/INSTALL.md)

Financial restore validation checks tests and Android lint before building the APK and verifying its actual package/version/certificate/source and alignment. Premium UX device validation then tests the exact immutable vc38 APK against vc37 on Android 35. It checks the earlier source gates, the APK checksum/identity and application-tree equality before testing, so automation-only changes can be checked without replacing the tested binary. Its validated artifact is uploaded only after the smoke passes and includes the same APK, identity/checksums, screenshots/logs and both source commits. An uploaded signing artifact alone does not imply device validation passed.

Calendar cell sizing and day taps, calendar widget layout/opacity, completed-task styling, task filters/order/density, shared task editing, and template management/use are covered by this candidate. Existing Classic/Neo preference is retained; select **Neo Luxury** in Appearance & Design if Classic is selected. Today lives in Tasks; calendar Today navigates to the date. Cash Book remains separate.

This is a test candidate on the repair branch. Earlier P01, recovery and ux-rescue reports describe historical checkpoints. Physical-phone/launcher acceptance and release approval remain separate; no protected-branch merge or release publication is included.

## Build and verify

Use Java 17, Gradle 8.9 and Android SDK/build tools 35. This repository has no Gradle wrapper. Signed debug builds use the existing secure `TODOSHIFT_SIGNING_FILE` and `TODOSHIFT_SIGNING_PASSWORD` inputs with alias `todoshift`; no credentials are bundled.

```sh
gradle --no-daemon -I tools/premium_qa.init.gradle testDebugUnitTest lintDebug
gradle --no-daemon assembleDebug
```

The older source-hash verifiers pin historical versions and are not current runtime acceptance gates.
