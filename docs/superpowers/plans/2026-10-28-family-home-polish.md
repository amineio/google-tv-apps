# Family Home polish Implementation Plan

> For agentic workers: Use superpowers:subagent-driven-development for the implementation task.

Goal: Simplify duration selection and the launcher while preserving family controls.

Architecture: Change existing presentation components only. Keep policy and timer logic separate.

Tech Stack: Kotlin, Compose for TV, existing independent Gradle build.

Spec: docs/superpowers/specs/2026-10-28-family-home-polish-design.md

## Global Constraints

Keep the profile chooser unchanged. Preserve the 20-minute default, five-minute steps, and 5–60 minute range.
Preserve the session, access, PIN, helper, and Cast behavior. Add no dependency.
Use one coordinator for builds, Git operations, and device input.
Use emulator-5680 on the isolated ADB server port 5041 for Android tests. Never run instrumented tests on the physical TV.
Keep family screenshots and device data outside Git. Do not enable cloud builds.

## Review Focus

Make sure that focus moves from Start session to both duration controls and returns without duplicate highlights.
Make sure that countdown changes keep the profile and timer bounds fixed.
Make sure that app return and catalog removal restore focus to an available tile.
Make sure that long labels and mixed banner/icon tiles remain readable without clipping.
Make sure that empty catalogs leave both footer actions reachable.

### Task 1: Native presentation and navigation

Files: FamilyTheme.kt, MainActivity.kt, HomeScreen.kt, LauncherCatalog.kt under apps/family-home/src/main/kotlin/com/amine/familyhome/.
Tests: FamilyRemoteTest.kt and HomePresentationTest.kt under apps/family-home/src/androidTest/kotlin/com/amine/familyhome/.

Interfaces: Preserve HomeScreen parameters and existing controller APIs. DurationScreen can become internal for focused presentation tests.

- [x] Update meaningful presentation regressions for initial focus, duration navigation, direct tile launch, fixed countdown geometry, and focus restoration.
- [x] The coordinator operates the failing emulator regressions before implementation.
- [x] Implement the duration and launcher presentation from the spec. Keep changes bounded to presentation.
- [x] Update the version to 1.2, versionCode 3, and revise the app README for final behavior.
- [x] The coordinator operates both unit variants, lintRelease, assembleRelease, assembleDebug, and assembleDebugAndroidTest.
- [x] The coordinator installs both test APKs and operates the Android instrumentation runner on emulator-5680 through ADB port 5041.
- [x] Resolve actionable findings from independent code and UI/UX reviews.
- [x] Commit only related app and design records. The coordinator owns the commit and delivery.

## Delivery

After emulator acceptance, install the release APK on the authorized Chromecast with an explicit serial.
Preserve device data and capture local recovery evidence. Make sure that the app opens.
Push the branch and merge the PR to main. Make sure that origin/main contains the installed change and the PR is closed.


### Task 2: Fit fixed-size banner layers

The TV capture exposes a fixed-size layer inside the Chess banner drawable.
Changing outer bounds does not scale that layer. Keep the correction inside Family Home.

Files: LauncherCatalog.kt and HomePresentationTest.kt under apps/family-home/src/.

- [x] Add an Android regression with a fixed-size colored layer rendered into a larger bitmap.
- [x] Make sure that the current renderer fails the corner coverage assertion.
- [x] Scale the canvas from intrinsic drawable bounds and preserve aspect ratio.
- [x] Operate the full local build and emulator suite. Repeat independent code and rendered artwork review.
- [x] Install the corrected release. Merge the small follow-up PR and make sure that it closes.
