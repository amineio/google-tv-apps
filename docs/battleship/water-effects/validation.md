# Battleship 1.4 validation

Date: 2026-10-29. Version: 1.4, code 5. Minimum Android version remains API 26.

## Local results

All 46 unit tests passed in debug and release after the final fallback repair.
The release APK built successfully. Android lint reported zero errors and three existing warnings about tool versions and fixed orientation.
The complete 16-test emulator suite passed before the final fallback repair.
All three presentation tests passed again after that repair. They cover the automatic shader, forced Canvas, lifecycle interruption, focus, and saved history.
The repository suite passed 13 tests and skipped one existing conditional case.

The tests first failed for missing impact events and presentation behavior. They passed after implementation.
A later concealment assertion caught a synthetic fixture error: filler shots sank the test ship during a HIT preview.
The corrected fixture excludes the target and neighboring ship cell from filler shots. Its regression failed before that correction and passed afterward.
Production enemy redaction remained unchanged.

The local command for the final production and presentation checks was:

```sh
ANDROID_SERIAL=emulator-5580 apps/battleship/gradlew -p apps/battleship \
  testDebugUnitTest testReleaseUnitTest lintRelease assembleRelease connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.amine.battleship.NativeWaterPresentationTest \
  -Pandroid.testInstrumentationRunnerArguments.captureWater=true --console=plain
```

Use the JDK and Android SDK paths from the app README. Omit the `class` argument to run the complete emulator suite.
The capture argument writes 81 PNG files to `/data/local/tmp/battleship-water-*.png` on the selected emulator.
These files survive the test runner's APK removal. The fixtures use generic synthetic board data.

## Native visual evidence

The emulator rendered at 1920 by 1080 pixels. The previews below are compressed native captures, separate from the ImageGen concepts.
The capture matrix covers both renderers, both boards, all three outcomes, both opposite corners, and central enemy impacts.
The stages show accepted time, 120 milliseconds, and completion at 480 milliseconds.

- [Miss splash](native-miss.webp)
- [Ship hit](native-hit.webp)
- [Ship sunk](native-sunk.webp)
- [Friendly hit](native-friendly-hit.webp)
- [Canvas fallback](native-canvas.webp)

An independent reviewer accepted the source, specification, and final native captures with no unresolved material finding.
Review first found repeating water bands and a weak hit burst. The final shader and cached fallback use irregular ripples.
Stronger local light, sparks, and smoke make the compact hit visible without changing turn timing.
The cursor, coordinates, permanent markers, and hidden enemy ships retain their existing behavior.

A separate release run used directional input, one human shot, and a computer response.
Two active captures differed. Two paused captures were pixel-identical. This establishes movement and pause behavior on the emulator.
It does not establish physical TV frame pacing.

## Installation and limits

The final release installed successfully on the physical Chromecast with app data preserved.
Package inspection reported version 1.4, code 5. The installed APK hash matched the locally built release.
Release SHA-256: `25882d0a6ac703b28969ad17f8bffc981a0637eafa603e4c1ae972922046326a`.

Family Home redirected both the preceding 1.3 build and the new build to its profile chooser.
Its access log reported `decision=CHOOSER`. Waking the TV and normal profile selection did not keep Battleship open.
Physical TV readability, directional play, motion comfort, and frame pacing therefore remain unverified.
The task did not change launcher permissions, access rules, Cast packages, or timer behavior.

API 26 execution was not observed. Forced Canvas rendering on API 34 and the guarded shader class provide narrower compatibility evidence.
No claim of 60 frames per second, native 4K acceptance, or testing on every Android TV device is made.
