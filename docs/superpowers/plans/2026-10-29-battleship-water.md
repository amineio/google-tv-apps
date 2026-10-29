# Battleship water implementation plan

Implement the approved [design](../specs/2026-10-29-battleship-water-design.md).

## Global constraints

Keep minSdk 26, current dependencies, native controls, hidden enemy ships, audio, saved matches, and turn timing.
Use native AGSL on API 33+ with a Canvas fallback. Complete impacts within 480 ms.
Keep all changes inside Battleship and related documentation. The coordinator owns builds, device input, and Git operations.
Use the current clean main checkout, as repository instructions permit. Preserve all other worktrees and backups.

## Review focus

Old shot history must not replay an impact on Continue or app recreation.
A completion for an older ID must not clear a newer impact.
Pause and background activity must stop decorative frame updates.
Continuous water must not prevent Compose tests from becoming idle.
Edge impacts must not cover coordinates or disclose intact enemy ships.

### Task 1: Accepted-shot presentation state

Own `viewmodel/SessionState.kt`, `viewmodel/GameSession.kt`, and focused session tests.
Read the approved design before implementation. Add the exact impact types and silent completion action from its impact contract.
Write meaningful failing regression tests before implementation. Include accepted shots on both boards, rejected fire, monotonic IDs,
stale completion, injected start time, pause, background, confirmation, Settings, Home, restart, Continue, and save restoration.
Preserve all existing turn delays and persistence behavior. Do not couple impacts to the sound channel.
The coordinator runs Gradle. Request focused test runs and inspect their results. Do not build, install, commit, or spawn agents.
Report modified files, red/green evidence, remaining concerns, and exact interfaces.

### Task 2: Native water and impacts

Own UI graphics helpers, `ui/GameBoard.kt`, `ui/GameplayScreen.kt`, and presentation tests.
Consume the Task 1 impact contract and dispatch `CompleteImpact` silently on finish or cancellation.
Implement AGSL water in an API-gated helper with a cached Canvas fallback and a way for tests to force that fallback.
Use cached procedural texture data for the fallback. Add compact native particles for splash, hit, and stronger sunk impacts.
Share measured geometry across layers. Preserve the single board focus target, input handling, semantics, and redacted ship view.
Use a controllable presentation clock for test captures and lifecycle gating. Avoid per-frame recomposition of static content.
Write focused regression tests before behavior changes. The coordinator runs compilation and emulator tests.
Do not change session logic, dependencies, game rules, audio, versions, docs, or installer. Do not build, install, commit, or spawn agents.
Report modified files, test requirements, capture controls, and remaining concerns.

### Task 3: Integration, evidence, and delivery

The coordinator integrates the tasks and sets version 1.4, code 5.
Run debug and release unit tests, lintRelease, debug/release APK builds, and the test APK build.
Use the dedicated Battleship emulator with explicit serials. Run instrumented tests once builds are stable.
Inspect miss, hit, sunk, fallback, dense-board, and interrupted-state captures. Fix material defects.
Obtain a fresh source and visual review. Test physical TV readability and release frame pacing when the TV is available.
Record concept prompts, source references, rendered evidence, test results, and limits in app documentation.
After successful installation, commit only scoped app/docs changes and push origin/main. Report installation and push separately.
