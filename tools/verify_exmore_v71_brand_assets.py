#!/usr/bin/env python3
"""Validate approved v2 ExMore brand assets and exact Android splash wiring.

No image generation, rescaling, or assumption of physical-device acceptance.
"""
import hashlib
import re
import struct
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
DRAWABLE = RES / "drawable-nodpi"
A = "{http://schemas.android.com/apk/res/android}"

FILES = {
    "exmore_launcher_adaptive_foreground.png": "0424b146733ef3071142da0657c8ff1e92b9a3fec2c9d04b8842834fd8d70d4d",
    "exmore_launcher_monochrome.png": "b502456406f0809698c666466e984ce33301ad07ca3e7c32283307851b6607f8",
    "exmore_shortcut_calendar_work.png": "37509ab45a4070c03d5c4038170fb9f0d31f321e3afc47a2b944f641c236a68a",
    "exmore_shortcut_tasks.png": "86e22a6c2fe20f27219ea4a1329662611da557ae63cc0256295d762c0056c9eb",
    "exmore_shortcut_notes.png": "083f39f260e098263657b45c1775aef5617a6d844101298675e1469e13ea0b2c",
    "exmore_shortcut_cash_book.png": "4ae144ee2b1f3505b73dae7adfaf4181aa3493db64a9b4e996c3b163c9a18e31",
    "exmore_splash_center.png": "0424b146733ef3071142da0657c8ff1e92b9a3fec2c9d04b8842834fd8d70d4d",
}

def check(test: bool, message: str):
    if not test:
        raise SystemExit("ExMore v71 brand QA FAILED: " + message)

for name, expected in FILES.items():
    image = (DRAWABLE / name).read_bytes()
    check(hashlib.sha256(image).hexdigest() == expected, name + ": non-approved image bytes")
    check(image[:8] == b"\x89PNG\r\n\x1a\n", name + ": invalid PNG header")
    width, height = struct.unpack(">II", image[16:24])
    check((width, height) == (512, 512), name + ": not 512x512")

# Fallback launcher images must be present for older devices and use correct sizes.
for density, size in [("mdpi",48), ("hdpi",72), ("xhdpi",96), ("xxhdpi",144), ("xxxhdpi",192)]:
    for base in ("ic_launcher.png", "ic_launcher_round.png"):
        png = (RES / ("mipmap-" + density) / base).read_bytes()
        check(png[:8] == b"\x89PNG\r\n\x1a\n", base + ": fallback PNG invalid")
        w,h = struct.unpack(">II", png[16:24])
        check((w,h) == (size,size), density + "/" + base + ": wrong size")

for version in ("v26","v33"):
    for base in ("ic_launcher.xml","ic_launcher_round.xml"):
        icon = ET.parse(RES / ("mipmap-anydpi-" + version) / base).getroot()
        check(icon.tag == "adaptive-icon", version + ": not adaptive icon")
        children = {child.tag:child.attrib.get(A + "drawable") for child in icon}
        check(children.get("background") == "@drawable/exmore_icon_background",
              version + ": incorrect adaptive background")
        check(children.get("foreground") == "@drawable/exmore_launcher_adaptive_foreground",
              version + ": incorrect adaptive foreground")
        check(children.get("monochrome") == "@drawable/exmore_launcher_monochrome",
              version + ": incorrect adaptive monochrome")

back = ET.parse(RES / "drawable/exmore_icon_background.xml").getroot()
check(any(n.attrib.get(A + "color") == "#071F45" for n in back.iter()),
      "adaptive background mismatch")
legacy = ET.parse(RES / "drawable/neo41_splash.xml").getroot()
check(any(n.attrib.get(A + "color") == "#071F45" for n in legacy.iter()),
      "legacy splash background mismatch")
check(any(n.attrib.get(A + "drawable") == "@drawable/exmore_splash_center" for n in legacy.iter()),
      "legacy splash foreground mismatch")
system = ET.parse(RES / "values-v31/themes.xml").getroot()
data = {n.attrib.get("name"): (n.text or "").strip() for n in system.iter("item")}
check(data.get("android:windowSplashScreenBackground") == "#071F45",
      "Android 12+ splash background mismatch")
check(data.get("android:windowSplashScreenAnimatedIcon") == "@drawable/exmore_splash_center",
      "Android 12+ splash icon mismatch")
check(data.get("android:windowSplashScreenAnimationDuration") == "0",
      "unapproved animation requested")
check(data.get("android:windowSplashScreenIconBackgroundColor") == "#071F45",
      "Android 12+ splash icon background mismatch")

manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
application = manifest.find("application")
check(application is not None and application.attrib.get(A + "label") == "ExMore",
      "visible app name not ExMore")
check(application.attrib.get(A + "icon") == "@mipmap/ic_launcher",
      "launcher icon path changed")
check(application.attrib.get(A + "roundIcon") == "@mipmap/ic_launcher_round",
      "round icon path changed")

expected = {"calendar":"calendar_work", "tasks":"tasks", "notes":"notes",
            "cashbook":"cash_book", "new_note":"notes", "new_task":"tasks", "accounts":"cash_book"}
shortcuts = ET.parse(RES / "xml/neo_shortcuts.xml").getroot()
actual = {s.attrib.get(A+"shortcutId"):s.attrib.get(A+"icon") for s in shortcuts}
check(set(actual) == set(expected), "launcher shortcut IDs unexpectedly changed")
for shortcut, icon in expected.items():
    check(actual[shortcut] == "@drawable/exmore_shortcut_" + icon,
          "wrong icon for shortcut " + shortcut)
check("R.drawable.exmore_shortcut_notes" in (
      ROOT / "app/src/main/java/com/exmore/app/v11/ui/NeoShortcutControls.kt").read_text(),
      "dynamic Notes icon missing")

gradle = (ROOT / "app/build.gradle.kts").read_text()
check(re.search(r"\bversionCode\s*=\s*77\b", gradle) is not None,
      "versionCode not incremented from stable v70")
check("versionName = \"4.33.0-premium-v71-exmore-branding\"" in gradle,
      "v71 version name mismatch")
print("ExMore v71 branding checks PASSED: original PNG hashes, densities, adaptive icons, "
      "both splash themes, launcher labels, static/dynamic shortcuts and version 77.")
