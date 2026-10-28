# Battleship validation

The local run on 2026-10-28 built the complete offline Kotlin TV game.
The work uses `apps/battleship` and package `com.amine.battleship`.
The root Java builds and cloud workflows remain unchanged.
PR [#6](https://github.com/amineio/google-tv-apps/pull/6) merged into `origin/main` at `16dc3d1a9d886dce066db8de9ebc07498ad56837`.
The original checkout follows that merge. The feature worktree retains the local APKs and build reports.

## Automated results

The final build used JDK 17, SDK platform 37, and this app's independent wrapper:

```sh
apps/battleship/gradlew -p apps/battleship testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease --no-daemon --max-workers=1 --console=plain
```

The command completed successfully in 54 seconds.
Debug unit tests: 25 passed, zero failures or errors.
Release unit tests: 25 passed, zero failures or errors.
Each variant includes 1,000 seeded complete matches, 512 placement seeds, rules, visibility, persistence, and session interruption.
Lint: zero errors and three advisory warnings.
The warnings concern newer build tools and the fixed landscape declaration. Tool upgrades remain separate work.
An earlier lint process crashed internally. The fresh process completed successfully without disabling lint rules.

The dedicated `Battleship_TV_API_34` emulator operated the instrumented tests:

```sh
ANDROID_SERIAL=emulator-5580 apps/battleship/gradlew -p apps/battleship connectedDebugAndroidTest --console=plain
```

Seven tests passed with zero failures. The command completed in 55 seconds.
Tests cover all 100 cells through directional input, held OK, blocked firing during computer turns, menus, dialogs, and focus restoration.
The real-session test completes a match, starts a rematch, and preserves the match through Activity recreation.
A lifecycle regression test pauses a visible Activity before the computer delay ends.
It failed before the `onPause` repair and passed afterward.
An earlier shared-emulator run overlapped with another task and is excluded from acceptance evidence.
The dedicated emulator removed that overlap.

Repository tests: 14 passed with zero failures. Shell syntax and `git diff --check` also passed.
Installer tests cover target isolation and an Android launch error that returns a zero shell status.

## Installation and manual observations

The debug APK installed and opened through the TV launcher intent.
The release installer also built, tested, installed, and opened the release APK successfully.
The release uses local Android debug signing. Store signing remains separate work.
Packaged permissions contain no internet permission.

A paused computer turn survived a process restart with the same complete saved match and selected coordinate.
Continue restored that coordinate and applied exactly one pending computer shot.
The release build retained the saved match during replacement of the debug build.

Actual 1080p captures cover the main menu, Continue menu, native game boards, shot feedback, and paused release dialog.
Independent source and rendered reviews found no remaining material defect.
The captures show clear focus, safe margins, aligned labels, hidden intact enemy ships, and no observed overlap or clipping.
Review images remain local and were attached directly in chat. They are not published to an image host.

## Limits

The emulator reported a 3840 by 2160 physical display after a separate 4K startup.
Its Google TV platform capped the logical UI and captures at 1920 by 1080.
The game operated in that mode. This does not establish native 4K rendering.
Native 4K rendering, physical TV viewing-distance readability, remote hardware, and TV audio volume remain unverified.
Short local sound resources are bundled and optional. No physical sound-quality claim is made.
The hit and sunk effects share a brief impact animation. The results screen uses a static victory or defeat presentation.

## Build artifacts

The debug APK is `apps/battleship/build/outputs/apk/debug/battleship-debug.apk`.
Its SHA-256 is `09b53b95174f34ebf0d0cec57f19892062e6ae07c1f969ebca8d367a1846c53f`.
The release APK is `apps/battleship/build/outputs/apk/release/battleship-release.apk`.
Its SHA-256 is `4ff5eff588c5c75cee6362453a0d22e5dd7686fb426eedc9597b9b6622d95c4c`.
No APK was uploaded and no public release or cloud build was created.
