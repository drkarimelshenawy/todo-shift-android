# Obtain and install ToDo Shift 4.1

## Artifacts

Delivery folder: `/workspace/output/todo-shift-4.1.0/`

- `ToDoShift-4.1.0-vc29-LOCAL-TEST-ONLY.apk`: installable debug APK, signed using a newly generated local test certificate.
- `ToDoShift-4.1.0-source.zip`: complete updated project, including both original brand archives, tests, Gradle files and reports. Excludes build caches, Git metadata, SDKs and private signing keys.
- `ToDoShift-4.1.0-delivery.zip`: APK, source ZIP, reports and verification evidence together.
- `SHA256SUMS.txt`: artifact hashes.

### Verified download availability

The files above are verified local artifacts in this cloud executor. No direct chat attachment/export tool, workspace download browser, externally reachable file URL, or port-forwarding interface is available for verification through this session's tools. Desktop capability is enabled in environment metadata, but its viewer/control tools are absent; this does not establish a phone-download function.

The earlier file-browser and port-forwarding instructions were unverified and are withdrawn. Starting `python -m http.server` in the executor alone does not create an address reachable from an Android phone. `/workspace/...` is a remote filesystem path, not a phone download link.

A phone download needs an actual HTTPS attachment/export link or an authorized transfer to a storage service accessible from the phone. Neither route has been established in this session. No upload, publication, tunnel, or repository push has been performed. Once an export/transfer destination is available, transfer the existing ZIP and APK bytes without rebuilding and verify SHA-256 against `SHA256SUMS.txt`.

The existing delivery ZIP/source ZIP retain their pre-commit reports and were intentionally not repackaged during local preservation. The APK is unchanged. This repository's current handoff document supersedes the older download advice inside those archives.

## Version and signing identity

- Package: `com.todoshift.app`
- Version: `4.1.0`
- Code: `29` (baseline was 28)
- SDK: minimum 26, target/compile 35
- Local test signer: `CN=ToDo Shift Local Test Only, O=Local Development`
- Local test SHA-256: `a257d6f5be339f6f317d58d641539ce605d61d9a8d822e76d4d58f974e98ba02`
- Repository CI expects certificate SHA-256 `aecf41e372d7204dacf6ebb93f02414b265c8d97fd5508023287aa2fc6da6ea9`. That is an existing CI expectation, not verification of the certificate installed on your phone.

The signing block, alias `todoshift` and credential environment variable names are unchanged. The delivered APK was built with a separately created local test keystore supplied only for this build. No production credential was changed or bundled.

## Safe testing on a spare device

Use the local-test APK on a spare device/emulator that does **not** already contain a differently signed `com.todoshift.app`. Install through Android's package installer or:

```sh
adb install ToDoShift-4.1.0-vc29-LOCAL-TEST-ONLY.apk
```

It uses the same application ID, so it is not a side-by-side alternate package. Do not uninstall an existing app containing real data to make room for it.

## Upgrade an existing installation without clearing data

1. Export a local backup from the existing app and retain it outside the app's private storage. Verify that the file is readable; do not replace live data merely to test restoration.
2. Read the installed version with `adb shell dumpsys package com.todoshift.app`. Confirm its version code is lower than or equal to the intended build; do not force a downgrade.
3. Read the installed APK path with `adb shell pm path com.todoshift.app`, copy the returned `base.apk` with `adb pull`, and compare certificates using `apksigner verify --print-certs installed.apk` and the same command for the candidate APK.
4. **If certificates differ, stop.** The delivered local-test APK will normally differ from production. Android must reject it with an update incompatibility. Do not uninstall, clear storage, use a downgrade flag or change application ID to bypass that check.
5. Obtain a build signed with the original installed app's key (or its legitimately supported signing lineage). In a secure build environment, set `TODOSHIFT_SIGNING_FILE` and `TODOSHIFT_SIGNING_PASSWORD` to the original signing configuration, then run:

```sh
gradle testDebugUnitTest lintDebug assembleDebug
apksigner verify --verbose --print-certs app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

6. Confirm calendars, shift IDs/times, rates, payments, tasks, visibility/primary selection, settings and security after upgrade. Check month cells and opening the same saved days, then restart the app and check again. Exercise reminders/widgets without clearing storage.

Application ID, launcher identity and parser formats are preserved, so an appropriately signed normal upgrade is designed to retain private app data. The actual production-signature upgrade and real-device preservation have **not** been performed here. Without the matching signing key, a non-destructive upgrade cannot be delivered by changing source code alone.

## Recovery

If startup fails, the app blocks editing and offers Retry or an explicit authenticated backup-recovery path. Recovery validates and previews the selected backup and requires confirmation before replacement. It preserves raw current preferences in private `neo41-before-recovery-<UUID>.json` snapshots. Those files are not portable encrypted backups and are never automatically uploaded. Do not clear storage as a troubleshooting shortcut.
