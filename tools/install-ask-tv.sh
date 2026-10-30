#!/usr/bin/env bash
# Build and install AI Chat only. Use one explicit device.
set -euo pipefail
: "${TV_SERIAL:?set TV_SERIAL to the connected TV or emulator shown by adb devices}"
ROOT=$(cd "$(dirname "$0")/.." && pwd)
APP="$ROOT/apps/ask-tv"
if [ -z "${JAVA_HOME:-}" ] && [ -d /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ]; then
  export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
fi
if [ -z "${ANDROID_HOME:-}" ]; then
  if [ -d "$HOME/Library/Android/sdk" ]; then export ANDROID_HOME="$HOME/Library/Android/sdk"
  else export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools; fi
fi
adb -s "$TV_SERIAL" get-state
"$APP/gradlew" -p "$APP" testReleaseUnitTest lintRelease assembleRelease --console=plain
APK="$APP/build/outputs/apk/release/ask-tv-release.apk"
shasum -a 256 "$APK"
INSTALL=$(adb -s "$TV_SERIAL" install -r "$APK")
printf '%s\n' "$INSTALL"
case "$INSTALL" in *Success*) ;; *) echo 'AI Chat installation failed.' >&2; exit 1 ;; esac
START=$(adb -s "$TV_SERIAL" shell am start -W -n com.amine.asktv/.MainActivity 2>&1)
printf '%s\n' "$START"
if ! printf '%s\n' "$START" | grep -q '^Status: ok'; then
  echo 'AI Chat did not open.' >&2
  exit 1
fi
adb -s "$TV_SERIAL" shell dumpsys package com.amine.asktv | sed -n '/versionCode=/p; /versionName=/p'
