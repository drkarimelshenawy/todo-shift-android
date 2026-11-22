# ExMore Premium — v72 brand integration

## Source and version
- Starting branch: `feat/v71-exmore-branding-splash-20261122`, HEAD `933c74822db83e4ff9b620ee05fe07f7aaa179ae`.
- Working branch: `feat/v72-exmore-premium-brand-20261122`.
- Supplied packs: `ExMore_Android_Resources_v71.zip` and `ExMore_Premium_Brand_Kit_v1.zip`.
- Their 23 `06_Android_Ready` files are byte-identical, imported without cropping or rescaling.
- Additional approved 1080×2340 dark/light splash PNGs come from `03_Splash` in the Brand Kit.
- Application ID remains `com.todoshift.app` so an update can preserve the existing app identity; versionCode increases from 77 to 78.
- Brand/app display label is ExMore; Life Planner is a description and on the boot-time artwork, not appended to the home-screen launcher label.

## Placement
- Launcher: original adaptive foreground, monochrome and correct legacy 48–192 px mipmaps.
- Android 12+ and legacy system splash: approved centered image against existing navy `#071F45`, no forced animation.
- In-app loading screen: approved dark or light full-screen image only during real database initialization; **no timer or deliberate startup delay**. Existing boot failure/recovery dialog takes precedence.
- Compact headers: dedicated `exmore_header_icon.png`, not the 432 px splash image.
- Launcher shortcuts: keep all existing static and pinned shortcut IDs/targets. Add optional pinned Settings shortcut mapped to root tab 3, using the approved Settings PNG. Salary icon is bundled for use only when the route has been validated.

## Non-goals and safeguards
No navigation-order changes, no data format/schema migration, no rename of application ID, no modifications to finance, tasks, shifts or backup semantics, and no merge to `main`/v70/v71. This is visual integration and shortcut wiring only.

## Acceptance checks
1. Run `python3 tools/verify_exmore_v72_brand_assets.py` to verify exact SHA-256 hashes, dimensions, logo locations, launcher IDs and version identity.
2. Execute complete existing Android unit tests, lint and signed release build; assert APK certificate and source identity.
3. On Android 35 emulator, install published signed v71, then `adb install -r` v72, inspect cold-start and collect visual evidence.
4. **Release stays a draft candidate** until a real device confirms an update without uninstall, visual clarity on light/dark modes and retained calendar/tasks/notes/finance data.

Android's system splash cannot display a full portrait marketing page on Android 12+. The separate in-app loading image is shown only while data loads; do not add artificial delay merely to display it.
