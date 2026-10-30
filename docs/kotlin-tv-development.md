# Kotlin TV development

Family Home is the working reference for new Kotlin apps in this repository.
Its separate build lets modern Android tools coexist with the existing Java apps.
Keep Java migration separate from new product work.

## Start from the working app

Place each new app in `apps/<name>/` with its own package and installer.
Use the Family Home Gradle wrapper and build structure as the starting reference.
Do not copy its package, profile data, accessibility service, or Home role unless the new app needs them.
Keep permissions limited to the app's behavior.

The Family Home tool versions on 2026-10-28 are:

| Tool | Version | Source |
| --- | --- | --- |
| JDK | 17 | `apps/family-home/build.gradle.kts` |
| Gradle | 9.6.0 | `apps/family-home/gradle/wrapper/gradle-wrapper.properties` |
| Android Gradle Plugin | 9.4.1 | `apps/family-home/build.gradle.kts` |
| Kotlin and Compose compiler plugin | 2.4.21 | `apps/family-home/build.gradle.kts` |
| Compose for TV Material | 1.1.0 | `apps/family-home/build.gradle.kts` |
| Android SDK platform | `android-37.0` | `apps/family-home/build.gradle.kts` |
| Android build tools | 36.0.0 | `.github/workflows/family-home.yml` |

Read those files before reuse. Keep compatible versions together instead of upgrading tools during a visual change.
Family Home targets SDK 37 and requires Android 14, API 34.
Choose the minimum Android version for each new app's requirements.

The existing Java apps use the root wrapper and SDK platform 34 with build tools 34.0.0.
Do not register an independent Kotlin build in the root `settings.gradle`.
Add shared components only after two apps need the same behavior.

## Design before implementation

For a substantial interface change, generate a few screen concepts with ImageGen.
Specify a TV screen, viewing distance, text size, app content, and remote focus.
Show the results in chat so that the user can review them from a phone.
Use generic profile names in shared concepts.
Record the selected direction before writing the interface.

Use native Compose for TV controls for the final interface.
Keep text, app tiles, focus rings, and buttons separate from background artwork.
Generated screen images are references, not interactive screens or proof of the final app.
Capture the running app after implementation and compare it with the selected direction.
For substantial game changes, use an independent game UI/UX reviewer for both concepts and final native captures.
Ask the reviewer to inspect readability, board size, visual hierarchy, focus, and unnecessary instructions.
Keep grids, teaching diagrams, labels, and controls in native code. Generated artwork does not establish their correctness.

## Reuse the Family Home lessons

The presentation reference files are:

| File in `apps/family-home/src/main/kotlin/com/amine/familyhome/` | Responsibility |
| --- | --- |
| `FamilyTheme.kt` | Palette, type, profile portraits, header, and TV action controls |
| `ProfileScreen.kt` | Profile focus and selection |
| `HomeScreen.kt` | Feature area, app row, footer, and focus restoration |
| `LauncherCatalog.kt` | Installed app labels, icons, banners, and ordering |
| `MainActivity.kt` | Screen routing and controller integration |

Use TV Material controls with clear focus outlines and readable contrast.
Keep large headings and long labels readable from a couch.
Make the feature area and its primary action refer to the same app.
Preserve the selected item after app return, list scrolling, and package changes.
Provide a usable screen when the app list is empty.
Keep footer controls clear of the independent Clock overlay at the bottom right.

Variable fonts need rendered inspection. Family Home sets the Outfit weight axis explicitly with `FontVariation.Settings`.
Use bounded bitmap sizes when loading installed app artwork.
Family Home loads 128-pixel icons and 384-by-216-pixel banners off the main thread.
Keep timer and access policy outside presentation components.
Do not copy the family's profile names into a new app or generic test fixtures.

## Launcher artwork for new apps

Every new app or game must include matching launcher artwork before release.
Plan the artwork with the app's visual design. Use ImageGen for suitable illustrated icons, banners, and backgrounds.
Show the generated artwork in chat for review before integration.
Use a consistent palette, subject, and style across these three separate files.
Use these repository defaults for raster assets:

| Asset | Bundled size | Composition |
| --- | --- | --- |
| App icon | 512 by 512 pixels | Use a clear central silhouette that remains recognizable at 64 and 128 pixels. Keep essential details inside the central 70 percent. |
| TV banner | 640 by 360 pixels, 16:9 | Include the app title. Keep it readable at 384 by 216 pixels and inside safe margins. |
| Launcher background | 1920 by 1080 pixels, 16:9 | Use a separate image without text or controls. Keep the left half quiet and the lower quarter dark. |

Keep the background's main subject clear of the native title, profile control, app row, and Clock overlay.
Do not derive the background from the icon, stretch the banner, or use a complete interface screenshot.
Keep all three resources inside the app.
Store raster artwork as compressed WebP files in `src/main/res/drawable-nodpi`. Use vectors for simple symbols.
Record the prompts, sources, dimensions, compression, file hashes, and rights beside the app's design record.

The app owns its artwork. Family Home owns presentation, contrast, focus, and fallback.
Register the icon, banner, and background in the app's `AndroidManifest.xml`:

```xml
<application
    android:icon="@drawable/app_icon"
    android:banner="@drawable/app_banner">
    <meta-data
        android:name="com.amine.familyhome.HERO_IMAGE"
        android:resource="@drawable/app_hero" />
</application>
```

Add these attributes and metadata to the existing `application` element.
The metadata value is a local drawable resource, not a file path or URL.
Use stable icon and banner names. For bitmap wrappers, use `android:gravity="fill"` and `android:filter="true"` with the matching source proportions.
Use [Battleship's manifest](../apps/battleship/src/main/AndroidManifest.xml) and [bitmap wrapper](../apps/battleship/src/main/res/drawable/battleship_icon.xml) as examples.
Family Home reads this metadata generically. Do not copy app artwork into the launcher or add a special case for each new app.

Family Home decodes only the selected background off the main thread, with a maximum size of 1920 by 1080 pixels.
It preserves aspect ratio and does not enlarge small images during decoding.
The existing Chess artwork takes precedence. Missing or unusable backgrounds retain the app tile and its gradient.

Before delivery, inspect the installed app through Family Home in the emulator:

1. Inspect the icon at small sizes and the banner at 320 by 180 and 384 by 216 pixels.
2. Make sure that artwork fills its bounds, preserves proportions, and shows the correct title without clipping.
3. Select the app with the D-pad and inspect title contrast, background crop, focus, and safe margins.
4. Move to another app and make sure that the previous background clears.
5. Open the app and return to the launcher, then make sure that focus and artwork match.
6. Replace the installed APK and make sure that the launcher refreshes its artwork while preserving the selected app.

Use emulator inspection during development. Complete physical TV installation before merging app changes or pushing them to `origin/main`.
Keep physical TV observations separate from emulator results.
Reuse the patterns in [Battleship's artwork tests](../apps/battleship/src/androidTest/kotlin/com/amine/battleship/LauncherArtworkTest.kt).
Read its [asset record](battleship/launcher-art/assets.json) and [validation record](battleship/launcher-art/validation.md) for a completed example.

## Generate and prepare assets

Use ImageGen for portraits, illustrations, textures, and background artwork when these improve the app.
Use native vectors or code for simple icons and shapes.
Keep readable labels in Compose so that generated artwork does not control text quality.
Save the prompts and reference sources beside the design record.
Record intended dimensions, output files, and applicable licenses.
Family Home records its image prompts in `docs/family-home/concepts/asset-prompts.json`.

Resize and compress bundled artwork before packaging it.
Family Home uses WebP assets and stores their final files in `drawable-nodpi`.
Preserve the Outfit font license when reusing the font.
Measure the APK size after adding assets. Family Home has a 4 MiB release limit.
Choose a size limit for each new app instead of copying that limit without review.

ImageGen does not create audio. Use an audio generation tool or local synthesis for music and sound effects.
Document the tool, prompt or synthesis parameters, license, and output duration.
Trim and normalize audio with an audio tool such as FFmpeg.
Listen to changed audio files locally when a listener is available.
Use physical TV listening only for an explicit user request or a user-reported audio issue.
Record any listening limit. Successful decoding and MediaPlayer state do not prove speaker balance or musical quality.
Keep audio optional and easy to mute. Avoid repeating music on a launcher unless the user requests it.
Do not add audio generation services to the app merely to bundle a sound file.

## Use scoped subagents

Every conversation's main thread remains the coordinator and delegates development and independent review to subagents.
This applies to new and follow-up threads.
For substantial work, separate design, implementation, and independent review.
Give each subagent a bounded task with file ownership and a clear completion condition.
Include the selected design, product constraints, and relevant build commands in the handoff.
Run independent research, asset exploration, and read-only reviews in parallel when useful.
Keep edits to shared files sequential.

One coordinator owns planning, integration commands, builds, Git delivery, emulator sessions, and physical TV input.
Delegate all implementation file edits to subagents, including small code changes.
Give shared files to one implementation owner at a time.
Do not let two agents build the same app or control the same device concurrently.
Use a fresh reviewer for the completed change and resolve material findings.
Delegate documentation and configuration edits too. Keep their review proportional to the risk.

## Build locally

On the development Mac, set the installed JDK and SDK paths:

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
```

Install the SDK packages if they are missing:

```sh
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "platforms;android-37.0" "build-tools;36.0.0"
```

From the repository root, operate the Family Home tests and release build:

```sh
apps/family-home/gradlew -p apps/family-home testDebugUnitTest testReleaseUnitTest lintRelease assembleRelease assembleDebugAndroidTest --console=plain
```

The release APK is `apps/family-home/build/outputs/apk/release/family-home-release.apk`.
The current release uses debug signing for local installation.
Store publishing and production signing are separate tasks.
For another app, use its own wrapper, directory, and package throughout these commands.

If installers or repository files change, operate the repository tests from the root:

```sh
CLEANYOURTV_STRICT=1 python3 -m unittest discover -s tests -v
git diff --check
```

GitHub builds are paused. Both retained workflows accept manual runs only.
Do not enable them, upload APKs, or create releases without an explicit request.

## Choose tests by risk

Use the smallest test set that covers the current change during iteration.
At an integration checkpoint, broaden coverage when logic, persistence, or shared behavior changes.
Keep the final release build and lint results, and record the commands and tested revision.

| Change | Iteration | Integration and delivery |
| --- | --- | --- |
| Text, artwork, or spacing | Focused emulator capture and affected navigation test | Release build, lint, and affected resource or interface tests |
| Language selection or translated layouts | Locale persistence tests and captures in each language | Relevant app suite once, release packaging, and offline language coverage |
| Game rules, saved state, lifecycle, or shared controls | Focused regression tests | Relevant unit and emulator suites once at a testable checkpoint |
| Playback, audio focus, accessibility, Cast, or wake | Focused logic and emulator tests | Relevant local and emulator coverage. State untested hardware behavior |
| Documentation only | Review links, commands, and the diff | No app installation or device suite unless behavior also changes |

After a minor resource correction, repeat affected captures and tests.
Do not repeat an unaffected full suite without a new failure or an unresolved concern.
If a required test fails, resolve it or report the blocker before delivery.
This workflow changes test scope, not the requirement to cover changed behavior.

For Battleship, use its wrapper and the dedicated emulator serial from [the app guide](../apps/battleship/README.md#install-and-inspect).
After setting the JDK and SDK paths above, select a single emulator test class with:

```sh
ANDROID_SERIAL=emulator-5580 apps/battleship/gradlew -p apps/battleship connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.battleship.HelpScreenTest' --console=plain
```

Replace the class with the test that covers the change.
Use one class per invocation or a package filter with proven selection behavior.
In two observed Battleship runs, a comma-separated class filter returned `BUILD SUCCESSFUL`, but XML contained only the first class.
This observation does not establish the cause or behavior of every runner version.
Inspect the XML test counts and class names before claiming coverage.
Build success alone does not establish that all selected tests ran.
Keep each invocation's XML report before another run replaces it.
For an integration checkpoint, use the app guide's unit, lint, build, and emulator commands.
Keep one coordinator responsible for builds and device input throughout these steps.

## Iterate in the emulator

Use emulator coverage for development and instrumented tests.
Install the tested release APK on the physical TV before merging app changes or pushing them to `origin/main`.
Use the existing `Family_TV_API_34` Google TV emulator for layout and D-pad work.
Read [the emulator instructions](../apps/family-home/README.md#local-tv-emulator) for startup, install, and capture commands.
Set an explicit device serial so that the emulator and physical TV cannot receive each other's commands.
From the repository root, operate the Family Home UI tests:

```sh
ANDROID_SERIAL=emulator-5554 apps/family-home/gradlew -p apps/family-home connectedDebugAndroidTest --console=plain
```

Use directional input to test initial focus, row scrolling, dialogs, and focus after app return.
Do not force focus onto a button when the test must prove navigation to that button.
Cover empty lists, long app names, removed apps, and saved state after recreation.
Make sure that final captures use the intended build, font weights, and screen density.
Use a fresh filename for each capture and inspect its modification time before review.
Do not reuse an old PNG after a capture command fails.
For Activity Back behavior, use `Instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)` in instrumented tests.
A Compose `Key.Back` event alone does not exercise the Activity Back dispatcher.

For in-app language changes, make Compose observe `LocalConfiguration` or `LocalResources` through a reactive locale provider.
Bundle all supported languages offline. If delivery uses app bundles, disable language splitting when the app changes language itself.
Inspect long translations on the actual layout, especially ship names.
Use semantic line breaks and shared board geometry instead of shrinking one board to fit a translated label.

Keep instrumented tests on the emulator. Do not run them on the physical TV because the runner can uninstall the tested app.
Presentation fixtures prove interface behavior. They do not prove the real accessibility helper or third-party playback behavior.
Android UI automation can suppress accessibility services during inspection.
During Battleship work on 2026-10-29, physical TV UI dumps coincided with Family Home redirects and accessibility interruptions.
This observation does not establish one cause for every redirect.
For ordinary visual inspection, prefer explicit ADB input and screenshots over repeated UI dumps.
If launcher redirects repeat, stop the loop and inspect the current screen.
Do not disable helpers or change Home routing to obtain a screenshot.
For manual helper tests, use the affected app guide.

## Physical TV and delivery

Always install the changed app's tested release APK on the physical TV before merging app changes or pushing them to `origin/main`.
This standing instruction authorizes routine physical installation without renewed permission.
After installation, make sure that the installed version and APK SHA-256 match the tested release artifact.
If the TV is unavailable or installation fails, block the merge, preserve the task branch, and report the blocker.
Documentation-only changes do not require an APK installation unless they accompany app delivery.
Keep development and instrumented tests on the emulator. The test runner can uninstall the tested app.
Physical installation does not require unrelated Cast, wake, playback, or audio tests.
Limit additional physical tests to the requested behavior or reported issue.
Requested development, testing, and installation take priority over TV viewing.
Stop device input when the user explicitly asks you to stop.
Preserve app data and unrelated apps. Do not end an existing session as routine test cleanup.
Preserve Google TV Home and `com.google.android.tungsten.setupwraith` because Cast uses their credentials.

Keep device identifiers, personal account screens, and backups out of Git.
Use generic names in shared captures. Attach review images directly in chat without public hosting.
The user deferred repository privatization. Do not detach the fork or change visibility without a new explicit request.
While the repository remains public, keep new family screenshots and personal data out of Git.

Report local tests, emulator results, physical observations, installation, and Git delivery separately.
State any untested hardware behavior and any requested physical tests that remain unperformed.
Do not infer physical behavior from interface tests or APK installation alone.
After the required physical installation, follow the commit and push instructions in [AGENTS.md](../AGENTS.md).
