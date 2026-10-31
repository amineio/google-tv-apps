# Memory Parade

Memory Parade is an offline animal matching game for Google TV. Choose 6, 12, or 16 cards to find 3, 6, or 8 pairs. Eight animals include squirrel and raccoon. Smaller boards draw a random animal subset. Each match adds one animal to the forest parade. Complete a round in as few moves as possible.

The game uses Kotlin, Compose for TV, local artwork, SoundPool effects, and SharedPreferences. It requires Android 8 or later. It needs no account, network connection, or permissions.

## Controls

Use the D-pad to select a card. Press OK to turn it. A move counts after two different cards turn. Matched cards stay visible. Different cards turn back after 900 milliseconds.

Press Back to pause. Select Resume, Restart, Sound On/Off, Choose board, or Exit. Holding OK does not repeat an action. The board selection remains in place after pause or a short app interruption. Each board size has its own lowest-move record. The original record migrates only to the 12-card board. The game remembers the last board choice. At results, Play Again keeps that board; Choose board returns to the three choices.

## Build

Use JDK 17 and the Android SDK from the repository's Kotlin development guide. Set these paths for the local Homebrew installation:

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
apps/memory-parade/gradlew -p apps/memory-parade testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease --max-workers=2 --console=plain
```

The independent project uses the same installed build versions as Battleship. It does not change the root Java build. The APK files appear at:

```text
apps/memory-parade/build/outputs/apk/debug/memory-parade-debug.apk
apps/memory-parade/build/outputs/apk/release/memory-parade-release.apk
```

Local release builds use the existing debug signing key. Store distribution requires separate signing.

## Install and test

Choose an emulator serial from `adb devices`. Replace `emulator-5584` below if the coordinator assigns another emulator.

```sh
adb -s emulator-5584 install -r apps/memory-parade/build/outputs/apk/debug/memory-parade-debug.apk
adb -s emulator-5584 shell am start -n com.amine.memoryparade/.MainActivity
ANDROID_SERIAL=emulator-5584 apps/memory-parade/gradlew -p apps/memory-parade connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.memoryparade.MemoryRemoteTest' --max-workers=2 --console=plain
ANDROID_SERIAL=emulator-5584 apps/memory-parade/gradlew -p apps/memory-parade connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.memoryparade.LauncherArtworkTest' --max-workers=2 --console=plain
```

The coordinator inspects each XML test report before reporting coverage. Instrumented tests can remove the tested app. Keep them on the emulator.

The full-game tests save timestamped screenshots after actual matches, at the full parade, and at results for every board size. It copies each image to `/sdcard/Download/` before cleanup. These copies survive test app removal. The coordinator pulls these files for visual review and keeps screenshots out of Git.

## Artwork and sound

Original generated forest artwork and eight transparent animal illustrations share one art direction. A separate embossed card skin and a connected eight-place mossy stage add material depth. Native code draws score panels, title text, focus outlines, and particles. Larger opaque counters replace small labels and repeated narration. Animal names remain in accessibility descriptions. The cyan selection outline differs from the gold match border. No complete concept image is a runtime background.

The app contains separate square icon, 16:9 title banner, and text-free launcher background resources. Application metadata exposes the background to Family Home.

The twelve short effects use original local synthesis at 44.1 kHz, with variations for navigation, card flips, and matches. Mute, pause, and app departure stop all voices. Consumed events do not replay after Activity recreation. Recreate them with `python3 apps/memory-parade/tools/synthesize_audio.py`. They use no third-party samples. See `docs/memory-parade/assets.json`, `docs/memory-parade/asset-prompts.md`, `docs/memory-parade/upgrade-art-2026-10-31.md`, and `docs/memory-parade/audio-upgrade-2026-10-31.json` for production records.

## Evidence limits

The implementation alone does not establish test success, physical TV audio quality, animation smoothness, or human enjoyment. The coordinator records test output and fresh runtime screenshots during final review. Emulator tests do not establish physical speaker balance. A family playtest remains separate evidence.

The current game survives Activity recreation through its ViewModel. Board choice, separate best records, and sound preference survive process restarts. The app does not restore a partially completed game after process death.
