# Clock dashboard validation, 2026-10-30

Local validation, emulator installation, independent review, and app delivery are complete.

## Revision and scope

- Base revision: `69bc5b0fe5a8fc904813f3ef61796694db3205fe`.
- Branch: `amine/clock-dashboard`.
- Checkout: `/Users/amine/Developer/worktrees/clock-dashboard/google-tv-apps`.
- Validation target: Android 14 Google TV emulator, `emulator-5582`.
- Tested and delivered app source commit: `c3274242`. A later evidence-only commit records this validation result; use Git history to identify it.

The change replaces the settings-style main page with the approved native Java dashboard. It also updates the appearance page, dialogs, Clock name, flat-ended clock hands, and launcher artwork. Existing timer calculations, preferences, and overlay behavior remain in scope for regression checks.

## Completed checks

The baseline debug unit suite passed 14 tests before implementation.

The first full local checkpoint passed:

```sh
env JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ANDROID_HOME=/opt/homebrew/share/android-commandlinetools ./gradlew :clock:testDebugUnitTest :clock:testReleaseUnitTest :clock:lintRelease :clock:assembleDebug :clock:assembleRelease :clock:assembleDebugAndroidTest
```

XML results show 14 debug and 14 release tests. Each variant contains `ClockLayoutTest` (3 tests) and `TimeLimitsTest` (11 tests).

The new focused D-pad test, `LimitsDeviceTest#dashboardRemoteNavigationRestoresFocusAfterDialogsAndAppearance`, passed one test on the emulator. Its initial RED run failed because the visible label was still `TV Clock` instead of `Clock`. Teardown also exposed the old hardcoded Home assumption. The test now resolves HOME for the device. Recovery through `ClockTestRecoveryTest#restoreInterruptedTestBackup` passed one test and cleared the backup.

After the final glyph-cap and bedtime text auto-size changes, the affected lint and debug, release, and Android test APK packaging checks passed in eight seconds. Two Python `test_app_installers` tests also passed.

Lint reports zero errors and nine warnings: `VectorRaster` (2), `GradleDependency` (2), `VectorPath` (3), `UnusedResources` (1), and `SetTextI18n` (1). Dependency upgrades are outside this change.

## Connected suite and installation

The full connected suite passed 19 tests in 1 minute 54 seconds. XML results contain `LimitsDeviceTest` (15 tests) and `ClockTestRecoveryTest` (4 tests), with zero failures, errors, or skipped tests.

```sh
env JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ANDROID_HOME=/opt/homebrew/share/android-commandlinetools ANDROID_SERIAL=emulator-5582 ./gradlew :clock:connectedDebugAndroidTest --console=plain
```

Release version 1.5 (code 6) installed successfully on the emulator. The installed release APK SHA-256 is `3b7ca7cb559d9e23d0a468707ea81441f5baec026bc002e01a64fb6e3b380a3b`.

Fresh native captures cover the ready dashboard, active timer, missing permissions, duration dialog, bedtime dialog, and appearance page. Private captures remain outside Git at `/Users/amine/.codex/visualizations/2026/10/30/01a18e5c-86e1-7920-bf78-be8cb8dce16a/clock-runtime`; the corrected appearance and bedtime filenames end in `-v2`.

The fresh independent reviewer `clock_final_review` passed compliance and code quality with no material findings. Optional state descriptions and permission-row spacing remain minor deferred observations. Launcher resource declarations and native vectors received static review; no rendered launcher capture was taken.

## Delivery

The app commit `c3274242` was pushed to `amine/clock-dashboard`. `origin/main` advanced by fast-forward from `69bc5b0f` to `c3274242`. The installed emulator `base.apk` was read independently; its SHA-256 matches the release artifact recorded above. A subsequent evidence-only commit updates this record and the completed plan.

## Evidence limits

The generated concept is a design reference, not a runtime capture. Emulator checks do not establish physical-TV audio, playback, Cast, wake, or accessibility behavior. No physical-TV installation is planned for this task.
