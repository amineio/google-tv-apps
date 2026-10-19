"""Opt-in acceptance test. Operates the connected TV and leaves the clock on."""
import os
import re
import subprocess
import time
import unittest
import xml.etree.ElementTree as ET

SERIAL = os.environ["TV_SERIAL"]
PACKAGE = "com.amine.tvclock"


def adb(*args):
    return subprocess.run(
        ["adb", "-s", SERIAL, *args], check=True, capture_output=True, text=True
    ).stdout


def wait_for(predicate, timeout=12):
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        if predicate():
            return
        time.sleep(0.3)
    raise AssertionError("Device did not reach the expected state")


def has_overlay():
    return "TV Clock overlay" in adb("shell", "dumpsys", "window", "windows")


def control(name):
    adb("shell", "uiautomator", "dump", "/sdcard/tvclock-test.xml")
    root = ET.fromstring(adb("shell", "cat", "/sdcard/tvclock-test.xml"))
    for node in root.iter("node"):
        if node.get("resource-id") == PACKAGE + ":id/" + name:
            return node
    raise AssertionError("Missing clock control: " + name)


def open_clock():
    adb("shell", "am", "start", "-W", "--activity-clear-top", "-n", PACKAGE + "/.MainActivity")
    click("clock_settings")


def switch():
    return control("clock_switch")


def click(name):
    node = control(name)
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))


def frame():
    windows = adb("shell", "dumpsys", "window", "windows")
    overlay = windows.split("TV Clock overlay}:", 1)[1].split("Window #", 1)[0]
    match = re.search(r" frame=\[(\d+),(\d+)\]\[(\d+),(\d+)\]", overlay)
    if not match:
        raise AssertionError("Clock has no visible frame")
    return tuple(map(int, match.groups()))


def set_clock(enabled):
    node = switch()
    if (node.get("checked") == "true") != enabled:
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
        adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
    wait_for(lambda: has_overlay() == enabled)


class ClockDeviceTest(unittest.TestCase):
    def test_layout_controls_apply_immediately_and_stay_saved(self):
        open_clock()
        self.assertEqual("Lower", control("position_down").get("text"))
        set_clock(True)
        click("reset_layout")
        original = frame()
        click("position_down")
        wait_for(lambda: frame()[1] > original[1])
        click("position_up")
        wait_for(lambda: frame()[1] == original[1])
        click("size_up")
        wait_for(lambda: frame()[3] - frame()[1] > original[3] - original[1])
        click("position_down")
        wait_for(lambda: frame()[1] > original[1])
        saved = frame()
        adb("shell", "input", "keyevent", "KEYCODE_HOME")
        open_clock()
        self.assertEqual(saved, frame())
        self.assertIn("24", control("size_value").get("text"))
        click("size_down")
        wait_for(lambda: frame()[3] - frame()[1] == original[3] - original[1])
        click("reset_layout")
        wait_for(lambda: frame() == original)
        set_clock(False)
        click("position_down")
        click("size_up")
        self.assertFalse(has_overlay(), "Changing layout must not turn on a disabled clock")
        set_clock(True)
        self.assertGreater(frame()[1], original[1])
        self.assertGreater(frame()[3] - frame()[1], original[3] - original[1])
        click("reset_layout")
        adb("shell", "input", "keyevent", "KEYCODE_HOME")

    def test_clock_can_stop_and_cover_other_apps_without_replacing_home(self):
        self.assertIn("package:" + PACKAGE, adb("shell", "pm", "list", "packages", PACKAGE),
                      "TV Clock is not installed")
        home_args = ("shell", "cmd", "package", "resolve-activity", "--brief", "-a",
                     "android.intent.action.MAIN", "-c", "android.intent.category.HOME")
        home = adb(*home_args)
        accessibility = adb("shell", "settings", "get", "secure", "enabled_accessibility_services")
        open_clock()
        set_clock(False)
        self.assertFalse(has_overlay())
        adb("shell", "input", "keyevent", "KEYCODE_DPAD_UP")
        adb("shell", "input", "keyevent", "KEYCODE_DPAD_UP")
        self.assertEqual("true", switch().get("focused"))
        adb("shell", "input", "keyevent", "KEYCODE_DPAD_CENTER")
        wait_for(has_overlay)
        adb("shell", "input", "keyevent", "KEYCODE_HOME")
        wait_for(lambda: re.search(r"topResumedActivity=.*com\.gathin\.tvhome/\.MainActivity",
                                  adb("shell", "dumpsys", "activity", "activities")))
        self.assertTrue(has_overlay())
        adb("shell", "am", "start", "-W", "-a", "android.settings.SETTINGS")
        self.assertTrue(has_overlay())
        open_clock()
        set_clock(False)
        self.assertFalse(has_overlay())
        set_clock(True)
        adb("shell", "input", "keyevent", "KEYCODE_HOME")
        open_clock()
        self.assertEqual("true", switch().get("checked"))
        self.assertEqual(home, adb(*home_args))
        self.assertEqual(accessibility, adb("shell", "settings", "get", "secure",
                                            "enabled_accessibility_services"))
        adb("shell", "input", "keyevent", "KEYCODE_HOME")


if __name__ == "__main__":
    unittest.main()
