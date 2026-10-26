# Chess and math simplification implementation plan

> Use subagent-driven development with separate file ownership.

Goal: Simplify the remote flow and align the visual states with the forest and ivory design.

Architecture: Keep behavior in SessionController, UI in MainActivity, and paint styles in TvStyle and ChessBoardView. Use controller tick for automatic advancement. Preserve native accessibility and saved progress.

Spec: ../specs/2026-10-26-chess-math-simplification-design.md

## Tasks

- [x] Controller agent: Add failing tests for exact-answer entry, zero, partial and wrong inputs, duplicate counting, foreground delay, pause, restore, storage failure, and phase boundaries. Implement automatic acceptance and advancement in SessionController. Run unit tests.
- [x] UI agent: Reduce copy and keypad size, remove OK and Next, show success feedback, and add promotion images. Update remote and layout tests with memory fixtures.
- [x] Palette agent: Remove artificial focus corners and align board markers with forest, ivory, and sage. Keep equal board geometry and controls.
- [x] Coordinator: Build, lint, and run unit tests. Install on the real TV without clearing saved progress. Run memory-only instrumentation tests and capture math, success, promotion, and board states.
- [x] Independent reviewer: Compare final screenshots and inspect flow/lifecycle changes. Resolve material findings.
- [x] Coordinator: Record evidence, commit changes, push the authorized branch, and report results.

## Review focus

Partial numeric prefixes do not count as wrong answers. Correct entry counts once despite repeated keys. Background timers never advance unseen exercises. Restored solved exercises retain feedback before advancement. Long hints remain reachable with the remote.
