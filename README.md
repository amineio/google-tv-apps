# Google TV Apps

Small native apps for Google TV and Android TV, maintained in one repository.
Each app has its own package, version, permissions, tests, and APK.

| App | Module | Package | Purpose |
| --- | --- | --- | --- |
| [TV Home](apps/home/) | `:home` | `com.gathin.tvhome` | A home screen with an app dock, grid, themes, and screensaver. |
| [Clock](apps/clock/) | `:clock` | `com.amine.tvclock` | A clock overlay, session timer, daily bedtime, and viewing reminders. |
| [Schaak & Reken](apps/chess-math/) | `:chessMath` | `com.amine.chessmath` | Offline chess against the computer or a local opponent, with animal portraits, names, and optional arithmetic. |
| [Family Home](apps/family-home/) | Separate Kotlin build | `com.amine.familyhome` | Family profiles, child timers, allowed apps, and an optional parent PIN. |
| [Battleship](apps/battleship/) | Separate Kotlin build | `com.amine.battleship` | Offline naval strategy against the computer or a local captain, with naval portraits, names, and a ten-second fleet preview. |
| [AI Chat](apps/ask-tv/) | Separate Kotlin build | `com.amine.asktv` | Spoken or typed questions through a ChatGPT subscription, with local example previews. |
| [Postbox](apps/postbox/) | Separate Kotlin build | `com.amine.postbox` | Locally captured post text, links, and screenshots sent from an Android phone for TV reading. |
| [Treasure or Retreat](apps/treasure-or-retreat/) | Separate Kotlin build | `com.amine.treasureorretreat` | Offline temple adventure with three expeditions and remote decisions. |
| [Memory Parade](apps/memory-parade/) | Separate Kotlin build | `com.amine.memoryparade` | Offline animal matching with three board sizes, eight animals, and an animated parade. |

Clock works independently of TV Home. Its stop screen supports voluntary viewing limits.
It remains dismissible and does not guarantee that another app pauses playback.

The [Chromecast device profile](docs/devices/google-chromecast-gtv-4k.md) records published specifications, observed firmware, and development limits for the current physical target.

## Build

Install JDK 17, Android SDK platform 34, and build tools 34.0.0.
Set `JAVA_HOME` and `ANDROID_HOME` to their installed locations.
From the repository root, build the apps:

```sh
./gradlew :home:assembleRelease :clock:assembleRelease :chessMath:assembleRelease
```

The APKs appear at:

```text
apps/home/build/outputs/apk/release/home-release.apk
apps/clock/build/outputs/apk/release/clock-release.apk
apps/chess-math/build/outputs/apk/release/chessMath-release.apk
```

These local release builds use the existing Android debug signing configuration.
Store publishing and production signing require separate configuration.
TV Home and Schaak & Reken support Android 8 and later. Clock requires Android 14 and later.
Family Home requires Android 14 and uses Compose for TV.
Its independent build requires SDK platform `android-37.0` and build tools `36.0.0`:

```sh
apps/family-home/gradlew -p apps/family-home testReleaseUnitTest lintRelease assembleRelease
```

Battleship uses the same independent Kotlin tools and requires Android 8 or later:

```sh
apps/battleship/gradlew -p apps/battleship testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease
```

Read the [Battleship guide](apps/battleship/README.md) for remote controls, saved matches, installation, and emulator tests.

Treasure or Retreat and Memory Parade also use independent Kotlin builds. They require Android 8 or later. Build each game separately:

```sh
apps/treasure-or-retreat/gradlew -p apps/treasure-or-retreat testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease --max-workers=2
apps/memory-parade/gradlew -p apps/memory-parade testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease --max-workers=2
```

Read each app guide for its APK paths, remote controls, and emulator commands. Both games bundle their artwork and short sound effects. They need no account or network connection.

## Install

Read the [home installation guide](apps/home/README.md) before changing the default home app.
The [clock guide](apps/clock/README.md) covers overlay permission, timers, and bedtime.
Both apps install separately. Building an app does not change the TV.
The [Family Home guide](apps/family-home/README.md) covers its separate installer and device configuration.

For Chromecast with Google TV, use [the Cast-compatible Home installer](docs/CAST-HOME.md).
It keeps the Google packages enabled so that the custom Home screen and casting can work together.

Before merging app changes or pushing them to `origin/main`, always install the tested release APK on the physical TV.
Make sure that the installed version and APK SHA-256 match the tested artifact.
If the TV is unavailable or installation fails, preserve the task branch and report the blocker without merging.
Use the emulator for development and instrumented tests. Documentation-only changes do not require an APK installation.
Read [the delivery instructions](AGENTS.md#app-installation-and-delivery) for the standing authorization, data protection, and delivery sequence.

## Test

From the repository root, run local tests and Android lint:

```sh
./gradlew :home:testReleaseUnitTest :clock:testReleaseUnitTest :chessMath:testReleaseUnitTest :home:lintRelease :clock:lintRelease :chessMath:lintRelease
python3 -m unittest discover -s tests -v
```

Run tests locally before delivery. GitHub builds are paused as described below.
TV Home retains a baseline for 29 existing upstream lint errors. New lint errors still fail the build.
See [the lint record](docs/upstream-lint.md) for the scope.
Device tests require a connected TV and run separately.
Local tests do not establish playback behavior inside every streaming app.

## Repository layout

```text
apps/home/       TV Home app, resources, and unit tests
apps/clock/      Clock app, resources, and device tests
apps/chess-math/ Offline chess and arithmetic app
apps/family-home/ Independent Kotlin family launcher and Gradle build
apps/battleship/  Independent Kotlin offline naval game and Gradle build
apps/treasure-or-retreat/ Independent Kotlin treasure game and Gradle build
apps/memory-parade/ Independent Kotlin animal matching game and Gradle build
gradle/          Shared Gradle wrapper
tools/           App installers and existing TV tools
tests/           Repository and installer tests
designs/         Design concepts
docs/            Guides and implementation records
examples/        Existing device lists
themes/          Existing TV Home themes
launcher/        Compatible entry points for the original installers
```

New apps belong in `apps/<name>/`. Apps that use the shared build register in the root `settings.gradle`.
Family Home has its own build so that modern Kotlin tooling does not change the Java app builds.
Keep app-specific code and permissions inside that app. Add shared code only when apps need it.

## Kotlin app development

Use Kotlin and Compose for TV for new apps. Keep the existing Java apps independent.
A Java migration is separate work and is not a prerequisite for a new Kotlin app.
Use Family Home as the working reference for build tools, native TV controls, focus, and local tests.

Read the [Kotlin development guide](docs/kotlin-tv-development.md) for the workflow and lessons from Family Home.
It covers emulator iteration, scoped subagents, ImageGen concepts, bundled artwork, and separate audio tools.
[AGENTS.md](AGENTS.md) gives the instructions for coding agents.

## Privacy and GitHub builds

The user deferred the change to a private repository.
While the repository remains public, keep new family screenshots and personal data out of Git.
Use generic names in shared previews. Keep device backups and personal account captures out of Git.
Share review images through the chat as attachments, without public image hosting.

Local builds and tests are the default. Both GitHub build workflows are paused.
Their definitions accept manual runs only, so pushes and pull requests do not start APK builds.
Do not enable cloud builds or publish APKs without an explicit request.
The retained workflow definitions upload APKs and reports when an operator enables and starts them.

## Upstream and license

This repository derives from [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv).
It retains the original history, TV Home implementation, tools, themes, and device lists.
[UPSTREAM.md](UPSTREAM.md) preserves the original project guide.
[CONTRIBUTING.md](CONTRIBUTING.md) explains the contribution workflow.

The code uses the [MIT license](LICENSE).
The Outfit font retains its [SIL Open Font License](apps/home/FONT-LICENSE-OFL.txt).
