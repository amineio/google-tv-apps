# Chess points and thinking time

Version 0.10 adds remaining piece values and a configurable thinking period.
The approved design is [recorded here](../superpowers/specs/2026-10-28-chess-points-thinking-design.md).

## Behavior

Each side starts with 39 points. Pawns are worth 1, knights and bishops 3, rooks 5, and queens 9.
The king has no point value. Points do not decide the winner.
Captures reduce the captured side's total. Promotion increases the promoting side's total by the replacement value minus 1.
Both scores update when the move lands. Signed changes use green upward motion or coral downward motion.
Disabled motion shows static feedback. Pause, focus loss, and screen replacement stop feedback.

The default thinking period is 20 seconds after each nonterminal computer move.
The period starts after the move lands. Directional input remains available, but selection and move confirmation remain blocked.
Settings offer Off, 5, 10, 20, 30, and 60 seconds.
Nonzero changes apply to the next computer reply. Off also clears the current period.
Pause, background time, save work, and errors do not consume the remaining period.
The saved state retains the remaining period and its original duration for a stable progress line.
Old saves load with no pending period. The next computer reply uses the selected preference.

## Local build

The final local build passes 129 unit tests with zero failures.
Android lint reports zero errors and 23 warnings in existing app paths.
Release, debug, and instrumented test APK builds pass.
The existing compiler notes concern deprecated Android APIs.

Command, from the feature worktree root:

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
ANDROID_HOME=/opt/homebrew/share/android-commandlinetools \
./gradlew :chessMath:testReleaseUnitTest :chessMath:lintRelease \
  :chessMath:assembleRelease :chessMath:assembleDebug :chessMath:assembleDebugAndroidTest --console=plain
```

New timing tests first failed without the feature. The final timing suite passed 123 tests before the score work.
Six material tests then failed against empty implementations. All six pass with board-derived values.
The display test first failed because the score views were absent.

## Emulator tests

The dedicated Google TV emulator uses Android 14, API 34, at 1920 × 1080 pixels.
The ADB serial is `emulator-5672`. Its AVD name is `Chess_TV_API_34`.
An initial port collision invalidated earlier device runs. Only the dedicated emulator results count as delivery evidence.

The six new feature tests pass with actual directional input.
They cover exact expiry, blocked OK input, stable focus, Settings choices, Off, scrolling, background return, both languages, and score feedback.
The larger-text test sets the real system font scale to 2.0 and restores its previous value.
It uses Android's [nonlinear font scaling](https://developer.android.com/about/versions/14/features#non-linear-font-scaling) for the expected countdown size.
The capture shows enlarged labels and countdown text without board overlap.
The promotion test observes upward and downward translation after the move lands.

The first broad run completed 56 tests with one failure in `StartScreenTest.savedGameCanStartNewOnlyAfterConfirmation`.
The test sent keys before native dialog focus transitions completed.
Explicit waits for actual window focus fixed the test. The focused regression passes.
The final broad run passes all 56 tests in 106.531 seconds.

Command:

```sh
adb -s emulator-5672 shell am instrument -w \
  com.amine.chessmath.test/androidx.test.runner.AndroidJUnitRunner
```

## Visual evidence

These captures contain only generic app data. JPEG quality 85 preserves the 1920 × 1080 dimensions.
The captures are app evidence, not generated designs.

- [English countdown](evidence/points-thinking/thinking-english.jpg)
- [Dutch scores](evidence/points-thinking/thinking-dutch.jpg)
- [Larger text](evidence/points-thinking/thinking-large-text.jpg)
- [Thinking time choices](evidence/points-thinking/thinking-choices.jpg)
- [Capture feedback](evidence/points-thinking/capture-points.jpg)
- [Promotion feedback](evidence/points-thinking/promotion-points-motion.jpg)

## Review and delivery

The timing implementation passed a separate independent review with no material findings.
The [whole-branch independent review](points-thinking-review-2026-10-28.md) passed with no material findings or required changes.
The reviewed implementation commit is `32ce3304d9c06a5275bcc69ab7363489aad672aa`.
The release APK installed successfully on `emulator-5672` and launched with status `ok`.
The installed package reports version code 10 and version name 0.10.
APK SHA-256: `e59b7c2f57b9040eae944eaad8f69acd38a0b056a6bbb91f73a797f40c55bd8f`.
Merge commit `e159ead91fb7301d88ae28c708933e41d5feb279` reached `origin/main` and the feature branch.
It preserves the Family Home and Battleship changes that reached remote `main` during this task.
Post-merge Chess unit tests, lint, and release build pass. The release APK hash remains unchanged.
The isolated worktree and feature branch remain available for further Chess work.

Physical-TV installation, hardware behavior, and final couch-distance review remain deferred.
Read-only TV diagnostics did not establish a ready, idle UI. No physical-TV input or installation formed part of this change.
Emulator evidence does not establish physical playback, wake, Cast, or hardware behavior.
The app still has its documented conservative dead-position limit.
