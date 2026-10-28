#!/usr/bin/env bash
# Install only Battleship. Leave Home routing and the other apps unchanged.
set -euo pipefail
: "${TV_SERIAL:?set TV_SERIAL to the connected TV or emulator shown by adb devices}"
ROOT=$(cd "$(dirname "$0")/.." && pwd)
APP="$ROOT/apps/battleship"
if [ -z "${JAVA_HOME:-}" ] && [ -d /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ]; then
  export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
fi
if [ -z "${ANDROID_HOME:-}" ]; then
  if [ -d "$HOME/Library/Android/sdk" ]; then export ANDROID_HOME="$HOME/Library/Android/sdk"
  else export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools; fi
fi
adb -s "$TV_SERIAL" get-state
"$APP/gradlew" -p "$APP" testReleaseUnitTest lintRelease assembleRelease --console=plain
INSTALL=$(adb -s "$TV_SERIAL" install -r "$APP/build/outputs/apk/release/battleship-release.apk")
printf '%s\n' "$INSTALL"
case "$INSTALL" in *Success*) ;; *) echo 'Battleship installation failed.' >&2; exit 1 ;; esac
START=$(adb -s "$TV_SERIAL" shell am start -W -n com.amine.battleship/.MainActivity 2>&1)
printf '%s\n' "$START"
if ! printf '%s\n' "$START" | grep -q '^Status: ok'; then
  echo 'Battleship did not open.' >&2
  exit 1
fi
