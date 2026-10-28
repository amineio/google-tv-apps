# Chess points and thinking time implementation plan

> For agentic workers: Use superpowers:subagent-driven-development to implement this plan task by task. Steps use checkbox syntax for tracking.

**Goal:** Show both sides' remaining piece values and enforce a configurable thinking delay after computer moves.

**Architecture:** Derive points from board contents. Save remaining thinking time with each computer move and track foreground time in the controller. Use native nonfocusable score groups in the board's side gutters.

**Tech Stack:** Java 17, native Android views, chesslib 1.3.7, existing Gradle build.

**Spec:** `docs/superpowers/specs/2026-10-28-chess-points-thinking-design.md`.

## Global constraints

Keep Chess in Java. Preserve the centered board, its size, and Honey & Ivory style.
Keep all app code within `apps/chess-math`. Keep other apps, builds, worktrees, and device backups unchanged.
Use generic Dutch and English labels. Add no audio, dependencies, permissions, or network access.
Use explicit ADB serials. Only the coordinator operates devices and starts builds.
Complete relevant local tests and independent review before merge. After installation, merge scoped changes into `origin/main`.

## Review focus

Presentation callbacks can complete synchronously or arrive late. They cannot start or restart the wrong delay.
Hidden time, save time, and errors cannot reduce the remaining delay.
Restored Off preferences must suppress a stale saved delay after a prior write failure.
Settings navigation and larger fonts must preserve focus and prevent score overlap with the board or Clock.
Restoration, pause, and screen changes must not replay score feedback.

## Task 1: Durable thinking time and preference

**Files:** Modify `Session.java`, `SessionCodec.java`, `SessionController.java`, and `PlayPreferences.java` under the app's main Java package. Modify their unit tests.

**Interfaces:** Produce `PlayPreferences.thinkingSeconds(): int`, `setThinkingSeconds(int): boolean`, `SessionController.setThinkingSeconds(int): void`, and `thinkingRemainingMs(): long`.
Expose `thinkingTotalMs(): long` for the progress line. Save the starting duration with remaining time so preference changes and restore cannot change its scale.
The default is 20 seconds. Valid presets are 0, 5, 10, 20, 30, and 60.

- [x] Write failing tests for 19,999/20,000 ms expiry and rejected selection before expiry.
- [x] Extend failures to presentation completion, duplicate callbacks, pause/restore, slow/failed saves, Off, next-reply preference changes, parent skips, reset, intended moves, draw claims, and old saves.
- [x] Run the tests before implementation and record the expected failure.
- [x] Implement remaining foreground time, guarded move input, compatible saved state, and durable preferences.
- [x] Run the full Chess unit suite and make sure that all tests pass.
- [x] Review the scoped diff before the coordinator commits it.

Command: `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ANDROID_HOME=/opt/homebrew/share/android-commandlinetools ./gradlew :chessMath:testReleaseUnitTest --console=plain`.
Expected: build success and zero test failures.

## Task 2: Points and native score display

**Files:** Create `MaterialPoints.java`, `ChessScoreView.java`, and tests in the app's existing packages. Modify `MainActivity.java`, localized strings, timer drawable, and focused instrumented tests. Modify `PlayModeRule.java` to default legacy fixtures to Off.

**Interfaces:** Consume Task 1 preference and controller methods. Produce `MaterialPoints.total(ChessGame, boolean): int`, `before(PresentationEvent, boolean): int`, and `after(PresentationEvent, boolean): int`.
`ChessScoreView` owns only score labels, signed feedback, and thinking status. `MainActivity` owns layout, presentation landing callbacks, lifecycle cancellation, and the Settings choice dialog.

- [x] Write failing material tests with literal expected totals for initial pieces, ordinary captures, en passant, castling, all promotions, and capture promotion.
- [x] Run the tests and make sure that they fail before adding the material implementation.
- [x] Implement board-derived totals with 1/3/3/5/9 piece values and zero for kings.
- [x] Add equal score gutters around the unchanged centered board. Keep the timer under the player's score.
- [x] Apply score changes at landing. Positive green values rise and negative coral values fall. Disabled motion shows static feedback.
- [x] Cancel feedback on pause, focus loss, and recreation. Keep countdown updates within existing views.
- [x] Add a Settings row with a timer icon and native Off/5/10/20/30/60 choice dialog.
- [x] Add actual D-pad tests for blocked OK, expiry, focus, settings, app return, languages, larger fonts, and no feedback replay.
- [x] Run local unit tests, lint, release/debug builds, and instrumented test build.

Command: `JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ANDROID_HOME=/opt/homebrew/share/android-commandlinetools ./gradlew :chessMath:testReleaseUnitTest :chessMath:lintRelease :chessMath:assembleRelease :chessMath:assembleDebug :chessMath:assembleDebugAndroidTest --console=plain`.
Expected: success, zero unit failures, and zero lint errors.

## Task 3: Device evidence, review, and delivery

**Files:** Update `apps/chess-math/README.md`, app version, and `docs/chess-math/points-thinking-verification-2026-10-28.md`. Store only generic app evidence under the related evidence folder.

**Interfaces:** Consume Tasks 1 and 2 as a complete app.

- [x] Run relevant tests on a Google TV emulator with one explicit serial.
- [x] Inspect app captures for score placement, thinking status, Settings, motion feedback, and larger fonts.
- [x] Request a fresh independent review of the complete branch. Fix material findings and rerun affected tests.
- [x] Install the verified release build on the emulator. Use the physical TV only when available and idle.
- [x] Record separate build, test, install, visual, and deferred physical evidence.
- [ ] Commit only scoped app and documentation changes. Push the feature branch and merge into `origin/main`.
- [ ] Make sure that the merged commit reaches `origin/main`. Preserve this worktree and unrelated local work.

Physical-TV testing is separate from emulator evidence. Do not claim playback, wake, Cast, or hardware acceptance from these tests.
