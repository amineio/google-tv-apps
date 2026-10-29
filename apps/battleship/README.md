# Battleship

Battleship is an offline Google TV game for one player against a Normal computer opponent.
The app uses Kotlin and native Compose for TV controls. It requires Android 8 or later.
It uses no network permission, accounts, database, advertisements, or telemetry.

## Play

Choose Play Game. Each side receives five randomly placed ships on a 10 by 10 board.
Ships occupy 5, 4, 3, 3, and 2 cells. Ships can touch. The human starts.
Use the arrows to aim. Press OK to fire. Press Back to pause.
The cursor stops at each board edge. A hit does not grant another turn.
Repeated attacks preserve the turn. The first side to sink all five ships wins.

The left board shows enemy waters. Intact enemy ships remain hidden.
Red crosses mark hits. Pale rings mark misses. Sunk ships reveal their full shape.
The matching right board shows your ships and their damage.
The computer uses only previous shot results and information about sunk ships.

Pause provides Resume, Restart Game, Settings, and Return to Main Menu.
Restart and replacement require confirmation. Returning home retains the match for Continue Game.
The app saves completed moves in an app-private file. Reopening the app offers Continue Game for an unfinished match.
The app pauses delayed actions when it enters the background. Resume continues the saved turn.
Settings provides one Sound control for music and effects. All audio stays locally bundled and remains optional.
Version 1.1 adds layered impacts, a distinct friendly damage sound, a victory fanfare, and a descending defeat bugle.
The right panel shows the target coordinate and compact shot feedback. Named sunk-ship feedback remains visible during aiming.
Version 1.2 adds a naval command panel, a generated battleship illustration, and five distinct native ship designs.
Separate target and last-shot regions keep feedback clear during aiming. Damage slashes and larger labels improve visibility.
The [three-round design review](../../docs/battleship/gameplay-design/review.md) records the native captures and expert scores.
Version 1.3 adds a generated ship icon, a TV banner, and a text-free launcher background.
Battleship owns these resources. Family Home reads the optional background through application metadata.
Version 1.4 introduced moving water and compact impacts.
Version 1.5 replaces that water with a generated still ocean image and adds five generated overhead ship sprites.
Both grids now have equal size. A fixed status strip and fleet roster preserve board space during play.
Miss splashes last 1.1 seconds, hits last 1.5 seconds, and burning and sinking last 2.5 seconds.
The next attack and results wait for the complete effect. Pause and background activity discard active effects.
Continue shows saved markers without replaying old impacts. The game rules and hidden enemy ships remain unchanged.
Recorded CC0 sounds replace the battle cues. A mixed sinking cue follows the full visual sequence.
Version 1.6 adds an orchestral menu loop and a dramatic opening for new matches.
New victory and defeat themes follow the complete final sinking sequence.
Continue and Resume do not replay the opening. Music stops on pause, mute, and app background activity.
Ordinary gameplay stays quiet after the opening. The app streams music separately from short battle effects.

## Build

Install JDK 17, Android SDK platform `android-37.0`, and build tools `36.0.0`.
Use this app's independent Gradle wrapper. The root Java build does not include Battleship.
On the development Mac, set the installed paths from the repository root:

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
apps/battleship/gradlew -p apps/battleship testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease assembleDebugAndroidTest --console=plain
```

The debug APK is `apps/battleship/build/outputs/apk/debug/battleship-debug.apk`.
The release APK is `apps/battleship/build/outputs/apk/release/battleship-release.apk`.
Both local variants use Android debug signing. Store signing remains separate work.
Build and test locally. GitHub APK builds remain paused. Do not upload APKs or start cloud builds for routine development.

## Install and inspect

Connect and authorize the target through ADB. Read its serial with `adb devices`.
From the repository root, select that exact target:

```sh
TV_SERIAL='<connected TV serial>' tools/install-battleship.sh
```

The installer builds and tests only Battleship. It preserves app data during replacement.
It does not change Home routing, accessibility services, other apps, or Cast packages.
For the dedicated `Battleship_TV_API_34` Google TV emulator, use its explicit serial:

```sh
"$ANDROID_HOME/emulator/emulator" -avd Battleship_TV_API_34 -port 5580 -gpu auto -scale 0.7
```

Use a second terminal for app commands:

```sh
adb -s emulator-5580 install -r apps/battleship/build/outputs/apk/debug/battleship-debug.apk
adb -s emulator-5580 shell am start -W -n com.amine.battleship/.MainActivity
ANDROID_SERIAL=emulator-5580 apps/battleship/gradlew -p apps/battleship connectedDebugAndroidTest --console=plain
adb -s emulator-5580 exec-out screencap -p > /tmp/battleship-game.png
```

Read the [Kotlin TV guide](../../docs/kotlin-tv-development.md) for emulator startup and repository conventions.
Instrumented tests use directional events. The runner can remove its tested APK afterward.
Keep personal account screens and device identifiers out of Git.
Emulator results establish layout and remote behavior. Physical TV readability, audio volume, and remote hardware require separate observations.

## Structure and assets

The `game` directory contains pure models, rules, seeded placement, and the hunt-and-target opponent.
The `viewmodel` directory controls turns, pause, screen transitions, and cancellable delays.
The `persistence` directory writes complete snapshots through one writer and atomic file replacement.
The `ui` directory renders native boards, menus, effects, and results from game state.
Unit tests cover placement, rules, hidden information, persistence, interruption, and 1,000 seeded complete matches.

The [design record](../../docs/superpowers/specs/2026-10-28-battleship-design.md) defines the interface.
The [asset record](../../docs/battleship/concepts/assets.json) contains prompts, dimensions, synthesis details, and rights information.
The [score record](../../docs/battleship/audio-polish-assets.json) contains the historical version 1.1 synthesis recipes.
The [gameplay art record](../../docs/battleship/gameplay-design/assets.json) contains the ImageGen prompts, dimensions, compressed paths, and rights for version 1.2.
The [launcher art record](../../docs/battleship/launcher-art/assets.json) records the same information for version 1.3.
The [launcher validation record](../../docs/battleship/launcher-art/validation.md) records its tests, review, and installation results.
The [version 1.4 water design](../../docs/superpowers/specs/2026-10-29-battleship-water-design.md) records the earlier shader approach.
The [water concept record](../../docs/battleship/water-effects/assets.json) contains the ImageGen prompts, references, dimensions, and rights.
The [water validation record](../../docs/battleship/water-effects/validation.md) contains native captures, test results, installation evidence, and device limits.
The [sprite design](../../docs/superpowers/specs/2026-10-29-battleship-sprites-design.md) defines the current scene and timing.
The [sprite record](../../docs/battleship/sprite-polish/assets.json) records generated ship and water assets.
The [current audio record](../../docs/battleship/sprite-polish/audio-assets.json) records CC0 sources, edits, levels, and hashes.
The [version 1.5 validation record](../../docs/battleship/sprite-polish/validation.md) records two scored design rounds, tests, and final TV installation.
The [music record](../../docs/battleship/music-polish/assets.json) records current orchestral sources, edits, hashes, and measured levels.
The [music credits](MUSIC-CREDITS.md) retain composer names and CC0 source terms.
To compare the current bundled audio with its deterministic recipes, use Python 3 from the repository root:

```sh
python3 apps/battleship/tools/generate_audio.py --check
```

Remove `--check` to regenerate current battle cues and the historical synthesis record.
To inspect current music with FFmpeg installed, run this separate comparison:

```sh
python3 apps/battleship/tools/prepare_music.py --check
```

The music script also accepts `--source-dir` to reproduce bundled bytes from original masters without changing app assets.
Outfit retains its [font license](FONT-LICENSE-OFL.txt).
Manual placement, other difficulties, networking, tablet layouts, and multiplayer remain outside this release.

Read the [validation record](../../docs/battleship/validation.md) for exact test results and remaining device evidence.
