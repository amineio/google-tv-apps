# Google TV apps monorepo

The repository is a GitHub fork at `amineio/google-tv-apps`.
The upstream repository remains `Gathin23/cleanyourtv`.
The existing local checkout remains untouched.

One root Gradle build contains two independent Android applications:

- `:home` uses `apps/home` and package `com.gathin.tvhome`.
- `:clock` uses `apps/clock` and package `com.amine.tvclock`.

Each app keeps its existing version, permissions, source, tests, and APK.
No shared runtime library is necessary. Additional apps can join as independent modules.

The root README explains builds, installation, and the app boundaries.
`UPSTREAM.md` retains the original project documentation and credit.
Existing lists, themes, tools, contribution history, and licenses remain available.
Legacy launcher installer paths remain valid and build only the home app.
The clock installer builds only the clock app and does not change Home.

CI builds and tests both apps from the root.
Build outputs, local SDK paths, TV backups, and credentials stay out of Git.
No TV operations are part of this repository migration.

Acceptance requires both release APKs, all existing unit tests, lint, repository tests,
installer path tests without a TV, and preservation of upstream fork metadata.
