# Clock Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the approved Clock dashboard with flat-ended hands and clear remote navigation.

**Architecture:** Retain the existing Java activities, native views, package, and timer services. Change presentation within the Clock app. One implementation owner edits the cohesive change; the coordinator owns builds, emulator input, integration, and Git delivery.

**Tech Stack:** Java, native Android views, existing Gradle build, vector drawables, Android instrumented tests.

**Spec:** `docs/superpowers/specs/2026-10-30-clock-dashboard-design.md`

## Global Constraints

- Keep `com.amine.tvclock`, timer logic, saved preferences, overlay behavior, existing view IDs, and the live-clock tag.
- Rename user-visible TV Clock text to Clock. Use flat clock-hand ends everywhere.
- Keep the existing Java app independent. Add no network access or dependencies.
- Use emulator `emulator-5582`; do not install on the physical TV.
- Keep private captures outside Git. The generated concept is design evidence only.

## Review Focus

- A running timer must retain its state while the dashboard and dialogs update.
- Missing permissions must leave their actions visible and reachable.
- Back from a dialog must preserve values when cancelled and restore useful focus.
- Return from Clock appearance must restore dashboard focus.
- Long date or status text must fit without hiding controls or focus outlines.

## Task 1: Implement the cohesive native dashboard

**Files:** Modify `apps/clock/src/main/java/com/amine/tvclock/{MainActivity,AnalogClockView,TvUi,SettingRow,ClockSettingsActivity}.java` as needed; app strings, styles, icon/banner drawables, and manifest. Create a separate launcher background drawable if practical. Update `README.md` and `apps/clock/README.md` for the visible name and current interface. Extend `apps/clock/src/androidTest/java/com/amine/tvclock/LimitsDeviceTest.java` for changed navigation where suitable.

**Interfaces:** Consume existing Clock preferences and limits APIs unchanged. Preserve public view IDs and the live-clock tag for existing tests and consumers.

- [x] Replace the hardcoded `com.gathin.tvhome` assumption in `LimitsDeviceTest` with the device-resolved HOME activity. Preserve assertions about returning Home. The emulator uses the stock Google TV launcher.
- [x] Extend focused instrumented coverage for the Clock name, actual D-pad navigation into timer and bedtime dialogs, cancelled edits, and focus after activity return. Avoid forcing focus onto the target control.
- [x] Implement the approved left clock card, right timer/bedtime cards, and bottom actions. Keep active, inactive, and missing-permission states usable.
- [x] Apply matching appearance-page and dialog styling. Draw analog hands with flat caps. Refresh native launcher artwork and add independent text-free background metadata if practical.
- [x] Update affected documentation and visible labels. Check long text and active timer presentation during emulator review.

## Coordinator verification and delivery

- [x] Run the focused navigation tests before the full checkpoint. Use an explicit serial for every ADB action, select one class per focused invocation, and inspect XML class names and counts.
- [x] Run the app checkpoint from the checkout root:

```sh
env JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ANDROID_HOME=/opt/homebrew/share/android-commandlinetools ./gradlew :clock:testDebugUnitTest :clock:testReleaseUnitTest :clock:lintRelease :clock:assembleDebug :clock:assembleRelease :clock:assembleDebugAndroidTest
```

- [x] Run the relevant existing instrumented regression suite on `emulator-5582`. Record the tested revision, exact commands, test counts, and results.
- [x] Install the validated build on the emulator. Capture fresh dashboard, appearance, and dialog images. Check initial focus, actual directional navigation, permission actions, cancelled edits, and focus after return.
- [x] Have one fresh reviewer inspect the final diff and native captures. Resolve material findings and repeat only affected checks.
- [x] Fetch `origin` again, integrate concurrent changes if needed, and repeat affected checks. Commit only this app and its related documentation. Push the task branch and safely merge to `origin/main`; confirm the resulting remote SHA. Report installation and merge separately.
