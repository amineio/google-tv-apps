# Battleship Implementation Plan

> For agentic workers: use superpowers:subagent-driven-development. Track each task below.

Goal: Deliver a complete offline Google TV Battleship game with remote controls and local saves.

Architecture: An independent Kotlin app contains a pure immutable engine, a restricted computer strategy, and a native Compose UI.
A ViewModel controls lifecycle, serialized persistence, sound, and cancellable turn delays.

Tech stack: Family Home Gradle 9.6.0, AGP 9.4.1, Kotlin 2.4.21, Compose BOM 2026.09.00, TV Material 1.1.0, JUnit 4.

Spec: `docs/superpowers/specs/2026-10-28-battleship-design.md` and the full user requirements in this task.

## Global constraints

- Keep all app code, resources, tests, and build files under `apps/battleship`.
- Preserve Java builds, other worktrees, backups, Home routing, and Cast packages.
- Use explicit emulator serials. Keep physical TV input separate and report deferred evidence.
- Use no network permission, telemetry, cloud, database, speculative multiplayer, or tablet code.
- Keep Git operations, integration, builds, and device control under the coordinator.

## Review focus

- Adjacent ships and several unresolved hits must not confuse AI target selection.
- Key repeat and lifecycle interruption must not create duplicate shots.
- A paused computer turn must resume once without hidden progress.
- A damaged save must not crash or expose enemy ships.
- Menus, confirmation dialogs, and background return must restore usable remote focus.

## Tasks

- [ ] Create independent build and failing engine tests. Run the tests before engine implementation.
- [ ] Implement `game` models, rules, placement, visibility, Normal strategy, and 1,000 seeded full-game simulations.
- [ ] Run unit tests. Review the engine API before session integration.
- [ ] Add failing persistence and session tests, then implement `persistence` and `viewmodel`.
- [ ] Add remote UI tests, then implement native menus, boards, results, dialogs, controls, and effects under `ui`.
- [ ] Generate a separate naval background. Compress the artwork and record prompts, origin, dimensions, and audio parameters.
- [ ] Add the launcher manifest, vector icon, banner, installer, app guide, and root README entry.
- [ ] Run debug and release unit tests, lint, APK builds, repository tests, and emulator instrumented tests.
- [ ] Inspect 1080p and 4K captures. Exercise save/restore, repeat input, restart, and complete-match flows.
- [ ] Obtain independent whole-change review. Fix material findings and repeat affected tests.
- [ ] Update the tracking PR with exact evidence. Merge tested installed changes into `origin/main` and report the APK path.
