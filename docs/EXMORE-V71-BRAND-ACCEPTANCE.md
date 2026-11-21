# ExMore v71 — branding and rollback gate

- Stable rollback: branch `feat/v70-calendar-navigation-salary-20261121` at `f6b84cd5300a85d36a0182df97a4d531e00fa1c6`; signed v70 APK in release `v70-implementation-candidate-f6b84cd5300a`.
- Development only: `feat/v71-exmore-branding-splash-20261122`. No modifications to main or v70, no schema or financial data changes.
- Preserve `applicationId=com.todoshift.app` and persistent signing certificate so updating the existing installation does not require removing the app.
- Original brand asset source: `ExMore_Brand_Assets_v2_Complete.zip`. PNG assets committed individually, not regenerated.
- API 31+: Android system splash: opaque `#071F45`, transparent centered ExMore mark, no extra animation or second fake Splash.
- API 26–30: drawable splash: identical background `#071F45`, centered ExMore mark.
- Launcher: adaptive, themed monochrome, round and density-specific original PNGs; static and pinned shortcuts have distinct module icons.
- Accept only after full tests, lint, signed release build, provenance+certificate verification and emulator/device visual startup/launcher check. Device-to-device upgrade compatibility is not inferred from CI.
