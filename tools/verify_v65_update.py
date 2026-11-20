"""Check the signed v64 to v65 upgrade on an isolated, rootable Android emulator."""
import json
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

APP = "com.todoshift.app"
PREF = f"/data/user/0/{APP}/shared_prefs/todo_shift_v11.xml"


def adb(*args):
    return subprocess.check_output(["adb", *args], text=True, timeout=60)


def start():
    adb("shell", "am", "start", "-W", "-n", APP + "/.MainActivity")
    for _ in range(45):
        result = adb("shell", "uiautomator", "dump", "/sdcard/update-screen.xml")
        if "dumped" in result.lower():
            tree = ET.fromstring(adb("exec-out", "cat", "/sdcard/update-screen.xml"))
            if any(node.get("text") in ("Settings", "الإعدادات") for node in tree.iter("node")):
                return
        time.sleep(1)
    raise AssertionError("Calendar navigation did not appear")


def read_db():
    root = ET.fromstring(adb("exec-out", "cat", PREF))
    node = root.find("string[@name='database']")
    return root, node, json.loads(node.text)


def main(baseline, candidate):
    adb("root")
    adb("wait-for-device")
    # The emulator is disposable. Begin with a clean v64 install.
    if adb("shell", "pm", "path", APP).strip():
        adb("uninstall", APP)
    adb("install", baseline)
    start()
    adb("shell", "am", "force-stop", APP)
    root, node, data = read_db()
    calendar = data["calendars"][0]["id"]
    task_id = "update-check-v65"
    data["tasks"].append(dict(id=task_id, title="Update fixture", details="",
                              date="2026-11-20", calendarId=calendar, status="TODO",
                              priority="NORMAL", repeat="NONE", createdAt=1, updatedAt=1,
                              completionHistory=[]))
    node.text = json.dumps(data, ensure_ascii=False)
    subprocess.run(["adb", "shell", "sh", "-c", "'cat > " + PREF + "'"],
                   input=ET.tostring(root), check=True, timeout=20)
    start()
    adb("shell", "am", "force-stop", APP)
    _, _, before = read_db()
    assert any(task.get("id") == task_id for task in before["tasks"])
    assert "Success" in adb("install", "-r", candidate)
    assert "versionCode=70 " in adb("shell", "dumpsys", "package", APP)
    start()
    _, _, after = read_db()
    for key in ("calendars", "days", "tasks", "dayItems", "payments", "settlements"):
        assert before[key] == after[key], f"Update changed {key}"
    print("PASS: signed v64 in-place update to versionCode 70 retained the task and record collections")


if __name__ == "__main__":
    main(*sys.argv[1:])
