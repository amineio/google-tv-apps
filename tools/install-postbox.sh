#!/usr/bin/env bash
# Build and install Postbox on one explicitly selected emulator or authorized TV.
set -euo pipefail
: "${TV_SERIAL:?set TV_SERIAL to the connected emulator or explicitly authorized TV shown by adb devices}"

ROOT=$(cd "$(dirname "$0")/.." && pwd)
APP="$ROOT/apps/postbox"
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

DEVICE_STATE=$(adb -s "$TV_SERIAL" get-state)
if [ "$DEVICE_STATE" != device ]; then
  echo 'The selected device is not ready.' >&2
  exit 1
fi
"$APP/gradlew" -p "$APP" testReleaseUnitTest lintRelease assembleRelease --console=plain
APK="$APP/build/outputs/apk/release/postbox-release.apk"
shasum -a 256 "$APK"
if ! INSTALL_OUTPUT=$(adb -s "$TV_SERIAL" install -r "$APK" 2>&1); then
  printf '%s\n' "$INSTALL_OUTPUT" >&2
  echo 'Postbox installation failed.' >&2
  exit 1
fi
printf '%s\n' "$INSTALL_OUTPUT"
if ! printf '%s\n' "$INSTALL_OUTPUT" | grep -q '^Success$'; then
  echo 'Postbox installation did not report success.' >&2
  exit 1
fi
START_OUTPUT=$(adb -s "$TV_SERIAL" shell am start -W -n com.amine.postbox/.MainActivity 2>&1)
printf '%s\n' "$START_OUTPUT"
if ! printf '%s\n' "$START_OUTPUT" | grep -q '^Status: ok'; then
  echo 'Postbox did not open.' >&2
  exit 1
fi
adb -s "$TV_SERIAL" shell dumpsys package com.amine.postbox | sed -n '/versionCode=/p; /versionName=/p'
