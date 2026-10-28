"""Exercise the independent Battleship installer without a real device."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class BattleshipInstallerTest(unittest.TestCase):
    def exercise(self, launch_error=False):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp) / "repo with spaces"
            script = root / "tools/install-battleship.sh"
            script.parent.mkdir(parents=True)
            shutil.copy2(ROOT / "tools/install-battleship.sh", script)
            app = root / "apps/battleship"
            app.mkdir(parents=True)
            gradle = app / "gradlew"
            gradle.write_text('''#!/bin/sh
printf 'gradle %s\\n' "$*" >> "$CAPTURE"
mkdir -p "$APP/build/outputs/apk/release"
printf apk > "$APP/build/outputs/apk/release/battleship-release.apk"
''')
            gradle.chmod(0o755)
            binary = root / "bin"
            binary.mkdir()
            adb = binary / "adb"
            adb.write_text('''#!/bin/sh
printf 'adb %s\\n' "$*" >> "$CAPTURE"
case "$*" in
 *get-state*) echo device ;;
 *install*) echo Success ;;
 *'am start'*) if [ "$LAUNCH_ERROR" = 1 ]; then echo 'Error: Activity class does not exist.'; else echo 'Status: ok'; fi ;;
esac
''')
            adb.chmod(0o755)
            capture = root / "capture"
            env = dict(os.environ, PATH=str(binary) + os.pathsep + os.environ["PATH"],
                       CAPTURE=str(capture), APP=str(app), TV_SERIAL="test-emulator",
                       JAVA_HOME=str(root / "jdk"), ANDROID_HOME=str(root / "sdk"),
                       LAUNCH_ERROR=str(int(launch_error)))
            result = subprocess.run(["bash", str(script)], env=env, capture_output=True, text=True)
            return result, capture.read_text()

    def test_installs_only_battleship_and_keeps_device_configuration(self):
        result, commands = self.exercise()
        self.assertEqual(0, result.returncode, result.stderr + result.stdout)
        self.assertIn("testReleaseUnitTest lintRelease assembleRelease", commands)
        self.assertIn("battleship-release.apk", commands)
        self.assertIn("adb -s test-emulator", commands)
        self.assertNotIn("set-home", commands)
        self.assertNotIn("settings put", commands)
        self.assertNotIn("pm clear", commands)
        self.assertNotIn("pm disable", commands)

    def test_zero_exit_launch_error_is_a_failure(self):
        result, _ = self.exercise(launch_error=True)
        self.assertNotEqual(0, result.returncode)
