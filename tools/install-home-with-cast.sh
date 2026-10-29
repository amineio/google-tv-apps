#!/usr/bin/env bash
# Install the custom Home screen while preserving Google TV's Cast dependencies.
set -euo pipefail
: "${TV_SERIAL:?set TV_SERIAL to the connected TV address shown by adb devices}"
ROOT=$(cd "$(dirname "$0")/.." && pwd)
STOCK=com.google.android.apps.tv.launcherx
PKG=com.gathin.tvhome
CONFIG=/sdcard/Android/data/$PKG/files/config.json
SERVICE=$PKG/.HomeKeyService
tv() { adb -s "$TV_SERIAL" shell "$@"; }
if [ -z "${JAVA_HOME:-}" ] && [ -d /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ]; then
  export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
fi
if [ -z "${ANDROID_HOME:-}" ]; then
  if [ -d "$HOME/Library/Android/sdk" ]; then export ANDROID_HOME="$HOME/Library/Android/sdk"
  else export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools; fi
fi
adb -s "$TV_SERIAL" get-state
cd "$ROOT"
./gradlew :home:assembleRelease :home:lintRelease --console=plain
BACKUP=$(mktemp -d "${TMPDIR:-/tmp}/tvhome-cast.XXXXXX")
chmod 700 "$BACKUP"
printf 'Device configuration backup: %s\n' "$BACKUP"
tv "if test -f $CONFIG; then cat $CONFIG; else echo '{}'; fi" > "$BACKUP/config-before.json"
tv 'settings get secure enabled_accessibility_services' | tr -d '\r' > "$BACKUP/services-before.txt"
tv 'cmd role get-role-holders --user 0 android.app.role.HOME' > "$BACKUP/home-role-before.txt"
# Fail before installation if the existing configuration is malformed. Preserve every other key.
python3 - "$BACKUP" <<'PY'
import json, pathlib, re, sys
folder = pathlib.Path(sys.argv[1])
config = json.loads((folder / 'config-before.json').read_text())
if not isinstance(config, dict):
    raise SystemExit('The existing Home configuration must be a JSON object.')
config['stock_home_redirect'] = 'com.google.android.apps.tv.launcherx'
(folder / 'config.json').write_text(json.dumps(config, indent=2) + '\n')
services = (folder / 'services-before.txt').read_text().strip()
if services not in ('', 'null') and not re.fullmatch(r'[A-Za-z0-9_.$/:]+', services):
    raise SystemExit('The accessibility service list contains unexpected characters.')
PY
APK=apps/home/build/outputs/apk/release/home-release.apk
adb -s "$TV_SERIAL" push "$APK" /data/local/tmp/tvhome.apk
OUT=$(tv 'pm install -r /data/local/tmp/tvhome.apk')
printf '%s\n' "$OUT"
case "$OUT" in *Success*) ;; *) echo 'Home installation failed.' >&2; exit 1 ;; esac
tv 'rm -f /data/local/tmp/tvhome.apk'
# Separate commands: never enable the recovery package until its stock Home is available.
tv "pm enable --user 0 $STOCK"
tv "pm list packages -e --user 0 $STOCK" | tr -d '\r' | grep -qx "package:$STOCK"
tv 'pm enable --user 0 com.google.android.tungsten.setupwraith'
tv 'pm list packages -e --user 0 com.google.android.tungsten.setupwraith' | tr -d '\r' | grep -qx 'package:com.google.android.tungsten.setupwraith'
HOME_NOW=$(tv 'cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME' | tr -d '\r' | tail -n 1)
case "$HOME_NOW" in "$STOCK/"*) ;; *) echo "Unexpected system Home: $HOME_NOW" >&2; exit 1 ;; esac
tv "mkdir -p /sdcard/Android/data/$PKG/files"
adb -s "$TV_SERIAL" push "$BACKUP/config.json" "$CONFIG"
tv "cmd package set-home-activity --user 0 $PKG/.MainActivity"
tv 'cmd role get-role-holders --user 0 android.app.role.HOME' | tr -d '\r' | grep -qx "$PKG"
# Rebind the helper after reading the new configuration; retain all other accessibility helpers.
CURRENT=$(tv 'settings get secure enabled_accessibility_services' | tr -d '\r')
OTHERS=$(python3 - "$CURRENT" <<'PY'
import re, sys
value = sys.argv[1]
if value not in ('', 'null') and not re.fullmatch(r'[A-Za-z0-9_.$/:]+', value):
    raise SystemExit('The accessibility service list contains unexpected characters.')
print(':'.join(s for s in value.split(':') if s and s != 'null' and s not in (
    'com.gathin.tvhome/.HomeKeyService', 'com.gathin.tvhome/com.gathin.tvhome.HomeKeyService')))
PY
)
tv "settings put secure enabled_accessibility_services '$OTHERS'"
SERVICES=${OTHERS:+$OTHERS:}$SERVICE
tv "settings put secure enabled_accessibility_services '$SERVICES'"
tv 'settings put secure accessibility_enabled 1'
# Ordinary activity, not ACTION_MAIN: keep launcherx as Android's home process for remote app buttons.
tv "am start -W -n $PKG/.MainActivity -c android.intent.category.HOME"
echo 'Installed. Test Home on the remote, then cast from the phone.'
echo 'Restore Google TV Home: adb -s "$TV_SERIAL" shell cmd package set-home-activity --user 0 com.google.android.apps.tv.launcherx/.home.HomeActivity'
