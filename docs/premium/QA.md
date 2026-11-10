# P01 checkpoint QA — incomplete phase, no GO

Source: 2604aa8a7d5a6b68c039d03ebff0cb07fe21a3cd. Linux cloud, Java17, Gradle8.9, SDK35; Robolectric SDK28 for Compose tests. Synthetic fixtures only. No device test, new screenshot or new signed APK is claimed.

Commands (existing writable cloud test JVM/proxy init; signing inputs intentionally not included):

```sh
export JAVA_HOME=/workspace/work/toolchain/jdk17
export ANDROID_HOME=/workspace/work/toolchain/android-sdk
export ANDROID_USER_HOME=/workspace/work/toolchain/android-user
export GRADLE_USER_HOME=/workspace/work/toolchain/gradle-home
export PATH="$JAVA_HOME/bin:$PATH"
/workspace/work/toolchain/gradle-8.9/bin/gradle --no-daemon \
  -I /workspace/work/toolchain/test-proxy.init.gradle \
  testDebugUnitTest --tests '*MoneyInputTest' --tests '*PremiumP01UiTest'
/workspace/work/toolchain/gradle-8.9/bin/gradle --no-daemon \
  -I /workspace/work/toolchain/test-proxy.init.gradle \
  testDebugUnitTest lintDebug assembleDebug
/workspace/work/toolchain/gradle-8.9/bin/gradle --no-daemon \
  -I /workspace/work/toolchain/test-proxy.init.gradle testDebugUnitTest lintDebug
python tools/verify_neo41_source.py
PATH=/workspace/work/toolchain/kotlinc/bin:$PATH bash tools/run_neo_checks.sh
git diff --check
```

| Check | Result | Evidence / limit |
|---|---|---|
| First focused suite | PASS | 4 new tests at that point, focused log |
| Final regular suite | PASS | 140 tests, 0 failures/errors/skips, 19 JUnit classes; eight new cases, existing132 retained |
| Final test/lint command | PASS | BUILD SUCCESSFUL in 2m54s; verify log |
| Lint | PASS with warnings | Local XML45 Warning, no Error/Fatal. Historic CI46 is a separate environment/run, not a candidate CI result. No warning-free claim. |
| Compile Kotlin/main+test | PASS | final Gradle command |
| Assemble | BLOCKED | Attempted; packageDebug failed because persistentDebug has no storeFile in invocation. Not a successful APK build; no new APK offered. |
| Protected source/parser/identity/assets | PASS | verifier:11 byte-identical protected files; lexical/XML/version/brand/signing/launcher checks |
| Standalone Kotlin regressions | PASS | dates/24h/visibility/overlap/finance/palettes/reduction |
| Device/Launcher/IME/Honor/upgrade | BLOCKED | ADB cannot create read-only /home/agent/.android; no emulator/KVM or verified device/certificate. Need isolated Android+Honor and authorized matching signing identity. |
| All-editor fault injection / note recovery / restore staging | NOT RUN (gate remains HOLD) | New task operation tests are not a substitute for these cases. See P01_HANDOFF. |

Raw evidence: /workspace/output/premium-p01/qa (outside Git); evidence hashes in EVIDENCE_SHA256.json. Unit tests are partial behavior evidence, not approval of Q01–Q05/Q15/Q18 in all required environments. No CI was triggered or code pushed for this local branch. No baseline test rerun is claimed: baseline132 is historical evidence; final140 was actually executed.

Retained risks: D07 root async Unit saves/dismissals; D17 note text/style atomicity; D18 root lifecycle/side-effect reconciliation; D24 asset staging and provider interruptions; full matrix remains unaccepted. P01 has no accepted exit SHA; P02–P06 NOT RUN. These are distinct from signing/device environment blockers and must be fixed/tested before advancing their dependent work.
