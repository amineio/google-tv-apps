# Battleship Two-player Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add named local captains, a single ten-second fleet reveal, and safe alternating shared-TV turns while preserving computer play.

**Architecture:** Extend the existing rules/session/save path with explicit mode and participant identity. The session owns acknowledged saves, transient reveal/handoff state, and fixed impact identity; local UI receives public board projections after reveal. Reuse current rules, native Compose boards, effects, and audio resources.

**Tech Stack:** Existing Kotlin/Compose for TV, coroutines, serialization, atomic file persistence, JUnit/coroutine tests, Compose instrumentation; retain current dependencies and tooling.

**Spec:** [Approved two-player design](../specs/2026-10-30-two-player-games-design.md).

## Global Constraints

- Use the existing isolated `amine/two-player-games` worktree. The root coordinator owns Git, all builds/tests, installation, and device input.
- One app code implementer owns all app edits at a time. Independent portrait work and read-only review may overlap with disjoint ownership.
- Preserve computer mode, ten-by-ten boards, ship sizes 5/4/3/3/2, touching ships, edge clamping, alternating shots, effects, audio catalog, and launcher artwork.
- Use one shared TV and remote. First participant owns the fixed left board and fires first; second owns the fixed right board. Hits still change turns; duplicate shots do not.
- Names are custom or localized character defaults. Limit custom names to 24 Unicode code points, trim whitespace, reject controls, and use defaults for empty names. Language changes preserve custom names.
- Require distinct portraits. Stable IDs: `fox`, `owl`, `bear`, `cat`, `storm`, `reed`, `kai`, `mira`. The human defaults are Captain Storm, Commander Reed, Lieutenant Kai, and Chief Mira. Unknown IDs use deterministic distinct fallbacks.
- Use chosen names and portraits in setup, fleet ownership, handoff, pause, results, and accessibility. Never expose numbered players or internal seat enums as local participant names.
- Keep one saved session. Draft setup and canceled replacement preserve it. Commit only after final replacement confirmation.
- After both sequential Ready steps, show both fleets for exactly ten seconds using a monotonic clock. Durably consume preview eligibility before any ship reveal. Failed save retains the previous match and hides the new fleets.
- Interruption hides both fleets and consumes the preview. Continue, Resume, recreation, focus loss, or process restoration cannot replay it. Unfinished local restoration starts paused and hidden, then requires the active participant's Ready.
- After reveal, both board projections and semantics contain only public hits, misses, and sunk ships. Do not expose per-ship unsunk damage, ship associations, or private cell data through rosters or accessibility.
- Every accepted shot must finish its complete fixed-board effect and acknowledged save before handoff/results. Consume the Ready press and its repeats before enabling targeting.
- No local AI work, including stale callbacks. Local victory celebrates either internal seat. Rematch preserves identities/assignments and creates fresh fleets and a fresh Ready/reveal sequence.
- Write Battleship schema 2 and accept existing schema-1 saves as computer games without a reveal. Preserve existing board/history validation and explicit invalid-save recovery.
- Target Battleship version `1.9`, code `10`. Preserve package, permissions, independent build, and installer.
- Portrait owner supplies only `apps/battleship/src/main/res/drawable-nodpi/profile_{fox,owl,bear,cat,storm,reed,kai,mira}.webp` at 512×512 and the portrait manifest under `docs/two-player-games/portraits/`. App implementer must not edit those files.
- No agent builds, installs, controls devices, or commits. The coordinator completes those actions. No additional design approval gate is required.

## Review Focus

- Failed or delayed writes before reveal and after an attack must never expose private fleets, lose the prior match, or allow early targeting.
- Background, focus loss, recreation, and relaunch at each reveal boundary must restore hidden fleets without replaying previews, effects, or sound.
- Switching active seats during a shot must leave the effect on its original board; Ready and held OK cannot fire across the handoff.
- Public rosters, board projections, semantic labels, and full chosen-name labels must not disclose intact ship cells or unsunk ship identity.
- Supplementary Unicode names, duplicate custom names, unknown portrait IDs, canceled setup, and locale changes must retain distinct identities and valid saved data.

---

### Task 1: Implement the complete local Battleship path

**Owner:** One Battleship implementation subagent, after the chess code owner finishes. No edits outside this app except by explicit coordinator handoff.

**Files:**
- Modify: `apps/battleship/src/main/kotlin/com/amine/battleship/game/model/GameState.kt`, `game/engine/GameEngine.kt`, and `persistence/GameSaveRepository.kt` for mode, metadata, schema support, and public projections.
- Create: `apps/battleship/src/main/kotlin/com/amine/battleship/game/model/PlayerProfile.kt` for portrait IDs and bounded custom-name validation.
- Modify: `apps/battleship/src/main/kotlin/com/amine/battleship/viewmodel/GameSession.kt`, `SessionState.kt`, `GameViewModel.kt`, and `MainActivity.kt` for draft setup, acknowledged saves, reveal, lifecycle, Ready, and release gating.
- Modify: `apps/battleship/src/main/kotlin/com/amine/battleship/ui/BattleshipApp.kt`, `GameplayScreen.kt`, `GameBoard.kt`, `BoardPresentation.kt`, and app localization resources for chooser, fixed boards, public roster, full-name semantics, and named results. Add a focused `ui/LocalSetupScreen.kt` if needed.
- Modify: `apps/battleship/build.gradle.kts` and `apps/battleship/README.md`. Preserve portrait-owner files and existing art/audio assets.
- Test: `apps/battleship/src/test/kotlin/com/amine/battleship/viewmodel/LocalGameSessionTest.kt`, `game/LocalProjectionTest.kt`, `game/PlayerProfileTest.kt`, and existing persistence/session/impact/outcome tests.
- Test: `apps/battleship/src/androidTest/kotlin/com/amine/battleship/LocalTwoPlayerRemoteTest.kt`; adjust existing entry-path expectations where explicit setup changes them.

**Interfaces:**
- Consume existing `GameEngine.newGame(seed: Long): GameState`, `attack(state: GameState, attacker: Player, coordinate: Coordinate): AttackResult`, `isValid(state: GameState): Boolean`, `GameSaveRepository.load(): SavedSession?` / `save(SavedSession): Unit`, and injected coroutine/time/seed sources.
- Keep serialized `Player.HUMAN` / `Player.COMPUTER` values as fixed internal owners. Produce `GameMode { COMPUTER, LOCAL }`, mode/participant metadata in the saved match, and `PlayerProfile(portraitId: String, customName: String?)`. Use stable IDs rather than drawable integers.
- Preserve `GameSession.action(action: GameAction): Unit`, `background(): Unit`, and `state: StateFlow<SessionState>`. Add explicit setup, Ready, and confirmation actions to `GameAction`; keep draft participants separate from the committed match.
- Produce `GameEngine.publicBoard(board: Board): VisibleBoard` containing sunk ships and public shots only. Local rendering and semantics must consume this projection after reveal, rather than raw `Board` or an opaque cover.
- Extend `ImpactBoard` with fixed `LEFT` / `RIGHT` targets for local play, preserving solo `ENEMY` / `FRIENDLY` behavior. Keep unique `ImpactPresentation.id`; an old completion callback must not finish a new effect or advance handoff.
- Resource contract: `R.drawable.profile_fox`, `profile_owl`, `profile_bear`, `profile_cat`, `profile_storm`, `profile_reed`, `profile_kai`, `profile_mira`. Connect the independently supplied portraits without changing their files.

- [ ] Write focused failing session/projection/save regressions first. Cover both local seats and no AI; fixed boards and effects; duplicate attacks; save-before-reveal with delayed/failed write; both Ready steps; full `10` initial countdown and hidden state at 10,000 ms; blocked reveal firing; stale work rejection; and save-plus-effect completion before Ready/results.
- [ ] Add interruption/restore and input regressions for the first three Review Focus conditions: reveal interrupted before/during/at expiry, hidden paused restoration, no replay on Resume/Continue/recreation, held Ready/OK rejection, stale impact IDs, save failure retry, and either-seat named victory with local celebration.
- [ ] Add projection/name/migration regressions for the last two Review Focus conditions: both fleets stripped of intact ships; public hit/miss/sunk data retained; no unsunk ship association/damage roster; schema-1 migration and schema-2 round trip; invalid mode recovery; bounded Unicode/control/empty names; distinct portrait fallback; custom names preserved by locale changes; canceled setup preserves the previous save.
- [ ] Report exact focused selectors and expected test names/counts to the coordinator. Wait for the coordinator's red-test result and implementation green-light; do not run Gradle.
- [ ] Implement the mode/profile/save changes and the session-controlled draft → final confirmation → both Ready steps → acknowledged consumed-preview save → ten-second reveal → hidden turns flow. Use acknowledged writes rather than conflated fire-and-forget saves for reveal, attack, and replacement gates.
- [ ] Implement native chooser/name editor and fixed-owner gameplay using the public projection; retain full names in semantics, distinct focus/assignment states, opponent-only targeting, public fleet counts, named results, and same-identity rematch. Wire pause, focus loss, background, and recreation to hide transient reveal and invalidate stale work.
- [ ] Add remote regressions using real D-pad input for chooser/editor/confirmation cancellation, initial/return focus, scrolling, reveal, both target boards, Ready release gating, public semantics, and results. Update app guide/version and report all changes/selectors to the coordinator for green testing. Do not commit.

### Task 2: Integrate, review, and deliver Battleship

**Owner:** Root coordinator, with a fresh independent code/game UI reviewer and the same implementation owner for corrections.

**Files:** Read Task 1 outputs and final portrait manifest. The coordinator records commands, revision, and fresh captures; any documentation edits remain delegated.

**Interfaces:** Consume Task 1 projections/actions and regression selectors. Produce passing app checks, accepted native captures, version/hash evidence, and delivery to `origin/main`.

- [ ] Run focused red tests before Task 1 implementation. Example: `apps/battleship/gradlew -p apps/battleship testDebugUnitTest --tests com.amine.battleship.viewmodel.LocalGameSessionTest --console=plain`. Confirm a missing-behavior failure and authorize implementation.
- [ ] Run the focused regressions green after handoff. At integration, run `apps/battleship/gradlew -p apps/battleship testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease assembleDebugAndroidTest --console=plain`.
- [ ] Use an explicit emulator serial for installation and `connectedDebugAndroidTest`. During iteration select one class per invocation, such as `'-Pandroid.testInstrumentationRunnerArguments.class=com.amine.battleship.LocalTwoPlayerRemoteTest'`; inspect XML class names/counts. Run the relevant full emulator suite at integration.
- [ ] Capture the current native chooser/name editor, both Ready steps, reveal, hidden turns targeting each board, stable shot impact, handoff, interruption recovery, and either-seat results with generic names. Ask the fresh reviewer to inspect code and captures for all spec requirements and the five Review Focus conditions. Delegate material fixes and repeat affected checks.
- [ ] Verify portrait packaging, release version/hash, lint/build results, and test counts. Fetch origin before integration; preserve concurrent changes and repeat affected checks if integration changes the app. After successful installation, commit only authorized changes and complete the shortest safe push/merge to `origin/main`. Report emulator installation and Git delivery separately; no physical TV operation is required.
