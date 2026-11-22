#!/usr/bin/env python3
"""ExMore v4.4.0 branding integration and SHA-256 asset invariants.

Approved source: ExMore_Premium_Brand_Kit_v1/06_Android_Ready +
03_Splash/*_1080x2340.png. Avoids unverifiable visual acceptance claims.
"""
from __future__ import annotations
import hashlib
import re
import struct
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
ANDROID = "{http://schemas.android.com/apk/res/android}"
EXPECTED_SHA256 = {
    "drawable-nodpi/exmore_header_icon.png": "6570a1992ad6e9a0ec3994888734e93a121322d0ce1ebf38f04310ffa306b98d",
    "drawable-nodpi/exmore_launcher_adaptive_foreground.png": "951f696d965ce32da41fb527f614367f69ed450b9d4d13a9146471ab1185cabf",
    "drawable-nodpi/exmore_launcher_monochrome.png": "03a96fde9f0dc421f0ec8f9b39d2f532a7230394e4cdb9e0b4fe1cac1b870c62",
    "drawable-nodpi/exmore_shortcut_calendar_work.png": "ab9a0fc826de6b1b716a71fd9f21d1befcb701638506e6c42d34326f6a37269b",
    "drawable-nodpi/exmore_shortcut_cash_book.png": "2947bda25f71487a325a59b886831b81bdf8ff804388487cd2b33192ea1336b0",
    "drawable-nodpi/exmore_shortcut_notes.png": "742742641fdfbd51b0fa7d1e93bfe6eebc71434bef7a6f263a3b026ddf80d952",
    "drawable-nodpi/exmore_shortcut_salary.png": "6c8cbad3dd83e923d0d974247ed0e04aff04e7a6dd9cf6ab4f984271597bf90c",
    "drawable-nodpi/exmore_shortcut_settings.png": "a17674890cd9e984a3e4a99fe545e5f3cf0ab0adda274cf2a5da009fba032a10",
    "drawable-nodpi/exmore_shortcut_tasks.png": "1c22142b087e1abfb93c8a76e5d35758358f99daa750c7e150e1960902abfdfd",
    "drawable-nodpi/exmore_splash_center.png": "951f696d965ce32da41fb527f614367f69ed450b9d4d13a9146471ab1185cabf",
    "drawable/exmore_icon_background.xml": "73a9bb62cf98d82726cf7f596b0adeec326e53b4bbd771bbd4a5fa1d665dc948",
    "mipmap-anydpi-v26/ic_launcher.xml": "0d3a5ac2bab7062783079c39075ca15bc768a2ec8d358da5edde9d1a399bf2ca",
    "mipmap-anydpi-v26/ic_launcher_round.xml": "0d3a5ac2bab7062783079c39075ca15bc768a2ec8d358da5edde9d1a399bf2ca",
    "mipmap-hdpi/ic_launcher.png": "b8e24cc9cf08a90ff1c5916e45e4ee6faeeb36bfb81d79a0e975f7759f8b2b67",
    "mipmap-hdpi/ic_launcher_round.png": "178539e17fc93a8d39f6f2b5028d47ae24ecde7ef6c79a461380692011be6146",
    "mipmap-mdpi/ic_launcher.png": "ff9d1c2c05142bcf69c9a04c97815d2897f6e04a7601f4e28c6e1779900370a0",
    "mipmap-mdpi/ic_launcher_round.png": "6f415b5b7bd449afaaf453a7c69dea72f0e3d47f4a10fe54ce6e1165ab5c29e5",
    "mipmap-xhdpi/ic_launcher.png": "fa93dff22e7cdaf970b1f55aa3c218fbdccc902dd35a14e47e31bbacd4967d1e",
    "mipmap-xhdpi/ic_launcher_round.png": "fa9fb1cd8a5654ae2522462bb1b4ef04e291e8b01377eccf84473c7ae8bc84ec",
    "mipmap-xxhdpi/ic_launcher.png": "b60efa025f60976fd0ffb02196d083b94357a50aff2d49c8e8932df13b46fa90",
    "mipmap-xxhdpi/ic_launcher_round.png": "2616b1eb4ba41922b988d2bad1e918a21e898220973ca81b25729fe2c57f0c57",
    "mipmap-xxxhdpi/ic_launcher.png": "4ffe5d99adda9a3cb5206b531c4b35441a55b2c10b696fc8878a59abab24b356",
    "mipmap-xxxhdpi/ic_launcher_round.png": "f5ebca1117afcadcb6a6ccee7b70ee98a6356c815fde3ceae6888454c521c37e",
    "drawable-nodpi/exmore_splash_full_dark.png": "d0f0b6da91fc7c5a50168f7b68b1138293af14ace9cfd45388abdafa511069e4",
    "drawable-nodpi/exmore_splash_full_light.png": "1ac193bdca6e280284a130b7b5467c50e0ec386bc2b0d5eeb51ce125696c12cf"
}


def verify(value: bool, context: str) -> None:
    if not value:
        raise SystemExit(f"ExMore v4.4.0 brand QA FAILED: {context}")


for file, expected in EXPECTED_SHA256.items():
    actual = RES / file
    verify(actual.is_file(), f"Missing approved asset {file}")
    payload = actual.read_bytes()
    verify(hashlib.sha256(payload).hexdigest() == expected, f"Asset changed: {file}")
    if actual.suffix.lower() == ".png":
        verify(payload[:8] == b"\x89PNG\r\n\x1a\n", f"Not a PNG: {file}")
        width, height = struct.unpack(">II", payload[16:24])
        if "splash_full" in file:
            verify((width, height) == (1080, 2340), f"Full splash wrong size: {file}")
        elif any(name in file for name in ("launcher_adaptive_foreground", "launcher_monochrome", "splash_center")):
            verify((width, height) == (432, 432), f"Adaptive image wrong size: {file}")
        elif "header_icon" in file:
            verify((width, height) == (192, 192), f"Header icon wrong size: {file}")
        elif "exmore_shortcut_" in file:
            verify((width, height) == (1024, 1024), f"Shortcut icon wrong size: {file}")
        elif "mipmap-" in file:
            density = file.split("/", 1)[0].removeprefix("mipmap-")
            verify(width == height == {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}[density],
                   f"Legacy density error: {file}")

for v in ("v26", "v33"):
    for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
        icon = ET.parse(RES / f"mipmap-anydpi-{v}" / name).getroot()
        verify(icon.tag == "adaptive-icon", f"Not adaptive icon: {v}/{name}")
        items = {child.tag: child.attrib.get(ANDROID + "drawable") for child in icon}
        expected = {
            "background": "@drawable/exmore_icon_background",
            "foreground": "@drawable/exmore_launcher_adaptive_foreground",
            "monochrome": "@drawable/exmore_launcher_monochrome",
        }
        verify(items == expected, f"Adaptive resource wiring changed: {v}/{name}")

background = ET.parse(RES / "drawable/exmore_icon_background.xml").getroot()
verify(any(x.attrib.get(ANDROID + "color") == "#071F45" for x in background.iter()),
       "Launcher background changed")
legacy = ET.parse(RES / "drawable/neo41_splash.xml").getroot()
verify(any(x.attrib.get(ANDROID + "drawable") == "@drawable/exmore_splash_center" for x in legacy.iter()),
       "Legacy splash center missing")
verify(any(x.attrib.get(ANDROID + "color") == "#FCFBF8" for x in legacy.iter()),
       "Legacy splash background changed")
android12 = ET.parse(RES / "values-v31/themes.xml").getroot()
data = {x.attrib.get("name"): (x.text or "").strip() for x in android12.iter("item")}
verify(data.get("android:windowSplashScreenAnimatedIcon") == "@drawable/exmore_splash_center", "System splash icon missing")
verify(data.get("android:windowSplashScreenBackground") == "#FCFBF8", "System splash background changed")
verify(data.get("android:windowSplashScreenAnimationDuration") == "0", "Unexpected system splash delay")

expected_shortcuts = {
    "calendar": "calendar_work", "tasks": "tasks", "notes": "notes",
    "cashbook": "cash_book", "new_note": "notes", "new_task": "tasks",
    "accounts": "cash_book",
}
shortcuts = ET.parse(RES / "xml/neo_shortcuts.xml").getroot()
real = {x.attrib.get(ANDROID + "shortcutId"): x.attrib.get(ANDROID + "icon") for x in shortcuts}
verify(set(real) == set(expected_shortcuts), "Previously supported static shortcut IDs changed")
for key, suffix in expected_shortcuts.items():
    verify(real[key] == "@drawable/exmore_shortcut_" + suffix, f"Wrong static shortcut: {key}")
controls = (ROOT / "app/src/main/java/com/todoshift/app/v11/ui/NeoShortcutControls.kt").read_text()
verify('Triple("settings",neoLabel("الإعدادات","Settings"),3)' in controls,
       "Settings pinned launcher shortcut does not target root tab 3")
for asset in ["calendar_work", "cash_book", "notes", "tasks", "settings"]:
    verify("R.drawable.exmore_shortcut_" + asset in controls, f"Missing dynamic shortcut artwork: {asset}")

for file in ["Neo4Shell.kt", "V11Home.kt", "V11CalendarUi.kt"]:
    txt = (ROOT / "app/src/main/java/com/todoshift/app/v11/ui" / file).read_text()
    verify("exmore_header_icon" in txt, f"Approved small header icon not used by {file}")
home = (ROOT / "app/src/main/java/com/todoshift/app/v11/ui/V11Home.kt").read_text()
verify("ExMoreBootSplash()" in home,
       "Boot-time branded splash composition missing")
verify("if (bootFailure)" in home and "SafeStorageRecovery(" in home,
       "Boot error recovery must remain available")

manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
app = manifest.find("application")
verify(app is not None and app.attrib.get(ANDROID + "label") == "@string/app_name", "App display name changed")
verify(app.attrib.get(ANDROID + "icon") == "@mipmap/ic_launcher", "Launcher resource path changed")
verify(app.attrib.get(ANDROID + "roundIcon") == "@mipmap/ic_launcher_round", "Round launcher path changed")
gradle = (ROOT / "app/build.gradle.kts").read_text()
verify('applicationId = "com.todoshift.app"' in gradle, "Application ID changed: update safety violation")
verify(re.search(r"\bversionCode\s*=\s*78\b", gradle) is not None, "v4.4.0 versionCode not 79")
verify('versionName = "4.4.0"' in gradle, "v4.4.0 versionName mismatched")
print("ExMore v4.4.0 brand QA PASS: approved SHA-256 images, full splash variants, launcher densities,"
      " adaptive icons, header wiring, static + pinned shortcuts, preserved app id and v72 identity")

# v4.4.0 complete application label across locales and compact headers.
for locale in ["values", "values-ar", "values-en"]:
    strings = ET.parse(RES / locale / "strings.xml").getroot()
    names = [e.text for e in strings if e.attrib.get("name") == "app_name"]
    verify(names == ["ExMore — Life Planner"], f"Launcher label not complete in {locale}")
header = (ROOT / "app/src/main/java/com/todoshift/app/v11/ui/Neo4Shell.kt").read_text()
verify('"Life Planner"' in header and 'append("More")' in header,
       "Split header not showing the complete ExMore Life Planner brand")
for name in ["V11Home.kt", "V11CalendarUi.kt"]:
    source = (ROOT / "app/src/main/java/com/todoshift/app/v11/ui" / name).read_text()
    verify('"ExMore — Life Planner"' in source, f"Full brand missing from {name}")
splash = (ROOT / "app/src/main/java/com/todoshift/app/v11/ui/ExMoreBootSplash.kt").read_text()
for item in ('text = "ExMore"', '"LIFE PLANNER"', '"Your Day. Your Work. Your Money."',
             "WindowInsets.safeDrawing", "R.drawable.exmore_splash_center"):
    verify(item in splash, f"Splash typography or safe-area missing: {item}")
print("PASS ExMore 4.4.0: complete launch labels, responsive headers and safe-area splash typography")
