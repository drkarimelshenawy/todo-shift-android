# Current repair candidate

**4.4.0-premium-ux-test / versionCode 38**, branch `fix/premium-financial-restore`.

Use [UX_4_4_ACCEPTANCE.md](UX_4_4_ACCEPTANCE.md) for the implementation map and phone checklist, and [INSTALL.md](INSTALL.md) for the current APK retrieval and identity instructions. Source gates and signing evidence are in Financial restore validation. The exact tested binary and Android 35 evidence are in the latest successful Premium UX device validation run. That workflow guards equality of the application/build tree, verifies the immutable APK identity/checksum and records APK/test-script commits separately.

Earlier P01/recovery reports and Development Gates are historical checkpoints; they do not certify this candidate. Tests, lint, signed APK inspection and Android emulator smoke are reported by the current workflow. Physical Android/Honor launcher, notifications/Doze and phone UX acceptance remain separate. No merge or release is included.
