# Battleship Gameplay Design Implementation Plan

> For agentic workers: use scoped subagent implementation and independent review. The coordinator owns integration, builds, Git, and devices.

Goal: Deliver three visible design iterations and a finished native naval gameplay screen.
Architecture: Keep the game engine unchanged. Use a native command panel, Canvas boards and ship drawing, plus one compressed illustration.
Tech stack: Existing Kotlin, Compose for TV, Canvas, JUnit, and independent Gradle build.
Spec: docs/superpowers/specs/2026-10-28-battleship-gameplay-design.md

## Global constraints

Preserve engine, AI, persistence, audio, hidden enemy ships, and one focusable board.
Keep five percent safe margins and the dominant enemy grid. Add no dependencies or network permissions.
Only the coordinator builds, changes shared files, operates Git, or controls devices.

## Review focus

An aim change must not pair a new coordinate with an old shot result.
Long sunk names and damaged fleets must fit inside the command panel.
Board labels, misses, hits, and selected cells must stay distinct in a dense match.
Decorative graphics must never receive focus or reveal enemy information.
Pause, resume, held OK, app return, and all 100 cells must retain their behavior.

## Task 1: Native ship artwork

Create: apps/battleship/src/main/kotlin/com/amine/battleship/ui/ShipArtwork.kt.
Interface: `internal fun DrawScope.drawNavalShip(ship: Ship, origin: Offset, pitch: Float, sunk: Boolean)`.
Consume the existing Ship, ShipType, Coordinate, and NavalTheme colors.
Produce five distinct fitted steel hulls with deck details and subtle shadows.
The carrier has a flight deck. The submarine has a slim capsule. Surface ships have different turrets and bridges.
Use Compose Canvas. Both orientations must fit their cell footprint. Sunk hulls retain coral damage and visible hit crosses.
Do not edit GameBoard.kt. The coordinator integrates the function.

- [x] Implement the reversible drawing change. Review it in native captures instead of writing tests that mirror paths.
- [x] Obtain source and rendered review before delivery.

## Task 2: Composition and three iterations

Modify: BattleshipApp.kt and GameBoard.kt. Create: GameplayScreen.kt, command_battleship.webp, and an asset record.
Tests: BattleshipRemoteTest.kt.

- [x] Add a failing UI regression for separate target and actual last-shot feedback after directional input.
- [x] Operate the regression on emulator-5580 and record the expected failure.
- [x] Implement iteration 1 and integrate task 1. Capture and score the actual app.
- [x] Implement iteration 2 from designer findings. Capture and score a normal and dense match.
- [x] Implement iteration 3 from remaining findings. Capture and score the final app.
- [x] Operate unit tests, release lint, both APK builds, full emulator tests, and applicable repository tests.
- [x] Obtain fresh independent source and rendered review. Fix material findings.
- [x] Record all scores, asset prompts, paths, sizes, rights, test results, and evidence limits.
- [x] Install version 1.2, code 3 in the background. Family Home remains in front.

Delivery follows the repository policy: commit installed changes, push the feature branch, and merge PR #10 into origin/main.
