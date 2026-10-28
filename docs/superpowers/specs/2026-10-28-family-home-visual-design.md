# Family Home visual design

Amine approved the two image concepts and requested implementation on 2026-10-28.
The references are `docs/family-home/concepts/profile-entry.png` and `child-home.png`.
They show two screens from one design.

Use a charcoal background, subtle warm light, off-white text, and restrained gold focus outlines.
Restore Outfit typography from the existing launcher without changing that app.
Use circular illustrated avatars for Amine, Elias, and Sofia.
Center the profile heading and avatar row. Keep the clock and wordmark in the header.

The home screen has a compact profile header and a visible child timer.
Show an app feature area above one horizontal row of landscape app tiles.
Use generated chess artwork for Chess & Math and actual installed app banners for other apps.
The feature area follows the focused app. Open app launches that same app.
Show only installed apps that the existing access policy permits.
Keep the approved six-app order for children. Keep all TV apps available to Amine.
Place Switch profile and the existing parent or session controls below the app row.

Use one visible focus target at a time. Support D-pad navigation, long app names, and scrolling app rows.
Restore focus to the app after returning from it. Refresh the catalog after an app changes or the launcher resumes.
Use the same visual style for duration, expiry, configuration, and PIN controls.
Keep the existing timer, PIN, storage, access rules, package, and helper behavior.
Keep the Clock, Chess, and Java Home source unchanged.

## Approach

Amine selected the image-generated direction inspired by Google TV and YouTube.
The first implementation used default Compose buttons and lost the previous launcher's visual refinement.
The implementation will use focused Compose components and the existing family controller.
The generated artwork supplies images only. Text, controls, focus, and app data remain native UI elements.

## Acceptance

Test profile navigation, child duration limits, parent configuration, app filtering, and focus after app return.
Capture the profile and child home screens in the emulator and on the Chromecast.
Share captures through phone-accessible links in the chat.
Run unit tests, Android UI tests, lint, a release build, and independent review before delivery.
Keep the release below its existing 4 MiB limit.
After installation, commit and merge the installed app source into `origin/main` under the repository instructions.
