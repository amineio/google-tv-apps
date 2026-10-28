# Usage chart and profile menu implementation plan

Goal: Add remote-friendly daily bars, weekly totals, and a profile action menu.

Architecture: Reuse existing local usage records and settings destinations. Keep chart state separate from the accounting engine. Use native TV controls for profile actions.

Tech stack: Existing Kotlin, Compose for TV, java.time, and local Gradle build.

Spec: `docs/superpowers/specs/2026-10-28-family-home-history-chart.md`.

## Constraints and review focus

Preserve schema 3, 30 recorded dates, session behavior, PIN gates, selected app restoration, and Clock clearance. Do not add dependencies or permissions. Test empty records, future dates, calendar boundaries, return focus, and missing apps. The coordinator owns builds, Git, and devices.

## Task 1: Weekly report

- [ ] Add regression tests for a pure week model and native directional presentation.
- [ ] Make sure that the new model tests fail before implementation.
- [ ] Add `UsageHistory.kt`, `UsageHistoryScreen.kt`, and their tests. Integrate the chart into `SettingsScreen.kt`.
- [ ] Test seven Monday–Sunday dates, per-profile totals, missing/future/zero records, week bounds, scaling, and Back/recreation focus.

## Task 2: Profile actions

- [ ] Add directional tests for the profile menu, child and parent actions, empty app lists, dismissal, app return, and settings return.
- [ ] Update `HomeScreen.kt` and optional `ProfileMenu.kt`. Update `FamilyRemoteTest.kt` for the new path.
- [ ] Keep the existing callbacks and parent PIN routing. Remove the bottom actions.

## Task 3: Delivery

- [ ] Run debug/release unit tests, release lint, and APK builds with one worker.
- [ ] Run explicit-serial emulator instrumentation and review actual captures.
- [ ] Obtain fresh independent code review and expert UI review. Resolve material findings.
- [ ] Install the release on the TV, commit and push the installed source, merge the PR, and synchronize main.
