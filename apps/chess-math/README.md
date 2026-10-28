# Schaak & Reken

Schaak & Reken is a free, offline chess and arithmetic app for Google TV. The Dutch and English interfaces work with a standard TV remote. Dutch is the default. Select the language in Settings.

Maths practice is optional. Turn it off in Settings to play chess without practice or breaks. The choice survives app restarts and progress resets.

The child plays White. The computer uses one strength at the strongest search limits in the app. The app needs Android 8 or later.

The app uses no permissions, network connection, account, advertisements, purchases, analytics, or background service. It keeps one game and its practice round in local app storage. It stores no child name or profile. Cloud backup and device transfer are disabled.

## Play

Open Schaak & Reken from the TV app grid. The title screen shows the game name, chess artwork, and a short menu with Settings. Select Start or Verder spelen. A saved session also offers Nieuw spel. This action asks for confirmation before it clears progress. With Maths practice on, it starts a fresh arithmetic cycle. With it off, it starts chess.

1. Use the arrows to choose a square. Press OK to select a White piece.
2. Use the arrows to choose a marked destination. Press OK to move.
3. Press Back to cancel a piece selection. Press Back again to open Pause.

Honey backgrounds mark the selected piece and legal captures. Sage or cream backgrounds mark legal empty destinations. A quiet sage background marks the last move. A brick background marks check. One square outline marks remote focus.

Both sides show the value of their remaining pieces beside the board. Each side starts with 39 points.
A pawn is worth 1, a knight or bishop 3, a rook 5, and a queen 9. The king has no point value.
A capture reduces the captured side's total. Promotion adds the replacement value after the pawn's value is removed.
Points do not decide the winner. Checkmate and draw rules stay the same.
At move landing, a green positive change rises slightly and a coral negative change falls slightly.
With piece movement off, the signed change appears briefly without motion.

After each computer move, a thinking period stops piece selection and moves for 20 seconds by default.
The arrows still move focus so that the player can study the board. The countdown starts after the computer's move lands.
Pause, menus, background time, and storage work stop the countdown. Saved games retain the remaining period.
The opening move and completed games have no thinking delay. The timer does not measure attention or prove thought.

Live moves slide once for 480–620 ms with smooth acceleration and deceleration. Captured pieces remain visible until landing.
Castling moves both pieces together. Promotion lands as the chosen piece.
A wooden tap confirms a move. Captures use two distinct wooden knocks, including captures that give check. Non-capturing checks use a warm notice. Correct
arithmetic answers use a cheerful four-note phrase. Wrong answers and navigation have
no custom sound. Quiet playback still depends on the TV volume.

Pause, focus loss, and recreation stop effects. Resume never replays a saved
move or sound. Sound loading or audio focus refusal keeps the action silent.
Motion can end early after two seconds if a completion callback is missing.

Promotion shows images and names for Dame, Toren, Loper, and Paard. Use Left and Right to choose the piece. Press OK to confirm. Press Back to cancel the choice before the move commits.

The computer evaluates legal moves without random choices or deliberate mistakes. Its search still needs family playtesting. The app makes no rating or age-suitability claim.

## Arithmetic rounds

With Maths practice on, a fresh session starts with five foreground minutes of arithmetic. Foreground time is time while the app is visible and active. The app pauses time while hidden, in standby, in parent controls, or on a pause screen. It never removes pieces after a wrong answer.

A math round also requires at least three correct answers. After five minutes, the child finishes the current exercise. Exercises continue throughout the math period, even after three correct answers. Foreground idle time counts as elapsed time, but it cannot complete the required exercises. The app does not measure thinking or prove attention.

Use the number pad to enter an answer. Wis deletes one digit. An exact answer shows Goed! and advances automatically after a short pause. There is no submit or Next action. Partial and wrong entries remain editable. Hint offers a useful step, then the worked answer. An answer completed with help counts toward the round. Paper and pencil are welcome. There is no question countdown or speed penalty.

After arithmetic, a one-minute break shows the remaining time. Only foreground time advances the countdown. Chess starts automatically when the break ends. The chess game continues until checkmate or a draw. It has no phase time limit. After checkmate or a draw, the final board and result remain visible with no time limit. With Maths practice on, select Start maths to begin arithmetic for the next cycle. With it off, select New game to start another chess match. Pause, resume, and app restart preserve the final board until this explicit action. The next chess phase starts a new game.

## Parent controls

Open Pause. Select Settings with one press. Home always leaves the app.

Settings use switches for Maths practice, sound, and piece movement. Press OK to change a switch.
Thinking time offers Off, 5, 10, 20, 30, and 60 seconds. A nonzero change applies after the next computer move.
Off also clears the current wait. The choice survives app restarts and progress resets.

Rekeninstellingen contains these controls:

- Enable addition, subtraction, and multiplication separately.
- Set the addition/subtraction number limit to 20, 50, or 100.
- Enable or disable crossing ten.
- Select multiplication tables 1 through 10.

Defaults are all three operations, limit 100, crossing ten off, and only tables 1, 2, 5, and 10 enabled. Addition operands and results remain within the limit. Subtraction stays nonnegative. With crossing ten off, 8 + 2 and 30 − 1 are excluded. Examples such as 20 + 10 and 23 − 3 remain available.

A selected table is the second operand. Table 5 means 1 × 5 through 10 × 5. An unchecked table 3 does not exclude 3 × 5. It excludes exercises with 3 as the second operand. The number limit does not shorten an enabled multiplication table.

Save applies new choices from the next exercise. Back or Annuleren discards unsaved choices. At least one valid operation must remain enabled. Multiplication requires at least one table.

Geluidseffecten and Stukbewegingen are separate parent choices. Both start On.
Sound controls appear only in Settings. These choices survive progress reset
and app restart. System animation Off disables slides. Android menu sounds
follow the TV system setting.

Parents can skip to chess or math, restart the chess game, reset progress, read help and licenses, or exit. A phase skip bypasses the normal cycle requirements. A new chess game starts directly. Chess continues until its result, without a phase time limit. Full progress reset requires confirmation.

## Chess rules and accepted limit

The app uses chesslib for generated legal moves. It supports castling, en passant, all four promotions, check, mate, and stalemate. It preserves full move history across restarts. Threefold repetition and fifty-move draws can be claimed from Pause or before a qualifying intended move. Fivefold repetition and the 75-move rule end the game automatically. Checkmate takes precedence.

The first version detects conservative material draws. It does not detect every rare locked position where no legal sequence can lead to mate. The parent accepted this first-version limit on 2026-10-26. Such a position can remain active. Use parent controls to restart or leave the game. Do not describe this version as a complete FIDE adjudicator.

Chesslib provides legal moves and board rules. It has no built-in search engine or difficulty levels. The app uses one local search at the former Hard limits. It examines up to five half-moves and 60,000 search nodes, with an 800 ms search budget. A half-move is one move by either side. A search node is a position or move that the search examines.

The search orders equal choices consistently. It uses no random move selection or shuffled move order. Root preparation and individual library calls can exceed the time budget. An unfinished search keeps its completed result or a legal fallback that considers immediate attacks. These are the strongest existing search limits in the app, with no chess-rating claim.

Saved games remain compatible. Earlier difficulty preferences no longer affect play.

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

The chess layout centers the board on the TV without a side text panel. Routine saves do not show a Saving message during chess or maths practice. Storage errors still appear. The equation and answer form one compact centered group.
