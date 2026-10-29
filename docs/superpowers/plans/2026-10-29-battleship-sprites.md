# Battleship sprites implementation plan

> For agentic workers: Use superpowers:subagent-driven-development.

**Goal:** Deliver the revised naval game with stable larger boards, generated sprites, overhead effects, and improved sounds.
**Architecture:** Native Compose boards draw compressed assets and finite effects. Session timing holds each sequence before the next turn or results.
**Tech Stack:** Kotlin, Compose for TV, Canvas, Android SoundPool, local WebP and WAV.
**Spec:** docs/superpowers/specs/2026-10-29-battleship-sprites-design.md

## Global Constraints

Keep minSdk26 and the independent Kotlin build. No new runtime dependencies, network permission, or changes to other apps. Preserve game rules, redaction, D-pad focus, persistence, pause and background cancellation. Use MISS1100ms, HIT1500ms, SUNK2500ms. Both grid side lengths match and remain stable. Coordinator owns assets, builds, device input, Git and delivery. User authorizes autonomous decisions and up to four rendered rounds. Only final version installs on physical TV.

## Review Focus

Final sunk shot stays visible before results. Pause/resume never replays stale effects. Consecutive shots never replace ongoing effects. All ship rotations stay within their cell strips. Long feedback never changes grid bounds.

### Task 1: Implement native sprite scene and timing

**Files:** apps/battleship/src/main/kotlin/com/amine/battleship/ui/, viewmodel/, audio/GameAudio.kt, MainActivity.kt and related unit/android tests. Build version1.5 code6.
**Interfaces:** Coordinator supplies drawable-nodpi ship_carrier.webp, ship_battleship.webp, ship_cruiser.webp, ship_submarine.webp, ship_destroyer.webp, ocean_still.webp. Each sprite is tightly cropped, bow-up. Retain the eight raw audio resource names. Coordinator supplies the final files and provenance separately.

- [ ] Read the spec and /tmp/battleship-art-direction.md for exact visual direction.
- [ ] Add failing unit tests for outcome-specific duration, hold of next attack/results through full sinking, final cue ordering, and pause/resume cancellation. Adapt old 480ms and old cadence assertions intentionally.
- [ ] Implement shared outcome timing, session gating, full-hull sinking metadata, and finite presentation clock. Remove procedural water from gameplay and continuous frame scheduling. Preserve public focus tags and semantics.
- [ ] Replace ship vectors with decoded remembered bitmap sprites. Keep hull aspect ratio, exact cell footprints and rotation. Dim sunk silhouettes and animate the active sunk hull with shared progress.
- [ ] Implement balanced equal-grid layout, fixed status and footer heights, all five health entries below friendly grid, overhead splash, slower hit and distributed fire/submersion.
- [ ] Keep SoundPool resource mapping and make mute/pause/background stop extended cues. Final-shot SUNK audio must play before victory/defeat.
- [ ] Add meaningful emulator assertions for fixed board bounds, first focus, directional aiming, long feedback, interruption, redaction and staged effects. Replace obsolete shader matrix tests with static-sprite presentation tests and optional capture flag. Do not run device inputs or builds until coordinator permits.
- [ ] Report changed paths, test red/green evidence, remaining concerns to task report. Do not commit or spawn subagents. Coordinator integrates and builds.

### Task 2: Prepare assets and review rendered rounds

Coordinator prepares generated raster assets and CC0 audio with source manifests. Build and test locally, capture emulator scenes and clips. Ask game designer for scores and actionable issues. Resume implementer for targeted fixes, up to four rendered rounds. Run a fresh independent final source review after tests, fix material findings, then install final APK on TV and push scoped changes to origin/main.
