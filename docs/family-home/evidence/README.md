# Family Home design evidence

Date: 2026-10-28.

`01-profile-chooser.png` captures the first installed Chromecast interface.
The user rejected its visual design.
`02-emulator-before-refinement.png` reproduces the dark heading in the Google TV emulator.
`03-emulator-contrast-fixed.png` captures the corrected text color in the release build.
These images do not establish approval of the layout.

The Google TV emulator uses Android 14, ARM64, 1080p, and density 320.
The Mac supports Hypervisor.Framework acceleration.
A local full-resolution screenshot took 1.65 seconds in one measurement.
This measurement is not a comparison with the Chromecast.

All three emulator UI tests passed after the text correction and remote input test changes.
The heading contrast test failed before the correction.
The profile test covers directional navigation between the three profiles.
The configuration test covers D-pad activation after an explicit focus request.
It does not establish directional navigation to the configuration control.

The release app opened successfully in the emulator.
Manual D-pad input reached the Elias duration screen with a default of 20 minutes.
Cast, streaming playback, physical wake behavior, and final design acceptance remain separate device tests.

## Version 1.1

Amine approved the two concept images before implementation.
`04-profiles-redesign-emulator.png` captures the native profile screen with the final font weights.
`05-child-home-redesign-emulator.png` captures a real Elias session in the release app.
Only Chess & Math was installed from the child allowlist in this emulator.
The image does not use invented app tiles or account data.

Eight emulator UI tests passed for this version.
They cover profiles, text contrast, duration limits, app filtering, row navigation, catalog changes, focus restoration, configuration, and PIN input.
The configuration test now uses directional navigation rather than an explicit focus request.
The release remains below the existing 4 MiB limit.
