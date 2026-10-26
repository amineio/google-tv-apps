# Chess and Math Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development. The parent approved the design and requested development on 2026-10-26. Continue through implementation, tests, and independent review.

**Goal:** Build a separate offline Google TV chess and arithmetic app with saved progress and remote controls.

**Architecture:** Native Java views use a serial session controller. Chesslib owns legal movement. Separate math classes own exercises and hints. An atomic local snapshot preserves board history and phase progress.

**Tech Stack:** Java 17, Android SDK 34, native Android views, chesslib 1.3.7, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-10-26-chess-math-design.md`, with the approved forest-green and ivory ImageGen direction in `docs/chess-math/concepts/imagegen/`.

## Global Constraints

- Work in the existing `amine/chess-math` checkout. Preserve the separate Cast worktree and existing Home and Clock behavior.
- Use package `com.amine.chessmath`, module `:chessMath`, minSdk 26, target/compile SDK 34. No network permission, service, account, ads, purchase, or physical TV deployment.
- Dutch text, White child versus deliberately easy computer, no lessons or chess clock. Every action works with arrows, OK, and Back.
- Default phases are 300000 foreground milliseconds. Math also needs three completed exercises. Finish the current turn/exercise. Pause hidden, unfocused, and standby time.
- Default operations are addition, subtraction, and multiplication. Limit is 100, crossing ten is off, and only tables 1, 2, 5, and 10 are enabled. Display multiplier × table.
- Include dependency licenses and notices. Report device evidence separately from builds and unit tests.

## Review Focus

- A timer fires during promotion or a computer reply: save the turn and hand off only at a stable boundary.
- A stale worker finishes after pause, reset, or phase skip: reject its result.
- A process dies after an answer or handoff: restore the committed phase without double credit.
- A parent disables all operations or all multiplication tables: reject invalid configuration without losing the current exercise.
- Focus reaches a board corner, keypad edge, or dialog: arrows remain deterministic and Back preserves work.

## Ownership and interfaces

All Java classes use `com.amine.chessmath`. Agent ownership is disjoint. Parallel implementation is explicitly authorized by the parent handoff. The coordinator owns shared Gradle files, controller, persistence, integration, documentation, and CI. Agents must not edit another owner's files or commit shared work.

### Task 1: Legal chess and easy opponent

Owner: chess agent. Files: `ChessGame.java`, `EasyOpponent.java`, their unit tests, and dependency notices under `apps/chess-math/third_party/` and `src/main/assets/licenses/`.

`ChessGame` provides `ChessGame()`, `static fromHistory(List<String>)`, `history()`, `fen()`, `pieceAt(int)`, `whiteToMove()`, `legalMoves()`, `play(String)`, `result()`, `claimableDraw()`, `claimableAfter(String)`, and `checkSquare()`. Squares use 0=a8 through 63=h1. Moves use UCI strings including promotion suffix. Pieces use FEN letters or `.`. Result is empty while active, otherwise a Dutch result description. `EasyOpponent.choose(ChessGame,long)` returns one legal UCI move or null at game end. No Android references.

- [x] Write and run failing legality, special-move, draw, history, and weak-opponent tests.
- [x] Implement the adapter and bounded opponent. Resolve the library acceptance questions with evidence, and disclose any residual rules limit.
- [x] Run focused tests and report commands, results, licenses, and integration concerns.

### Task 2: Arithmetic and answer entry

Owner: math agent. Files: `MathConfig.java`, `Exercise.java`, `MathGenerator.java`, `MathInput.java`, and their tests. No Android references.

`MathConfig` exposes booleans `addition`, `subtraction`, `multiplication`, `crossingTen`, integer `limit`, and `boolean[11] tables`; `copy()` and `validate()` (throws IllegalArgumentException). Defaults follow global constraints. `Exercise` exposes final `long id`, `int a,b`, `String operation` (ADD/SUBTRACT/MULTIPLY), plus `answer()`, `expression()`, `hint(int)` and constructor `(long,int,int,String)`. `MathGenerator(long)` provides `next(MathConfig)`, `snapshot()` (String), and `static restore(String)`. `MathInput.append(String,int)`, `delete(String)`, and `matches(String,Exercise)` implement bounded entry.

- [x] Write and run failing generator-bound, configuration, table-orientation, hint, recovery, and numeric-entry tests.
- [x] Implement deterministic generation with saved family/bag state and mathematically correct hints.
- [x] Run focused tests and report the reproducible results.

### Task 3: Session, persistence, and lifecycle

Owner: coordinator. Files: `Session.java`, `SessionController.java`, `SessionStore.java`, `SessionCodec.java`, and tests.

Session owns chess history, phase, elapsed time, pending handoff, selection, promotion/claim state, math state, generator snapshot, settings, and revision. Controller serializes actions, settles monotonic elapsed time before actions, and produces an acknowledged saved state before allowing the next committed action. Worker replies carry a revision/token. Storage uses AtomicFile and excludes cloud backup.

- [x] Add failing fake-clock boundary, duplicate-answer, stale-worker, background, and restore tests.
- [x] Implement controller transitions and strict snapshot validation. Restore paused after process death.
- [x] Test atomic writes, invalid saves, delayed completion, and settings changes during a current exercise.

### Task 4: TV interface and visual design

Owner: UI agent. Files: `MainActivity.java`, `ChessBoardView.java`, `PiecePainter.java`, `TvStyle.java`, app manifest/resources, and Android remote tests. Coordinator supplies the controller contract before integration. UI agent starts with board/pieces/style and reads ImageGen references.

- [x] Implement original vector-like chess silhouettes with contrasting outlines. Draw exactly 64 square cells with distinct focus, selection, legal destination, last move, and check markers.
- [x] Implement start, saved session, chess, promotion, math keypad, hints, handoffs, pause, parent controls, settings, reset confirmation, and local licenses.
- [x] Add remote-focus tests. Capture rendered app evidence on an emulator if available. Preserve the physical TV.

### Task 5: Integration and independent review

Owner: coordinator plus fresh reviewer. Shared files: `settings.gradle`, `.github/workflows/ci.yml`, root/app guides.

- [x] Build APK, run new and existing unit tests, lint, repository tests, and compile Android tests without weakening checks.
- [x] Inspect merged permissions and APK notices. Record dependency checksums and the APK path.
- [x] Obtain independent correctness, child-usability, visual, license, and scope review. Fix material findings and repeat affected checks.
- [x] Deliver the APK path, usage guide, actual evidence, test results, and explicit emulator/TV limits.

## Execution ledger

Ruling: reuse the current feature checkout because it contains this task's design and images, while the separate Cast worktree owns unrelated work. No additional worktree is needed.

Ruling: the latest request authorizes implementation of the reviewed design and routine implementation choices. Do not add another plan approval stop.

Ruling: the parent requested real Chromecast testing, superseding the initial restriction on device installation. No emulator was started.

Ruling: the parent explicitly accepted incomplete automatic detection of rare general dead positions for this first version. The app and guide disclose the limit.

Completion: 40 game release unit tests, 56 existing app unit tests, eight strict repository tests, all release builds/lint, and seven tests against the installed release on the Chromecast passed. See docs/chess-math/verification.md for findings, fixes, images, exact APK hash, and device limits.
