# Battleship validation

## Version 1.1 audio and feedback

The 2026-10-28 update adds distinct attack and friendly damage sounds, a victory fanfare, and a descending defeat bugle.
Five original PCM effects reproduce exactly with `python3 apps/battleship/tools/generate_audio.py --check`.
The files use mono 16-bit PCM at 22,050 Hz. Their measured peak is below full scale, with zero endpoints.
The victory cue lasts 2.30 seconds. The defeat cue lasts 2.05 seconds.
The [audio record](audio-polish-assets.json) contains scores, synthesis details, levels, rights, and hashes.

The right panel retains the target coordinate, friendly board, fleet status, and compact shot feedback.
It removes repeated shot coordinates, narration, and repeated turn instructions.
Arrow input preserves named sunk-ship feedback. The repeat-shot warning clears when the player selects a different coordinate.

The final local command built debug, release, and instrumented APKs, with both unit-test variants and release lint.
It completed successfully in 46 seconds.
Debug unit tests: 32 passed, zero failures or errors.
Release unit tests: 32 passed, zero failures or errors.
The suites retain the 1,000 seeded match simulations in each variant.
New coverage includes compact feedback, damage audio, exactly one completion cue, mute, and feedback during aiming.
The two navigation regressions failed before the repair and passed afterward.
Lint reports zero errors and three existing advisory warnings.
Repository tests: 14 passed, zero failures.
Independent source review found no remaining material issue after the feedback repair.

Eight instrumented tests passed on the dedicated Google TV emulator, with zero failures or errors.
The command completed in 1 minute, 7 seconds.
The release APK installed and opened on the emulator. A native 1080p capture shows the compact panel after an actual shot.
Independent capture review found clear focus, readable labels, retained fleet status, and no visible clipping.

The physical Chromecast update also completed successfully. Android reports version 1.1, code 2.
The installer repeated the release unit tests, lint, and build before installation.
Android returned `Success` for replacement and `Status: ok` for launch.
The resumed Activity belongs to Battleship. Installation uses `-r` and does not clear app data.
The Home role, enabled helper list, Google TV Home, and setupwraith match the captured states before installation.
The physical capture remains black, so this update does not add physical visual or listening acceptance.
Device records remain in an ignored local backup.

Version 1.1 debug APK SHA-256: `9c9254db1a8bca00466be4e50fbd86cf2cb4b42e5751c137179d939391b10ec8`.
Version 1.1 release APK SHA-256: `caaf8b74d42b7c0b96f3c418560a6ccdfe1a45861e4a10e7e1c125fcd28dd952`.
Both artifacts remain at the paths below. No APK upload or cloud build occurred.

Subjective sound character and TV volume require listening through the target speakers.
File analysis and automated routing tests do not establish that acceptance.

## Version 1.0 delivery

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

The physical Chromecast installation on 2026-10-28 also completed successfully.
The device operates Android 14. The installed game reports version 1.0, code 1.
The release installer passed its unit tests, lint, and APK build before installation.
Android reported `Success` for installation and `Status: ok` for launch.
The family launcher required profile selection before it allowed the game to remain open.
The parent profile opened the game without a PIN prompt. Child app permissions remain unchanged.
A device capture shows the Battleship main menu with clear controls and the separate Clock overlay.
The Home role, enabled helper list, Google TV Home, and setupwraith match their captured states.
The capture and device records remain local. No device identifier or family screen is committed.

Actual 1080p captures cover the main menu, Continue menu, native game boards, shot feedback, and paused release dialog.
Independent source and rendered reviews found no remaining material defect.
The captures show clear focus, safe margins, aligned labels, hidden intact enemy ships, and no observed overlap or clipping.
Review images remain local and were attached directly in chat. They are not published to an image host.

## Limits

The emulator reported a 3840 by 2160 physical display after a separate 4K startup.
Its Google TV platform capped the logical UI and captures at 1920 by 1080.
The game operated in that mode. This does not establish native 4K rendering.
Native 4K rendering, a complete match with the physical remote, viewing-distance readability, and TV audio volume remain unverified.
Short local sound resources are bundled and optional. No physical sound-quality claim is made.
The hit and sunk effects share a brief impact animation. The results screen uses a static victory or defeat presentation.

## Build artifacts

The debug APK is `apps/battleship/build/outputs/apk/debug/battleship-debug.apk`.
Its SHA-256 is `09b53b95174f34ebf0d0cec57f19892062e6ae07c1f969ebca8d367a1846c53f`.
The release APK is `apps/battleship/build/outputs/apk/release/battleship-release.apk`.
Its SHA-256 is `4ff5eff588c5c75cee6362453a0d22e5dd7686fb426eedc9597b9b6622d95c4c`.
No APK was uploaded and no public release or cloud build was created.
