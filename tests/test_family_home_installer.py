"""Exercise Family Home installation without changing a TV."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class FamilyHomeInstallerTest(unittest.TestCase):
    def install(self, fail_install=False, helpers="other.helper/.Service:com.gathin.tvhome/.HomeKeyService", fail_launch=False):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder) / "repo with spaces"
            (root / "tools").mkdir(parents=True)
            script = root / "tools/install-family-home.sh"
            if not (ROOT / "tools/install-family-home.sh").exists():
                self.fail("Family Home installer is not implemented")
            shutil.copy2(ROOT / "tools/install-family-home.sh", script)
            app = root / "apps/family-home"
            app.mkdir(parents=True)
            wrapper = app / "gradlew"
            wrapper.write_text('''#!/bin/sh
mkdir -p "$APP/build/outputs/apk/release"
printf apk > "$APP/build/outputs/apk/release/family-home-release.apk"
''')
            wrapper.chmod(0o755)
            binary = root / "bin"
            binary.mkdir()
            adb = binary / "adb"
            adb.write_text('''#!/usr/bin/env python3
import json, os, sys
args=sys.argv[3:]
with open(os.environ['CAPTURE'], 'a') as f: f.write(json.dumps(args)+'\\n')
cmd=' '.join(args)
if cmd=='get-state': print('device')
elif 'am get-current-user' in cmd: print('0')
elif 'enabled_accessibility_services' in cmd and ' get ' in cmd: print(os.environ['HELPERS'])
elif 'accessibility_enabled' in cmd and ' get ' in cmd: print('1')
elif 'get-role-holders' in cmd:
    print('com.amine.familyhome' if os.path.exists(os.environ['ROLE_SET']) else 'com.gathin.tvhome')
elif 'set-home-activity' in cmd: open(os.environ['ROLE_SET'], 'w').write('yes')
elif args and args[0]=='install':
    if os.environ['FAIL_INSTALL']=='1':
        print('Failure [INSTALL_FAILED]'); sys.exit(1)
    print('Success')
elif 'pm list packages -e' in cmd: print('package:'+args[-1])
elif 'am start' in cmd: print('Error type 3: Activity class does not exist' if os.environ['FAIL_LAUNCH']=='1' else 'Status: ok')
''')
            adb.chmod(0o755)
            capture = root / "commands.jsonl"
            env = dict(os.environ, PATH=str(binary) + os.pathsep + os.environ["PATH"],
                       TV_SERIAL="fake-tv", JAVA_HOME=str(root / "jdk"), ANDROID_HOME=str(root / "sdk"),
                       APP=str(app), CAPTURE=str(capture), ROLE_SET=str(root / "role"), HELPERS=helpers,
                       FAIL_INSTALL="1" if fail_install else "0", FAIL_LAUNCH="1" if fail_launch else "0")
            result = subprocess.run(["bash", str(script)], cwd=folder, env=env, text=True, capture_output=True)
            calls = [json.loads(line) for line in capture.read_text().splitlines()] if capture.exists() else []
            return result, calls

    def test_installs_only_new_app_and_preserves_cast_and_unrelated_helper(self):
        result, calls = self.install()
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        installed = [call for call in calls if call[0] == "install"]
        self.assertEqual(1, len(installed))
        self.assertIn("family-home-release.apk", installed[0][-1])
        writes = [" ".join(call) for call in calls if "put" in call]
        helper = next(text for text in writes if "enabled_accessibility_services" in text)
        self.assertIn("other.helper/.Service", helper)
        self.assertIn("com.amine.familyhome/.FamilyAccessService", helper)
        self.assertNotIn("com.gathin.tvhome", helper)
        commands = "\n".join(" ".join(call) for call in calls)
        self.assertIn("pm enable --user 0 com.google.android.apps.tv.launcherx", commands)
        self.assertIn("pm enable --user 0 com.google.android.tungsten.setupwraith", commands)
        self.assertNotIn("pm disable", commands)
        self.assertNotIn("pm clear", commands)

    def test_install_failure_does_not_change_home_or_helpers(self):
        result, calls = self.install(fail_install=True)
        self.assertNotEqual(0, result.returncode)
        commands = "\n".join(" ".join(call) for call in calls)
        self.assertNotIn("set-home-activity", commands)
        self.assertNotIn("settings put", commands)

    def test_invalid_helper_list_stops_before_installation(self):
        result, calls = self.install(helpers="unsafe helper; command")
        self.assertNotEqual(0, result.returncode)
        self.assertFalse(any(call[0] == "install" for call in calls))

    def test_zero_exit_launch_error_is_not_reported_as_success(self):
        result, _ = self.install(fail_launch=True)
        self.assertNotEqual(0, result.returncode)
        self.assertIn("could not open", result.stderr)


if __name__ == "__main__":
    unittest.main()
