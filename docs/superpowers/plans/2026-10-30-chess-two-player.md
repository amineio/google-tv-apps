# Chess Two-player Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add named local chess participants with animal portraits while preserving computer play and the existing chess design.

**Architecture:** Extend the existing session, codec, and controller with an explicit match mode and two participant records. Native Java Views render draft setup and participant identity; the existing chess rules and Canvas board remain authoritative. Keep setup outside the saved session until the final replacement action succeeds.

**Tech Stack:** Java 17, Android Views/Canvas, existing chesslib 1.3.7, JUnit 4, Android instrumentation; retain current dependencies and tooling.

**Spec:** [Approved two-player design](../specs/2026-10-30-two-player-games-design.md), including the confirmed chess mood constraint.

The full integration suites passed. Corrected release installation passed on the physical TV and emulator.
Six focused native tests, lint, and release packaging passed for that resource correction.
Final native review accepted the corrected result actions. App changes reached `origin/main` at `67bbb0d0321c872c256d2b4213cb86b9941ad7c8`.
The [validation record](../../two-player-games/validation-2026-10-30.md) records executed commands, test counts, revisions, and delivery evidence.
The latest standing instruction requires physical release installation before delivery to `origin/main`.

## Global Constraints

- Use the existing isolated `amine/two-player-games` worktree. The root coordinator owns Git, all builds/tests, installation, and device input.
- One app code implementer owns all app edits at a time. Independent portrait work and read-only review may overlap with disjoint ownership.
- Preserve forest-green, ivory, and honey colors, the calm mood, forest backdrop, piece artwork, and board-first layout. Portraits complement this design; no naval styling or broad restyle.
- Keep computer mode, solo arithmetic/thinking preferences, sound, motion, rules, and launcher artwork. Local mode starts in chess with zero forced thinking time and no arithmetic or computer work.
- Use one shared TV and remote; White stays at the bottom. First participant is White; second is Black.
- Names are custom or localized character defaults. Limit saved custom names to 24 Unicode code points, trim whitespace, reject controls, and use defaults for empty names. Language changes preserve custom names.
- Require distinct portraits. Stable IDs: `fox`, `owl`, `bear`, `cat`, `panda`, `rabbit`, `turtle`, `dragon`. Unknown IDs use deterministic distinct fallbacks.
- Never expose numbered players or internal computer/human seat names in local mode. Preserve full names in persistence and accessibility labels.
- Keep one saved session. Back/Cancel from draft setup preserves the old session. Require final replacement confirmation when a saved session exists.
- Write schema 3; accept schemas 1 and 2 as computer games, preserving their existing progress and migration behavior.
- Target Chess version `0.13`, code `13`. Keep app package, installer, permissions, and independent resources intact.
- Portrait owner supplies only `apps/chess-math/src/main/res/drawable-nodpi/profile_{fox,owl,bear,cat,panda,rabbit,turtle,dragon}.webp` at 512×512 and the portrait manifest under `docs/two-player-games/portraits/`. App implementer must not edit those files.
- No agent builds, installs, controls devices, or commits. The coordinator completes those actions. No additional design approval gate is required.

## Review Focus

- Supplementary Unicode characters, controls, empty names, duplicate custom names, and language changes must keep valid names and distinct identities.
- A canceled keyboard, portrait change, setup exit, or replacement dialog must preserve the previous match and restore directional focus.
- Black selection, every Black promotion, and intended draw claims must survive restoration with the correct active color.
- A late computer callback from a replaced solo match must never mutate local play or re-enable arithmetic/thinking delays.
- A held OK press across move presentation or a dialog must never become the next participant's move; portraits must preserve board space and chess styling.

---

### Task 1: Implement the complete local chess path

**Owner:** One chess implementation subagent. No edits outside this app except by explicit coordinator handoff.

**Files:**
- Modify: `apps/chess-math/src/main/java/com/amine/chessmath/Session.java`, `SessionCodec.java`, `SessionController.java` for mode, identities, schema migration, and human-turn gating.
- Modify: `apps/chess-math/src/main/java/com/amine/chessmath/MainActivity.java`, `ChessScoreView.java`, `ChessBoardView.java`, `LocalizedText.java` for setup, chosen-name presentation, accessibility, and input release handling.
- Create: `apps/chess-math/src/main/java/com/amine/chessmath/PlayerProfile.java` for stable portrait IDs, custom-name validation, and deterministic distinct fallback.
- Modify as needed: app-owned `src/main/res/values*/strings.xml`, `apps/chess-math/build.gradle`, and `apps/chess-math/README.md`. Preserve portrait-owner files and existing visual resources.
- Test: `apps/chess-math/src/test/java/com/amine/chessmath/LocalChessTest.java`, `PlayerProfileTest.java`, and existing `SessionCodecTest.java`, `SessionControllerTest.java`, `SessionControllerThinkingTest.java`.
- Test: `apps/chess-math/src/androidTest/java/com/amine/chessmath/LocalChessRemoteTest.java`; adjust existing UI assertions only where the new explicit setup changes their entry path.

**Interfaces:**
- Consume existing `SessionController.Persistence.load(): Session` / `save(Session): void`, `Session.copy(): Session`, `SessionCodec.encode(Session): String` / `decode(String): Session`, and controller clock/executor injection.
- Preserve `chooseSquare(int): void`, `promote(char): void`, `playIntendedMove(): void`, `claimDraw(): void`, `continueAfterGame(): void`, `restartGame(): void`, `resume(): void`, and `foreground(boolean): void` as mode-aware entry points.
- Produce `PlayerProfile(String portraitId, String customName)` with validated custom-name data; persist stable IDs rather than drawable integers. Resolve default labels in the active locale at rendering time.
- Produce `Session.Mode { COMPUTER, LOCAL }`, `Session.mode`, `Session.whiteProfile`, and `Session.blackProfile`; copy every new field. Add `SessionController.startLocalGame(PlayerProfile white, PlayerProfile black): void` as the single committed local-start action and `isHumanTurn(): boolean` as the shared rules-input predicate.
- Resource contract: `R.drawable.profile_fox`, `profile_owl`, `profile_bear`, `profile_cat`, `profile_panda`, `profile_rabbit`, `profile_turtle`, `profile_dragon`. Connect the independently supplied portraits without changing their files.

- [x] Write focused failing regression tests first. Cover local White/Black alternation; wrong-color rejection; all promotions for both colors; castling/en passant and draw-claim entry paths; no local arithmetic/delay/computer scheduling; stale solo callback rejection; mode-preserving rematch and final-board retention.
- [x] Add codec/name regressions: schema-3 round trip and copy; schema-1/2 compatibility; restored Black selection/promotion; invalid mode recovery; local chess-only/zero-thinking validation; distinct fallback; 24 supplementary code points accepted and 25 rejected; controls rejected; trim/empty/default behavior; custom names preserved after portrait/language changes.
- [x] Report the test files and exact focused selectors to the coordinator. Wait for the coordinator's red-test result and implementation green-light; do not run Gradle.
- [x] Implement the model/controller/codec contract, then native draft setup, two-by-four chooser, optional name editor, final replacement confirmation, and mode-aware rematch. Use chosen portraits/names and White/Black throughout setup, score, pause, result, and accessibility text. Keep focus and selected-portrait states distinct.
- [x] Add remote regressions for the five Review Focus conditions: real D-pad navigation; canceled editor/setup/replacement; focus after app return; held OK across move completion; local Black promotion; and full chosen-name accessibility labels. Preserve native board focus, legal moves, check, selection, and last-move distinction.
- [x] Update app guide and version. Report changed files, focused test selectors, expected test counts, and any limitations to the coordinator for green testing. Do not commit.

### Task 2: Integrate, review, and deliver chess

**Owner:** Root coordinator, with a fresh independent code/game UI reviewer and the same implementation owner for corrections.

**Files:** Read Task 1 outputs and final portrait manifest. The coordinator records commands, revision, and fresh captures; any documentation edits remain delegated.

**Interfaces:** Consume the Task 1 controller/resource contract and regression selectors. Produce passing app checks, accepted native captures, version/hash evidence, and delivery to `origin/main`.

- [x] Run focused red tests before Task 1 implementation. Example selector: `./gradlew :chessMath:testReleaseUnitTest --tests com.amine.chessmath.LocalChessTest --console=plain`. Confirm the failure concerns the missing local behavior, then authorize implementation.
- [x] Run the focused regressions green after handoff. At integration, run `./gradlew :chessMath:testReleaseUnitTest :chessMath:lintRelease :chessMath:assembleRelease :chessMath:assembleDebugAndroidTest --console=plain`.
- [x] Use an explicit emulator serial for installation and `connectedDebugAndroidTest`. During iteration select one class per invocation, such as `'-Pandroid.testInstrumentationRunnerArguments.class=com.amine.chessmath.LocalChessRemoteTest'`; inspect XML class names/counts. Run the relevant full emulator suite at integration.
- [x] Capture the current native chooser, name editor, White and Black turns, promotion, pause, and results with generic names. Ask the fresh reviewer to inspect code and captures for all spec requirements, the five Review Focus conditions, and the preserved chess mood. Delegate fixes and repeat affected checks.
- [x] Install the corrected release version `0.13`, code `13`, on the physical TV and its emulator. Make sure that package versions and installed SHA-256 values match the tested release artifact. Preserve app data.
- [x] Make sure that portrait packaging, release versions and hashes, lint/build results, and test counts match the recorded evidence.
- [x] Fetch origin before integration. Preserve concurrent changes and repeat affected checks if integration changes the app. Commit only authorized changes and complete the shortest safe push/merge to `origin/main`. Report emulator tests, physical installation, and Git delivery separately.

Both `origin/amine/two-player-games` and `origin/main` reached `67bbb0d0321c872c256d2b4213cb86b9941ad7c8`.
The final merge incorporates validation documentation for other games from `aceb0e14`. App sources remain identical to the tested and installed `e4d433f0` revision.
