# Battleship Launcher Artwork Implementation Plan

Goal: Deliver matching app branding and a selected-app background through the existing launcher.
Spec: docs/superpowers/specs/2026-10-28-battleship-launcher-art-design.md
Architecture: App-owned bitmap resources and optional metadata, with selected-only decoding in Family Home.
Tools: Existing Kotlin, Compose for TV, ImageGen, compressed WebP, independent Gradle builds.

## Boundaries

Root coordinator owns app resources, manifest, versions, builds, Git, and devices.
The launcher implementer owns only its agreed Kotlin and test files. A fresh reviewer assesses the completed change.
Keep game logic, profiles, sessions, timers, permissions, helpers, Chess art, other apps, and Cast unchanged.
Do not run concurrent builds or test sessions against the same app or device.

## Work

- [x] Record generated prompts, dimensions, compression, source references, rights, and files.
- [x] Add failing metadata/branding tests in Battleship and focused selected-art tests in Family Home.
- [x] Observe the expected failures before integrating the contract.
- [x] Add the app's icon, banner, and hero. Use metadata android:resource, not a string or URL.
- [x] Implement selected-only bounded hero decoding, fallback, stale-image protection, and package refresh.
- [x] Inspect actual launcher captures with generic fixtures and actual installed Battleship resources.
- [x] Run app unit tests, release lint, APK builds, focused Battleship art tests, full Family Home emulator tests, and repository tests.
- [x] Obtain fresh independent source and native capture review. Fix material findings.
- [x] Install Battleship 1.3, code 4, and Family Home 1.6, code 7, without clearing data or changing Home/helper configuration.
- Git delivery follows [PR #13](https://github.com/amineio/google-tv-apps/pull/13). The coordinator pushes the tested commits and merges into `origin/main`.

## Review focus

Invalid art must not hide an installed app. An old hero must never appear under another selected title.
A large source bitmap must remain bounded. Native title and controls must stay legible over art.
Package replacement must refresh all branding and preserve the selected app.
No family screenshots or device identifiers enter Git. The icon and banner must survive Android resource scaling.
