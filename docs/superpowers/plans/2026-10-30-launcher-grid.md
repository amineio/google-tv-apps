# Family Home App Grid Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the horizontal app row with a five-column grid and fix the small, compressed Home profile picture.

**Architecture:** Keep Home presentation in native Compose for TV. Preserve the flat saved app order and catalog filtering. Use explicit grid navigation and scroll-before-focus behavior for distant tiles.

**Tech Stack:** Kotlin, Compose for TV, existing independent Android Gradle build, Android 14 or later.

**Spec:** [Approved design](../specs/2026-10-30-launcher-grid-design.md).

## Global Constraints

- Use five equal landscape cells per row and show two complete rows with labels below.
- Scroll vertically and preserve the selected app background and gold focus outline.
- Use a 64 dp Home header and a 48 dp circular profile picture with equal width and height.
- Preserve the right-side profile control, saved app order, child filtering, and all existing persisted data.
- Keep other screens at their existing header size.
- Use native controls and retain existing dependencies, permissions, Home routing, and accessibility behavior.
- Use generic names in new fixtures and captures. Keep family captures and device identifiers out of Git.
- The coordinator alone operates builds, emulator input, integration, and Git commands.
- Do not use the physical TV for this task.

## Review Focus

- A destination outside the composed viewport receives focus after scrolling.
- A short final row selects the nearest available column and does not trap focus.
- Reordering across rows preserves focus on the moved package and supports save and cancel.
- Resume and catalog removal restore a valid app without an invalid focus request.
- The profile picture remains square while the timer changes and the menu opens or closes.

## Execution status

The user authorized implementation after approving the concept.
The coordinator accepted this plan and selected delegated implementation under the repository instructions.
The baseline passed all 94 debug unit tests before implementation.
The coordinator operated the new Home presentation tests before product changes.
Eight tests ran, with three expected failures and no errors.
Failures covered grid geometry, focus restoration outside the visible area, and the 30 dp portrait instead of 48 dp.
Product implementation and focused tests are complete.
The Home class passed eight tests. The app order class passed six tests.
Both classes reported no failures or errors.
A catalog replacement test passed both its initial and stronger queued-navigation triggers on unchanged product code.
Native captures passed coordinator and independent visual review.
The independent final reviewer accepted specification compliance and code quality with no material findings.
The full integration checkpoint, installation, and Git delivery remain pending.
Builds, device operations, and final evidence remain with the coordinator.

---

### Task 1: Implement the grid and Home portrait correction

**Files:**
- Modify: `apps/family-home/src/main/kotlin/com/amine/familyhome/HomeScreen.kt`.
- Modify: `apps/family-home/src/main/kotlin/com/amine/familyhome/FamilyTheme.kt`.
- Optional helper: `apps/family-home/src/main/kotlin/com/amine/familyhome/AppGridNavigation.kt`.
- Optional unit test: `apps/family-home/src/test/kotlin/com/amine/familyhome/AppGridNavigationTest.kt`.
- Modify tests: `HomePresentationTest.kt` and `AppOrderPresentationTest.kt` in `apps/family-home/src/androidTest/kotlin/com/amine/familyhome/`.
- Audit tests: `ProfileMenuTest.kt` and `FamilyRemoteTest.kt` in the same directory.

**Interfaces:**
- Consumes: existing `HomeScreen` arguments, `LauncherApp`, `ProfilePortrait`, and `onSaveOrder: suspend (List<String>) -> Boolean`.
- Produces: the same Home screen contract, existing test tags, and an optional `FamilyHeader` height parameter with a 38 dp default.
- Keep grid navigation internal. Do not change controller or persistence interfaces.

- [x] **Step 1: Update directional presentation tests before product code.**

Test five-column geometry, two visible rows, Left/Right boundaries, vertical movement, and the nearest available tile in a short final row.
Test navigation to a row beyond the viewport with actual arrow input.
Test profile entry from the first row and return to the selected app.
Test resume to a distant tile, catalog removal, empty catalogs, and profile dialogs.
Test hold-OK movement in four directions, one save per OK, and Back cancellation with package focus retained.
Assert that the Home portrait measures 48 dp in both dimensions and keeps its bounds when the countdown changes.
Capture generic Home fixtures with enough apps for multiple rows.

- [x] **Step 2: Ask the coordinator to operate the affected test classes and record the expected failures.**

Use one class per invocation with explicit `ANDROID_SERIAL=emulator-5554`.
The old row layout must fail the new grid geometry or navigation assertions before implementation.

- [x] **Step 3: Replace the row and fix Home header constraints.**

Use five equal landscape cells, labels below, vertical scrolling, and a compact heading.
Preserve artwork proportions, backgrounds, focus outlines, empty messages, and existing profile menu behavior.
Move vertically by the column count and horizontally within each row.
Resolve short final rows to the nearest available column.
Scroll before requesting focus on a tile outside the viewport.
Use the same geometry during move mode, retaining save and cancel semantics.
Give Home a 64 dp header and a strictly square 48 dp portrait without changing other header callers.

- [x] **Step 4: Ask the coordinator to operate focused tests and inspect fresh captures.**

```sh
ANDROID_SERIAL=emulator-5554 apps/family-home/gradlew -p apps/family-home connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.familyhome.HomePresentationTest' --console=plain
ANDROID_SERIAL=emulator-5554 apps/family-home/gradlew -p apps/family-home connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.familyhome.AppOrderPresentationTest' --console=plain
```

Expected: all selected tests pass, XML lists the intended classes, and captures show two readable rows and a circular portrait.
The coordinator also operates affected profile menu and remote tests separately.

- [ ] **Step 5: Complete the integration evidence and independent review.**

```sh
apps/family-home/gradlew -p apps/family-home testDebugUnitTest testReleaseUnitTest lintRelease assembleRelease assembleDebugAndroidTest --console=plain
ANDROID_SERIAL=emulator-5554 apps/family-home/gradlew -p apps/family-home connectedDebugAndroidTest --console=plain
git diff --check
```

Expected: passing unit and emulator suites, no lint errors, a release APK, and a clean whitespace report.
Record XML counts, tested revision, commands, capture paths, and emulator limits in `docs/family-home/grid-validation.md`.
Resolve material findings from a fresh independent reviewer before delivery.

- [ ] **Step 6: Let the coordinator complete installation and Git delivery.**

The documentation owner updates the app guide and version to `1.12`, code `13`.
After emulator installation, the coordinator commits only task changes, fetches `origin`, and integrates concurrent changes if needed.
The coordinator pushes the task branch and fast-forwards `origin/main` from the tested branch.
Report emulator installation and Git delivery separately.
