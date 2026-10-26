# Chess + Math for Google TV

Date: 2026-10-26. Status: approved for first-version development by the parent on 2026-10-26. The later ImageGen forest-green and ivory concepts replace the original navy visual direction. The parent also accepted incomplete detection of rare general dead positions for this version. See the implementation verification record for the remaining evidence and limits.

## Intent

A separate, free, offline TV app for a seven-year-old. The child plays normal chess as White against an easy computer. About five minutes of chess alternate with about five minutes of arithmetic. A remote is sufficient. Paper and pencil are welcome. The app supports a family routine; it does not lock the TV or establish learning outcomes.

Proposed name: **Schaak & Reken**. Proposed initial language: Dutch. All visible strings belong in Android resources. Dutch is a proposal, not a confirmed language choice. No accounts, advertisements, analytics, network access, lessons, chess clock, ratings, online play, opening books, or game downloads. Home always works normally.

## Repository fit and approaches

The repository uses native Android Java apps with separate packages and APKs. `CONTRIBUTING.md` asks for native controls, visible focus, and small dependencies. `:clock` uses Java 17, Android SDK 34, and native Activities. No root or app-specific AGENTS.md was found during this review.

| Approach | Benefits | Costs | Recommendation |
| --- | --- | --- | --- |
| Native Java UI + chesslib + a small local easy opponent | Matches current code; one language; legal chess without writing movement rules; small offline app | Must verify Android dependency compatibility; opponent needs child playtesting | Recommended proposal |
| Native Java UI + bundled Stockfish | Mature search with adjustable strength; good future range | Native binaries per ABI, engine process/protocol, GPL distribution obligations; low strength still needs calibration | Too much machinery for the first version |
| Bundled web chess UI in WebView | Many existing chessboard components | Extra rendering/runtime layer and focus bridge; weaker fit with current TV controls | Avoid for this app |

The user has not selected an implementation approach yet. These are reviewable alternatives, not an implementation instruction. Recommended future module: `apps/chess-math/`, Gradle `:chessMath`, package `com.amine.chessmath`. Use minSdk 26, compile/target SDK 34 initially to match this repository; confirm the actual TV and current distribution requirements before implementation. Keep all permissions and resources in this app. Do not couple it to TV Clock or TV Home. No service, overlay, exact alarm, boot receiver, or Internet permission is needed.

## Chess dependency and limits

Official sources inspected on 2026-10-26:

- [chesslib releases](https://github.com/bhlangonijr/chesslib/releases): latest published release shown was 1.3.7, dated 2026-06-30. This is evidence of recent maintenance, not a support guarantee.
- [1.3.7 POM](https://github.com/bhlangonijr/chesslib/blob/1.3.7/pom.xml): Java 11 bytecode and runtime dependency `org.apache.commons:commons-lang3:3.18.0`.
- [official installation instructions](https://github.com/bhlangonijr/chesslib#quick-start): `com.github.bhlangonijr:chesslib:1.3.7`, resolved through JitPack.
- [1.3.7 license](https://github.com/bhlangonijr/chesslib/blob/1.3.7/LICENSE): Apache 2.0.
- [1.3.7 Board source](https://github.com/bhlangonijr/chesslib/blob/1.3.7/src/main/java/com/github/bhlangonijr/chesslib/Board.java): exposes legal moves, history, mate, and draw helpers. `doMove` and `isMoveLegal` alone do not validate all movement geometry. `isDraw` automatically includes repetition and the fifty-move threshold.

Pin this release and dependency checksums. Restrict JitPack resolution to the chesslib group in root repository settings. Build-time downloads do not imply runtime Internet access. The local Android SDK was found at `/opt/homebrew/share/android-commandlinetools`; JDK 17 is available at `/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home`. Neither was used to build this proposal. Java bytecode compatibility suggests feasibility, but does not prove Android API compatibility. Before adopting the dependency, resolve and build it with Android desugaring as needed, run Android lint, and run legal-move/replay tests on the target TV. Do not silently replace a failed dependency with custom movement rules.

`ChessRules` wraps the library. Every human move, computer move, and restored move must exactly match an entry from the current `legalMoves()` list, including promotion. Only then apply it. Never trust a move string, cached destination, or engine output as authority. Cover standard chess: castling, en passant, promotion to queen/rook/bishop/knight, pins, check, checkmate, and stalemate. Never capture a king. Standard initial position only; no Chess960 or position editor.

The [FIDE Laws, Articles 5 and 9](https://handbook.fide.com/chapter/e012023) define draw conditions. Use standard claim rules rather than the library's combined `isDraw()` shortcut. Threefold repetition and fifty moves per side without a pawn move or capture are claimable. A White player can claim from the current position or select a legal intended move that would meet a claim condition. Show **Remise vragen** only when applicable; an intended-move claim confirms that move without first committing it. A failed/stale claim leaves the position unchanged. The computer claims when eligible, including before its intended move. Automatic draws occur at fivefold repetition or 75 moves per side without a pawn move or capture. Checkmate on the last move takes precedence over the 75-move condition. A successful claim ends the game without executing the intended move.

For a current-position claim, Pause exposes **Remise vragen** only when eligible. For an eligible intended destination, intercept before commit with a modal offering **Zet spelen** and **Remise vragen**; start focus on **Zet spelen**. This dialog appears only when a valid intended-move claim exists, never on ordinary moves. Arrows select, OK confirms, and Back returns to the original selected source/destination without committing. Persist the modal state and intended move with the board revision; recovery restores the decision. Revalidate the claim on confirmation. Both claim entry points pause active time while their dialog is open.

`ChessRules` needs explicit tests for these policies; do not call `isDraw()` as final authority. Detect stalemate and verified dead-material patterns, and retain the complete replay history. A general dead position means neither side can ever checkmate through any legal sequence; insufficient-material patterns do not detect every such position. General dead-position coverage is an unresolved library acceptance issue. Full tournament adjudication cannot be claimed until this is resolved. If the chosen library cannot meet the approved rules scope, return for a dependency/design decision rather than quietly weaken the game.

## Licenses and distribution

Keep repository contributions under MIT. Bundle the exact chesslib Apache 2.0 license, required copyright/attribution text, and any NOTICE file shipped by the pinned artifact. Commons Lang is also Apache 2.0; preserve its required NOTICE and license from the exact resolved artifact. Inspect and preserve notices for every transitive dependency. Add `ThirdPartyNotices` and the relevant license/NOTICE files to the APK, a local **Licenties** screen under parent help, plus a checked-in dependency/license record. Use original vector piece artwork under the repository license; do not copy unlicensed piece sets or assume platform glyphs are bundled assets. Existing Outfit assets keep their OFL notice if reused. No license inventory has been packaged or verified yet.

## Easy computer

chesslib supplies rules, not a playing opponent. Add `EasyOpponent` over its generated legal moves. Use one worker, a fresh board reconstructed from history, fixed one-ply evaluation, and no opening book, deep search, quiescence, tablebase, or remote model.

Proposed Easy mode: 70% uniform random legal move; 30% random choice among moves within one pawn of the best immediate material score. Prefer mate only within that scored branch. Use a persisted per-turn random seed so recovery does not change a planned response. This deliberately makes frequent mistakes, unlike a shallow search that still takes every hanging piece. It makes no Elo or age suitability claim. Parents get only “Makkelijk” in version one; tune these probabilities after observing real play with this child.

Bound work to 256 candidate evaluations, 100 ms computation, and one ply. Always keep a legal fallback chosen before evaluation. On budget exhaustion, use it. Stop/cancel work when paused. A result includes session ID, chess revision, side, and generation token. The controller drops stale results. Revalidate membership in current legal moves before commit. No arbitrary sleep is needed to imitate thinking; show “De computer is aan zet” while work runs. Recovery after a black-to-move snapshot schedules exactly one response.

## Child flow and active time

One saved game continues across rounds and app launches. Start: **Verder spelen** when a session exists; otherwise **Start**. The start screen explains: “Ongeveer 5 minuten schaken, dan rekenen. Reken 5 minuten en maak minstens 3 sommen goed.” No streaks, countdown pressure, punishment, or score comparisons.

Active time means elapsed monotonic time while the app is resumed, the screen is interactive, and the activity has focus. Menus, parent settings, pause panels, result panels, and the handoff screen do not count. Reading, thinking, and using paper count while the exercise is visible. Do not infer attention or thinking from remote presses. Foreground idle time alone cannot complete math because each round also needs three distinct correct exercises. The app cannot prove engagement; disclose this limit.

At 300 active chess seconds, latch `chessBoundaryPending`. If White is choosing or moving, allow that turn, including promotion, then one Black response. If Black is already thinking, allow its response. If the event arrives at a clean White-to-move boundary with no selection and no White input since that boundary, transition immediately. If White has selected a piece, finish that turn. If Back clears the selection while the boundary is pending, hand off immediately from the unchanged stable White-to-move board. A selected piece can delay the handoff; there is no forced move or chess clock. A parent can override. Serialize timer and input events so only one case applies. Show “Maak je zet af. Daarna gaan we rekenen.” Once a stable White-to-move state is committed, pause the board and show a handoff with **Rekenen**. Never transition mid-move or before an owed computer response.

An ended game is itself a stable boundary. If chess ends before five minutes, show a neutral result and **Nieuw spel** to use the remaining chess time. A new game resets board/history only, not round time. If the threshold is pending or reached, the result offers **Rekenen**. No computer move follows mate or draw. A child can stay on a result screen; its time is paused. At a later chess phase, start a new game if the previous one ended.

Math completes only when active math time is at least 300 seconds AND at least three distinct exercises were answered correctly in that round. At the time threshold, latch `mathBoundaryPending`; finish the current exercise. If three are not yet correct, show “Nog N sommen goed.” Keep providing exercises until the third correct answer. If the third answer is reached before five minutes, keep practicing until time expires and finish that current exercise. A correct exercise counts once, including after a hint or retry. If no input occurs, math does not finish. The completion screen offers **Verder schaken**. Starting the next chess phase resets both phase counters and preserves the chess position/history. There is no overall session limit.

## State and architecture

Use a small native Activity with screen views, native buttons, a custom board View, and plain Java domain objects. `SessionController` owns immutable `SessionState`, event ordering, timing, and persistence. `ChessRules`, `EasyOpponent`, `MathGenerator`, and `SessionStore` have no Activity references. No database or general workflow framework is needed.

| Stored phase | Meaning | Allowed next phase |
| --- | --- | --- |
| `CHESS` | White input, promotion choice, or owed Black reply | `TO_MATH` at stable boundary |
| `TO_MATH` | Saved board paused, handoff waiting for OK | `MATH` |
| `MATH` | Current exercise, answer, hint/retry, round count | `TO_CHESS` after both requirements and exercise completion |
| `TO_CHESS` | Math round complete, waiting for OK | `CHESS` |

Pause is an orthogonal state over any phase. Persist the phase before showing its destination screen. The same controller handles timer ticks, remote events, pause, resume, save results, and worker results through one serial queue. No view, worker, or periodic callback changes phase directly. Duplicate events use stable move/exercise IDs. Only an acknowledged save becomes the displayed committed state. Disable further submissions while a transaction saves.

Treat timer ticks as observations, not transitions. Before each action, settle active elapsed time from a monotonic anchor. Latch a boundary once; run the same pure reducer to decide if it can be crossed. A phase transition resets its anchor and applicable counters in the same snapshot. Pause increments the worker token, settles elapsed time, and cancels the worker. Resume reconstructs one anchor and reschedules owed work. Never compare a saved monotonic anchor across process death or reboot.

## Persistence and interruption

Keep one versioned JSON snapshot in app-private storage using Android `AtomicFile` (write, flush, sync, atomic finish). Store settings with the session snapshot so overrides and phase changes are coherent. Exclude this data from cloud backup. Small current-game storage does not require SQLite.

Persist: schema version, session ID and revision, phase, pending boundary flags, paused state, elapsed phase milliseconds, math correct count, complete chess move history from the standard initial position, game result, last move, selected source, last checkpointed focus, pending promotion destination, pending draw-claim modal/intended move/revision, current exercise ID/operands/operator/answer/hint state, submitted status, configuration, computer seed/token state, and math generator bag order, cursor, and random seed. The move history includes promotion identifiers; FEN is an optional checksum, never the only source. Replay restores repetition history, castling rights, en passant, and move counters.

Checkpoint on every accepted move, selection change/cancel, promotion open/cancel, draw-claim dialog open/cancel/confirm, answer edit, hint, phase transition, settings change, pause, and every five active seconds. Square focus is checkpointed with those events; a hard kill can restore the last saved square rather than the latest arrow press. A hard kill can lose elapsed time since the last acknowledged checkpoint, normally about five seconds. Write latency or a storage stall can increase that interval; there is no strict five-second durability guarantee. Committed answers and moves remain atomic. Pause timing and input when a save stalls; show a retry state rather than accumulating uncommitted progress. Do not grant elapsed time while the process is absent. Catch up to the current monotonic instant before a checkpoint. Saving should run off the UI thread, with serialized revision ordering and no next committed input until acknowledgement. On save failure, keep the last committed snapshot and show a paused retry screen; never advance without storage.

On restart, validate schema and bounds, replay every move through legal generation, verify phase consistency, and restore the current exercise without re-crediting its answer. A persisted Black turn resumes its reply; a promotion restores its choice panel. Always return paused with **Verdergaan**, so loading cannot consume time. Activity recreation also restores state without applying a move twice. A malformed snapshot is preserved for local recovery and offers an adult-confirmed reset; never silently erase the game. Unsupported future schema shows a clear reset choice. No upload or remote diagnostics.

Background, Home, standby, loss of focus, or Back pause the session and cancel pending computer work. Lifecycle callbacks settle time at the actual focus-loss/pause event, even if an earlier save is still in flight; acknowledgement time must never extend the active interval. Queue the settled pause snapshot after the earlier revision. Abrupt termination falls back to the last atomic checkpoint. Android lifecycle evidence does not guarantee flawless standby detection on every TV: validate on the actual device. No alarms run while the app is hidden.

## Chess remote and screen

White stays at the bottom. The board is the main focus area. Arrows move one square in the indicated direction, clamped at edges; there is no wrap or diagonal jump. Initial focus: e2; restore the last valid square thereafter. OK on a White piece selects it. OK on another White piece changes selection. OK on the selected square cancels it. OK on a legal destination commits that move. Other destinations keep selection and show “Deze zet kan niet.” No penalty or motion shake.

Back from promotion closes the chooser and restores the original board selection; no move is committed. Back with a selection clears it. Back with no selection opens Pause, focused on **Verdergaan**. Back from Pause returns to the board. Home uses Android behavior. Board arrows never accidentally enter the sidebar; the Pause panel holds parent access and exit actions. While Black moves, OK cannot select or submit; Back still pauses.

Focus: thick white outer ring and corner marker. Selected source: amber inner ring and a small “Gekozen” marker. Legal empty destination: centered turquoise dot. Legal capture: turquoise four-corner brackets. Last move: subtle hatch on source and destination. Checked king: red double border plus exclamation badge and “Schaak: bescherm je koning”. Layers can coexist; focus remains visible over every other state. Never use color alone. The right panel names the focused piece/square and explains the next action.

Promotion is a modal row of four large named piece buttons: Dame, Toren, Loper, Paard. Arrows move focus, OK chooses, Back cancels. Focus starts at Dame but no automatic promotion occurs. Computer promotion comes from its legal move choice. Expose square/piece names to accessibility services; use system-scaled text where practical. Do not rely on emoji chess glyphs for shipped pieces: use original filled vector silhouettes with a dark outline around White pieces and tested contrast on both square colors.

## Math content and input

Generate all exercises locally from a bounded candidate set, with a saved random seed and shuffled bags per enabled family. Avoid immediate repeats when at least two candidates exist. Families are addition, subtraction, and multiplication; choose enabled families in shuffled rotation rather than allowing the large addition pool to suppress tables. Require at least one family. Default: all three enabled, crossing ten disabled, tables 1/2/5/10 enabled. Do not assume the child has mastered crossing ten.

Parents choose a number limit N from 20, 50, or 100; proposed initial N is 100 to match the requested range. For addition, operands are integers in 0..N and `a+b <= N`. For subtraction, `0 <= b <= a <= N`. Zero and answers of 100 are allowed; negative answers and sums above 100 never occur. “Over het tiental” is one shared switch for addition/subtraction. When disabled, exclude addition with `(a % 10) + (b % 10) >= 10`, and subtraction with `(a % 10) < (b % 10)`. Thus 8+2 is excluded, 20+10 is allowed, 30-1 is excluded, and 23-3 is allowed. When enabled, allow both crossing and non-crossing exercises. Show these examples in parent help; the inclusive landing-on-ten interpretation is an explicit proposal.

For multiplication, choose a table `t` only from the checked set and a multiplier `m` in 1..10; display `m × t`. “Tafel van 5” therefore means `1×5` through `10×5`. The unchecked 3 table does not remove `3×5`; it removes exercises whose table operand is 3. Do not generate commuted forms independently. Results stay within 100 for tables 1..10. Settings expose tables 1..10; initially only 1, 2, 5, and 10 are checked. The number limit applies to addition/subtraction only; enabled tables retain every multiplier 1..10. Multiplication requires at least one selected table, otherwise disable that family or block Save with a clear message.

Use a fixed remote number pad: rows 1/2/3, 4/5/6, 7/8/9, Wis/0/OK. Arrow focus is explicit and clamped. Hint is above the pad; Up from its top row enters Hint, Down returns to the last pad cell. Accept up to three digits, values 0..100, and normalize leading zeros. Empty OK shows “Vul je antwoord in.” Wis removes one digit; no system keyboard is required. Back opens Pause and preserves input. Correct: “Goed gedaan” plus **Volgende**, initially focused, requiring OK before progression. Wrong: “Probeer nog eens”; keep the problem and answer editable. No time or attempt penalty.

Hint one provides a concrete step without revealing the answer: addition/subtraction split around the next/previous ten, or use units when crossing is off; multiplication shows a small array or repeated groups. Hint two can show the worked answer. Copying that answer still counts as completing this exercise; this is supported practice, not an assessment. Never pretend hint-assisted answers demonstrate independent mastery. “Je mag papier gebruiken” stays visible. No division, adaptive curriculum, progress profile, or performance report in version one.

## Parent controls

Pause includes **Voor ouders**. Hold OK on it for three seconds; display a clear hold-progress ring and release-to-cancel. This separates routine controls but is not secure parental authentication. No PIN is needed in the first proposal. The child can always leave with Home.

Parents can change enabled arithmetic families, the addition/subtraction number limit, crossing ten, and tables 1..10; skip to math or chess; restart the chess game; reset the session; or exit. Confirm destructive reset/restart. Phase skip explains it bypasses the current time/correct-answer requirement. Skip to math while Black is owed a response saves a deferred chess-resume marker; on returning to chess, complete that one reply before White input. Alternatively a parent may explicitly restart the game. Parent settings pause time. Apply content changes to the next exercise; persist the current exercise's original configuration until it is completed. Timer durations and three-correct minimum remain fixed initially, keeping the controls small.

Parent math settings use the fixed layout in concept 7. Rows are arithmetic families, number limit, crossing-ten toggle, tables 1–5, tables 6–10, then Save/Cancel. Left/Right move within each row and clamp at edges. Up/Down move to the nearest control center in the adjacent row; ties choose the left control. Each row remembers its last focused control when revisited. OK toggles a family/table, selects a number limit, toggles crossing ten, or activates Save/Cancel. Focus starts at Optellen. Back and Cancel discard edits and return to the parent menu; Save validates and commits all settings together. All controls fit the 1080p concept. At larger accessibility text sizes, the same row order scrolls vertically and always brings the focused row fully into view. Disabled multiplication does not hide the table controls; choices persist for later use. The parent menu itself is a vertical clamped focus list; Back returns to Pause.

## Static screen concepts

See [concepts index](../../chess-math/concepts/README.md). SVGs are review artifacts, not screenshots or implemented Android views. All concepts use 1920×1080, generous TV margins, a dark navy ground, large text, turquoise actions, and amber selections. Focus rings remain explicit. Final sizes require couch-distance testing. The concepts cover Start, Chess selection, Promotion, Math, Hint/retry, Pause/parent controls, and an explicit math-settings screen. Short handoff text follows the same large action layout: “Tijd om te rekenen” / “Verder schaken”.

## Verification and acceptance plan

No product tests apply to this design-only change. Implementation acceptance needs:

1. Rule adapter tests for generated-move membership, castling through check, en passant exposing check, pinned pieces, mate/stalemate, all promotions, repetition restored from complete history, claimable repetition/fifty-move cases including intended moves and current-position Pause entry, claim dialog Play/Claim/Back/recovery, automatic fivefold/75-move draws with mate precedence, and difficult dead-position/material endings. Start-position perft counts provide an additional library integration check, not proof of all rules.
2. Deterministic opponent tests for legal outputs, budget fallback, fixed seed, cancellation, stale result rejection, and no second Black move after duplicate callbacks. Observe real play before calling it suitable for this child.
3. Fake-monotonic-clock controller tests at 299999/300000 ms, each move/selection/promotion boundary, Black thinking, game end, pause/resume, time corrections, background, zero answers, retries, hints, third-correct/time ordering, duplicate submission, and overridden phases. Test all event orderings at a phase boundary.
4. Generator property tests across all configurations: range, nonnegative subtraction, crossing-ten examples, exact table semantics, disabled families, no empty bag, no immediate repeat where possible. Hints must match their stored exercise.
5. Atomic store tests for crashes before/after rename, stale saves, storage stalls, lifecycle pause during an in-flight save with elapsed time settled at focus loss, malformed snapshots, migration, Black-to-move recovery, pending promotion, pending phase transition, and answer credit exactly once. Resume after reboot never adds hidden elapsed time.
6. New-module unit tests, Android lint, APK build, and both existing releases if shared build files change. Preserve existing app behavior. Inspect the merged manifest to verify no network permission or extra services. Include third-party notices and offline operation with Wi-Fi disabled.
7. Actual remote-only TV checks: all four board corners, legal captures, cancellation, every promotion choice, every number pad key, hint/pause/parent focus, text at couch distance, safe margins, standby, Home, hard process death, relaunch, and long sessions. Capture real TV screenshots separately from these concepts.

## Approval decisions and remaining risks

Proposals to approve together: native Java + chesslib approach; Dutch; white-side child; easy randomized opponent; approximately five foreground minutes per phase; three correct exercises minimum; standard claim/automatic draw policies with the dead-position acceptance gate; crossing-ten disabled initially and its exact definition; number limit; multiplication display `m × t`; hold-to-enter parent controls; and the shown visual direction. None are recorded as prior user approvals.

Technical gates after approval: chesslib Android build/runtime validation, dependency resolution/checksums and notices, correct draw edge cases, remote focus/device lifecycle behavior, and easy-opponent playtesting. If a gate fails, bring the smallest changed design back for review. Do not scaffold a module, install dependencies, or write product code before the human approves this document. Next authorized step after approval is a concrete implementation plan.
