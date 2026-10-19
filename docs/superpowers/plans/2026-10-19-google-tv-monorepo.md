# Google TV apps implementation plan

Goal: Publish the existing home and clock apps as one maintained Google TV repository.
Architecture: One root Gradle build, independent app modules, shared development tools.
Tech stack: Existing Java 17, Android SDK 34, Gradle, Python, and shell scripts.
Spec: ../specs/2026-10-19-google-tv-monorepo.md.

## Constraints

Preserve source behavior, package names, versions, history, licenses, and installed TV state.
Keep the original local checkout intact. Use a fresh clone of the fork.
Do not publish build outputs, TV backups, SDK paths, or credentials.

## Tasks

- [x] Create the fork and import the tested TV Clock source in a separate commit.
- [x] Move the Gradle build to the root and apps to apps/home and apps/clock.
- [x] Update installer paths and retain legacy launcher entry points.
- [x] Add installer tests with fake build and TV clients. No real TV calls.
- [x] Add monorepo documentation, upstream credit, and CI for both apps.
- [x] Build releases, run unit tests and lint, and run repository tests.
- [ ] Review changes, publish the tested commits, and inspect the remote state.

## Review focus

The home installer must not build or install the clock app.
The clock installer must not change the home app.
Both module APK paths must match their Gradle project names.
Historical TV lists and font license links must remain usable.
A fresh clone must build without local files from the previous checkout.

## Local validation and review

Both release APKs build. All 56 Android unit tests and eight repository tests pass.
Clock device test sources compile. No device operations ran during this migration.
TV Home is 278633 bytes. TV Clock is 62636 bytes.
The home lint baseline records 29 existing errors. New lint errors remain fatal.
Independent review found no blocking migration defects.
The review found one stale documentation anchor. The anchor and related installer messages now point to the retained guides.
The reviewer confirmed byte-identical home and clock source files.
GitHub CI and publication to main remain the final steps.
