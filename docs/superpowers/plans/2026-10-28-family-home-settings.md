# Family Home settings Implementation Plan

> For agentic workers: Use superpowers:subagent-driven-development for the implementation tasks.

Goal: Provide familiar TV settings and explicit child profile creation without adding a real profile during delivery.
Architecture: Stable serialized IDs and profile metadata preserve saved state. Native TV list rows separate Profiles, Parent PIN, and Device setup.
Tech Stack: Existing Kotlin, Compose for TV, DataStore, and serialization. No new dependency.
Spec: docs/superpowers/specs/2026-10-28-family-home-settings-design.md

## Global Constraints

Preserve exact AMINE/ELIAS/SOFIA IDs, existing portraits, balances, allowed maps, PIN, and state_v1 storage key.
Default and migrated state contain exactly three profiles. Do not create a fourth real profile.
Keep child sessions at default 20 minutes, steps of 5, and range 5–60 minutes.
Keep the single protected parent, current helper/Cast policy, package, permissions, and independent app build.
Only explicit Save creates a child. Remote Back does not write a profile.
One coordinator owns builds, devices, integration, and Git. Workers do not build or use ADB.
Use emulator-5680 on isolated ADB server 5041. Keep personal screenshots and backups out of Git.

## Review Focus

Legacy state migration must preserve balances, PIN fields, and deliberately missing child allowlist entries.
Unknown profile commands and invalid names must not disable storage or grant parent access.
Allowed-app toggles must persist, keep focus, and reflect saved state after write failures.
Back, keyboard dismissal, Android configuration return, and recreation must preserve the correct settings destination and focus.
New profiles must remain reachable on the chooser without changing existing profile state or creating default profiles.

### Task 1: Stable profile state and minimum UI adapters

Files: FamilyState.kt, new FamilyStateCodec.kt, FamilyApplication.kt, FamilyController.kt, SessionEngine.kt, AccessPolicy.kt, related unit tests.
Adapters: ProfileScreen.kt, FamilyTheme.ProfilePortrait, HomeScreen metadata parameters, and MainActivity labels/metadata only.

Interfaces: Viewer serializes as a string ID and retains AMINE/ELIAS/SOFIA constants and built-in fallback label/defaultPortrait.
FamilyProfile(viewer: Viewer, name: String, portrait: Portrait = CHILD).
FamilyState.profile(viewer): FamilyProfile?, FamilyState.label(viewer): String, FamilyState.children: List<FamilyProfile>.
FamilyStateCodec.decode(text): FamilyState and encode(state): String.
FamilyController.addChild(name: String): Viewer?.
Append optional displayName and portrait parameters to ProfilePortrait, HomeScreen, and DurationScreen. Preserve existing positional calls and the current single-action duration and parent dialogs. Remote Back cancels them.

- [ ] Add a meaningful failing unit regression that uses existing APIs with a literal schema-2 state and a new child ID. Prove that the fixed enum/model rejects the required child behavior.
- [ ] The coordinator operates the regression before implementation.
- [ ] Implement the stable-ID model, schema migration, command guards, and authorized addChild contract from the spec.
- [ ] Add tests for literal legacy-state preservation, schema-2 round trips and rejection, invalid references, invalid and duplicate names, authorization, failed writes, and independent child balances/app lists.
- [ ] Adapt chooser and portrait/name rendering for stored profile metadata. Preserve the three-card initial layout and use a LazyRow for growth.
- [ ] The coordinator operates testDebugUnitTest. A focused reviewer assesses model and migration before Task 2.

### Task 2: Native settings hierarchy and profile creation

Files: New SettingsScreen.kt and related native controls, MainActivity Configuration replacement/routing, Android settings tests, app README, build.gradle.kts.
Consume Task 1 interfaces exactly. Core state files remain owned by Task 1.

- [ ] Add meaningful UI regressions for a three-row root, D-pad navigation, single-focus switch rows, child allowed-app persistence, Back restoration, and cancelled creation.
- [ ] Implement the hierarchy and native keyboard creation form from the spec. Use TV ListItem controls and an accessible non-focusable switch indicator.
- [ ] Show device setup status as Android navigation actions. Do not offer switches for platform-controlled values.
- [ ] Use isolated generic controller fixtures to prove explicit Save creates exactly one child, duplicates do not create, and recreation retains the saved profile.
- [ ] Cover scrolling, unavailable apps, creation field validation, keyboard dismissal, PIN dialogs, configuration return, and long labels.
- [ ] Update the app to version 1.4, code 5, after reading the current 1.3/code-4 build.
- [ ] The coordinator operates both unit variants, lintRelease, assembleRelease, assembleDebug, and assembleDebugAndroidTest with a fresh single-worker Gradle process.
- [ ] The coordinator installs test APKs and operates AndroidJUnitRunner on emulator-5680 through ADB port 5041. Inspect actual captures.
- [ ] Resolve material code and UI review findings. Keep the implementation small.

### Task 3: Upgrade proof and delivery

- [ ] Prove a schema-1 installation upgrades without adding a profile, losing saved time, changing app lists, or losing the PIN.
- [ ] Record test evidence and independent reviews. Keep new family screenshots outside Git.
- [ ] Commit only Family Home and its design/validation records. Push the feature branch and prepare the PR.
- [ ] When the TV is available, install the release without clearing data or creating a profile.
- [ ] Merge the PR and make sure that origin/main contains the installed source. Report physical tests separately from emulator evidence.
