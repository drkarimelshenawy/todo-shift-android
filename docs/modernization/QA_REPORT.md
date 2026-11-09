# QA report — 4.1.1 (30)

This report distinguishes passing automated checks from unperformed acceptance. No user device or installed application was modified. The build uses a separate local test signing identity; no production signing credentials are in the source or delivery archive.

## Final command environment

Repository `/workspace/todo-shift-android`. Java 17, Gradle 8.9, Android compile SDK 35. `JAVA_HOME=/workspace/work/toolchain/jdk17`, `ANDROID_USER_HOME=/workspace/work/toolchain/android-user`, `ANDROID_HOME=/workspace/work/toolchain/android-sdk`, `GRADLE_USER_HOME=/workspace/work/toolchain/gradle-home`. Existing signing environment variables select the local test key outside the repository; values are deliberately not recorded here.

Commands executed:

```sh
/workspace/work/toolchain/gradle-8.9/bin/gradle --no-daemon --continue \
  -I /workspace/work/toolchain/test-proxy.init.gradle \
  :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
python tools/verify_neo41_source.py
PATH=/workspace/work/toolchain/kotlinc/bin:/workspace/work/toolchain/jdk17/bin:$PATH \
  JAVA_HOME=/workspace/work/toolchain/jdk17 bash tools/run_neo_checks.sh
git diff --check
```

The external init script forwards configured proxy/trust-store JVM properties to Robolectric, uses Maven Central for its Android runtime dependency, sets a writable test home, and caps test heap at 1536 MiB. It does not change application code or signing. Standard developer machines can use their ordinary Gradle environment without this cloud-only init script.

## Results

Final machine-readable results and APK verification are in DELIVERY_VERIFICATION.json in this directory. Gradle logs and JUnit XML are packaged separately in the delivery bundle. The expanded suite contains 111 tests, including 33 existing P0 persistence tests, 9 backup recovery tests, 5 dated-rate/paint safety tests and 3 safety-copy/notification-policy tests. All must show zero failures/errors/skips in final verification before packaging.

Static source protection passes: eleven protected files unchanged, package/signing/launcher checks, version increment, Kotlin lexical scan, XML parse and unchanged brand archives. Standalone Kotlin checks pass overnight/24h, visibility, immutability, overlap, finance, palettes and reduced motion. These checks are not substitutes for Android runtime testing.

Lint has no errors. Remaining warnings include unused/variant resources and attributes, vector rasterization, dependency suggestions, legacy launcher shape/duplicates, obsolete API guards, legacy modifier naming and possible widget overdraw. They are recorded in the raw lint XML/HTML rather than suppressed broadly. Build logs may mention the read-only home analytics directory and duplicate SDK platform registration; these did not prevent build/test completion. A legacy Notes icon deprecation remains.

## Repair history

Intermediate runs failed and were repaired: stale tests assumed notes counted as events and Arabic language forced UI direction; API compatibility of BigInteger exact conversion; generic provider MIME recognition; a missing edit-obligation database argument; indentation lint; a nullable audit entity ID type and a deeply nested undo expression flagged by indentation lint. The final results, rather than earlier failed runs, govern delivery. No tests were disabled to obtain a pass. Existing expectations changed only for explicitly approved note/direction semantics.

## What is actually covered

- Fail-closed parser/storage failures, checked writes, stale snapshots, migration refusal, restore rollback/checksum/token validation and record identity.
- Month/date boundaries and localized names; visible calendars versus primary; single/OFF/multiple projection; disabled decorative paint; existing Arabic/large-font Compose regression cases.
- Approved localized money examples/invalid inputs/rounding, legacy versus explicit month allocation, cent-exact distribution/resizing, shift/day/OFF statistics and clock/band separation.
- Task-template idempotency/nonretroactivity, preference direction/motion round-trip, old preference defaults, metadata backup validation, PDF attachment round-trip, dated-rate history/future/reconciliation, paint conflict preservation, safety-copy freshness and quiet-hour deferral.
- Selected quick task/payment/note/privacy interactions, explicit payment discard confirmation and rendering of thirteen Tools screens in Robolectric. Rendering is not full end-to-end acceptance of every action.

## Not performed

No emulator/instrumented device suite, Honor testing, real signed upgrade, real user-data restore, reboot/doze/OS-kill delivery, external PDF/image viewer matrix, biometric hardware, physical widget launcher, TalkBack walkthrough, exhaustive direction/language/font/theme/screen-size matrix, sustained performance benchmark or power-loss/storage-full hardware test. See REMAINING.md for required acceptance and known limitations. Full completion is not claimed.

## Recorded final outcome

Final Gradle run: BUILD SUCCESSFUL. JUnit: {'tests': 111, 'failures': 0, 'errors': 0, 'skipped': 0}. Lint severities: {'Warning': 45}. APK signature verification and 16 KiB zip alignment: PASS. Package/version metadata: PASS. Raw final log: modernization-build-11.log.
