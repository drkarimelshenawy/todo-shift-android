# UX rescue QA report

Candidate 4.2.0 (31), package com.todoshift.app, feature branch only. Results are recorded in VERIFICATION.json after final checks. The 4.1.1 user review FAILED and is not replaced by automated pass counts.

## Exact local commands

Environment: Java 17, Gradle 8.9, SDK 35, existing signing configuration. Toolchain is /workspace/work/toolchain; credentials are supplied securely outside source and omitted here.

```sh
/workspace/work/toolchain/gradle-8.9/bin/gradle --no-daemon --continue \
  -I /workspace/work/toolchain/test-proxy.init.gradle \
  :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
python tools/verify_neo41_source.py
PATH=/workspace/work/toolchain/kotlinc/bin:/workspace/work/toolchain/jdk17/bin:$PATH \
  JAVA_HOME=/workspace/work/toolchain/jdk17 bash tools/run_neo_checks.sh
/workspace/work/toolchain/gradle-8.9/bin/gradle --no-daemon \
  -I /workspace/work/toolchain/test-proxy.init.gradle \
  -I tools/visual_capture.init.gradle \
  :app:testDebugUnitTest --tests '*NeoVisualProbeTest'
git diff --check
```

The cloud-only proxy init forwards existing test JVM proxy/CA properties and supplies a writable test home. The optional visual init adds only tools/visual-tests to the test source set. It is separate from the ordinary suite. CI preserves ordinary test/lint reports before optional visual diagnostics overwrite test output.

## Regression coverage

Retained original P0/backup/date/finance/number/template/attachment tests. Added six-group/search routing, direct brush/Done, focused operations, compact Studio, draft restoration without record creation, inherited dark foreground, profile direction preservation, independent overview toggles, additive workplace-source repair, fresh attachment cleanup preserving references/unknown files, widget plus PendingIntent to QuickTaskActivity, launcher/dialog identity, all transition frames/reduction, and quiet-hour rescheduling after the original time.

The quiet-hour test injects the rescheduling clock instead of assuming Android's shadow wall clock changes Java System.currentTimeMillis. No production clock behavior is altered. An initial date-button assertion was corrected to match its combined label/date. Capture failures and frame lag were diagnosed rather than reported as successful screenshots. No ordinary tests were disabled.

## Visual scope

See VISUAL_EVIDENCE.md and the visual artifact. Native graphics uses actual MainActivity, synthetic isolated records and window drawing. English light, Arabic dark/AMOLED, large text, narrow/empty, colored single/multiple/OFF shifts, painting, major destinations and Settings controls are attempted. The theme foreground bug was found through rendered inspection. Modal drawing and full device behavior remain separately qualified.

## Remaining checks

Physical Android/Honor, actual keyboard/window insets, launcher/device lifecycle, reboot/doze delivery, exact-alarm policy, biometric hardware, TalkBack, actual installed certificate/upgrade, power-loss and full-storage behavior, sustained performance and exhaustive preference combinations are unperformed. See REMAINING.md and the per-item matrix. No zero-bug or full-acceptance claim is made.

## Final local results

- Regular tests: 132 passed, zero failures/errors/skips. Preserved JUnit evidence in delivery local-qa/junit.
- Optional native-render probe: 2 passed; 26 PNGs. Separate from regular test count.
- Lint: zero errors, 45 warnings retained for review; not claimed warning-free.
- Debug APK assembly, apksigner verification (v2), package/version check and 16 KiB zip alignment: passed.
- Source invariant verifier: passed all 11 protected files; standalone Kotlin/date/finance/motion checks passed. git diff --check passed.
- Local APK SHA-256: 9b7e3fb4b214cb3056a3b1be87a99c8a4c7610672cf3c3fe3e39693f37197103.
- Local APK certificate: A257D6F5BE339F6F317D58D641539CE605D61D9A8D822E76D4D58F974E98BA02. This differs from CI; installed-app identity is unknown.

CI results are independent and recorded in the delivery CI metadata after the final feature commit is pushed.
