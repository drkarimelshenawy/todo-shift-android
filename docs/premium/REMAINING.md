# Resume checkpoint — P01 HOLD

The sequential execution remains at P01. P02–P06 have not started and have no accepted exit SHA. This is not completion of the Premium rescue or a production-ready build.

## Software evidence still needed

The checked write protocol, transactional notes/restore, reference guards and durable service intent are implemented and have targeted regressions. This does not prove every editor/Activity interleaving. Expand Q01/Q02 across every create/edit/pay/allocation/rate editor, all save/delete entrances in Premium and Classic, busy rejection, stale CAS, I/O failure, saved-state restoration and conflict decisions. Complete full-Activity adversarial coverage and external document-provider cancellation/error scenarios. Do not label missing test coverage an unavailable-device blocker when it can run in the cloud.

Review the fault-injection and UI coverage inventories in QA.md and WRITE_CALLSITES.txt before marking any protected row accepted. Current tests use synthetic data; no original user incident or actual full-disk/power-loss scenario is claimed reproduced. Inspect any newly found defect and fix it before rerunning its affected gate.

## BLOCKED — ENVIRONMENT

- Android emulator: SDK installation contains no emulator executable and /dev/kvm is absent. Robolectric is simulation, not an emulator.
- Physical Android and Honor: adb enumeration works but has no connected devices. Need isolated authorized devices with launcher, notifications, permissions, Doze/reboot, real document providers, keyboard/gesture, TalkBack and the requested language/density matrix.
- Upgrade/signature: installed APK certificate is unavailable. This checkpoint uses an isolated test key different from the documented CI certificate; no installed-app update is authorized by its mere existence. Need public certificates/lineage, a matching authorized signing job and a candidate versionCode greater than every supported installed version, then non-destructive fixture upgrades and oldest-supported migration checks.
- Performance: no comparable physical baseline/candidate traces, startup20-run/p95, memory, jank or battery dataset. No performance acceptance claim.

The obsolete signing-input packaging blocker was resolved using an isolated local key. The obsolete adb-directory blocker was resolved; the remaining absence of connected devices is distinct.

## Safe continuation

Resume on work/todo-shift-premium-rescue at the final checkpoint. Preserve the input and source commits listed in P01_HANDOFF.md. Finish the open software verification above and run the missing P01 device acceptance. Record evidence and evaluate Q01–Q05/Q15/Q18 individually; P01 exit_sha remains null until the required gate passes. Only then enter P02 on its accepted exit SHA and continue P03→P06 in order. Independent P05 notification-public-version model protection and P06 packaging diagnostics in this checkpoint do not pass those stages.

Never reset user data, downgrade/uninstall to bypass signature checks, force-push, merge or touch the protected branches. Code rollback uses a separate checkout/worktree at a recorded checkpoint. No real application install or data migration happened in this session, so no user-data rollback was performed.
