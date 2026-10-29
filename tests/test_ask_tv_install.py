"""Ask TV scripts must target one explicit device and never load Mac credentials."""
from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
class AskTvInstallTest(unittest.TestCase):
    def test_installer_is_independent(self):
        text=(ROOT/'tools/install-ask-tv.sh').read_text()
        self.assertIn('TV_SERIAL:?',text)
        self.assertIn('adb -s "$TV_SERIAL" install -r',text)
        self.assertIn('com.amine.asktv/.MainActivity',text)
        for forbidden in ['pm clear','pm disable','set-home','settings put','apps/family-home','apps/clock']: self.assertNotIn(forbidden,text)
    def test_helper_owns_loopback_forward_only(self):
        text=(ROOT/'tools/sign-in-ask-tv.sh').read_text()
        self.assertIn('TV_SERIAL:?',text)
        self.assertIn('forward --no-rebind tcp:1455 tcp:1455',text)
        self.assertIn('forward --remove tcp:1455',text)
        self.assertIn('http://127.0.0.1:1455/start',text)
        self.assertIn('--ez authenticate true',text)
        for forbidden in ['.codex','.chatgpt','auth.json','access_token','refresh_token','id_token']: self.assertNotIn(forbidden,text)

class AskTvScriptExecutionTest(unittest.TestCase):
    def exercise(self, helper=False, collision=False, launch_error=False, existing_attempt=False):
        import os, shutil, subprocess, tempfile
        with tempfile.TemporaryDirectory() as temporary:
            root=Path(temporary)/'repo with spaces'
            tools=root/'tools'; tools.mkdir(parents=True)
            name='sign-in-ask-tv.sh' if helper else 'install-ask-tv.sh'
            shutil.copy2(ROOT/'tools'/name,tools/name)
            app=root/'apps/ask-tv'; app.mkdir(parents=True)
            (app/'gradlew').write_text('''#!/bin/sh
printf 'gradle %s\\n' "$*" >> "$CAPTURE"
mkdir -p "$APP/build/outputs/apk/release"
printf apk > "$APP/build/outputs/apk/release/ask-tv-release.apk"
''')
            (app/'gradlew').chmod(0o755)
            binary=root/'bin'; binary.mkdir()
            commands={
                'adb': '''#!/bin/sh
printf 'adb %s\\n' "$*" >> "$CAPTURE"
case "$*" in
 *'forward --list'*) if [ "$COLLISION" = 1 ]; then echo 'other-device tcp:1455 tcp:9999'; elif [ -f "$FLAG" ]; then echo 'test-device tcp:1455 tcp:1455'; fi ;;
 *'forward --no-rebind'*) touch "$FLAG" ;;
 *'forward --remove'*) rm -f "$FLAG" ;;
 *'am start'*) touch "$AUTH_STARTED"; if [ "$LAUNCH_ERROR" = 1 ]; then echo 'Error: no activity'; else echo 'Status: ok'; fi ;;
 *install*) echo Success ;;
 *get-state*) echo device ;;
esac
''',
                'curl': '''#!/bin/sh
if [ "$EXISTING_ATTEMPT" != 1 ] && [ ! -f "$AUTH_STARTED" ]; then exit 7; fi
if [ -f "$POLL" ]; then exit 7; fi
touch "$POLL"
exit 0
''',
                'open': '#!/bin/sh\nprintf "open %s\\n" "$*" >> "$CAPTURE"\n',
                'sleep': '#!/bin/sh\nexit 0\n',
                'python3': '#!/bin/sh\nexit 0\n',
            }
            for command,source in commands.items():
                (binary/command).write_text(source); (binary/command).chmod(0o755)
            capture=root/'capture'
            env=dict(os.environ,PATH=str(binary)+os.pathsep+os.environ['PATH'],CAPTURE=str(capture),APP=str(app),TV_SERIAL='test-device',JAVA_HOME=str(root/'jdk'),ANDROID_HOME=str(root/'sdk'),FLAG=str(root/'forward'),POLL=str(root/'poll'),AUTH_STARTED=str(root/'auth-started'),EXISTING_ATTEMPT=str(int(existing_attempt)),COLLISION=str(int(collision)),LAUNCH_ERROR=str(int(launch_error)))
            result=subprocess.run(['bash',str(tools/name)],env=env,capture_output=True,text=True)
            return result,capture.read_text()

    def test_install_targets_own_package_on_explicit_device(self):
        result,commands=self.exercise()
        self.assertEqual(0,result.returncode,result.stderr)
        self.assertIn('adb -s test-device install -r',commands)
        self.assertIn('ask-tv-release.apk',commands)
        self.assertNotIn('pm clear',commands)
        self.assertIn('testReleaseUnitTest lintRelease assembleRelease',commands)

    def test_launch_error_fails_install(self):
        result,_=self.exercise(launch_error=True)
        self.assertNotEqual(0,result.returncode)

    def test_helper_cleans_only_owned_forward(self):
        result,commands=self.exercise(helper=True)
        self.assertEqual(0,result.returncode,result.stderr)
        self.assertIn('forward --no-rebind tcp:1455 tcp:1455',commands)
        self.assertIn('open http://127.0.0.1:1455/start',commands)
        self.assertIn('adb -s test-device forward --remove tcp:1455',commands)

    def test_helper_preserves_existing_forward(self):
        result,commands=self.exercise(helper=True,collision=True)
        self.assertNotEqual(0,result.returncode)
        self.assertNotIn('forward --remove',commands)
        self.assertNotIn('am start',commands)

    def test_helper_preserves_an_existing_explicit_consent_attempt(self):
        result,commands=self.exercise(helper=True,existing_attempt=True)
        self.assertEqual(0,result.returncode,result.stderr)
        self.assertIn('open http://127.0.0.1:1455/start',commands)
        self.assertNotIn('am start',commands)
        self.assertIn('forward --remove tcp:1455',commands)
