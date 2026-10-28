# Family Home 1.6 validation

## Scope

Each profile now has a seven-day bar chart with daily selection and a recorded weekly total. Week controls navigate retained history. The home profile picture opens the existing profile and management actions. The bottom action row is removed. Usage accounting, schema 3, profile records, PIN gates, and session limits remain unchanged.

The feature starts from current main at `642af350` in a separate worktree and branch. PR #14 tracks the change. Other app work remains separate.

## Local evidence

The baseline passes 62 debug unit tests. New week-model tests fail before implementation because the model does not exist. Four profile-menu tests fail against the original home screen before the menu implementation.

The completed app passes 66 unit tests in each build variant. Release lint reports zero errors and six warnings. Release, debug, and Android test APK builds pass. All four Family Home installer tests pass.

Explicit emulator instrumentation reports `OK (35 tests)`. Tests use directional input for profile actions, app return, empty catalogs, PIN dialogs, confirmation dismissal, settings return, week paging, selected-day totals, bounds, Back, and recreation. Cases distinguish missing records, future dates, recorded zero, and seven varied daily totals.

The first full emulator run finds test input that targets both the app and a new dialog window. The corrected helpers select the active dialog and wait between keys. Product callbacks remain unchanged.

Fresh independent code review reports zero actionable findings after the final changes. Expert UI review reports zero actionable findings across actual chart, empty, zero, future-record, menu, and home captures. The final review includes seven varied bars and the corrected empty-week scale label. Captures remain outside Git. Dialog captures show that window only. The home fixture uses test tiles.

## Old Java launcher removal

The initial user-only removal leaves the old package installed for another Android user. A global uninstall without `-k` removes `com.gathin.tvhome` and its retained data for all users. Android no longer lists the package, including uninstalled retained packages. Accessible external data, media, and OBB folders are absent. Helper and screensaver settings contain no old components.

The exact temporary retirement backup from the preceding task is deleted from the Mac at the user's request. The Java source remains available in this repository. Family Home retains the Home role. Google TV Home, setupwraith, and TV Clock remain installed. A before/after storage observation shows 1,036 KiB more available space. No device identifiers, family screenshots, or backups enter Git.

## Limits

The canonical installer installs release 1.6, code 7, on the physical TV. Family Home retains its Home role and a bound helper with no crashed services. Directional input opens the profile menu, Family configuration, Daily usage, and the weekly chart. The chart retains the existing parent total after upgrade. Actual app tiles render on Home. The Clock overlay remains visible without overlap on Home, the menu, and the chart. No real fourth profile is created.

History contains up to 30 recorded dates. Missing records do not establish zero use. This change adds no hourly or per-app records. Emulator evidence does not establish Cast or physical streaming behavior.
