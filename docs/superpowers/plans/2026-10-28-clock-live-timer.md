# TV Clock timer and visual update

Use the existing flat TV settings layout. Keep only meaningful status and values.
Remove the bottom-left navigation hints and redundant row subtitles.
Show a live analog clock on the left and update it for time and time zone changes.
Use the new image reference for a cleaner matching clock icon and launcher banner.

Replace preset timer choices with a remote-controlled hours and minutes editor.
Keep the saved `minutes` preference. Accept positive durations and reject zero.
Do not start a timer when the user saves the duration. Back cancels edits.

Add device tests for a custom duration, zero, cancellation, and live clock time.
Run Clock tests, lint, builds, strict repository tests, and remote interaction tests.
Capture and inspect the main screen, timer editor, appearance screen, and launcher tile.
Get an independent review. Install, commit, push, and merge the Clock changes.
