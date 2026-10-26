# First-version verification

Date: 2026-10-26. Branch: `amine/chess-math`.

The parent approved development and the forest-green/ivory direction. The parent later requested real TV testing instead of an emulator. The parent accepted the rare general dead-position limit for this first version. No claim of complete FIDE adjudication follows.

## Local evidence

The full Gradle release command passed for Home, Clock, and Schaak & Reken. It ran unit tests, Android lint, release builds, and new-app instrumentation compilation. Home retains its existing lint baseline. No new baseline or disabled lint check was added.

| Module | Release unit tests | Failures |
| --- | ---: | ---: |
| Home | 42 | 0 |
| Clock | 14 | 0 |
| Schaak & Reken | 40 | 0 |

Strict repository tests passed all eight cases. The earlier debug build and 38 debug unit tests also passed before two additional hint regressions. The final release suite includes those regressions. The APK contains no permission or service declaration. Its assets include the repository MIT license, chesslib and Commons Lang licenses, and the Commons Lang NOTICE. The build enforces both inspected library hashes.

The unit tests cover generated legal moves, special rules, initial perft 8902, draw claims and automatic thresholds, material boundaries, math configuration and hints, deterministic recovery, exact timer boundaries, promotion and owed computer replies, duplicate replies, process restore, current exercise preservation, storage failure, and pause during a save.

## Physical TV evidence

The connected device is a Chromecast (`sabrina`), Android SDK 34, 1920 × 1080, density 320. Only the new game and its test package were installed. The existing launcher, Home app, Clock app, and system configuration were not changed.

The initial instrumentation run passed board-edge focus and Activity recreation during a pending save. Two default button-focus tests failed because the harness called Activity.dispatchKeyEvent directly. A separate real key injection moved Start focus to App sluiten correctly. The corrected tests inject through the normal input system. The expanded suite passed all seven tests on the TV. It covers keypad edges and Hint return, Start navigation, a legal computer reply, parent hold cancellation and entry, settings Save/Cancel, all promotion choices and cancellation, board corners, and recreation during a pending write.

The final screenshot helper waits for frame submission and accessibility-event quiet time. UI-idle alone initially captured stale buffers. The resulting images were inspected before inclusion. The live hint frame was captured separately after the screen settled.

The final release APK itself also passed all seven TV tests after the hint fix. The retained hint frame comes from that final release run. Test duration was 17.347 seconds. Vendor RenderThread messages about a missing Mali module parameter appeared, but the runner reported `OK (7 tests)` and no application failure.

The live math phase stayed active at 315446 ms with zero completed exercises and its boundary pending. It did not return to chess. Manual Home and standby checks preserved paused elapsed time. Android reported `mWakefulness=Asleep` during standby. Force stop followed by explicit launch restored the same exercise, phase, input, and correct count, paused. See [machine-readable checks](evidence/device-checks.json).

Actual TV screenshots: [Start](evidence/start.png), [Chess](evidence/chess.png), [Selection](evidence/selection.png), [Math](evidence/math.png), [Hint](evidence/hint.png), [Settings](evidence/settings.png), and [Promotion](evidence/promotion.png). The visible top-right clock comes from the existing Clock overlay. Text outlines reflect the TV's existing accessibility configuration. Neither was changed for screenshots.

## Independent review

A separate GPT-6.1 Sol reviewer with medium reasoning reviewed the code and actual Start/Chess screenshots. The reviewer did not author the implementation. Findings led to fixes for focus-loss timing, pre-action phase boundaries, a shared writer across Activity recreation, opponent retry, transient board focus, immediate piece descriptions, and enforced dependency hashes.

The reviewer inspected all interaction frames and found no remaining visual blocker. Settings require scrolling to show Save/Cancel fully. The TV test confirms remote reachability. This is a minor departure from the single-screen concept. The review also identified the need to enforce library hashes and add transition tests. Both changes are included.

Live hint review exposed an unnecessary crossing-ten step for non-crossing sums. Two regression tests failed before the fix and passed afterward. Hints now infer carrying and borrowing from the units. For example, 32 + 24 splits 24 into 20 and 4; 74 − 12 splits 12 into 10 and 2.

Final independent review found no unresolved material code or visual finding within the accepted first-version scope. The reviewer independently inspected the hint fix and the final APK's size, hash, license assets, and byte-exact repository MIT text.

## Review APK

Path: `apps/chess-math/build/outputs/apk/release/chessMath-release.apk`.

Size: 425436 bytes. SHA-256: `d8a1850d2e682c590fb15c5d6d99f8bbccd3e593edaf0a3c8cb3acce8b4b0efe`.

## Limits

No emulator was started or used. Its download completed before the parent redirected testing. The unused emulator and test image were then uninstalled; the existing Android build tools were retained. The temporary TV test package and generated test session were removed after verification. The release app was left focused on Start.

No learning-effect, independent mastery, rating, full curriculum, or long-term child suitability claim is established. Automated injected keys do not establish the feel of the physical remote. Reboot behavior, prolonged family use, and every Android TV model remain outside this run. Wi-Fi stayed enabled; offline behavior is supported by local execution and the absence of network permissions, not a Wi-Fi-toggle test.
