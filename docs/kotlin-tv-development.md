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

Before delivery, inspect the installed app through the actual Family Home launcher:

1. Inspect the icon at small sizes and the banner at 320 by 180 and 384 by 216 pixels.
2. Make sure that artwork fills its bounds, preserves proportions, and shows the correct title without clipping.
3. Select the app with the D-pad and inspect title contrast, background crop, focus, and safe margins.
4. Move to another app and make sure that the previous background clears.
5. Open the app and return to the launcher, then make sure that focus and artwork match.
6. Replace the installed APK and make sure that the launcher refreshes its artwork while preserving the selected app.

Use the emulator first. Keep physical TV observations separate from emulator results.
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
Listen to the final files and test their volume on the TV.
Keep audio optional and easy to mute. Avoid repeating music on a launcher unless the user requests it.
Do not add audio generation services to the app merely to bundle a sound file.

## Use scoped subagents

For substantial work, separate design, implementation, and independent review.
Give each subagent a bounded task with file ownership and a clear completion condition.
Include the selected design, product constraints, and relevant build commands in the handoff.
Run independent research, asset exploration, and read-only reviews in parallel when useful.
Keep edits to shared files sequential.

One coordinator owns integration, Git delivery, emulator sessions, and physical TV input.
Do not let two agents build the same app or control the same device concurrently.
Use a fresh reviewer for the completed change and resolve material findings.
For small documentation or configuration edits, work directly and keep review proportional to the risk.

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

## Iterate in the emulator

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

Keep instrumented tests on the emulator by default. The runner can uninstall the tested app afterward.
Presentation fixtures prove interface behavior. They do not prove the real accessibility helper or third-party playback behavior.
Android UI automation normally suppresses accessibility services during inspection.
For manual helper tests, use explicit ADB input and screenshots, as described in the app guide.

## Physical TV and delivery

Use the physical TV for Cast, streaming pause, wake behavior, and final visual review.
Make sure that the TV is available before tests that interrupt playback.
If someone is watching, stop device input and finish independent work.
Do not clear app data or end an existing session as routine test cleanup.
Preserve Google TV Home and `com.google.android.tungsten.setupwraith` because Cast uses their credentials.

Keep device identifiers, personal account screens, and backups out of Git.
Use generic names in shared captures. Attach review images directly in chat without public hosting.
The user deferred repository privatization. Do not detach the fork or change visibility without a new explicit request.
While the repository remains public, keep new family screenshots and personal data out of Git.

Report local tests, emulator results, physical observations, installation, and Git delivery separately.
State which physical tests remain deferred. Do not infer their results from interface tests.
After an authorized installation, follow the commit and push instructions in [AGENTS.md](../AGENTS.md).
