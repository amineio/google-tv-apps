# Treasure or Retreat validation record

Date: 2026-10-30.

## Source and environment

The coordinator tested the task changes on `amine/treasure-memory-games`.
The branch base was `14bfc957` after a fast-forward that included an unrelated Clock change.
The new game files remained uncommitted during these tests. The final commit is pending.

The build used Homebrew JDK 17 and the installed Homebrew Android SDK.
Instrumented tests used `emulator-5584`, the `Casual_Games_TV_API_34` Google TV emulator.
The emulator used API 34, a 1920×1080 display, and density 320.
No physical TV was tested.

From the repository root, the coordinator operated the app wrapper with this environment and command:

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export ANDROID_SERIAL=emulator-5584
apps/treasure-or-retreat/gradlew -p apps/treasure-or-retreat testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease connectedDebugAndroidTest --max-workers=2 -Dorg.gradle.jvmargs=-Xmx1g -Dfile.encoding=UTF-8 --console=plain
```

## Automated results

The final command passed after removal of a redundant Back key condition.
Compose `BackHandler` still owns Back navigation.
The coordinator inspected XML test counts and class names.

| Test group | Result |
| --- | --- |
| Debug rules tests | 10 passed, no failures |
| Release rules tests | 10 passed, no failures |
| `TreasureRemoteTest` | 6 passed, no failures |
| `LauncherArtworkTest` | 2 passed, no failures |
| Android lint for release | 0 errors, 6 warnings |
| Debug APK assembly | Passed |
| Release APK assembly | Passed |
| Repository Python suite | 29 tests, 28 passed, 1 skipped |

Rules tests cover the deck distribution, guaranteed opening, seeded shuffling, draw guards, banking, traps, progression, restart, and card values.
Remote tests cover directional selection, three expeditions, results, restart, held Select, pause, trap loss, recreation, records, and sound preference.
Artwork tests cover manifest references, resource dimensions, and rendering at launcher sizes.
These tests do not establish animation smoothness or audible quality.

## Packages

The debug APK path is `apps/treasure-or-retreat/build/outputs/apk/debug/treasure-or-retreat-debug.apk`.
The release APK path is `apps/treasure-or-retreat/build/outputs/apk/release/treasure-or-retreat-release.apk`.
The release APK is approximately 2.6 MiB.
The local release build uses the repository's debug signing pattern. It is not a store-signed package.
Package hashes and the final installed-build record are pending.

## Runtime visual review

Status: PENDING.

The coordinator will compare fresh screenshots of the running game with the supplied Treasure or Retreat concept.
The review must assess the complete 16:9 screen, including chest assembly, card artwork, text, focus, panels, safe margins, and visual hierarchy.
The first runtime review found a detached lid and neighboring fragments in atlas crops.
The implementation replaced the lid with an interior view and corrected the sprite crops.
A later capture still showed a visible hinge gap and an oversized ledger panel.
The presentation correction uses the measured bitmap alpha bounds and an explicitly bounded 205×230 dp ledger.
The results above describe the build before this correction. Repeated affected tests and fresh captures remain pending.
Independent visual acceptance remains pending.
No final visual acceptance is claimed by this record.

Capture paths, APK hashes, and the final commit will be added after review.
Logs, screenshots, and other local artifacts remain outside Git.

## Evidence limits

Physical-TV tests, audio listening, and a human family playtest remain unperformed.
Emulator tests do not establish physical remote behavior, speaker balance, audio latency, or Chromecast performance.
Successful decoding and SoundPool state do not establish audible quality.
The app preserves records and sound preference locally. Process death can end the current expedition session.

Git delivery and final screenshot comparison remain pending.
