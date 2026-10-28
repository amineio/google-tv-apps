# Battleship launcher artwork validation

Date: 2026-10-28. Installed releases: Battleship 1.3, code 4, and Family Home 1.6, code 7.
The feature includes Family Home 1.5 daily usage from `origin/main` at `642af350`.
Existing daily usage source and tests remain unchanged by the artwork update.

## Artwork and ownership

The built-in ImageGen tool created the square icon, TV banner, and text-free background.
The [asset record](assets.json) contains the exact prompts, source references, dimensions, compression, rights, and file hashes.
All recorded image sizes and hashes match the bundled files.
The icon remains recognizable at 128 pixels. The banner title remains readable at 384 by 216 pixels.

Battleship owns all three images. Family Home reads the optional `com.amine.familyhome.HERO_IMAGE` drawable resource.
The launcher decodes only the selected background off the main thread.
It preserves aspect ratio and limits decoded dimensions to 1920 by 1080 pixels without enlarging small images.
Missing or unusable backgrounds retain the app tile and its gradient.
Existing Chess artwork retains precedence. The background never receives focus.

## Automated results

Both apps used JDK 17, Android SDK platform 37, and their independent Gradle wrappers.
The Battleship command completed successfully in 1 minute, 20 seconds:

```sh
ANDROID_SERIAL=emulator-5580 apps/battleship/gradlew -p apps/battleship testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.amine.battleship.LauncherArtworkTest --no-daemon --max-workers=1 --console=plain
```

Battleship passed 32 debug unit tests, 32 release unit tests, and three Android artwork tests.
Each unit variant includes 1,000 seeded complete games. There were zero test failures or errors.
The artwork tests cover installed application metadata, decoded background dimensions, banner proportions, and opaque icon/banner edges at multiple sizes.
The existing game control tests were not repeated for this artwork-only change.
Release lint reported zero errors and three advisory warnings.

The final Family Home command includes the daily usage merge and version 1.6.
It completed successfully in 2 minutes, 43 seconds:

```sh
ANDROID_SERIAL=emulator-5580 apps/family-home/gradlew -p apps/family-home testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease connectedDebugAndroidTest --no-daemon --max-workers=1 --console=plain
```

Family Home passed 62 debug unit tests, 62 release unit tests, and all 35 Android tests.
There were zero test failures or errors. Release lint reported zero errors and six advisory warnings.
New Android tests cover metadata lookup, wide/tall/small decoding, missing packages, invalid resources, unsupported drawables, selection changes, fallback, resume, and Chess precedence.
Directional input covers app focus, footer navigation, and return to the selected tile.
The existing profile, configuration, and daily usage tests also pass.

The Battleship metadata test failed as expected before the metadata declaration existed.
The Family Home contract tests failed to compile as expected before the loader contract existed.
Both suites pass after implementation.
The tests do not directly cover corrupt image bytes, malformed metadata, or a deliberately delayed obsolete decoder.

Repository tests passed: 14 tests, zero failures, with `CLEANYOURTV_STRICT=1`.
`git diff --check` passed. No cloud build, APK upload, or public release was created.

## Native launcher observations

The dedicated Google TV emulator rendered the actual installed Battleship artwork under Family Home controls at 1920 by 1080 pixels.
Directional selection changed the background correctly. Moving away restored the other app's gradient without residual ship artwork.
Footer navigation restored the Battleship tile. Replacing Battleship retained its selected tile and artwork.
OK opened the game. Back returned to the selected launcher tile.

A fresh independent reviewer found no material source or visual issue.
The reviewer inspected actual emulator captures and a physical Chromecast capture.
The selected title, banner, background, and focus outline match.
The title and controls remain clear. The ship retains its main features.
Footer controls remain separate from the Clock overlay.

## Physical installation

Both release APKs installed successfully on the Chromecast through explicit-serial `adb install -r` updates.
The installed package versions and APK SHA-256 values match the tested local releases.
No app data was cleared. Existing profile and child access policies remain unchanged.
Home role, Home resolution, enabled accessibility helpers, Google TV Home, and setupwraith match their captured prior states.
Physical ADB directional input selected another app and restored Battleship.
OK opened Battleship, and Back returned to its focused launcher tile with the new background.
Device records, original APKs, and family captures remain in ignored local backups.

These observations establish the captured layout and ADB navigation behavior.
Native 4K rendering, viewing-distance readability, and navigation with the physical remote remain untested in this change.
Game rules, sound, and gameplay visuals remain unchanged by this artwork update.

## Build artifacts

Battleship debug APK: `apps/battleship/build/outputs/apk/debug/battleship-debug.apk`.
Battleship release APK: `apps/battleship/build/outputs/apk/release/battleship-release.apk`.
The release contains 1,782,666 bytes. Its SHA-256 is `8e54944f231ea47de88f41938cae0f877000c96c225d035cb1478de2e7697f03`.

Family Home debug APK: `apps/family-home/build/outputs/apk/debug/family-home-debug.apk`.
Family Home release APK: `apps/family-home/build/outputs/apk/release/family-home-release.apk`.
The release contains 2,694,066 bytes, below the 4 MiB limit.
Its SHA-256 is `670aa3c5a22e041b6aff2f3ae3125bf85109e084a9c3b7b666c8912f7c9dee13`.
Both builds use the existing local Android debug signing certificate. Store signing remains separate work.
