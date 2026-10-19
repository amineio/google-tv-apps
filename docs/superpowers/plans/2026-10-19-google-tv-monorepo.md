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
- [ ] Move the Gradle build to the root and apps to apps/home and apps/clock.
- [ ] Update installer paths and retain legacy launcher entry points.
- [ ] Add installer tests with fake build and TV clients. No real TV calls.
- [ ] Add monorepo documentation, upstream credit, and CI for both apps.
- [ ] Build releases, run unit tests and lint, and run repository tests.
- [ ] Review changes, publish the tested commits, and inspect the remote state.

## Review focus

The home installer must not build or install the clock app.
The clock installer must not change the home app.
Both module APK paths must match their Gradle project names.
Historical TV lists and font license links must remain usable.
A fresh clone must build without local files from the previous checkout.
