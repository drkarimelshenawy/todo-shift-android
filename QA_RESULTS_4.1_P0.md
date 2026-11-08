# QA evidence — 4.1 P0

Code under test: f77e4e7 (Prevent failed storage loads from overwriting shift data), based on e7ca3ba8d9b72f1d2c570d24a334117e5543107a. Verification performed 2026-11-08. Reports were added after code verification; no source edits followed the final tests/build.

## Environment verified

Temurin Java 17.0.17+10; Gradle 8.9; installed Android platform 35 and build-tools 34.0.0. Existing /workspace/.android-dev/env.sh was sourced; inherited proxy and Java CA trust were retained. No SDK download was performed. Only test dependencies and branch build dependencies were resolved as needed.

## Executed checks

| Check | Result |
|---|---|
| Remote Luxury-v4 tip and baseline ancestry | Passed; exact baseline e7ca3ba verified |
| compileDebugKotlin | Passed; existing deprecation warnings |
| Targeted SafePersistenceTest development runs | Passed; rerun after added guards/tests |
| Final testDebugUnitTest --tests com.todoshift.app.storage.SafePersistenceTest | 31 tests; 0 failures, 0 errors, 0 skipped |
| Initial combined tests + assembleDebug | Tests passed; packaging FAILED because persistentDebug lacked storeFile |
| Focused assembleDebug retry via existing signing environment variables | Passed in 13 seconds, 35 tasks (2 executed, 33 up-to-date) |
| apksigner verify app/build/outputs/apk/debug/app-debug.apk | Passed; synthetic build-only certificate |
| git diff --check | Passed |
| applicationId/signing config comparison to baseline | Unchanged; Gradle diff adds two test dependencies only |
| Development branch push | BLOCKED: existing HTTPS authentication/ GH_TOKEN binding rejected; no remote development branch created |
| Reference branch tips before delivery | Luxury-v4 e7ca3ba; main 73e898b, unchanged |

Commands, from the repository after sourcing /workspace/.android-dev/env.sh:

```sh
gradle --no-daemon --console=plain compileDebugKotlin
gradle --no-daemon --console=plain testDebugUnitTest --tests com.todoshift.app.storage.SafePersistenceTest
gradle --no-daemon --console=plain testDebugUnitTest --tests com.todoshift.app.storage.SafePersistenceTest assembleDebug
# Focused build retry: temporary synthetic keystore supplied through the EXISTING
# TODOSHIFT_SIGNING_FILE / TODOSHIFT_SIGNING_PASSWORD variables; no source override.
gradle --no-daemon --console=plain assembleDebug
"$ANDROID_HOME/build-tools/34.0.0/apksigner" verify app/build/outputs/apk/debug/app-debug.apk
```

The real signing credentials were absent. The retry used a disposable two-day RSA certificate with alias todoshift, generated outside the checkout; its keystore was deleted after the build. The APK is a development verification artifact, NOT a production-signed update and cannot be used to update an existing differently signed installation. Signing configuration was not edited.

APK SHA-256: `56ee68d06605441dfbb69ded26e060f3117b4b32aa57a4fd35f9786717e40f61`.

Local execution evidence:

- app/build/test-results/testDebugUnitTest/TEST-com.todoshift.app.storage.SafePersistenceTest.xml: final suite timestamp 2026-11-08T01:21:36 UTC, 31/0/0/0.
- app/build/reports/tests/testDebugUnitTest/index.html.
- /tmp/p0-compile.log, /tmp/p0-tests.log, /tmp/p0-verification.log (includes the signing failure), /tmp/p0-debug-build.log (successful retry).
- app/build/outputs/apk/debug/app-debug.apk: freshly generated after the retry, not the earlier main-branch artifact.

These generated files are local execution outputs, not committed user data or source archives. The committed tests and this record provide the reproducible review path.

## Coverage and status

Implemented/tested with synthetic JVM fixtures: valid V11 and intentionally empty data; missing vs failed reads; malformed JSON, arrays and nested elements; invalid preference type; unsupported/fractional schemas; unknown shift kind; orphaned calendar; no writes after failed startup or an attempted empty save; raw-payload preservation; schemas 11/12/13 including a schema-11 payment without later allocations; legacy failure and malformed nested IDs; supported legacy migration; exact six November dates; existing workplace/payment IDs; stable V11 identities after migration restart; first-install IDs; ambiguous/missing migrated stores; simulated first-install/migration/ordinary commit failures; current backup parser round-trip without writes.

Partially verified: real storage behavior is represented by an in-memory SharedPreferences double and a real JVM org.json implementation. Recovery and widget-refresh callbacks are injected no-ops. Production constructor paths compile in the Android build, but those integrations are not exercised on a device.

Untested: Android disk durability, power loss, process-death restoration, Compose boot error UI, signed production upgrade, device backup restore, and the original November incident. No emulator/instrumentation/device tests were run. Unit tests do not prove device-level persistence or reproduction/resolution of the reported incident.

Unimplemented/out-of-scope work is listed in KNOWN_ISSUES_4.1_P0.md. The single cumulative progress report is IMPLEMENTATION_REPORT_4.1_P0.md.
