# Family Home grid validation

## Scope

Version 1.12, code 13, replaces the horizontal app row with a five-column grid.
It also gives the Home profile picture equal 48 dp dimensions in a 64 dp header.
The approved design is in [the design record](../superpowers/specs/2026-10-30-launcher-grid-design.md).

## Design reference

The preview-only ImageGen concept shows two rows of five landscape cells, a compact heading, and a larger circular profile picture.
It uses generic app labels and a generic profile.
The built-in ImageGen tool created a 1672 by 941 pixel PNG without external source images.
The prompt specified a dark charcoal background, gold focus outline, equal landscape cells, labels below, and vertical scrolling.
The concept is a design reference, not runtime evidence or a bundled app asset.
No new image or font license is required because the release reuses existing resources.

## Baseline

The coordinator reported 94 passing debug unit tests before implementation.
The task started from `origin/main` at `14bfc9578f69b3c26e1f4bded992feb41603e5d2`.

## Implementation evidence

### Tests before product changes

The coordinator operated the updated `HomePresentationTest` against the existing product implementation on `emulator-5554`.
The class contained eight tests. Three tests failed as expected, and no test errors occurred.
The failures covered grid geometry, directional focus restoration outside the visible area, and portrait dimensions.
The portrait measured 30 dp instead of the required 48 dp.

From the repository root, the coordinator used the JDK 17 and Android SDK paths from the app guide:

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
ANDROID_SERIAL=emulator-5554 apps/family-home/gradlew -p apps/family-home connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.familyhome.HomePresentationTest' --console=plain
```

The coordinator retained the XML reports under `/tmp/launcher-grid-evidence/red` outside Git.

### Focused tests after implementation

`HomePresentationTest` passed eight tests with no failures or errors.
`AppOrderPresentationTest` passed six tests with no failures or errors.
The coordinator used one class per invocation on `emulator-5554` with the JDK and SDK paths above:

```sh
ANDROID_SERIAL=emulator-5554 apps/family-home/gradlew -p apps/family-home connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.familyhome.HomePresentationTest' --console=plain
ANDROID_SERIAL=emulator-5554 apps/family-home/gradlew -p apps/family-home connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.familyhome.AppOrderPresentationTest' --console=plain
```

The test `catalogReplacementDuringQueuedNavigationKeepsAValidFocusedApp` covers catalog replacement during queued navigation.
Its initial trigger passed one test with no failures or errors.
A stronger trigger queued Down from index 5 to index 10, then replaced the catalog before focus moved.
That trigger also passed one test with no failures or errors on unchanged product code.
The tests did not reproduce the suspected race. No product correction was made for that suspicion.

### Native captures and review

The coordinator and a fresh reviewer inspected these emulator captures outside Git:

- `/tmp/launcher-grid-evidence/captures/home-grid-initial.png`
- `/tmp/launcher-grid-evidence/captures/home-grid-scrolled.png`
- `/tmp/launcher-grid-evidence/captures/home-grid-portrait.png`

The captures use synthetic app labels.
They show five columns, two complete rows, and part of the next row.
The circular portrait, timer spacing, gold focus outline, and Clock overlay clearance passed visual review.
The independent final reviewer accepted specification compliance and code quality with no material findings.

### Integration build

The tested implementation revision is `7334cc3fdef77a463a4d2673c1b5752c4f92049d`.
With the JDK 17 and Android SDK paths above, the coordinator operated:

```sh
apps/family-home/gradlew -p apps/family-home testDebugUnitTest testReleaseUnitTest lintRelease assembleRelease assembleDebugAndroidTest --console=plain
```

All 94 debug unit tests and all 94 release unit tests passed with no failures.
Release lint reported no errors and six warnings outside the changed behavior.
The warnings comprise two `AndroidGradlePluginVersion`, one `DiscouragedApi`, one `DataExtractionRules`, and two `UseKtx` findings.
The release APK is 2,814,615 bytes.
Its SHA-256 is `45c1e03aeb8b9a5fe158b4426cd0139b29bb5e152a96d82ba9168f5c58960f36`.

### Full emulator suite and focused corrections

The full emulator suite operated 72 tests against the implementation revision.
It reported eight failures and no errors.
Seven `FamilyRemoteTest` failures came from ambiguous Settings selectors.
The new visible Settings app label and the profile menu action matched the same text selector.
The implementation owner limited three Settings selectors to the profile menu dialog.
A fresh reviewer accepted this test correction.
The focused `FamilyRemoteTest` rerun passed all 18 tests with no failures or errors.
The coordinator retained its XML under `/tmp/launcher-grid-evidence/remote-final` outside Git.

One `VoiceRuntimeTest` failure came from a missing Home role precondition after Gradle uninstalled the tested app.
The other 52 tests outside `FamilyRemoteTest` and `VoiceRuntimeTest` passed in the full suite.
These failures did not establish a product defect. The product implementation revision remains unchanged.

The coordinator installed the debug and test APKs directly and assigned the emulator Home role to Family Home.
The coordinator restored the role and operated the direct voice tests with these commands:

```sh
adb -s emulator-5554 shell cmd package set-home-activity --user 0 com.amine.familyhome/.MainActivity
adb -s emulator-5554 shell am instrument -w -r -e class com.amine.familyhome.VoiceRuntimeTest com.amine.familyhome.test/androidx.test.runner.AndroidJUnitRunner
```

The direct invocation returned `OK (2 tests)`.
The coordinator retained its log at `/tmp/launcher-grid-voice-final.log` outside Git.
All 72 unique emulator tests have passing outcomes across the full run and focused reruns.
These comprise 52 unaffected tests, 18 Family remote tests, and two voice runtime tests.
No single full-suite invocation passed all tests.
No product code changed after `7334cc3fdef77a463a4d2673c1b5752c4f92049d`.

## Delivery

### Emulator installation

The coordinator installed release version 1.12, code 13, on `emulator-5554`.
Cold launch returned `Status: ok` in 367 ms.
The foreground activity was `com.amine.familyhome/.MainActivity`.
The installed APK matched the release build at 2,814,615 bytes and SHA-256 `45c1e03aeb8b9a5fe158b4426cd0139b29bb5e152a96d82ba9168f5c58960f36`.

### Git delivery

The coordinator owns the final documentation commit, task branch push, and integration into `origin/main`.
The final task response reports the Git delivery result separately from installation evidence.

## Limits

This task uses the Google TV emulator for presentation and directional navigation.
Physical TV installation and testing are outside this task.
Emulator evidence does not establish physical remote behavior, Cast, streaming playback, wake behavior, audio focus, or audible quality.

## Physical TV follow-up: 2026-10-30

The earlier emulator-only delivery record above describes the original task boundary.
The user then requested TV installation and a standing requirement to install app changes on the TV before they reach `main`.
The active repository instructions now require physical installation of the tested release APK before merging app changes or pushing them to `origin/main`.

### Installation and artifact match

The coordinator installed Family Home through `tools/install-family-home.sh` with the JDK 17 and Android SDK paths recorded above.
The installer build passed all 94 release unit tests, release lint with no errors, and release packaging.
The cached build completed in one second.
The source revision was `f4166576279108d53dea8d2a4797a3b7d4fe2673`.
Its product code matches the previously tested implementation at `7334cc3fdef77a463a4d2673c1b5752c4f92049d`.

The physical Chromecast upgraded from version 1.11, code 12, to version 1.12, code 13.
The pulled installed APK matched the tested release at 2,814,615 bytes.
Its SHA-256 was `45c1e03aeb8b9a5fe158b4426cd0139b29bb5e152a96d82ba9168f5c58960f36`.
Family Home held the Home role, launch returned `Status: ok`, and its MainActivity was in the foreground.
Google TV Home and setupwraith remained enabled.
The installer backup remains private and ignored by Git.

### Observed presentation

The initial profile chooser retained a resumable child balance.
The coordinator selected an adult profile with ADB input and observed the Home screen.
The TV showed five columns, two complete rows, a larger circular profile picture, and clear space around the Clock overlay.
The fresh TV capture remains private outside Git.

These observations establish installation, TV presentation, and the observed ADB navigation.
The coordinator did not test the physical remote handset, Cast, wake behavior, audio, or streaming playback during this follow-up.
The final task response reports the policy commit and Git integration result separately.
