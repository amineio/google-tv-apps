# Chess points and thinking time

Status: proposed design. A separate builder review found no unresolved implementation questions. User approval is pending.

## Intent

Show the cost of losing chess pieces. Give the player time to study the computer's move before the next move.
Keep the game calm, offline, and easy to control with a TV remote.

## Starting state

Worktree: `/Users/amine/Developer/worktrees/chess-points-thinking/google-tv-apps`.
Branch: `amine/chess-points-thinking`.
Base: current `main` and `origin/main`, commit `240f7680a8c4f5a6dc833dac49e65f56901b56d1`.
The unchanged baseline passes 99 Chess unit tests.

## Constraints

Keep Chess in Java with its native Android controls and existing chess engine.
Preserve the centered board, its size, Honey & Ivory colors, sculpted pieces, and visible remote focus.
Keep Home, Clock, Family Home, device configuration, and unrelated work separate.
Use generic labels. Do not add accounts, network access, analytics, new audio, or bundled screen images.

## Approaches

Remaining piece value is the recommended approach. Each side starts with 39 points.
Losing a piece reduces that side's total. Promotion increases that side's total.
This gives each score a direct, visible relationship to the board.

Capture totals are an alternative. Each side starts at zero and gains the value of captured pieces.
This makes captures rewarding, but losing a piece does not reduce the player's displayed total.

A computer evaluation score is unsuitable for this change. Its meaning depends on position and search depth, which obscures the cost of losing pieces.
The user did not select an approach yet. The remaining sections describe the recommended approach.

## Points

A pawn is worth 1 point. A knight or bishop is worth 3 points.
A rook is worth 5 points. A queen is worth 9 points.
The king has no point value. Checkmate and draw results keep their existing meaning.
Points do not decide the winner or change legal moves.

Derive both totals from the current board. Do not save a second score ledger.
Include captures, en passant, and all promotions. Castling and ordinary moves do not change points.
Show both totals during play and on the final board.

## Visual presentation

Place the player's score to the left of the board and the computer's score to the right.
Use symmetrical, compact groups with ivory totals, smaller labels, and quiet sage secondary text.
Keep the groups outside the board. Keep the upper-right area clear for the separate Clock overlay.
Scores do not receive remote focus.

At move landing, show a short signed change beside each affected total.
A capture reduces the captured side's score. It does not add a gain beside the unchanged score of the capturing side.
A green positive value rises slightly. A coral negative value falls slightly.
Use a sign and an arrow so that color is not the only signal.
Respect the existing piece movement preference and Android animation controls.
With movement disabled, show a brief static signed change.
Pause, focus loss, screen changes, and recreation cancel feedback. Restore never replays it.
Use committed presentation identities to prevent duplicate feedback.

The [ImageGen concept](../../chess-math/concepts/points-thinking/concept.jpg) explores the score placement and countdown.
Place the final countdown beneath the player's score, instead of beneath the board, to preserve the board's size.
The concept is a visual reference. It does not establish a legal chess position or completed app behavior.

## Thinking time

Default to 20 seconds after each nonterminal computer move.
Start the countdown after that move finishes its visual presentation.
With piece movement disabled, start after the durable move appears.
Do not add a delay before the player's opening move or after a game result.

Show a clear countdown and a thin honey progress line beneath the player's score.
Show a short prompt to study the opponent's move. Replace it with the normal turn label when the delay ends.
Support Dutch and English. Keep text readable at larger font sizes.

During the countdown, directional input can move board focus.
OK cannot select a piece, start promotion, or commit a move.
Guard controller actions, including intended-move confirmation, rather than relying on disabled views.
Back can open Pause. Keep existing legal draw claims and parent controls available.
The countdown measures available foreground time. It does not prove attention or thought.

## Settings

Add Thinking time to the existing Play section, with a semantic timer icon.
Offer Off, 5, 10, 20, 30, and 60 seconds in a native choice dialog.
Keep the selected value visible in the row. Back closes the dialog and restores focus to that row.
Save the preference separately from game progress. It survives app restart and progress reset.
Handle preference write failure with the existing visible error pattern.

A duration change applies after the next computer move.
Selecting Off also clears an active delay through a durable controller action.
Increasing or decreasing a nonzero duration does not restart an active delay.

## State and timing

Add a saved remaining duration to Session and its copy and codec paths.
Keep old saves compatible by defaulting a missing duration to zero.
Initialize the duration in the same saved snapshot as each nonterminal Black move.
Use a controller-only monotonic anchor to subtract active time after presentation.
Never save an absolute wall-clock deadline.

Settle remaining time into candidate snapshots before save and pause.
Stop the anchor while paused, hidden, busy, in error, or in move presentation.
Restore paused with the saved duration. Background time and process absence do not reduce it.
Duplicate or stale presentation callbacks cannot restart the countdown.
An interrupted computer presentation preserves the remaining thinking time.
New games and completed games clear the pending duration.
A committed White move also clears it. A successful draw claim clears it without a move.
Parent phase skips preserve the pending duration when they preserve the board. Only active Chess time reduces it.
The codec accepts remaining durations from zero through 60,000 milliseconds.
A positive value requires an unfinished board after a Black move.
Selecting Off suppresses stale saved durations after reload, even if the earlier snapshot clear failed.
Snapshot write failure still pauses play and offers the existing retry action.
Sudden process death can restore the last saved duration, normally with up to five seconds of already spent time.
This follows the app's existing checkpoint limit. Storage delays can increase that interval.

## Validation and delivery

Test material values, captures, en passant, castling, and promotion deltas.
Test exact countdown expiry, input guards, presentation completion, pause, restore, slow saves, and failed saves.
Test old-save compatibility, preference defaults, persistence, Off, and next-reply behavior.
Use actual D-pad input for emulator tests of focus, dialogs, scrolling, countdown, and app return.
Capture generic emulator evidence for both languages, larger fonts, score feedback, and countdown.
Use one fresh independent reviewer after tests. Fix material findings before delivery.
Run Chess unit tests, lint, release build, and relevant instrumented tests locally.
Do not enable cloud builds, upload APKs, or create a public release.

Use an explicit ADB serial for device commands.
Keep physical-TV work separate. If the TV is in use, defer its input and report the missing evidence.
After successful app installation, commit the scoped change, push the branch, and merge into `origin/main` under AGENTS.md.
Report installation and merge separately.

## Coordinator decisions

The point values, preset durations, side placement, and next-reply preference policy are proposals from the coordinator.
The user requested points, signed motion feedback, a delay near 20 seconds, and an adjustable period.
The user did not approve the detailed design yet.
