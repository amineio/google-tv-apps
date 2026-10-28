# TV Clock settings evidence

These screenshots come from the connected Chromecast on October 28, 2026.
The after captures show the installed release app, version 1.3, version code 4.
The Imagine concept is a design reference at `../../concepts/2026-10-28-settings-impression.png`.

The before captures show the old main page, appearance controls, and launcher tile.
The after captures show the flat settings lists, focused switch and adjustment rows, native choice dialogs, and new launcher tile.
Some dialog captures use temporary bedtime values from the capture setup.
The final main capture shows the original 22:30 bedtime. The clock is on, at the bottom, with text size 28.

The Clock debug and release suites each pass 14 unit tests.
All eight Android device tests pass against the release app.
Both Python device acceptance tests pass. They cover the overlay, immediate layout changes, saved values, and Home behavior.
The Python cleanup now waits for the Home transition before it reopens Clock and restores the saved layout.
All eight strict repository tests pass. Release assembly, release lint, and Android test compilation pass.
Lint reports two existing test dependency update warnings and no errors.

The added tests failed against the old controls before implementation.
They cover remote adjustment, hidden-clock behavior, duration selection, reminder selection, and Back cancellation.
The independent source and screen review found no actionable issues.
