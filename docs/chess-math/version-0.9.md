# Version 0.9

Version 0.9 uses Android version code 9. About reads both values from BuildConfig.

## Gameplay and controls

- The computer uses the strongest existing local search, without random move selection or shuffled ties.
- Maths practice is optional. Its preference survives app restarts and progress resets.
- With practice enabled, the cycle is five minutes of maths, one minute of rest, and a full chess match.
- With practice disabled, Continue enters chess and a completed match offers another chess game.
- The final board remains visible until the player explicitly continues.
- Captures use two wooden knocks. Smooth piece slides take 480, 560, or 620 milliseconds.
- Routine saves remain silent on chess and maths screens. Storage failures still show an error and Retry.

## Settings and About

The title menu offers Continue, New game, Settings, and Close app when progress exists. New game retains confirmation.
Settings uses grouped rows for Play and learning, App preferences, Current session, and Information.
Boolean preferences use native switches. Language keeps its current value as supporting text.
Only the focused row has an amber border. Confirmation dialogs use the forest theme and focus Cancel first.
About shows the app name, Version 0.9, Build 9, and scrollable help.

The grouping follows [Android settings guidance](https://developer.android.com/design/ui/mobile/guides/patterns/settings) and the [AOSP TV Settings preference categories](https://android.googlesource.com/platform/packages/apps/TvSettings/+/8f47225/Settings/res/xml/main_prefs.xml).
The app retains its native Java views and adds no runtime dependency.

## Verification on 2026-10-27

- All 155 release unit tests pass: Home 42, Clock 14, Chess and Maths 99.
- All eight repository and installer tests pass with CLEANYOURTV_STRICT enabled.
- Release lint passes for all three apps with no errors. Warnings remain.
- Release APKs and device-test APKs build. All three release APKs remain below the CI size limits.
- All 40 selected physical Chromecast tests pass in 122.056 seconds.
- The device run covers both play modes, preferences, title actions, grouped Settings, and About in both languages. It also covers large text, promotions, board layout, maths controls, and effects.
- The About test compares visible version and build values with the installed Android package metadata.
- Independent review finds no blocking code issue. The README now describes New game correctly in both play modes.

The device tests use memory-backed game fixtures. The play-mode preference test restores its original Android preference after each test.
The selected run excludes the separate recreation and launcher-focus tests that use the family session.

The old five-minute chess stop did not reproduce in the current build. Accelerated device tests preserve playable chess beyond five and twenty simulated minutes.
These tests establish software behavior. They do not establish a chess rating or learning effect.
The previously documented limits for rare dead positions remain unchanged.

The version 0.8 record is historical. It does not describe the current opponent or release validation.

## TV screenshots

- [Settings in English](evidence/settings-v9/settings-en.png)
- [Settings in Dutch](evidence/settings-v9/settings-nl.png)
- [About in English](evidence/settings-v9/about-en.png)
- [About in Dutch](evidence/settings-v9/about-nl.png)
