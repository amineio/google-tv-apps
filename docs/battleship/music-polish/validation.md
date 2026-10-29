# Battleship 1.6 music validation

Date: 2026-10-29. Scope: offline music for the menu, accepted new matches, victory, and defeat.

## Behavior

The menu uses the complete 108-second composer loop. Home, Help, and Settings keep the same playback session. An accepted new match starts a six-second opening. Continue and Resume do not repeat it. Ordinary gameplay stays quiet after the opening. Battle effects reduce the opening volume while they play.

Victory uses a 12-second excerpt. Defeat uses the complete 15-second source. Both play once after the existing final sinking delay. Pause, mute, background, and destruction release music. A canceled opening or result does not replay on app return. The Sound control covers music and effects.

## Assets

Four local Ogg Vorbis files contain stereo audio at 44100 Hz. They add approximately 2.12 MiB before APK compression. The app adds no network permission or runtime dependency. MUSIC-CREDITS.md and assets.json record the source authors, CC0 licenses, source hashes, edits, encoded hashes, measured levels, and runtime gains.

The source-based preparation command reproduces all four files byte for byte. The music preparation inspection passes for all four hashes, formats, and durations. The existing battle audio preparation inspection also passes.

## Evidence limits

Native playback state establishes that MediaPlayer decodes and starts the bundled tracks. It does not establish perceived sound quality. No listening acceptance claim covers the musical cuts, loop continuity, speaker balance, or household volume. The source record and measured levels support the initial selection and quiet playback gains.

## Final automated results

The final local build passes. Debug and release each pass 69 unit tests. All 18 emulator tests pass, including native decoding and playback of the four bundled tracks. Release lint reports zero errors and three existing warnings. Debug, release, and device-test APKs build successfully.

The independent review accepts the specification and code. It first found a retained audio-focus request after denial or permanent loss. Six regression tests cover the repair. Four tests failed before the repair. All six pass after it. Later explicit user input can request audio control again. Automatic game updates do not compete for it. Old callbacks cannot restart canceled music.

The final release also passes an app-level emulator inspection. The menu starts native music playback. Settings keeps the same player. The Sound control releases the player. Background also leaves no app music player active. These observations cover Activity integration, separately from native backend tests.

## Physical TV installation

The final release installs successfully over the existing app. The installed package reports version 1.6 and code 7. Its APK hash matches the local release exactly:

`7384b5bd3d97059a53bcddc489ccebb5b7e48db72f7aa5b5e8ea387084184efc`

The menu renders on the physical TV. Android reports an active stereo 44100 Hz music player for the app. A D-pad selection starts a new match and replaces the menu player. Android records the opening player starting and stopping approximately six seconds later. Pause leaves no app music player active. The final match stays paused. These observations establish installed playback behavior, not subjective listening acceptance.

Physical screenshots and device logs stay outside Git because they can contain personal information.
