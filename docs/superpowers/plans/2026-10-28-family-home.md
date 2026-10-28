# Family Home Implementation Plan

> For agentic workers: use superpowers:executing-plans. Track progress in the plan workspace.

Goal: Deliver the first Family Home release with profiles, child timers, app access rules, and an optional PIN.

Architecture: A pure Kotlin session engine feeds one serialized Android controller.
DataStore saves state before the controller publishes it. Compose renders that state.
An accessibility helper handles foreground packages, Home routing, screen events, and timer deadlines.

Tech Stack: Kotlin, Compose for TV, Android 14+, DataStore, coroutines, JUnit, and Android instrumentation.

Spec: `docs/superpowers/specs/2026-10-28-family-home-design.md`

## Global Constraints

- Create a separate app at `apps/family-home`, package `com.amine.familyhome`.
- Keep all existing Java app source and builds unchanged.
- Start with Amine, Elias, and Sofia. The parent PIN starts off.
- Set the default child duration to 20 minutes, in five-minute steps from 5 through 60 minutes.
- Count all screen-on time. Pause on standby and profile changes. Resume saved balances.
- Keep Google TV Home and setupwraith enabled. Read window package and class metadata only.
- Deliver installed source to `origin/main` after successful installation.

## Review Focus

- Process restart restores a paused balance rather than granting a new session.
- Wake from another app opens the chooser before child playback continues.
- A failed state save cannot enable app access or lose the previous timer.
- Transient system dialogs do not create redirect loops or permit unrestricted Settings.
- An old deadline callback cannot interrupt a later profile or session.

### Task 1: Session engine and independent Kotlin build

Files: create the standalone Gradle build, `FamilyState.kt`, `SessionEngine.kt`, and `SessionEngineTest.kt`.

Interfaces: immutable `FamilyState`, `Viewer`, `ChildSession`; pure engine actions accept explicit elapsed milliseconds.
The engine produces snapshots, remaining balances, and eligibility for app access.

- [ ] Write duration, profile switch, standby, expiry, recovery, and stale-deadline tests first.
- [ ] Operate the test task and observe the missing behavior.
- [ ] Implement the minimal engine and state records. Save only bounded, valid session values.
- [ ] Operate `./gradlew testDebugUnitTest` in `apps/family-home`. Expect all engine tests to pass.
- [ ] Commit the build and engine.

### Task 2: App access and optional PIN

Files: create `AccessPolicy.kt`, `ParentPin.kt`, and their unit tests.

Interfaces: package policy consumes `FamilyState`, package/class metadata, and elapsed time.
PIN operations consume an entered PIN and return a salted credential or an authentication result.

- [ ] Write tests for excluded packages, expired sessions, narrow system dialogs, and PIN routing.
- [ ] Observe failing tests. Implement one policy for grid and foreground access.
- [ ] Implement salted PIN credentials, default off, and protected new-session and admin routes.
- [ ] Operate `./gradlew testDebugUnitTest`. Expect all tests to pass.
- [ ] Commit the policy and PIN code.

### Task 3: Durable Android controller and Compose interface

Files: create the DataStore repository, controller, accessibility helper, Compose activity, resources, and Android tests.

Interfaces: the controller consumes engine actions, a clock, and a durable repository.
It publishes a state flow only after a successful save. The helper owns active deadlines.
The Compose interface sends actions to that controller and never owns a separate timer.

- [ ] Write Android tests for chooser, duration bounds, session resume, PIN, focus, persistence, and recovery.
- [ ] Observe missing routes. Implement the controller, helper, and Compose screens.
- [ ] Add foreground redirects, screen events, media pause, warnings, and tested Cast/Home routing.
- [ ] Operate unit tests, lint, release build, and instrumentation compilation. Expect success.
- [ ] Commit the complete first release.

### Task 4: Installer, CI, and operating instructions

Files: create `tools/install-family-home.sh`, installer tests, a separate CI job, and the app README.

Interfaces: installer consumes `TV_SERIAL`, builds only Family Home, and records device configuration before changes.
It preserves unrelated helpers and Google Cast packages, and returns a nonzero status on any failed device operation.

- [ ] Write behavioral installer tests with a fake ADB process.
- [ ] Observe the missing installer failure. Implement installation and recovery instructions.
- [ ] Add the independent app to CI without changing existing Java tasks or size limits.
- [ ] Operate repository installer tests and the complete new-app build gates. Expect success.
- [ ] Commit installer, CI, and documentation.

### Task 5: Physical acceptance, review, and delivery

Files: record acceptance evidence under `docs/family-home/evidence`.

- [ ] Capture the original device roles, helpers, package versions, and source state.
- [ ] Install the tested release and prove chooser, Home, wake, and reboot behavior.
- [ ] Prove app restrictions, independent child balances, expiry, actual media pause, PIN, and system dialogs.
- [ ] Capture and inspect the rendered TV interface and normal Cast behavior for Amine.
- [ ] Request a fresh whole-branch review. Fix important findings with regression tests.
- [ ] Deliver all installed changes to `origin/main`. Make sure that the remote commit contains the installed source.
- [ ] Report accepted behavior and remaining device or app-specific limits separately.
