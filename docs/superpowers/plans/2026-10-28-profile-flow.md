# Profile flow implementation plan

Goal: Remove profile-switch friction, show portrait timer badges, and save app order for each profile.

Architecture: Keep controller pause and recovery behavior. Use native TV long-click controls and the existing state store for app order.

Spec: `docs/superpowers/specs/2026-10-28-profile-flow.md`.

- [x] Add meaningful directional tests before implementation. Demonstrate their failure against the old interface.
- [x] Replace every switch-confirmation callback with direct chooseProfiles. Retain parent and End session PIN behavior.
- [x] Center the chooser and add noninteractive timer badges. Remove obsolete text-based test selectors.
- [x] Run unit tests, release lint, APK builds, and explicit-serial emulator instrumentation.
- [x] Obtain fresh code review and expert review of actual captures. Resolve actionable findings.
- [x] Install version 1.8, code 9, on the TV. Commit and push installed source, merge the PR, and synchronize main.

The implementation worker owns MainActivity, ProfileScreen, and affected Android tests. The coordinator owns builds, Git, version, documentation, and devices. Reviewers perform read-only work.

An additional worker owns app-order state, codec migration, controller save, catalog ordering, HomeScreen move mode, and their tests. MainActivity integration remains with the coordinator after profile routing edits finish. Write core regression tests before implementation. Run real directional and held-OK tests on the emulator. Include schema migration, persistence failure, independent profile orders, allowlist filtering, boundaries, scrolling, save, and cancel in validation.
