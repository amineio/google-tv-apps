# Profile flow and app order validation

Family Home 1.8, version code 9, adds direct profile switching, resume badges, and saved app order for each profile.

## Local evidence

The debug and release variants each passed 73 unit tests. Release lint reported zero errors and six warnings. Release, debug, and Android test APK builds passed. Four installer tests passed.

The initial chooser regression run failed all four new presentation tests against the previous interface. App-order core tests first failed because the state and save API were absent.

The first integrated emulator run completed 52 tests with two initial-focus failures. A temporary diagnostic probe showed that the initial request returned false before the LazyRow attached its first child. The chooser now waits for first-card placement and window focus, then records a successful initial request once. A regression test covers dialog return after the parent item leaves the scroll composition.

A real held OK event entered move mode in the running emulator app without opening the app. Right moved the selected app. Short OK saved the order. A process restart retained the new order in the selected profile.

The fresh source review reports zero remaining actionable findings. The UI reviewer found no material issues in the chooser and move-mode captures. Captures remain outside Git.

A later badge-boundary assertion compared scaled child coordinates with unscaled Surface layout bounds. The corrected test uses the visual card extent and checks all four edges against the viewport. The UI reviewer accepted final focused and scrolled chooser captures.

The final full emulator run passed all 53 Android tests. The suite includes real held-OK and directional input, save and cancel, boundary scrolling, direct switching, resume badges, and dialog return after scrolling.

Physical installation results follow after execution.
