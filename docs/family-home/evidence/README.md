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

`06-profiles-redesign-tv.png` and `07-child-home-redesign-tv.png` capture version 1.1 on the Chromecast.
The child home shows the six installed permitted apps and the independent Clock overlay.
The footer controls leave the Clock overlay clear.
The installed helper was bound, with no crashed accessibility services.
The YouTube shortcut redirected to child Home with the recorded decision `CHILD_HOME`.
Home returned from an allowed app without granting a new balance.
After standby, the device returned to the profile chooser.

The user asked to finish delivery because someone was watching the TV.
The physical expiry test was not completed. Cast and streaming pause were not retested for this visual change.
A temporary Sofia test session remains paused with about 17 minutes left.
End that session through the launcher when the TV is available to remove the test balance.
No existing streaming account data was cleared.

The source passed independent review with no actionable findings.
All Java app source and the existing root build tools remain unchanged.


## Version 1.2

The duration screen centers the portrait and controls. Start session receives initial focus.
The countdown uses fixed bounds and digit cells. The profile stays at the right edge.
App tiles launch directly. Other apps use a subdued icon tint instead of a large banner background.
Landscape banners and square icons use matching tile proportions.

Both unit variants passed 32 tests each. All 12 repository tests passed.
Release lint and the release, debug, and Android test builds passed.
The build used a fresh Gradle process with one worker after the parallel lint process stalled.
All 11 Android UI tests passed on an isolated 1080p Google TV emulator.
The coordinator used an explicit ADB serial and a separate ADB server to prevent concurrent test input.
The tests cover duration limits, initial focus, label alignment, recovery notices, fixed countdown geometry, tile launch, scrolling, focus restoration, empty lists, configuration, and PIN input.
The label test permits one dp of font rounding and rejects the original 15.5-pixel alignment error.
Independent code and rendered UI reviews report zero actionable findings.

New screenshots remain outside Git because the repository is public.
The generic home fixtures use solid-color artwork. Installed TV apps retain their native artwork.
Physical playback, timer expiry, wake, and Cast tests remain separate from this presentation evidence.
