"""Exercise the Cast-safe Home installer without a device or Android SDK."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class CastHomeInstallerTest(unittest.TestCase):
    def exercise(self, fail_stock=False):
        with tempfile.TemporaryDirectory() as tmp:
            repo = Path(tmp) / 'repo with spaces'
            (repo / 'tools').mkdir(parents=True)
            shutil.copy2(ROOT / 'tools/install-home-with-cast.sh', repo / 'tools')
            apk = repo / 'apps/home/build/outputs/apk/release/home-release.apk'
            apk.parent.mkdir(parents=True)
            apk.write_bytes(b'fake APK')
            (repo / 'gradlew').write_text('#!/bin/sh\nexit 0\n')
            (repo / 'gradlew').chmod(0o755)
            binary = repo / 'bin'
            binary.mkdir()
            adb = binary / 'adb'
            adb.write_text('''#!/usr/bin/env python3
import json, os, pathlib, sys
args = sys.argv[1:]
with open(os.environ['CAPTURE'], 'a') as f:
    f.write(' '.join(args) + '\\n')
if args[2] == 'push' and args[4].endswith('config.json'):
    pathlib.Path(os.environ['MERGED']).write_text(pathlib.Path(args[3]).read_text())
cmd = ' '.join(args[3:])
if 'pm enable --user 0 com.google.android.apps.tv.launcherx' in cmd and os.environ['FAIL_STOCK'] == '1':
    sys.exit(1)
if 'cat /sdcard/' in cmd:
    print(json.dumps({'dock_apps': ['my.player'], 'columns': 3, 'background_color': '#123456'}))
elif 'pm install' in cmd:
    print('Success')
elif 'pm list packages -e' in cmd:
    print('package:' + cmd.split()[-1])
elif 'cmd role get-role-holders' in cmd:
    print('com.gathin.tvhome')
elif 'resolve-activity' in cmd:
    print('com.google.android.apps.tv.launcherx/.home.HomeActivity')
elif 'settings get secure enabled_accessibility_services' in cmd:
    print('other.helper/.Service')
''')
            adb.chmod(0o755)
            capture = repo / 'capture'
            merged = repo / 'merged.json'
            env = dict(os.environ, PATH=str(binary) + os.pathsep + os.environ['PATH'],
                       TV_SERIAL='fake:12345', CAPTURE=str(capture), MERGED=str(merged),
                       FAIL_STOCK=str(int(fail_stock)))
            result = subprocess.run(['bash', str(repo / 'tools/install-home-with-cast.sh')],
                                    env=env, capture_output=True, text=True)
            return result, capture.read_text(), json.loads(merged.read_text()) if merged.exists() else None

    def test_keeps_cast_packages_and_existing_configuration(self):
        result, commands, config = self.exercise()
        self.assertEqual(result.returncode, 0, result.stderr + result.stdout)
        self.assertNotIn('pm disable', commands)
        self.assertLess(commands.index('pm enable --user 0 com.google.android.apps.tv.launcherx'),
                        commands.index('pm enable --user 0 com.google.android.tungsten.setupwraith'))
        self.assertEqual(config['dock_apps'], ['my.player'])
        self.assertEqual(config['columns'], 3)
        self.assertEqual(config['background_color'], '#123456')
        self.assertEqual(config['stock_home_redirect'], 'com.google.android.apps.tv.launcherx')
        self.assertIn('other.helper/.Service:com.gathin.tvhome/.HomeKeyService', commands)
        self.assertNotIn('am force-stop com.google.android.apps.mediashell', commands)

    def test_failed_stock_enable_never_enables_setupwraith(self):
        result, commands, _ = self.exercise(fail_stock=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertNotIn('pm enable --user 0 com.google.android.tungsten.setupwraith', commands)
