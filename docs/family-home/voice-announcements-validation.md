# Family Home voice announcements validation

Current combined release: offline Android timer speech and the merged Home key correction, version 1.10 / code 11.

## Combined release integration

Merge commit `85d0091d554474eb255d8fa17e9804593102e249` includes current main through `d13f6b78`.
Concurrent main commit `95c590d1` corrected Home key handling and also used version 1.9 / code 10.
The combined release uses version 1.10 / code 11 to avoid that version collision.
The merge preserves both the Home key correction and the speech lifecycle code.
Only release configuration and documentation changed after this merge.

Tested combined source commit: `1c5d3b3ea9a3602c28d18bc9d379030ebdb72d1a`.
Debug and release unit suites each passed 90 tests in 14 suites, with zero failures, errors, or skips.
Release lint and release assembly passed.

Affected emulator checks passed, with one class selected per invocation and exact XML names and counts inspected:

| Class | Passed | Failures / errors / skips |
| --- | ---: | --- |
| FamilyHomeKeyTest | 9 | 0 / 0 / 0 |
| VoiceRuntimeTest | 2 | 0 / 0 / 0 |
| VoiceAnnouncementsPresentationTest | 1 | 0 / 0 / 0 |

The earlier 57 distinct voice-integration cases below remain historical evidence. This affected run adds the nine merged Home key cases and reruns three existing voice cases; those reruns are not counted as new cases.
The unchanged 55-case presentation suite was not repeated for this bounded integration.
The coordinator used these checks:

```sh
apps/family-home/gradlew -p apps/family-home testDebugUnitTest testReleaseUnitTest lintRelease assembleRelease --console=plain
ANDROID_SERIAL="$ANDROID_SERIAL" apps/family-home/gradlew -p apps/family-home connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.amine.familyhome.FamilyHomeKeyTest --console=plain
ANDROID_SERIAL="$ANDROID_SERIAL" apps/family-home/gradlew -p apps/family-home connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.amine.familyhome.VoiceRuntimeTest --console=plain
ANDROID_SERIAL="$ANDROID_SERIAL" apps/family-home/gradlew -p apps/family-home connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.amine.familyhome.VoiceAnnouncementsPresentationTest --console=plain
adb -s "$ANDROID_SERIAL" install -r apps/family-home/build/outputs/apk/release/family-home-release.apk
```

Final release installation succeeded on the dedicated API 34 emulator: version 1.10 / code 11, min SDK 34, target SDK 37.
The APK is 2,743,974 bytes. The built release and installed `base.apk` have the same SHA-256:
`917e497a2fa208139c1b1febd91432dd643b2d96dd15b774c1a56ab407dab2ae`.

The coordinator used actual directional and OK input to inspect the final native settings and voice controls.
All three fresh 1.10 captures were readable, with no clipping. Voice was restored to enabled after capture.
These private files remain in `/tmp`, outside Git:

- `family-home-voice-1-10-settings-20261029.png`
- `family-home-voice-1-10-enabled-20261029.png`
- `family-home-voice-1-10-muted-20261029.png`

Independent review approved combined source `1c5d3b3e`, its affected checks, and all three fresh 1.10 captures, with no material findings.
The reviewer confirmed readable text, no clipping, and clear toggle states.
Build, affected tests, installation, installed artifact identity, and independent review are complete.

## Historical voice release: 1.9 / code 10

The following evidence applies to the speech source before the concurrent Home key merge.
Tested source commit: `eca75b8ed91e7b65675af3b36ca116527670d6b0`.
Implementation base: `0c9ea812d984d9f8741ae93edf00b40f0aac3622`.
Final integration passed. The release installed successfully on the dedicated emulator.
Installed artifact identity and fresh native captures passed final independent review.

### Historical build and unit evidence

The baseline passed 73 unit tests in 12 suites.
The first RED run failed to compile because the new voice APIs did not exist; no tests executed.
After API skeletons, the assertion RED run executed 16 tests, with 14 expected failures and zero XML errors.
VoiceAnnouncementsTest executed 11 tests with nine failures. SpeechPlaybackTest executed five tests with five failures.
The subsequent focused GREEN run passed 34 tests: voice 11, playback 5, controller 9, and codec 9.
Android test assembly also passed.

The first integration checkpoint passed 89 debug and 89 release unit tests, with 14 suites per variant and no failures or errors.
Release lint and debug, release, and Android-test assembly passed. This checkpoint predates the foreground-start correction.
After the foreground-start correction, final integration passed 90 debug and 90 release unit tests.
Each variant had 14 suites, with zero failures, errors, or skipped tests. Release lint and release assembly passed.
The final presentation run passed all 55 tests across 10 classes, with no failures, errors, or skipped tests.
The separate runtime run passed two tests, giving 57 distinct instrumented cases.
The coordinator inspected the XML class names and test counts.

The release APK is 2,743,974 bytes. Its SHA-256 is `9dad3802ca8c96e14e2e8c34124a9183a634775c045a4bd436d71ab4e9c633a4`.
The coordinator used these local checks:

```sh
apps/family-home/gradlew -p apps/family-home testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease assembleDebugAndroidTest --console=plain
```

Tests cover durable write-before-request ordering, failed storage, helper loss during a suspended write, parent-only saved mute, recovery, warning overlap, expiry, and stale session cancellation.
A later regression covers standby cancellation while an earlier checkpoint write holds the controller mutex.
The output tests cover delayed initialization, invalid requests, focus denial/loss, initialization/synthesis failure, and cleanup once.
Native settings tests use actual directional input and verify saved mute, scrolling, Back, recreation, and setup-return focus.

### Historical runtime evidence

A dedicated Google TV API 34 emulator completed `VoiceRuntimeTest`: two tests, zero failures and errors; XML class and test names were inspected.
The installed engine was Google Text-to-speech, with device locale `en-US` and 421 enumerated voices.
The successful run selected an eligible installed offline voice and generated a 113,564-byte WAV from a generic test phrase.
The test deleted that temporary file. An earlier probe reported no eligible offline voice; it did not claim synthesis success.

The helper-backed test confirmed the actual bound accessibility helper and the Home role.
Android Settings served as a permitted external app while the real controller resumed a generic child session.
The helper dispatched the real foreground service. After service cleanup, speech status was `READY`.
A subsequent cancellation observed an active service, invalidated the request, and confirmed service shutdown without a retained notification.
The fixture restored its local family state and accessibility configuration.
These observations establish the API 34 route and resource cleanup under the test conditions.
They do not establish audible output or behavior in a streaming app.

The runtime class runs separately from the presentation suite, with the helper and Home role configured:

```sh
ANDROID_SERIAL="$ANDROID_SERIAL" apps/family-home/gradlew -p apps/family-home connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.amine.familyhome.VoiceRuntimeTest --console=plain
```

The presentation suite ran separately with one explicit exclusion for the runtime class:

```sh
ANDROID_SERIAL="$ANDROID_SERIAL" apps/family-home/gradlew -p apps/family-home connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.notClass=com.amine.familyhome.VoiceRuntimeTest --console=plain
adb -s "$ANDROID_SERIAL" install -r apps/family-home/build/outputs/apk/release/family-home-release.apk
```

The final source commit's release installation succeeded with version 1.9 / code 10, min SDK 34, and target SDK 37.
The installed `base.apk` SHA-256 matches the built release: `9dad3802ca8c96e14e2e8c34124a9183a634775c045a4bd436d71ab4e9c633a4`.

The coordinator used actual ADB directional and OK input to open the five-row settings screen and the native Timer voice page.
Fresh release captures show the enabled control and setup action, then the saved Muted state.
Voice announcements were restored to enabled after capture.
The captures remain local in `/tmp`, outside Git:

- `family-home-voice-release-settings-stable-20261029.png`
- `family-home-voice-release-enabled-20261029.png`
- `family-home-voice-release-muted-20261029.png`

Independent visual review confirmed readable controls and text, with no clipping, in all three captures.
The combined independent specification and code-quality review approved source `eca75b8e` with no open material findings.
The foreground-start defect was addressed and retained as a targeted regression.
No screenshot is bundled in the repository.

### Corrections and incomplete attempts

The first presentation attempt was interrupted at 41 of 55 tests by a concurrent package installation force-stop.
That attempt does not count as completed coverage.
A later isolated attempt reached 50 of 55 tests and exposed a product defect: a stale foreground start stopped before promotion and triggered Android's foreground-service watchdog.
The correction promotes every delivered start before stale rejection and leaves queued cancellation to the service owner.
It also preserves immediate standby cancellation while a controller write is suspended.
The stale-start regression retains a watchdog observation window.

Runtime test setup initially failed before any speech request.
Live system inspection found literal quote characters in the test's accessibility component setting.
The corrected fixture validates and passes component lists directly to UiAutomation, preserves other helpers, and restores settings.
The subsequent two-test runtime run passed. This fixture failure was separate from production speech behavior.

## Evidence limits

Only Android 14 / API 34 runtime coverage is available.
Android 15 and Android 17 foreground-service and audio-focus behavior has source review, without runtime verification.
No physical TV test, cloud build, APK upload, or public release was performed.

Generated file bytes, readiness status, and callback completion do not prove heard speech, pronunciation, or audible quality.
The external Settings test does not establish streaming-app ducking, playback, Cast, wake, or physical-TV accessibility reliability.
The app keeps visual warnings and expiry behavior when voice setup, foreground-service start, or audio focus is unavailable.
