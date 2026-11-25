# ExMore 4.4.2 identity migration

ExMore is installed as `com.exmore.app`. Android therefore treats it as a separate application from the former `com.todoshift.app`; the operating system does not allow either sandbox to read the other's private files.

## Safe transfer

1. Keep the former application installed. In its Account & Backup screen, select every section and export a full encrypted `.tdsb` backup. Keep the password and the file.
2. Install the original-key signed ExMore 4.4.2 APK alongside the former application.
3. In ExMore, open Account & Backup, inspect the exported file, confirm that Calendar, Tasks, Salary, Cash Book and Settings are available, and choose **Replace** on the fresh ExMore installation.
4. After restore, verify calendars and shifts, tasks, notes and documents, entitlements and receipts, Cash Book accounts, debts and balances, attachments, appearance and settings. Keep the backup and former application until this check is complete.

The ExMore reader accepts backup manifests from both `com.exmore.app` and `com.todoshift.app`. It still validates archive paths, sizes, checksums, schema, record identities, linked receipts and Cash Book data before writing. Restore uses the existing transaction journal and rollback path. Unrelated application IDs are rejected.

PINs, biometric enrollment, Android keystore keys, OAuth tokens and notification/widget registrations are intentionally device/application scoped and are not transferred. Configure those again in ExMore after data verification.

## External registrations

The Android package must be registered separately as `com.exmore.app` in the existing Firebase/Google project with the SHA fingerprints of the retained signing certificate. The old `com.todoshift.app` registration must remain during migration. This repository currently contains no Firebase Android SDK or `google-services.json`; Firestore is used only by the optional credential-free AppCore release lookup for stable project ID `exmore`.
