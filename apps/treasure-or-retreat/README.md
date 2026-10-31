# Treasure or Retreat

Treasure or Retreat is an offline Kotlin game for Android TV, Android 8 or later.
The package is `com.amine.treasureorretreat`.

Collect treasure during three expeditions. Bank it before a trap takes the current expedition treasure.
Each expedition starts with one guaranteed treasure card from a 20-card deck.
The deck contains eight gold cards worth 40, five rubies worth 100, three emeralds worth 150, and four traps.
The remaining counts and exact next-draw trap fraction appear on screen.
Banked treasure stays safe. The game saves the highest final score and the sound preference locally.

## Remote controls

Press OK to start. Press Left or Right to select Explore Again or Bank Treasure.
Press OK once to activate the selected action. Holding OK does not repeat an action.
Press Back to open the pause menu. Use Up and Down to select Resume, Restart, Sound, or Exit.
Press Back again to return to the game. Leaving the app pauses the expedition.
The game retains its session while its activity model remains alive. Process death starts a new session.

## Build

This app uses its own Gradle build. It does not change the root Java build.
Install JDK 17, SDK platform `android-37.0`, and build tools `36.0.0`.
Set `JAVA_HOME` and `ANDROID_HOME` to those installations.
From the repository root, build and test the app:

```sh
apps/treasure-or-retreat/gradlew -p apps/treasure-or-retreat testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease --max-workers=2 --console=plain
```

The debug APK is `apps/treasure-or-retreat/build/outputs/apk/debug/treasure-or-retreat-debug.apk`.
The release APK is `apps/treasure-or-retreat/build/outputs/apk/release/treasure-or-retreat-release.apk`.
The local release build uses the repository's debug signing pattern. It is not a store-signed release.

## Install and inspect

Select an emulator serial from `adb devices` before installation.
Replace `emulator-5554` below with that exact serial.

```sh
adb -s emulator-5554 install -r apps/treasure-or-retreat/build/outputs/apk/debug/treasure-or-retreat-debug.apk
adb -s emulator-5554 shell am start -n com.amine.treasureorretreat/.MainActivity
ANDROID_SERIAL=emulator-5554 apps/treasure-or-retreat/gradlew -p apps/treasure-or-retreat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.treasureorretreat.TreasureRemoteTest' --max-workers=2 --console=plain
ANDROID_SERIAL=emulator-5554 apps/treasure-or-retreat/gradlew -p apps/treasure-or-retreat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.treasureorretreat.LauncherArtworkTest' --max-workers=2 --console=plain
```

Instrumented tests operate on the emulator and can uninstall the tested app.
Keep each invocation's XML reports before another invocation replaces them.
Inspect the test counts and class names before reporting coverage.

## Artwork and sound

The app includes original generated temple art, layered chest sprites, treasure sprites, and four playful trap illustrations.
Native Compose code renders the text, panels, controls, focus border, particles, and animated counters.
The launcher icon, TV banner, and separate text-free hero remain inside this app.
The manifest registers the hero through `com.amine.familyhome.HERO_IMAGE`.

SoundPool plays nine short original effects. The app uses no music, network permissions, remote media, or external services.
`tools/synthesize_audio.py` reproduces the quiet mono PCM effects.
The pause menu provides the sound toggle. Visual cues explain every action without sound.
See `docs/treasure-or-retreat/asset-record.json` for prompts, sources, dimensions, hashes, and rights.

## Evidence limits

The coordinator records actual build, test, and capture results separately.
Compilation alone does not establish TV appearance, animation quality, or sound quality.
Final review compares fresh running-game captures with the supplied concept.
Emulator results do not establish physical-TV audio latency, audible balance, or hardware performance.
A real family playtest remains separate from automated tests.
