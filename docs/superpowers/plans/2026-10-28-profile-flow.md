# Profile flow implementation plan

Goal: Remove profile-switch friction and replace paused-session prose with portrait badges.

Architecture: Keep controller pause and recovery behavior. Update MainActivity routing and ProfileScreen presentation only.

Spec: `docs/superpowers/specs/2026-10-28-profile-flow.md`.

- [ ] Add meaningful directional tests before implementation. Demonstrate their failure against the old interface.
- [ ] Replace every switch-confirmation callback with direct chooseProfiles. Retain parent and End session PIN behavior.
- [ ] Center the chooser and add noninteractive timer badges. Remove obsolete text-based test selectors.
- [ ] Run unit tests, release lint, APK builds, and explicit-serial emulator instrumentation.
- [ ] Obtain fresh code review and expert review of actual captures. Resolve actionable findings.
- [ ] Install version 1.8, code 9, on the TV. Commit and push installed source, merge the PR, and synchronize main.

The implementation worker owns MainActivity, ProfileScreen, and affected Android tests. The coordinator owns builds, Git, version, documentation, and devices. Reviewers perform read-only work.
