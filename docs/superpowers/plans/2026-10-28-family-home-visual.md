# Family Home Visual Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans for native implementation and one independent final review.

**Goal:** Implement the two approved concepts as usable Android TV screens.

**Architecture:** Keep FamilyController and the access helper unchanged. Split the Compose presentation into shared styling, profiles, and app home components. Load installed banners outside the UI thread and preserve native remote controls.

**Tech Stack:** Kotlin, Compose for TV, local generated artwork, existing Outfit font.

**Spec:** `docs/superpowers/specs/2026-10-28-family-home-visual-design.md`.

## Global Constraints

- Keep all existing family timer, access, PIN, storage, and recovery rules.
- Keep the Java apps and root build tools unchanged.
- Use only installed permitted apps and real app labels or the existing approved labels.
- Keep the release below 4 MiB. Share final images through phone-accessible links.

## Review Focus

- Empty or changing app catalogs keep a usable profile control.
- Long labels fit without moving controls outside the TV safe area.
- D-pad moves between app tiles, Open app, and footer controls without a focus trap.
- Returning from an app restores its tile focus and preserves the session balance.
- PIN, duration limits, and expiry remain usable after the presentation changes.

### Task 1: Shared styling and profile screen

**Files:** Create `FamilyTheme.kt` and `ProfileScreen.kt` under `apps/family-home/src/main/kotlin/com/amine/familyhome`. Add artwork and font resources. Modify MainActivity and FamilyRemoteTest.

**Interfaces:** FamilyTheme wraps existing content. ProfileScreen consumes FamilyUi and an onSelect(Viewer) callback. Shared focus controls use native TV Material surfaces.

- [ ] Extend the remote test to require avatar controls and directional profile focus. Run it before implementation and observe failure.
- [ ] Add optimized avatar and chess artwork from the approved concepts. Copy Outfit into the new app.
- [ ] Implement the shared theme, header, focus controls, and centered circular profile screen.
- [ ] Run the profile and contrast UI tests. Capture the emulator result and compare it with the reference.
- [ ] Commit the profile presentation.

### Task 2: App home and remaining flows

**Files:** Create `HomeScreen.kt` and `LauncherCatalog.kt` in the same Kotlin directory. Modify MainActivity, FamilyRemoteTest, and the app README.

**Interfaces:** LauncherApp includes its existing launch component plus icon and optional banner. HomeScreen consumes the permitted list, viewer, remaining time, focus state, and existing action callbacks.

- [ ] Add remote tests for a child duration choice, bounds 5..60, filtered app tiles, footer navigation, and configuration navigation without an explicit focus request. Run the new tests first and observe the unmet cases.
- [ ] Load real app banners and approved labels. Refresh after resume. Implement the feature area, app row, timer header, and footer. Preserve focused app identity across resume.
- [ ] Apply the theme and compact focus controls to duration, expiry, configuration, and PIN screens.
- [ ] Run all unit and emulator UI tests, release lint/build, and installer tests. Compare full-resolution captures against both references.
- [ ] Obtain an independent review. Fix important findings with failing regressions and repeat the relevant suite.
- [ ] Install the release on the Chromecast, capture both screens, and test remote profile/session flows. Preserve unrelated device state and the Java apps.
- [ ] Commit, push, and merge the installed source to origin/main. Make sure that both CI workflows pass and share the actual captures in chat.
