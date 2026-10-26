# Schaak & Reken

Schaak & Reken is a free, offline chess and arithmetic app for Google TV. The Dutch interface works with a standard TV remote. The child plays White against a deliberately easy computer. The app needs Android 8 or later.

The app uses no permissions, network connection, account, advertisements, purchases, analytics, or background service. It keeps one game and its practice round in local app storage. It stores no child name or profile. Cloud backup and device transfer are disabled.

## Play

Open Schaak & Reken from the TV app grid. Select Start or Verder spelen.

1. Use the arrows to choose a square. Press OK to select a White piece.
2. Use the arrows to choose a marked destination. Press OK to move.
3. Press Back to cancel a piece selection. Press Back again to open Pause.

An ivory outline marks focus. A sage inner border marks the selected piece. Dots mark empty legal destinations. Rings mark legal captures. A quiet sage overlay marks the last move. A checked king has a double border and an exclamation mark.

Promotion shows images and names for Dame, Toren, Loper, and Paard. Use Left and Right to choose the piece. Press OK to confirm. Press Back to cancel the choice before the move commits.

The computer makes frequent mistakes. Its strength needs family playtesting. The app makes no rating or age-suitability claim.

## Arithmetic rounds

The default rhythm is five foreground minutes of chess, then five foreground minutes of arithmetic. The app pauses time while hidden, in standby, in parent controls, or on a pause screen. It finishes a selected chess turn and the computer reply before the handoff. It never removes pieces after a wrong answer.

A math round also requires at least three completed exercises. After five minutes, the child finishes the current exercise. Exercises continue throughout the math period, even after three correct answers. Foreground idle time counts as elapsed time, but it cannot complete the required exercises. The app does not measure thinking or prove attention.

Use the number pad to enter an answer. Wis deletes one digit. An exact answer shows Goed! and advances automatically after a short pause. There is no submit or Next action. Partial and wrong entries remain editable. Hint offers a useful step, then the worked answer. An answer completed with help counts toward the round. Paper and pencil are welcome. There is no question countdown or speed penalty.

After a round, select Verder schaken to return to the saved chess position.

## Parent controls

Open Pause. Focus Voor ouders. Hold OK for three seconds. The visible bar shows the hold duration. This action separates the controls but does not authenticate an adult. Home always leaves the app.

Rekeninstellingen contains these controls:

- Enable addition, subtraction, and multiplication separately.
- Set the addition/subtraction number limit to 20, 50, or 100.
- Enable or disable crossing ten.
- Select multiplication tables 1 through 10.

Defaults are all three operations, limit 100, crossing ten off, and only tables 1, 2, 5, and 10 enabled. Addition operands and results remain within the limit. Subtraction stays nonnegative. With crossing ten off, 8 + 2 and 30 − 1 are excluded. Examples such as 20 + 10 and 23 − 3 remain available.

A selected table is the second operand. Table 5 means 1 × 5 through 10 × 5. An unchecked table 3 does not exclude 3 × 5. It excludes exercises with 3 as the second operand. The number limit does not shorten an enabled multiplication table.

Save applies new choices from the next exercise. Back or Annuleren discards unsaved choices. At least one valid operation must remain enabled. Multiplication requires at least one table.

Parents can skip to chess or math, restart the chess game, reset progress, read help and licenses, or exit. A phase skip bypasses that round's time and answer requirements. A new chess game preserves the current round time. Reset requires confirmation.

## Chess rules and accepted limit

The app uses chesslib for generated legal moves. It supports castling, en passant, all four promotions, check, mate, and stalemate. It preserves full move history across restarts. Threefold repetition and fifty-move draws can be claimed from Pause or before a qualifying intended move. Fivefold repetition and the 75-move rule end the game automatically. Checkmate takes precedence.

The first version detects conservative material draws. It does not detect every rare locked position where no legal sequence can lead to mate. The parent accepted this first-version limit on 2026-10-26. Such a position can remain active. Use parent controls to restart or leave the game. Do not describe this version as a complete FIDE adjudicator.

The computer uses a one-ply search, a search that considers one move ahead. Most choices are random legal moves. Its 100 ms budget is cooperative between library calls, with at most 256 evaluations. The worker keeps a legal fallback. This is not a strict real-time guarantee.

See [dependency records and notices](third_party/DEPENDENCIES.md). Gradle checks the exact SHA-256 hashes of both runtime libraries before each build. The APK also includes their licenses and notices.

## Saved progress

The app saves moves, draw history, selection, pending promotion, current exercise, hints, answer input, settings, elapsed time, and phase. It restarts paused. It never credits time while the process is absent.

An atomic snapshot preserves the previous saved revision if a write fails. A sudden process death can lose time since the last acknowledged checkpoint, normally about five seconds. Storage delays can increase that interval. Committed moves and answers remain coherent. A save failure pauses the app and offers a retry. An invalid save offers an explicit reset instead of silently deleting progress.

## Build and test

Install JDK 17, Android platform 34, and Android build tools 34. Set JAVA_HOME and ANDROID_HOME to those installations. From the repository root, use:

```sh
./gradlew :chessMath:testReleaseUnitTest :chessMath:lintRelease :chessMath:assembleRelease :chessMath:assembleDebugAndroidTest
```

The review APK is `apps/chess-math/build/outputs/apk/release/chessMath-release.apk`. It uses the repository's local debug signing key. Store publication and production signing are separate work.

Instrumentation tests need a connected TV and the debug app. They are not run by the APK build. The test package is `com.amine.chessmath.test`. The math, parent, and promotion tests use temporary memory fixtures. Other smoke tests can change the test app's current session. Preserve existing family progress before operating the test suite.

See the [verification record](../../docs/chess-math/verification.md) for actual build, test, screenshot, and device evidence. Design images are not app screenshots.

The v0.2 visual pass adds original ImageGen pieces and a forest backdrop. Text, controls, and board cells remain native. See the [visual evidence](../../docs/chess-math/visual-polish-verification-2026-10-26.md) and [design review](../../docs/chess-math/visual-review-2026-10-26.md).
