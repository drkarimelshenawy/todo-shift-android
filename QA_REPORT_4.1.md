# QA report — ToDo Shift 4.1.0 (29)

## Scope and environment

Baseline: `f71bf40a87aa5df7cc10da7085b3e44ac196dad0`. Feature branch only. Host tools were installed in `/workspace/work/toolchain`: Java 17 (Temurin), Gradle 8.9, Kotlin compiler 2.0.21, Android SDK 35 and build tools 34.0.0/35.0.0. The Android Gradle plugin selects build tools 34.0.0. SDK downloads were from Google's official repository, Gradle from its official distribution, and Kotlin/JDK from their official distributions. The build retains compile/target SDK 35 and minimum 26.

No test touched a connected user's application storage. JVM tests use synthetic in-memory preferences; Robolectric tests use isolated temporary Android application sandboxes and synthetic records/images.

## Final commands

The signing environment points to a separately generated **local test** keystore, alias `todoshift`; its private key is not in the delivery. Production credentials were unavailable and unchanged. SDK/proxy/user-home paths below are environment-specific and are not hardcoded into application source.

```sh
JAVA_HOME=/workspace/work/toolchain/jdk17 \
ANDROID_USER_HOME=/workspace/work/toolchain/android-user \
ANDROID_HOME=/workspace/work/toolchain/android-sdk \
GRADLE_USER_HOME=/workspace/work/toolchain/gradle-home \
/workspace/work/toolchain/gradle-8.9/bin/gradle --no-daemon --continue \
  -I /workspace/work/toolchain/test-proxy.init.gradle \
  :app:testDebugUnitTest :app:lintDebug :app:assembleDebug

python tools/verify_neo41_source.py
JAVA_HOME=/workspace/work/toolchain/jdk17 \
PATH=/workspace/work/toolchain/kotlinc/bin:$PATH bash tools/run_neo_checks.sh
git diff --check

/workspace/work/toolchain/android-sdk/build-tools/34.0.0/apksigner \
  verify --verbose --print-certs app/build/outputs/apk/debug/app-debug.apk
/workspace/work/toolchain/android-sdk/build-tools/34.0.0/aapt \
  dump badging app/build/outputs/apk/debug/app-debug.apk
/workspace/work/toolchain/android-sdk/build-tools/34.0.0/aapt \
  dump permissions app/build/outputs/apk/debug/app-debug.apk
```

The local test-only signing variables `TODOSHIFT_SIGNING_FILE` and `TODOSHIFT_SIGNING_PASSWORD` were supplied for the Gradle command. The external init script forwards this cloud environment's proxy/CA settings to test JVMs and places Robolectric's user cache under writable `/workspace/work/toolchain/test-home`. It does not disable tests or alter app signing configuration. Normal developer/CI machines need neither those paths nor this proxy init script.

## Results

| Check | Result |
|---|---|
| Kotlin/Java compilation and Android resource linking | PASS |
| `testDebugUnitTest` | PASS — 66 tests, zero failures/errors/skips |
| `lintDebug` | PASS — zero errors, 45 warnings |
| `assembleDebug` | PASS — locally signed installable debug APK |
| `verify_neo41_source.py` | PASS — 12 P0/business-rule files byte-identical to baseline; application/manifest/signing identity and brand archives preserved; version increment, XML and lexical checks pass |
| `run_neo_checks.sh` | PASS — overnight/24-hour shifts, visibility, immutability, overlap, finance, palettes and reduced motion |
| `git diff --check` | PASS |
| APK ZIP alignment | PASS — `zipalign -c -P 16 4` on delivered APK |
| APK signature | PASS — APK v2 signing; one RSA-3072 local-test signer; minimum SDK 26 supports this scheme |
| Package metadata | PASS — `com.todoshift.app`, version name `4.1.0`, code `29`, min SDK 26, target 35 |
| Delivered archive and artifact hashes | Recorded in `SHA256SUMS.txt`; ZIP readability checked during packaging |

## Test distribution

- `SafePersistenceTest`: **33**. Includes all existing 31 P0 cases plus stale-snapshot rejection and confirmed-snapshot preservation. Existing malformed/unknown schema, missing storage, migration, commit failure, identity and financial payload tests remain.
- `NeoCalendarPresentationTest`: **11**. Single/OFF/multiple shifts, all-visible-calendar aggregation, hidden/global task/event policy, paint owners, leap-year/year-boundary clamping, localized month names, RTL/LTR swipe direction, referenced-calendar protection.
- `BackupRecoveryTest`: **9**. Exact confirmation token, checksums, explicit corrupt-store recovery with raw quarantine, interrupted-journal recovery, secret exclusion, stale export rejection, record/time identity round trip, preference-type rejection despite valid checksums, primary selection without database mutation.
- `CalendarUiRegressionTest`: **5**. Rendered Compose assertions under Robolectric for a single shift with decorative options disabled, OFF marker, visibility toggles, Arabic direction/month labels and large text.
- `ScreenUiSmokeTest`: **7**. Tasks/Today integration and scrolling, protected finance, preview/discard persistence, base contrast across palettes and brightness modes, invalid security mode, PIN/protection persistence and failed-boot raw-data preservation.
- `PhotoImportTest`: **1**. Valid synthetic photo import/load; protects the discovered bounds-decode-null regression.

These are JVM/Robolectric/Compose tests, not instrumentation tests running on a physical Android device. Assertions on UI semantics/layout direction are not a pixel-perfect visual review of every screen.

## Lint warnings reviewed

No lint baseline or broad suppression was introduced. Remaining warnings:

- 10 unused resources retained for compatibility/historical code.
- 8 API-31 widget attributes with existing minimum-size fallbacks for older supported devices.
- 6 large vector background intrinsic sizes (existing illustrations).
- 5 newer dependency versions available; broad dependency upgrades were not required for this release.
- 5 legacy launcher shape advisories and 5 identical legacy round/square icon advisories. Adaptive icons and monochrome resources are provided separately; real launcher-mask testing remains pending.
- 3 redundant SDK checks retained in compatibility code.
- 2 modifier-parameter naming/order advisories in existing Classic rendering.
- 1 widget-background overdraw advisory; RemoteViews launcher rendering is not the activity's ordinary root background.

Existing RemoteViews API deprecations remain for supported older Android versions. The compiler may report older icon API usage in historical code. Build tooling also reports that its analytics home is read-only and that the manually installed extension-platform path differs from its package name; AGP installed/resolved the regular SDK 35 platform. These are environment warnings, not claims of runtime faults. One native library contains no removable debug symbols and is packaged unchanged.

## Repair-loop history

Intermediate failures were retained in the evidence bundle:

1. First environment build could not create Android tool state in read-only `/home/agent/.android`. Tool/user caches were redirected to workspace locations where supported.
2. A build configured before test dependencies were added failed to resolve Robolectric symbols. Reconfiguration resolved it.
3. Robolectric initially tried to create its download lock under the read-only home. Its test JVM `user.home` was redirected via the external init script; the full suite then ran.
4. Building without production credentials reached packaging and correctly failed because `persistentDebug.storeFile` was absent. An isolated local test key was generated for the test APK; the production signing block was not edited.
5. An integrated Tasks test expected one literal “Today” node but correctly found both dashboard and filter labels. The assertion now targets the dashboard's unique accessibility control.
6. Initial lint found ten errors: producer-state analysis findings, an unused constraints scope, and missing English metadata translations. State loading was clarified and translations completed; lint errors were fixed without suppression. The audit also fixed the independent photo import bug with regression coverage.
7. A refactor left three obsolete `quickTask` assignments; compilation caught them and they were replaced with the consumed-action state.
8. The final icon-resource rebuild incorporates safe adaptive/splash insets; the final command validates the actual delivered resource set.

The old `python tools/verify_neoluxury4_source.py` was also run: **FAIL as a historical hash/inventory check**, because it pins a pre-P0/4.0 snapshot, immutable old version/dependencies and old icon bytes. Its raw output is included; it was not rewritten to conceal authorized differences. `verify_neo41_source.py` is the explicitly versioned replacement for current source safety checks. It preserves baseline hashes for P0 and business rules rather than requiring every old UI asset to remain identical.

## Not performed

- Physical-device/emulator installation and production-signature upgrade.
- Actual user-data upgrade, restore or migration on a user's installation.
- Hardware-backed biometric/device-credential tests.
- Power-loss or real disk-full/failed-commit durability tests.
- OEM launcher masks, Android 12+ splash animation/timing and notification delivery.
- Real reboot/task reminder survival and widget host sizing/refresh acceptance.
- Pixel screenshots across the full device/font/orientation/theme matrix.
- Performance benchmarks, battery profiling, accessibility service/TalkBack manual review or store/legal certification.

The available adb binary failed while creating its default home state in a read-only directory; setting Android cache-location variables did not make that binary enumerate devices. Probes found no configured emulator binary, `/dev/kvm` or USB device interface. No install/uninstall/storage-clear command was issued. This limitation was not treated as permission to modify the user's home directory or real application data.
