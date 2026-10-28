#!/usr/bin/env bash
# Install Family Home while preserving Google TV's Cast services and unrelated apps.
set -euo pipefail
: "${TV_SERIAL:?set TV_SERIAL to the connected TV address shown by adb devices}"
ROOT=$(cd "$(dirname "$0")/.." && pwd)
APP="$ROOT/apps/family-home"
PKG=com.amine.familyhome
SERVICE=$PKG/.FamilyAccessService
if [ -z "${JAVA_HOME:-}" ] && [ -d /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ]; then
  export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
fi
if [ -z "${ANDROID_HOME:-}" ]; then
  if [ -d "$HOME/Library/Android/sdk" ]; then export ANDROID_HOME="$HOME/Library/Android/sdk"
  else export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools; fi
fi
tv() { adb -s "$TV_SERIAL" shell "$@"; }
adb -s "$TV_SERIAL" get-state
USER_ID=$(tv am get-current-user | tr -d '\r')
case "$USER_ID" in ''|*[!0-9]*) echo 'Cannot identify the active Android user.' >&2; exit 1 ;; esac
"$APP/gradlew" -p "$APP" testReleaseUnitTest lintRelease assembleRelease --console=plain
mkdir -p "$ROOT/backup"
BACKUP=$(mktemp -d "$ROOT/backup/family-home-install.XXXXXX")
chmod 700 "$BACKUP"
tv cmd role get-role-holders --user "$USER_ID" android.app.role.HOME > "$BACKUP/home-role-before.txt"
tv settings --user "$USER_ID" get secure enabled_accessibility_services | tr -d '\r' > "$BACKUP/helpers-before.txt"
tv settings --user "$USER_ID" get secure accessibility_enabled | tr -d '\r' > "$BACKUP/accessibility-before.txt"
tv getprop > "$BACKUP/device-properties.txt"
tv dumpsys package "$PKG" > "$BACKUP/package-before.txt"
python3 - "$BACKUP" "$USER_ID" <<'PY'
import json, pathlib, re, sys
folder = pathlib.Path(sys.argv[1])
role = (folder/'home-role-before.txt').read_text().strip()
helpers = (folder/'helpers-before.txt').read_text().strip()
enabled = (folder/'accessibility-before.txt').read_text().strip()
if role and not re.fullmatch(r'[A-Za-z0-9_.]+', role):
    raise SystemExit('Unexpected Home role. No device changes were applied.')
if helpers not in ('', 'null') and not re.fullmatch(r'[A-Za-z0-9_.$/:]+', helpers):
    raise SystemExit('Unexpected helper list. No device changes were applied.')
if enabled not in ('0', '1', 'null', ''):
    raise SystemExit('Unexpected accessibility state. No device changes were applied.')
competing = {'com.gathin.tvhome/.HomeKeyService', 'com.gathin.tvhome/com.gathin.tvhome.HomeKeyService',
             'com.amine.familyhome/.FamilyAccessService', 'com.amine.familyhome/com.amine.familyhome.FamilyAccessService'}
retained = [s for s in helpers.split(':') if s and s != 'null' and s not in competing]
retained.append('com.amine.familyhome/.FamilyAccessService')
(folder/'helpers-after.txt').write_text(':'.join(retained))
original = json.dumps({'user':sys.argv[2], 'role':role, 'helpers':helpers, 'enabled':enabled}, indent=2)
(folder/'original.json').write_text(original+'\n')
PY
APK="$APP/build/outputs/apk/release/family-home-release.apk"
shasum -a 256 "$APK" > "$BACKUP/apk-sha256.txt"
git -C "$ROOT" rev-parse HEAD > "$BACKUP/source-commit.txt" 2>/dev/null || true
OUT=$(adb -s "$TV_SERIAL" install -r "$APK")
printf '%s\n' "$OUT"
case "$OUT" in *Success*) ;; *) echo "Installation failed. Backup: $BACKUP" >&2; exit 1 ;; esac
tv pm enable --user "$USER_ID" com.google.android.apps.tv.launcherx
tv pm enable --user "$USER_ID" com.google.android.tungsten.setupwraith
tv cmd package set-home-activity --user "$USER_ID" "$PKG/.MainActivity"
HOME_ROLE=$(tv cmd role get-role-holders --user "$USER_ID" android.app.role.HOME | tr -d '\r')
if [ "$HOME_ROLE" != "$PKG" ]; then
  echo "Cannot select Family Home. Restore the captured role and helper list from $BACKUP/original.json." >&2
  exit 1
fi
SERVICES=$(cat "$BACKUP/helpers-after.txt")
# Rebind this helper after role assignment. Retain all unrelated accessibility services.
tv settings --user "$USER_ID" put secure enabled_accessibility_services "$SERVICES"
tv settings --user "$USER_ID" put secure accessibility_enabled 1
START=$(tv am start -W -n "$PKG/.MainActivity" -c android.intent.category.HOME 2>&1)
printf '%s\n' "$START"
if ! printf '%s\n' "$START" | grep -q '^Status: ok'; then
  echo "Family Home could not open. Device configuration backup: $BACKUP" >&2
  exit 1
fi
echo "Family Home is installed. Device configuration backup: $BACKUP"
echo 'Test Home, standby, child expiry, and casting from the phone.'
