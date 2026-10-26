# Google TV Apps

Small native apps for Google TV and Android TV, maintained in one repository.
Each app has its own package, version, permissions, tests, and APK.

| App | Module | Package | Purpose |
| --- | --- | --- | --- |
| [TV Home](apps/home/) | `:home` | `com.gathin.tvhome` | A home screen with an app dock, grid, themes, and screensaver. |
| [TV Clock](apps/clock/) | `:clock` | `com.amine.tvclock` | A clock overlay, session timer, daily bedtime, and viewing reminders. |

TV Clock works independently of TV Home. Its stop screen supports voluntary viewing limits.
It remains dismissible and does not guarantee that another app pauses playback.

## Build

Install JDK 17, Android SDK platform 34, and build tools 34.0.0.
Set `JAVA_HOME` and `ANDROID_HOME` to their installed locations.
From the repository root, build both apps:

```sh
./gradlew :home:assembleRelease :clock:assembleRelease
```

The APKs appear at:

```text
apps/home/build/outputs/apk/release/home-release.apk
apps/clock/build/outputs/apk/release/clock-release.apk
```

These local release builds use the existing Android debug signing configuration.
Store publishing and production signing require separate configuration.
TV Home supports Android 8 and later. TV Clock requires Android 14 and later.

## Install

Read the [home installation guide](apps/home/README.md) before changing the default home app.
The [clock guide](apps/clock/README.md) covers overlay permission, timers, and bedtime.
Both apps install separately. Building an app does not change the TV.

For Chromecast with Google TV, use [the Cast-compatible Home installer](docs/CAST-HOME.md).
It keeps the Google packages enabled so that the custom Home screen and casting can work together.

## Test

From the repository root, run local tests and Android lint:

```sh
./gradlew :home:testReleaseUnitTest :clock:testReleaseUnitTest :home:lintRelease :clock:lintRelease
python3 -m unittest discover -s tests -v
```

GitHub Actions builds both releases, runs these checks, and retains APK artifacts.
TV Home retains a baseline for 29 existing upstream lint errors. New lint errors still fail the build.
See [the lint record](docs/upstream-lint.md) for the scope.
Device tests require a connected TV and run separately.
Local tests do not establish playback behavior inside every streaming app.

## Repository layout

```text
apps/home/       TV Home app, resources, and unit tests
apps/clock/      TV Clock app, resources, and device tests
gradle/          Shared Gradle wrapper
tools/           App installers and existing TV tools
tests/           Repository and installer tests
designs/         Design concepts
docs/            Guides and implementation records
examples/        Existing device lists
themes/          Existing TV Home themes
launcher/        Compatible entry points for the original installers
```

New apps belong in `apps/<name>/`. Register each app in the root `settings.gradle`.
Keep app-specific code and permissions inside that app. Add shared code only when apps need it.

## Upstream and license

This repository is a fork of [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv).
It retains the original history, TV Home implementation, tools, themes, and device lists.
[UPSTREAM.md](UPSTREAM.md) preserves the original project guide.
[CONTRIBUTING.md](CONTRIBUTING.md) explains this fork's contribution workflow.

The code uses the [MIT license](LICENSE).
The Outfit font retains its [SIL Open Font License](apps/home/FONT-LICENSE-OFL.txt).
