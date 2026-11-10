# P01 resumed checkpoint QA — HOLD

App source checkpoint: `4c78f0bddfa134d593ce891e991456ccce0fd4e5`; app Git tree `9f2edbcb390ccaf2db74de831e128574229db754`.
Resumed from `faf1c93dc7dc9eef9c9aab3ffe374e6bcc03c784` on `work/todo-shift-premium-rescue`; original baseline `e178b134658afad04d645b044f24d236594cbe33` remains an ancestor.
Linux cloud, Java17, Gradle8.9, compile/targetSDK35, Robolectric SDK28. Synthetic fixtures only. No CI execution, phone test, install, uninstall, storage reset or user-data migration.

## Commands actually run

Environment paths (no credentials shown):

```sh
export JAVA_HOME=/workspace/work/toolchain/jdk17
export ANDROID_HOME=/workspace/work/toolchain/android-sdk
export ANDROID_USER_HOME=/workspace/work/toolchain/android-user
export GRADLE_USER_HOME=/workspace/work/toolchain/gradle-home
export PATH="$JAVA_HOME/bin:$PATH"
/workspace/work/toolchain/gradle-8.9/bin/gradle --no-daemon --continue \
  -I /workspace/work/toolchain/test-proxy.init.gradle \
  testDebugUnitTest lintDebug assembleDebug
python tools/verify_neo41_source.py
PATH=/workspace/work/toolchain/kotlinc/bin:$PATH bash tools/run_neo_checks.sh
git diff --check
/workspace/work/toolchain/gradle-8.9/bin/gradle --no-daemon \
  -I /workspace/work/toolchain/test-proxy.init.gradle \
  -I tools/visual_capture.init.gradle testDebugUnitTest --tests '*NeoVisualProbeTest'
/workspace/work/toolchain/android-sdk/build-tools/35.0.0/apksigner verify --verbose --print-certs <experimental-apk>
/workspace/work/toolchain/android-sdk/build-tools/35.0.0/aapt dump badging <experimental-apk>
/workspace/work/toolchain/android-sdk/build-tools/35.0.0/zipalign -c -P 16 4 <experimental-apk>
/workspace/work/toolchain/android-sdk/platform-tools/adb devices -l
```

The full build command was launched by an external private runner supplying the already-supported signing environment variables for a newly generated isolated test key. The key/password/config are excluded from Git and delivery. Production signing configuration was not changed. Proxy init provides writable test-JVM/network settings, not skipped assertions. Ordinary JUnit evidence was copied **before** the separately filtered native probe overwrote Gradle test output.

## Actual results

| Check | Result | Scope |
|---|---|---|
| Regular unit/integration/Compose/Robolectric suite | **168 PASS, 0 failures, 0 errors, 0 skipped**, 27 classes | Exact names in external qa/TEST-INVENTORY.json and JUnit XML; no physical device |
| lintDebug | PASS: **0 errors/fatal, 45 warnings** | XML/TXT/HTML preserved. Categories: UnusedResources10, UnusedAttribute8, VectorRaster6, GradleDependency5, IconLauncherShape5, IconDuplicates5, ObsoleteSdkInt3, ModifierParameter2, Overdraw1. Warnings are not described as resolved. |
| assembleDebug | PASS | Full serial validation BUILD SUCCESSFUL in3m; experimental isolated signature |
| Protected/source verifier | PASS, 11 protected files | Parser/unaffected business-file byte identity, signing/application/launcher identity, XML/Kotlin lexical checks, unchanged brand ZIPs. Not durability or full source semantics proof. |
| Standalone Kotlin checks | PASS | Overnight/24h/visibility/immutability/overlap/finance/palettes/reduced-motion checks; no device timing |
| diff whitespace | PASS | No `git diff --check` findings |
| Optional native probe | **2 PASS, 0 failures/errors/skipped**, **28 PNGs** | Actual MainActivity workflows plus2 separately labelled component captures; SDK28 NATIVE graphics simulation |
| APK inspection | PASS signature verification(v2), badging, 16KiB-aware alignment | Package com.todoshift.app, 4.2.0/code31; **not accepted upgrade** |
| ADB | Enumeration works, empty device list | Physical Android/Honor tests BLOCKED — ENVIRONMENT; emulator executable and /dev/kvm absent |
| CI/push/release | NOT RUN / NOT PERFORMED | Local branch only; no fabricated Actions link |

Kotlin compiler/test deprecation warnings remain. One Kotlin daemon startup attempt reported termination; Gradle recovered and the final command exited successfully. The read-only Android analytics-settings metrics warning persists and did not fail lint/build. No secret contents were printed.

## P01 requirement-level evidence and limits

| QA | Executed evidence | Outstanding acceptance |
|---|---|---|
| Q01 localized/financial amounts | MoneyInputTest; FinancialRequestIdentityTest; ClassicReceiptSafetyTest; PremiumP01UiTest.groupedSourceEditReopensAsNineThousandAndInvalidStaysInEditor; ModernUiIntegrationTest.localizedPaymentInputPersistsTheExactNormalizedAmount | Not every editor/time/rate/allocation route from full MainActivity, IME and all number/device combinations |
| Q02 checked commit/drafts/concurrency | PremiumP01UiTest completion/stable-ID/restored-revision cases; NeoTaskCommitTest before/ambiguous/disk-unacknowledged/service failures; WorkplaceSaveAcceptanceTest; NewReferenceSafetyTest; CommittedEffectsTest; BackupRecoveryTest.databaseWriteFailureAndInterruptionRecoverBeforeRetry | Full inventory adversarial matrix, real OS process death and every editor's cursor/conflict state not proven |
| Q03 atomic notes | NoteTransactionTest text/style reopen; simulated interruption journal/effects/text/style; failed write and concurrent metadata | Actual power loss/full disk/filesystem behavior and whole-Activity saved cursor/draft state unverified |
| Q04 backups/restore | BackupRecoveryTest all named tests; AttachmentBackupTest; interruption before/after preference/asset/effects checkpoints, checksums/token/secret exclusion/old IDs | Real SAF/provider cancellation, Activity/process interruptions, all file collisions/permissions and Android/Honor restore matrix incomplete |
| Q05 privacy/data health | Existing privacy/safe recovery regressions; ClassicBackupAuthorizationTest stale-prop/live-relock export gate; ReminderPublicVersionTest independent prep | Full background lock/search/export/widget leak matrix, actual OS redaction and every approved repair remain unaccepted |
| Q15 external task/widget | NeoWidgetIntegrationTest actual broadcast and Activity/launcher identity; NeoTaskCommitTest; PremiumP01UiTest editor retry | Actual Launcher dialog, recreation/permission/background and Honor behavior blocked by absent devices |
| Q18 attachments | AttachmentBackupTest and PhotoImportTest synthetic content, restore and reference/cleanup checks | Real content providers/files/images/permissions/open/cancel/lifecycle matrix and user fixtures incomplete |

Q06–Q14/Q16–Q17/Q19–Q24/Q26–Q28 are not passed phase acceptance. Existing regular tests and sampled screenshots may exercise parts; they do not enter P02–P06. QA_STATUS.json retains all28 IDs and ACCEPTANCE.json retains all147 protected IDs with no accepted row.

## Failure/repair evidence

- Intentional pre-fix NewReferenceSafetyTest run: 4 tests,3 failed (deleted-calendar task/note and referenced-workplace removal),1 passed (historical preservation). All4 pass after guards. Raw before log/XML retained.
- An optional native probe's modal idling failure was corrected by explicit diagnostic Activity content/capture handling;2 final tests pass, original failing log/XML retained.
- A nullability compile error in a newly added alarm assertion was fixed. Its first runtime assertion then consumed the alarm via getNextScheduledAlarm; inspecting Robolectric bytecode showed poll/deschedule. Replaced with non-consuming peek, preserving the actual behavioral assertion. Final alarm rollback/current-snapshot tests pass.
- Serial full runs progressed through163/167/168 passing tests. Only168 is the final regular suite; do not combine stale XML or count optional2 in that figure.

## Visual inspection performed

Viewed final English calendar, Arabic AMOLED large-text calendar, painter/type strip, narrow empty large-text calendar, Classic backup dialog and seeded task-error component PNGs. Calendar fixture shows single/multiple/OFF entries and note/task labels; workplace chips/type colors are visible; task error and raw draft remain visible. Backup displays separate fullZIP/legacyJSON/preview controls with plaintext warning.

Remaining visual observations: calendar has unused vertical area on the sampled tall window; narrow large-text header is ellipsized; the long task form requires scrolling (one control partially visible above its fixed actions); the painter type strip is horizontally clipped/scrollable. No claim these satisfy Premium density/accessibility or all controls are discoverable. P02–P05 must address the corresponding product defects after P01 gate acceptance. No measured complete contrast/TalkBack/keyboard test or user approval. See VISUAL_EVIDENCE.md for component-vs-workflow boundaries.

## APK / safety

Experimental filename: `ToDoShift-Premium-P01-EXPERIMENTAL-4.2.0-vc31.apk`.
SHA256: `7d5c3195c36d7fb220c0e5e3ed9edb7605e93bc14c0238fd542ef979abf7673a`.
Certificate SHA256: `3a969f25d41d3f07a1f5ea352005ecd02a3afe1e186e82b5f631c227418214d4`.
Public subject: CN=ToDo Shift Isolated Premium Test. Different from documented CI certificate; installed certificate unavailable. **Do not use for in-place user upgrade.** Version is unchanged baseline31, not a higher release candidate. See INSTALL.md.

Raw evidence and generated files are outside Git at `/workspace/output/premium-p01-resume/`. Checksums are recorded without including keystores/config/secrets. P01 gate stays HOLD; required acceptance is neither replaced by this build nor silently skipped.
