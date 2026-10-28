# Family Home 1.5 validation

The work starts from main at `74b07ef1` in a separate worktree and feature branch. PR #12 tracks the change.

## Local evidence

The initial regression run fails because the usage model and engine do not exist. After implementation, 62 unit tests pass in each build variant. Tests cover migration, midnight, daylight saving, clock changes, expiry, profile changes, standby, helper loss, suspended writes, failed persistence, retention, and storage size.

Release lint completes with zero errors and six warnings. Release, debug, and Android test APK builds pass. All four Family Home installer tests pass.

Explicit emulator instrumentation reports `OK (30 tests)`. The tests use directional input and cover the settings routes, PIN dialogs, allowed apps, optional profile creation fixtures, usage totals, empty history, 30-date scrolling, and focus after Back and recreation. Fixtures do not create a real fourth profile.

The independent full-change code review reports zero material findings. The UI reviewer inspects four actual emulator captures and reports zero material findings. Captures remain outside Git.

After instrumentation, Android leaves the emulator helper unbound. Rebinding the helper restores the service without an app code change. A separate runtime observation records a 10,867 ms parent checkpoint. Standby clears selection. After five seconds asleep and wake, the total remains 19,113 ms and the chooser remains selected. These observations use the real debug repository, not presentation fixtures.

## Delivery boundaries

Emulator evidence does not establish physical streaming, Cast, or Clock overlay behavior. The existing timer semantics remain unchanged. Usage collection begins with this update. There is no reconstructed history.

The previously installed Java Home package is removed from the TV with `pm uninstall -k --user 0`. Its APK and package state are backed up privately. Its app data and repository source remain. Family Home, TV Clock, stock Google TV Home, setupwraith, the Home role, and the helper configuration remain present after removal. No device identifiers or backups enter Git.
