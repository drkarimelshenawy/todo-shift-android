#!/usr/bin/env python3
"""Android update/startup smoke using fresh UIAutomator node bounds."""
import json, pathlib, re, subprocess, sys, time, xml.etree.ElementTree as ET
APP = "com.todoshift.app"
OUT = pathlib.Path("device-smoke")
OUT.mkdir(exist_ok=True)
def adb(*args, binary=False):
    return subprocess.check_output(["adb", *args], timeout=45, text=not binary)
def shell(*args): return adb("shell", *args)
def dump():
    shell("uiautomator", "dump", "/sdcard/smoke.xml")
    return ET.fromstring(adb("exec-out", "cat", "/sdcard/smoke.xml"))
def snapshot(name):
    (OUT / (name + ".png")).write_bytes(adb("exec-out", "screencap", "-p", binary=True))
    (OUT / (name + ".xml")).write_text(ET.tostring(dump(), encoding="unicode"))
def wait(fn, seconds=45):
    end = time.monotonic() + seconds
    last = None
    while time.monotonic() < end:
        try:
            value = fn()
            if value is not None and value is not False: return value
        except (subprocess.SubprocessError, ET.ParseError, AssertionError) as exc: last = exc
        time.sleep(.4)
    raise AssertionError("Timed out: " + str(last))
def find(root, labels=(), cls=None):
    return next((n for n in root.iter("node") if
        (not labels or n.get("text") in labels or n.get("content-desc") in labels)
        and (not cls or n.get("class") == cls) and n.get("enabled") != "false"), None)
def tap(node):
    assert node is not None
    ns = list(map(int, re.findall(r"\d+", node.get("bounds", ""))))
    assert len(ns) == 4 and ns[2] > ns[0] and ns[3] > ns[1]
    shell("input", "tap", str((ns[0]+ns[2])//2), str((ns[1]+ns[3])//2))
def click(*labels): tap(wait(lambda: find(dump(), labels)))
def launch(activity="MainActivity"): shell("am", "start", "-W", "-n", APP + "/." + activity)
def loaded(): return find(dump(), ("Paint shifts", "رسم الورديات"))
def database():
    raw = adb("exec-out", "run-as", APP, "cat", "shared_prefs/todo_shift_v11.xml")
    return json.loads({n.get("name"):n.text for n in ET.fromstring(raw)}["database"])
def task(title): return next((t for t in database()["tasks"] if t["title"] == title), None)
def install(path):
    result = adb("install", "-r", str(path))
    assert "Success" in result, result
def run():
    old, candidate = map(pathlib.Path, sys.argv[1:3])
    adb("logcat", "-c")
    install(old); launch(); wait(loaded); snapshot("37-before-update")
    launch("QuickTaskActivity")
    tap(wait(lambda: find(dump(), cls="android.widget.EditText")))
    shell("input", "text", "UpgradeFixture")
    shell("input", "keyevent", "KEYCODE_BACK")
    click("Save", "حفظ")
    wait(lambda: task("UpgradeFixture"))
    shell("am", "force-stop", APP)
    before = database()
    fixture_id = task("UpgradeFixture")["id"]
    (OUT / "before.json").write_text(json.dumps(before, ensure_ascii=False, indent=2))
    install(candidate); launch(); wait(loaded)
    after = database()
    for key in ("calendars", "days", "dayItems", "tasks", "payments", "settlements"):
        assert before[key] == after[key], "Upgrade changed " + key
    assert task("UpgradeFixture")["id"] == fixture_id
    snapshot("38-after-update")
    click("Tasks", "المهام"); click("UpgradeFixture")
    wait(lambda: find(dump(), ("Title", "العنوان"))); snapshot("38-task-editor")
    click("Cancel", "إلغاء")
    def task_checkbox():
        root = dump()
        for n in root.iter("node"):
            if any(c.get("text") == "UpgradeFixture" for c in n.iter("node")):
                boxes = [c for c in n.iter("node") if c.get("class") == "android.widget.CheckBox"]
                if len(boxes) == 1: return boxes[0]
        return None
    tap(wait(task_checkbox)); wait(lambda: task("UpgradeFixture")["status"] == "COMPLETED")
    snapshot("38-completed-task")
    tap(wait(task_checkbox)); wait(lambda: task("UpgradeFixture")["status"] == "TODO")
    for i in range(3):
        shell("am", "force-stop", APP); launch(); wait(loaded)
        assert task("UpgradeFixture")["status"] == "TODO"
        snapshot("38-restart-" + str(i+1))
    log = adb("logcat", "-d", "-v", "threadtime")
    (OUT / "logcat.txt").write_text(log)
    assert not re.search(r"(ANR in com\.todoshift\.app|Process: com\.todoshift\.app|am_anr.*com\.todoshift\.app)", log)
    (OUT / "RESULT.txt").write_text("PASS: vc37 -> vc38 update; task save/edit/toggle/restart; fixture IDs/data retained; no observed app crash/ANR.\n")
    print((OUT / "RESULT.txt").read_text())
try: run()
except Exception:
    try: snapshot("failure")
    except Exception: pass
    try: (OUT / "logcat.txt").write_text(adb("logcat", "-d", "-v", "threadtime"))
    except Exception: pass
    raise
