# Adult profile validation

Family Home 1.11 (version code 12), 2026-10-29. Baseline: `894861472be34be5e34c571909fdd598c958b965`. Tested source commit: `84eacf2c87f96093f961cfc42e1d422bd0db6674`.

The change adds the fixed adult Hasnaa, shared parent controls and PIN, Settings menu icons, and adult Home Back behavior. Schema 5 preserves existing data, including a child already named Hasnaa and a core state at the 65,536-character limit.

## Results

- Before implementation, all four `AdultProfileTest` regressions failed on the missing adult behavior. The actual Activity Back regression also failed because Back opened the chooser.
- Final unit checks passed: 94 debug tests and 94 release tests. Final release lint and packaging passed. Lint reported 0 errors and 6 warnings.
- The complete emulator run executed 68 tests: 67 passed. One `VoiceRuntimeTest` failed its HOME-role precondition. After assigning Family Home as HOME on the emulator, both tests in that class passed. This was a focused rerun, not a second clean complete-suite run.
- The additional real-key Settings-return test exposed a visual failure: semantics reported focus, but the profile control had zero gold border pixels. After the fix, that test passed. It checks both adults, the visible border after Settings Back, and D-pad navigation to apps and back.
- After the visual fix, all 29 affected navigation tests passed: `FamilyRemoteTest` 18, `ProfileMenuTest` 4, and `HomePresentationTest` 7. Each class ran separately with the explicit emulator serial. XML class names and counts were inspected.
- A fresh independent reviewer found no remaining findings after inspecting the final gold-border capture.
- Distinct emulator coverage across these runs is 69 tests. This count does not describe a single clean complete-suite run.

The visual fix draws the profile border and background from `onFocusChanged`, following the chooser's existing native pattern. All temporary diagnostic logging was removed. Native Surface interaction, focus request timing, and root Back routing retain their intended behavior.

## Commands

From the repository root, with the documented JDK and SDK environment:

```sh
apps/family-home/gradlew -p apps/family-home testDebugUnitTest testReleaseUnitTest lintRelease assembleRelease --console=plain
ANDROID_SERIAL=emulator-5554 apps/family-home/gradlew -p apps/family-home connectedDebugAndroidTest --console=plain
ANDROID_SERIAL=emulator-5554 apps/family-home/gradlew -p apps/family-home connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.familyhome.VoiceRuntimeTest' --console=plain
ANDROID_SERIAL=emulator-5554 apps/family-home/gradlew -p apps/family-home connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.familyhome.FamilyRemoteTest#settingsBackRestoresEachAdultProfileControlWithRealRemoteKeys' --console=plain
for test_class in FamilyRemoteTest ProfileMenuTest HomePresentationTest; do
  ANDROID_SERIAL=emulator-5554 apps/family-home/gradlew -p apps/family-home connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.amine.familyhome.$test_class" --console=plain
done
```

The initial unit regression command used `testDebugUnitTest --tests com.amine.familyhome.AdultProfileTest`. The initial Activity Back command selected `com.amine.familyhome.FamilyRemoteTest#activityBackKeepsEachAdultOnTheirHome` through the same instrumentation class argument.

## Installed artifact and evidence limits

Release 1.11 (12) was installed on `emulator-5554`. The HOME role and helper were restored. The release APK is 2,781,847 bytes. Its SHA-256 is `2af9987aa527cf0ebc286fd510c1de181681bc1474706d6a500fb09076e04ef1`.

Fresh screenshots remain local in `/tmp/family-home-adult-profile-delivery-20261029-2205/`: `chooser.png`, `menu.png`, `settings-return.png`, and `home-back.png`. They are excluded from Git. The physical TV was not touched. Emulator interface and key-routing results do not establish physical-TV playback, Cast, wake, accessibility-helper reliability, audio focus, pronunciation, or audible quality. Git delivery is reported separately.
