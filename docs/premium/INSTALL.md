# Experimental P01 checkpoint — not an installed-app upgrade

Package remains com.todoshift.app; version remains baseline 4.2.0 / versionCode31. No release version increment was made because P01 is not accepted and P02–P06 are not implemented. No application was installed, uninstalled or cleared in this session.

The local debug APK is signed only with an isolated test certificate, SHA256:

`3a969f25d41d3f07a1f5ea352005ecd02a3afe1e186e82b5f631c227418214d4`

The Blueprint's documented prior CI certificate is:

`aecf41e372d7204dacf6ebb93f02414b265c8d97fd5508023287aa2fc6da6ea9`

These differ. The certificate of the user's installed app is unknown. Do **not** install this checkpoint over the current app or suggest uninstall/clear/downgrade as a workaround. Its versionCode31 is also not a higher upgrade candidate over baseline31. A separate user/work profile on the same phone does not bypass Android's package signing checks.

For isolated diagnostics, use an authorized emulator or physically separate test device that has never held the package. Populate synthetic fixtures only. This does not establish user-data upgrade compatibility. No connected target is available in this session.

Before any future upgrade candidate:

1. Export and validate a backup without modifying the current app's storage. Keep existing app and data intact.
2. Obtain the public installed base APK or the actual installed4.1.0/4.2.0 delivery APK; use `adb shell pm path com.todoshift.app`, pull the returned base APK with authorized device access, then `apksigner verify --verbose --print-certs <base.apk>`. No private key is needed to compare certificates.
3. Compare certificate SHA256 and any explicit signing lineage with the authorized candidate. Same package and a production-labelled key do not establish equivalence. Stop on mismatch.
4. Use the matching protected signing job, a versionCode greater than every supported installed version and passing gates. Test updates on synthetic baseline and oldest-supported fixtures without uninstall/clear; verify counts, IDs, cents, calendar ownership, notes, settings, attachments and reminders after reopening.
5. Only after that evidence exists, provide normal in-place Android installation instructions for the accepted candidate. Never expose signing credentials in chat/source/artifacts.

## Retrieval

Local artifacts and reports are under `/workspace/output/premium-p01-resume/`; source is a Git archive of the final local checkpoint. Exact names/checksums are in the external delivery manifest and SHA256SUMS.txt. These are actual workspace paths, **not public URLs**. No GitHub Actions run, uploaded artifact, release or download URL was created.

This session has no verified Android-phone workspace browser/download interface. If the client renders the supplied local-file links as downloads, save the ZIP/APK through that control. If it does not, use the client's documented export/download support or transfer the named files through an explicitly available authorized channel. No port-forwarding UI or public file server is assumed. Do not upload keys/private signing config; they are deliberately excluded from the source and evidence archives.
