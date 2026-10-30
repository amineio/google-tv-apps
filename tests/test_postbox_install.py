"""Exercise the Postbox installer with fake build and ADB commands only."""

import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]


class PostboxInstallTest(unittest.TestCase):
    def exercise(self, failure="", serial="emulator-test"):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary) / "repo with spaces"
            scripts = root / "tools"
            scripts.mkdir(parents=True)
            script = scripts / "install-postbox.sh"
            shutil.copy2(ROOT / "tools/install-postbox.sh", script)
            app = root / "apps/postbox"
            app.mkdir(parents=True)
            wrapper = app / "gradlew"
            wrapper.write_text("""#!/bin/sh
printf 'gradle %s\\n' "$*" >> "$POSTBOX_COMMANDS"
if [ "$POSTBOX_FAILURE" = build ]; then exit 7; fi
mkdir -p "$POSTBOX_APP/build/outputs/apk/release"
printf test-apk > "$POSTBOX_APP/build/outputs/apk/release/postbox-release.apk"
""")
            wrapper.chmod(0o755)
            binaries = root / "bin"
            binaries.mkdir()
            adb = binaries / "adb"
            adb.write_text("""#!/bin/sh
printf 'adb %s\\n' "$*" >> "$POSTBOX_COMMANDS"
case "$*" in
 *get-state*)
   if [ "$POSTBOX_FAILURE" = device ]; then echo offline; exit 1; fi
   echo device ;;
 *'install -r'*)
   if [ "$POSTBOX_FAILURE" = install ]; then echo 'Failure [TEST]'; exit 1; fi
   echo 'Performing Streamed Install'; echo Success ;;
 *'am start'*)
   if [ "$POSTBOX_FAILURE" = launch ]; then echo 'Error: no activity'; else echo 'Status: ok'; fi ;;
 *'dumpsys package'*) echo 'versionCode=1 minSdk=34 targetSdk=37'; echo 'versionName=0.1' ;;
esac
""")
            adb.chmod(0o755)
            commands = root / "commands"
            environment = dict(
                os.environ,
                PATH=str(binaries) + os.pathsep + os.environ["PATH"],
                POSTBOX_COMMANDS=str(commands),
                POSTBOX_APP=str(app),
                POSTBOX_FAILURE=failure,
                JAVA_HOME=str(root / "jdk"),
                ANDROID_HOME=str(root / "sdk"),
            )
            if serial:
                environment["TV_SERIAL"] = serial
            else:
                environment.pop("TV_SERIAL", None)
            result = subprocess.run(
                ["bash", str(script)], env=environment, capture_output=True, text=True
            )
            return result, commands.read_text() if commands.exists() else ""

    def test_requires_explicit_serial_before_any_command(self):
        result, commands = self.exercise(serial="")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("TV_SERIAL", result.stderr)
        self.assertEqual("", commands)

    def test_success_checks_builds_installs_and_launches_only_postbox(self):
        result, commands = self.exercise()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn("testReleaseUnitTest lintRelease assembleRelease", commands)
        self.assertIn("postbox-release.apk", commands)
        self.assertIn("com.amine.postbox/.MainActivity", commands)
        self.assertIn("versionName=0.1", result.stdout)
        adb_commands = [line for line in commands.splitlines() if line.startswith("adb ")]
        self.assertTrue(adb_commands)
        self.assertTrue(all(line.startswith("adb -s emulator-test ") for line in adb_commands))
        for forbidden in ("pm clear", "pm disable", "set-home", "settings put", "com.amine.familyhome", "com.amine.tvclock"):
            self.assertNotIn(forbidden, commands)

    def test_unavailable_device_stops_before_build(self):
        result, commands = self.exercise(failure="device")
        self.assertNotEqual(0, result.returncode)
        self.assertNotIn("gradle ", commands)

    def test_failed_build_stops_before_install(self):
        result, commands = self.exercise(failure="build")
        self.assertNotEqual(0, result.returncode)
        self.assertNotIn("install -r", commands)

    def test_failed_install_stops_before_launch(self):
        result, commands = self.exercise(failure="install")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Failure [TEST]", result.stderr + result.stdout)
        self.assertNotIn("am start", commands)

    def test_failed_launch_is_reported(self):
        result, _ = self.exercise(failure="launch")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Postbox did not open", result.stderr)


if __name__ == "__main__":
    unittest.main()
