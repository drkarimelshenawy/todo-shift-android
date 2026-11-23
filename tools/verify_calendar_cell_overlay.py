#!/usr/bin/env python3
"""Source regression gate for calendar cell paint coverage and transparent overlays.
No APK build, signing, version changes or data migrations.
"""
from pathlib import Path
root = Path(__file__).resolve().parent.parent
neo = (root / "app/src/main/java/com/todoshift/app/v11/ui/NeoCalendar.kt").read_text()
legacy = (root / "app/src/main/java/com/todoshift/app/v11/ui/V11CalendarUi.kt").read_text()
coverage = (root / "app/src/main/java/com/todoshift/app/v11/Neo4ShiftPaint.kt").read_text()
month = neo.split("    Surface(\n        modifier\n            .alpha(if (muted)", 1)[1].split("\nfun neoHexColor(hex: String)", 1)[0]
for required in ("Neo4ShiftFill(", "Modifier.fillMaxSize()", "val overlayShadow", "shadow = overlayShadow", "Date is a transparent overlay"):
    assert required in month, "missing overlay rule: " + required
for forbidden in (
    "Modifier.background(c.surface.copy(alpha",
    "Modifier.background(c.surface.copy(alpha=",
    ".background(\n                                                c.surface.copy",
):
    assert forbidden not in month, "paint-obscuring overlay background: " + forbidden
assert month.index("Neo4ShiftFill(") < month.index("Text(\n                listOfNotNull(weekdayLabel"), "Date must be composited above shift paint"
assert ".background(Color.White.copy(alpha = 0.90f))" not in legacy, "Legacy date label still has an opaque mask"
assert "val coverage = neo4Coverage(segment.kind)" in legacy, "Legacy shifts don't use approved visual bands"
for spec in (
    "ShiftKind.FULL_DAY -> Neo4Coverage(0f, 1f)",
    "ShiftKind.LONG -> Neo4Coverage(0f, 0.5f)",
    "ShiftKind.MORNING -> Neo4Coverage(0f, 0.25f)",
    "ShiftKind.EVENING -> Neo4Coverage(0.25f, 0.5f)",
    "ShiftKind.NIGHT -> Neo4Coverage(0.5f, 1f)",
):
    assert spec in coverage, "Approved 24h visual fraction changed: " + spec
print("PASS: calendar date / note / badge overlays do not obscure approved full-height shift paint")
