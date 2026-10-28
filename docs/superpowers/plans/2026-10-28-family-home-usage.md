# Daily usage delivery plan

1. Start from current main in an isolated feature worktree. Record the design reference and create a draft PR.
2. Add core regression tests before implementation. Add a pure duration engine, schema migration, and serialized controller accounting.
3. Add native TV usage pages and directional presentation tests. Increase the app version to 1.5.
4. Run unit tests, lint, release and debug builds, and emulator instrumentation through one coordinator.
5. Obtain independent code and UI reviews. Resolve material findings and repeat affected tests.
6. Install the release on the TV without clearing profiles. Commit, push, merge the PR, and make sure that origin/main contains the installed source.

The coordinator owns Git, builds, and device input. Core and UI workers own separate files. Generated concepts remain references only.
