# Battleship menu and help implementation plan

> For agentic workers: use superpowers:subagent-driven-development for focused implementation and independent review.

Goal: simplify menu copy, add language choice, organize Help, and clarify aiming.

Architecture: scoped Android resources provide app-local translations. SavedSession persists a default-compatible language enum. Native Compose cards share the gameplay targeting sight. Raster artwork supplies a fictional opponent portrait.

Spec: docs/superpowers/specs/2026-10-29-battleship-menu-help.md

- [x] Generate help and portrait references. Obtain independent game UI/UX concept review.
- [x] Add failing persistence/session language tests and implement translated resources and a focused language control.
- [x] Add a failing pixel regression for rectangular cursor corners. Implement a circular sight without old-shot corner brackets.
- [x] Build organized native Help cards and tests for all three languages.
- [x] Run both unit variants, lint, builds, and the full emulator suite. Review native multilingual captures and source independently.
- [x] Adapt the supplied tactics photograph into a separate translated native page and inspect focused test results.
- [x] Add risk-based test guidance and documented solutions to repeated development issues.
- [x] Complete the final native Tactics review in all three languages.
- [x] Complete the final physical TV installation identity. The installed APK matches the local release.
- [ ] Commit and push scoped installed changes to origin/main.

Review focus: old save compatibility, paused match preservation, focus during language changes, French/Dutch text overflow, reticle movement without persistent highlights, hidden enemy ships, and unchanged music lifecycle.

Coordinator owns builds, device input, integration, and Git. Language, concept, help, and expert review tasks use scoped workers. Existing clean main atfeff842a is reused. No approval questions are required. The user supplied `30213.jpg` during implementation. Its four tactics now use rewritten text and native diagrams.
The photograph stays outside the app and repository.
The base integration checkpoint passes 75 unit tests per variant and 23 emulator tests.
The initial combined filter selects only Help, with one passing test across all three languages.
A later single-class Tactics invocation passes one test across all three languages.
Release lint and packaging pass after the addition.
Final version 1.8/code9 installs on the TV with a matching APK hash.
The bounded physical capture shows the launcher profile chooser, so physical Tactics review remains deferred.
The final native Tactics review and focused emulator tests pass in all three languages.
Commit and push remain pending.
