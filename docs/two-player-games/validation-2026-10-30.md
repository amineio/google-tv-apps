# Two-player games validation

This record covers local two-player modes in Schaak & Reken and Battleship.
The coordinator ran the builds, tests, installations, and device commands.
Implementation agents edited each app separately. Independent reviewers inspected each app and its corrections.

## Scope and revisions

Both games retain computer mode and one saved match. Local setup offers eight portraits per game and optional custom names.
Empty names use translated character names. Draft cancellation preserves the saved match.
The [approved design](../superpowers/specs/2026-10-30-two-player-games-design.md) defines the behavior.
The [portrait record](portraits/assets.json) records prompts, dimensions, source hashes, bundled hashes, and rights.

| App | Version | Code | Final app correction |
| --- | --- | --- | --- |
| Schaak & Reken | `0.13` | `13` | `e4d433f032d13a9996aad3da5efa92f0b5744f55` |
| Battleship | `1.9` | `10` | `9086b25778575832d525242c94fa1e31eba64bef` |

The task branch is `amine/two-player-games` in its existing isolated worktree.
Commit `038c45bd6642ead0bab390d9af37beef0116e035` merges fresh `origin/main` at `e5edd5875f2f008a684166f439029406107b0a66`.
That merge changes neither app's runtime source, tests, resources, nor build configuration.
It preserves concurrent Clock, Family Home, Treasure or Retreat, Memory Parade, and repository documentation changes.
Chess commit `e4d433f0` later shortens only the Dutch and English rematch labels.
The earlier functional correction is `4a4aa4c4`. Battleship remains unchanged after `9086b257`.

Chess keeps its forest backdrop, forest-green and ivory palette, honey focus, existing pieces, and centered board.
Both colors accept human moves. Local matches omit computer turns, arithmetic, and the thinking delay.
Battleship uses eight naval characters. Both captains press Ready before both fleets appear once for ten seconds.
Afterward, both fixed owner boards hide intact ships. Every accepted shot changes the active captain after its effect and acknowledged save finish.
Interruption consumes the preview. Continue and Resume keep both fleets hidden.

## Executed tests

The full integration XML reports contain no failed, errored, or skipped tests.
Counts below come from executed test cases and class names, rather than build success alone.
The coordinator retained the original reports under the ignored `.superpowers/sdd/` task directories.

| App and task | Tests | Classes | Result |
| --- | ---: | ---: | --- |
| Chess `testReleaseUnitTest` | 152 | 15 | Pass |
| Chess `connectedDebugAndroidTest` | 63 | 15 | Pass |
| Battleship `testDebugUnitTest` | 105 | 14 | Pass |
| Battleship `testReleaseUnitTest` | 105 | 14 | Pass |
| Battleship `connectedDebugAndroidTest` | 32 | 11 | Pass |

These totals represent 257 distinct unit tests and 95 emulator tests.
Battleship ran the same 105 unit tests in both build variants.
These full suites precede the final Chess rematch-label resource correction.
Chess used `Chess_TV_API_34` at `emulator-5582`. Battleship used `Battleship_TV_API_34` at `emulator-5580`.
Both emulators ran Android 14.

Chess local unit classes are `LocalChessTest` (12), `PlayerProfileTest` (5), and `SessionCodecTest` (13).
`LocalChessRemoteTest` ran six native tests, including Black promotions, held OK, cancellation, restoration, and rematch focus.
The complete suite also covers computer play, arithmetic, localization, board layout, motion, effects, preferences, and recreation.
The final rematch regression removes the old result footer, restores board focus, and plays e2e4 through directional input.
The recreation regression retains the existing paused state while a writer remains pending.

Battleship local unit classes are `LocalGameSessionTest` (17), `LocalProjectionTest` (3), `PlayerProfileTest` (4), and `LocalSaveCompatibilityTest` (5).
`LocalTwoPlayerRemoteTest` ran seven native tests with actual directional input.
These tests cover setup and editor cancellation, Ready release, reveal interruption, both target boards, failed-save Retry focus, and final-save recovery through Continue.
`NativeWaterPresentationTest` ran four tests. Its local case covers LEFT and RIGHT impacts for miss, hit, and sunk outcomes.
That case also examines public projections, opposite-board pixels, bounds, duration, fire, and completion count.
The complete suite retains computer, saved-session, help, locale, launcher, music-state, and targeting coverage.

The coordinator ran these commands from the repository root with the installed JDK and SDK paths:

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
./gradlew :chessMath:testReleaseUnitTest --console=plain
ANDROID_SERIAL=emulator-5582 ./gradlew :chessMath:connectedDebugAndroidTest :chessMath:lintRelease :chessMath:assembleRelease --console=plain
ANDROID_SERIAL=emulator-5580 apps/battleship/gradlew -p apps/battleship testDebugUnitTest testReleaseUnitTest connectedDebugAndroidTest lintRelease assembleRelease --console=plain
```

Both release packages and Android lint passed.
Chess lint reports zero errors and 22 warnings. Battleship lint reports zero errors and five warnings.
The final builds retain existing SDK metadata, deprecated API, and native-library stripping notices.
These notices do not establish physical audio or playback quality.

After integration at `038c45bd`, the coordinator repeated unit, lint, and release tasks for both apps.
Both commands passed in seven seconds. The Chess unit task executed again.
The Battleship release unit task and both release packages were up to date.
These repeated tasks do not add another native test run.

```sh
./gradlew :chessMath:testReleaseUnitTest :chessMath:lintRelease :chessMath:assembleRelease --console=plain
apps/battleship/gradlew -p apps/battleship testReleaseUnitTest lintRelease assembleRelease --console=plain
```

After the Chess resource correction at `e4d433f0`, all six `LocalChessRemoteTest` tests passed again with zero failures, errors, or skips.
The XML contains only that selected class. Lint and release packaging passed in the same 25-second command.
These six repeated tests supplement the full integration results above. They do not increase the distinct test count.

```sh
ANDROID_SERIAL=emulator-5582 ./gradlew :chessMath:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.chessmath.LocalChessRemoteTest' :chessMath:lintRelease :chessMath:assembleRelease --console=plain
```

## Review and native captures

Independent app reviews accepted the corrected source scopes.
The Chess review required a rematch layout repair. The final native regression reproduced the original failure and passed after the repair.
The Battleship review required reachable Retry focus and delayed results after a failed final save.
Unit and native regressions reproduced those failures before their repairs. Scoped reviews accepted both corrections.

The final independent code review accepted `e5edd587..038c45bd` with no material finding.
It covered both apps and their integration with existing computer modes.
Optional maintenance observations concern dense Java formatting and reflection in tests. They do not require a release change.
The scoped code re-review accepted the two resource changes in `038c45bd..e4d433f0` with no introduced material issue.
The independent native game UI review accepted both games after inspecting 23 native captures.
It found a clipped Dutch rematch action in Chess before the resource correction.
Commit `e4d433f0` changes the action to `Nog een partij` in Dutch and `Play again` in English.
The focused native tests and release packaging passed.
The scoped visual re-review accepted both corrected result actions. Both labels fit on one line inside the unchanged native button.
The Chess forest-green, ivory, and honey mood remains intact. Battleship retains its separate naval characters and fixed board ownership.
ImageGen concepts remain design references, separate from native captures and test results.

The coordinator selected the sixteen native captures below. They contain only character names and fictional custom names.
WebP encoding uses quality 88 and retains the original 1920 × 1080 dimensions.
The [native capture manifest](native/assets.json) records source PNG hashes, encoded hashes, byte counts, dimensions, languages, and source revisions.
The original PNGs remain in the ignored local evidence directories.

| App | Native screen | Capture |
| --- | --- | --- |
| Chess | Eight animal portraits | [Chooser](native/chess-chooser-nl.webp) |
| Chess | White turn with a custom name | [River](native/chess-white-turn-nl.webp) |
| Chess | Black turn after e2e4 | [Uil](native/chess-black-turn-nl.webp) |
| Chess | Name editor with the keyboard visible | [Native editor](native/chess-name-editor-native-nl.webp) |
| Chess | Name editor after the keyboard closes | [Editor controls](native/chess-name-editor-no-ime-nl.webp) |
| Chess | Dutch result with the corrected rematch action | [Uil wins](native/chess-result-nl.webp) |
| Chess | English result with the corrected rematch action | [Owl wins](native/chess-result-en.webp) |
| Battleship | Eight naval portraits | [Chooser](native/battleship-captain-chooser-en.webp) |
| Battleship | Chosen captains | [Review](native/battleship-review-captains-en.webp) |
| Battleship | Both fleets and the initial countdown | [Reveal](native/battleship-reveal-countdown-en.webp) |
| Battleship | Hidden fleets and right-board targeting | [Captain Storm](native/battleship-storm-turn-en.webp) |
| Battleship | Named handoff after the shot | [Owl handoff](native/battleship-owl-handoff-en.webp) |
| Battleship | Hidden fleets and left-board targeting | [Owl](native/battleship-owl-turn-en.webp) |
| Battleship | Named result from an actual completed match | [Captain Storm wins](native/battleship-result-storm-en.webp) |
| Battleship | Name editor with the keyboard visible | [Native editor](native/battleship-name-editor-native-en.webp) |
| Battleship | Name editor after the keyboard closes | [Editor controls](native/battleship-name-editor-no-ime-en.webp) |

The independent game UI reviewer accepted the five result and editor captures added after the initial selection.
The Battleship result comes from an actual played match. Both editors appear with their native keyboards visible and hidden.
Both corrected Chess result captures follow an actual match of four individual moves. Owl wins on the final board.
The reviewer reports no remaining material visual finding.
Optional polish concerns the quiet Ready outline and inactive Chess turn prompts beside the final result. Neither requires a release change.

## Installation and delivery

The tested release artifacts are `apps/chess-math/build/outputs/apk/release/chessMath-release.apk` and `apps/battleship/build/outputs/apk/release/battleship-release.apk`.
The current standing instruction requires physical release installation before delivery to `origin/main`.
The coordinator installed both initial release APKs on the physical Chromecast with `adb install -r` at 22:14 UTC on 2026-10-30.
Both emulator release installations occurred earlier during capture setup. Each installation preserved app data.
At 22:14 UTC, package and hash readback and launch succeeded for both apps on both target types.
The Chess resource correction later superseded its initial APK.
The corrected Chess release installed on its emulator before the final result captures and on the physical Chromecast at 23:16 UTC.
Readback at 23:16 UTC matched the corrected release version, code, and SHA-256 on both targets. App data remained intact.
Battleship retains its tested and installed artifact from the earlier installation.

| App | Installed version / code | Release and installed APK SHA-256 on both targets | Status |
| --- | --- | --- | --- |
| Chess | `0.13` / `13` | `41c4d0e2d46aed98ca691dab2745fcd8ea52530f9b4c5fa7c8c058df5885878e` | Current corrected and installed artifact |
| Battleship | `1.9` / `10` | `e2c9475a37ebf20c807375b03833be2f3ced929a1df16381fc421d1291e52287` | Current tested and installed artifact |

The coordinator read package versions with `dumpsys package` and installed APK locations with `pm path`.
Device-side `sha256sum` matched each local artifact hash above.
`am start -W` returned `Status: ok` for both apps on both target types.
The corrected Chess physical launch also returned `Status: ok`. Its emulator launch succeeded before the final captures.
The coordinator left Chess in the physical TV foreground. Git delivery remains pending.

The coordinator used this command pattern for each app and explicit target. Private target identifiers remain outside Git.

```sh
adb -s '<explicit target>' install -r '<tested release APK>'
adb -s '<explicit target>' shell dumpsys package '<package>'
adb -s '<explicit target>' shell pm path '<package>'
adb -s '<explicit target>' shell sha256sum '<installed APK path from pm path>'
adb -s '<explicit target>' shell am start -W -n '<package>/<activity>'
```

Emulator results establish the observed native layout and injected directional behavior.
They do not establish physical remote behavior, audio focus, audible quality, playback, Cast, wake, or accessibility service behavior on the TV.
Installation evidence remains separate from those untested hardware behaviors.
The coordinator sent no physical gameplay input and captured no physical screenshots. All instrumented tests ran on emulators.
