#!/usr/bin/env bash
# Build and install TV Clock without changing the default home screen.
set -euo pipefail
: "${TV_SERIAL:?set TV_SERIAL to the connected TV address shown by adb devices}"
ROOT=$(cd "$(dirname "$0")/.." && pwd)
if [ -z "${JAVA_HOME:-}" ] && [ -d /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ]; then
  export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
fi
if [ -z "${ANDROID_HOME:-}" ]; then
  if [ -d "$HOME/Library/Android/sdk" ]; then
    export ANDROID_HOME="$HOME/Library/Android/sdk"
  else
    export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
  fi
fi
adb -s "$TV_SERIAL" get-state
cd "$ROOT/launcher"
./gradlew :clock:assembleRelease :clock:lintRelease --console=plain
APK="$ROOT/launcher/clock/build/outputs/apk/release/clock-release.apk"
adb -s "$TV_SERIAL" push "$APK" /data/local/tmp/tv-clock.apk
OUT=$(adb -s "$TV_SERIAL" shell pm install -r /data/local/tmp/tv-clock.apk)
printf '%s\n' "$OUT"
case "$OUT" in *Success*) ;; *) exit 1 ;; esac
adb -s "$TV_SERIAL" shell rm -f /data/local/tmp/tv-clock.apk
adb -s "$TV_SERIAL" shell appops set --user 0 com.amine.tvclock SYSTEM_ALERT_WINDOW allow
adb -s "$TV_SERIAL" shell appops set --user 0 com.amine.tvclock SCHEDULE_EXACT_ALARM allow
adb -s "$TV_SERIAL" shell am start -W -n com.amine.tvclock/.MainActivity
echo 'TV Clock is open. Use Clock appearance for the clock, or Time limits for a timer and bedtime.'
