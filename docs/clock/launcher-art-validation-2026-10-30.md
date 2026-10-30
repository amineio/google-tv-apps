# Clock launcher artwork validation, 2026-10-30

This follow-up refreshes the launcher artwork and prepares an authorized physical-TV installation. Focused emulator validation, rendered review, and physical installation are complete. Git delivery is complete.

## Scope and revision

The task continues in branch `amine/clock-dashboard` at `/Users/amine/Developer/worktrees/clock-dashboard/google-tv-apps`. Version 1.6 (code 7) changes artwork resources without changing app logic.

The refreshed native vector icon and banner use flat-ended hands at 10:10 and 12 simplified ticks. The separate launcher background is exported from SVG to a 1920 × 1080 WebP. It has no text, places the clock at the upper right, and keeps the left and lower areas quiet and dark. Export targets are 512 × 512 for the icon and 640 × 360 for the banner.

## Completed evidence

The existing version 1.5 XML hero drawable could not decode through the Family Home `ImageDecoder` path. The new focused `ArtworkDeviceTest` confirmed the RED case against installed version 1.5: `ImageDecoder.DecodeException` with an unimplemented decode result.

Version 1.6 build and lint checks passed in 10 seconds. Current lint results contain zero errors and four warnings. These resource checks are separate from the earlier dashboard validation. The earlier 28 unit tests and 19 instrumented tests remain historical evidence; they were not rerun for this artwork change.

Reviewer `clock_art_review` approved static and rendered review with no material findings. Native icon, banner, and background exports were inspected at the dimensions above.

## Emulator validation

The GREEN connected `ArtworkDeviceTest` passed in 43 seconds. XML results contain one test with zero failures, errors, or skipped tests. A direct repeat passed one test in 2.028 seconds to export native images after the Gradle runner removed its export files.

## Physical-TV installation and presentation

The authorized installer ran with an explicit `TV_SERIAL` through `bash tools/install-clock.sh`. Release assembly and lint passed in eight seconds. Package installation returned `Success`, and `MainActivity` opened.

The physical TV reports version 1.6 (code 7). Its installed `base.apk` was read independently and matches the release artifact: SHA-256 `4c1e3ce853f73951e84a689e2580394d2f63abbad0b9955be723067756eb1198`, 102565 bytes.

After waking the TV from its dream screen, four ADB directional Right key events selected Clock in Family Home. The physical capture confirms the refreshed Clock banner and separate hero background are visible. Focus, profile controls, the app row, and the Clock overlay remain clear. An ADB Center key event then opened Clock. This is ADB navigation on the TV, not handheld remote validation.

Private native exports and TV captures remain outside Git in the thread visualization directory `clock-art-tv`. No physical instrumented tests ran. No preferences were changed manually. Launcher packages were retained, and no launcher configuration changed.

## Delivery

The tested app source was committed as `3820f536` and pushed to the task branch. `origin/main` advanced by fast-forward from `88bbfda4` to `3820f536`. The physical installed APK hash is recorded above. A final record-only commit updates this validation evidence; use Git history to identify that documentation commit.

## Evidence limits

The focused test establishes drawable decoding on the emulator. Native exports and the physical capture establish the inspected artwork and launcher presentation. These checks do not establish unrelated timer, playback, Cast, wake, or accessibility behavior. Keep device addresses, account details, and personal captures outside Git.
