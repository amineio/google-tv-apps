# Battleship atmosphere implementation plan

> For agentic workers: use superpowers:subagent-driven-development for focused tasks and independent review.

Goal: add quiet adventurous battle music and a restrained naval game screen.

Architecture: retain native Compose grids and MediaPlayer policy. Add one loop and one small portrait. Keep all device, build, integration, and Git operations under the coordinator.

Spec: docs/superpowers/specs/2026-10-29-battleship-atmosphere.md

## Tasks

- [x] Generate a screen concept and generic portrait with recorded prompts. Ask a separate game UI/UX expert to review the concept critically.
- [x] Add failing music tests for opening completion, resume, final-shot cancellation, and interruption. Implement BATTLE in MusicController and native resource mapping.
- [x] Add a failing native UI test for removed copy and equal grids. Implement the compact captain header and restored scenery.
- [x] Run both unit variants, lint, debug/release builds, and the full emulator suite. Inspect native captures and obtain independent game UI/UX and source review.
- [x] Install the accepted final release on the TV. Make sure that the installed APK matches. Commit and push origin/main.

Review focus: lost focus must not replay openings; final-shot timing must preserve result music; paused gameplay must not leak music; captain artwork must not reduce board size; native labels and hidden enemy information remain correct.

The coordinator reuses the clean current main checkout at99196c9c. The user opts out of approval gates. Other worktrees and device backups remain untouched.
