# TV Clock implementation plan

Spec: `docs/superpowers/specs/2026-10-28-clock-flat-settings-design.md`.
Implement in the isolated `amine/clock-flat-settings` workspace from `fd1e60f7`.
Preserve the existing services, limit calculations, and saved preference keys.

## Task 1: Flat settings and remote controls

Add TV interaction tests in `LimitsDeviceTest.java` before product changes.
The tests must catch lost Left/Right adjustment, accidental clock enabling, and timer choice changes that start a timer.
The tests also cover reminder selection and Back cancellation.
Operate the new tests against the existing app and record the expected missing-control failures.
Add a reusable native `SettingRow` with an icon, title, detail, trailing value, and one focus target.
Update `TvUi` with a shared charcoal settings layout, pale row focus, and native switch styling.
Update `ClockSettingsActivity` to use full rows for position, size, reset, and Time limits.
Update `MainActivity` to use the same list style for appearance and time limits.
Use native single-choice dialogs for duration and reminder action. Preserve existing duration preference storage.
Keep the existing bedtime editor and permission handling.
Update the Python device acceptance test for Left/Right adjustment.
Run Clock tests, lint, assembly, and Android test compilation. Expected: all pass.
Install the debug app and test APK. Operate the Android tests on the connected TV. Expected: all pass.

## Task 2: Brand assets and visual proof

Replace `clock_icon.xml` and `clock_banner.xml` with matching clock symbols and a legible banner wordmark.
Keep assets as Android vectors. Update the app version to 1.3, code 4.
Update the Clock README for the new control flow.
Install the release app with `tools/install-clock.sh`.
Capture the main list, appearance list, choice dialogs, and launcher tile from the TV.
Inspect the captures for clipping, alignment, icon contrast, and focus visibility.
Run release tests, lint, build, Android test compilation, and strict repository tests. Expected: all pass.

## Task 3: Review and delivery

Request an independent review of the complete change, including screenshots and remote controls.
Resolve material findings and repeat affected tests.
Commit only Clock changes, its reference, design, plan, and evidence.
Push the branch, open a focused PR, and wait for CI.
Merge into origin/main and update the original checkout with a fast-forward pull.
Report the Chess merge, Clock installation, TV screenshots, tests, and Clock merge separately.

## Review focus

Inspect choice-dialog focus restoration and Back cancellation.
Inspect adjustment bounds and hidden-clock behavior.
Inspect permission return handling and state refresh while a page is open.
Inspect native switch focus colors and launcher branding at TV size.
