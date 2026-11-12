# ToDo Shift

Current repair candidate: **4.4.0-premium-ux-test / versionCode 38**, package **com.todoshift.app**, on `fix/premium-financial-restore`.

- [Builds and APK downloads](https://github.com/drkarimelshenawy/todo-shift-android/actions/workflows/financial-restore-validation.yml?query=branch%3Afix%2Fpremium-financial-restore): choose the latest successful **push** run. Download **Premium-Repair-PERSISTENT-SIGNED-vc38**, extract it, then open **ToDoShift-4.4.0-premium-ux-test-vc38.apk**.
- [Changes and phone test checklist](docs/premium/UX_4_4_ACCEPTANCE.md)
- [Installation and identity](docs/premium/INSTALL.md)

The workflow checks tests and Android lint before building the APK, verifies its actual package/version/certificate/source and alignment, and runs an Android 35 update/startup smoke against candidate 37. Its artifacts include the APK, identity/checksums, QA output and device-smoke evidence. Check all jobs on the chosen run; an uploaded APK alone does not mean every later gate passed.

Calendar cell sizing and day taps, calendar widget layout/opacity, completed-task styling, task filters/order/density, shared task editing, and template management/use are covered by this candidate. Existing Classic/Neo preference is retained; select **Neo Luxury** in Appearance & Design if Classic is selected. Today lives in Tasks; calendar Today navigates to the date. Cash Book remains separate.

This is a test candidate on the repair branch. Earlier P01, recovery and ux-rescue reports describe historical checkpoints. Physical-phone/launcher acceptance and release approval remain separate; no protected-branch merge or release publication is included.

## Build and verify

Use Java 17, Gradle 8.9 and Android SDK/build tools 35. This repository has no Gradle wrapper. Signed debug builds use the existing secure `TODOSHIFT_SIGNING_FILE` and `TODOSHIFT_SIGNING_PASSWORD` inputs with alias `todoshift`; no credentials are bundled.

```sh
gradle --no-daemon -I tools/premium_qa.init.gradle testDebugUnitTest lintDebug
gradle --no-daemon assembleDebug
```

The older source-hash verifiers pin historical versions and are not current runtime acceptance gates.
