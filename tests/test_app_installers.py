"""Exercise app installers in an isolated fake checkout without a TV or SDK."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class AppInstallersTest(unittest.TestCase):
    def exercise(self, script):
        with tempfile.TemporaryDirectory() as tmp:
            repo = Path(tmp) / "repo with spaces"
            target = repo / script
            target.parent.mkdir(parents=True)
            shutil.copy2(ROOT / script, target)
            (repo / "tools").mkdir(exist_ok=True)
            capture = repo / "capture.txt"
            gradle = repo / "gradlew"
            gradle.write_text('''#!/bin/sh
printf 'build %s\\n' "$*" >> "$CAPTURE"
case "$1" in
 :home:assembleRelease) apk=apps/home/build/outputs/apk/release/home-release.apk ;;
 :clock:assembleRelease) apk=apps/clock/build/outputs/apk/release/clock-release.apk ;;
 *) exit 9 ;;
esac
mkdir -p "$(dirname "$apk")"
printf 'fake APK' > "$apk"
''')
            gradle.chmod(0o755)
            client = repo / "tools/venv/bin/python"
            client.parent.mkdir(parents=True)
            client.write_text('''#!/bin/sh
printf 'client %s\\n' "$*" >> "$CAPTURE"
case "$2" in
 */b64push.py) test -f "$3" || exit 7 ;;
 *) case "$3" in
  --batch) cat >> "$CAPTURE" ;;
  *resolve-activity*) echo com.gathin.tvhome/.MainActivity ;;
  *'pm install'*) echo Success ;;
 esac ;;
esac
''')
            client.chmod(0o755)
            binary = repo / "bin"
            binary.mkdir()
            adb = binary / "adb"
            adb.write_text('''#!/bin/sh
printf 'adb %s\\n' "$*" >> "$CAPTURE"
if [ "$3" = push ]; then test -f "$4" || exit 7; fi
case "$*" in
 *'pm install'*) echo Success ;;
 *get-state*) echo device ;;
esac
''')
            adb.chmod(0o755)
            env = dict(os.environ, CAPTURE=str(capture), JAVA_HOME=str(repo / "jdk"),
                       ANDROID_HOME=str(repo / "sdk"), TV_HOST="fake-tv",
                       TV_SERIAL="fake-tv:12345", STOCK_LAUNCHER="none",
                       INPUT_KEY_SERVICE="0", HOME_KEY_SERVICE="0",
                       PATH=str(binary) + os.pathsep + os.environ["PATH"])
            result = subprocess.run(["bash", str(target)], cwd=tmp, env=env,
                                    capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stderr + result.stdout)
            return capture.read_text()

    def test_home_installer_uses_only_home_module_and_its_apk(self):
        commands = self.exercise("launcher/build-and-install.sh")
        self.assertIn("build :home:assembleRelease", commands)
        self.assertIn("apps/home/build/outputs/apk/release/home-release.apk", commands)
        self.assertNotIn(":clock:", commands)
        self.assertNotIn("com.amine.tvclock", commands)
        self.assertNotIn("pm disable-user", commands)

    def test_clock_installer_uses_only_clock_and_preserves_home(self):
        commands = self.exercise("tools/install-clock.sh")
        self.assertIn("build :clock:assembleRelease :clock:lintRelease", commands)
        self.assertIn("apps/clock/build/outputs/apk/release/clock-release.apk", commands)
        self.assertNotIn(":home:", commands)
        self.assertNotIn("set-home-activity", commands)
        self.assertNotIn("pm disable", commands)
        self.assertNotIn("com.gathin.tvhome", commands)
