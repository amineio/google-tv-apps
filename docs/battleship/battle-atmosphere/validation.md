# Battleship 1.7 validation

Date: 2026-10-29. Base: 99196c9c. Scope: battle music and a quieter gameplay screen.

## Automated evidence

The final local build passes. Both debug and release pass 73 unit tests. All 19 emulator tests pass. Release lint reports zero errors and three existing warnings: two AndroidGradlePluginVersion warnings and one DiscouragedApi warning. Debug, release, and instrumented-test APKs build successfully. No dependencies or permissions change.

Three battle music tests fail before implementation. They cover opening completion, interrupted opening recovery, and stopping battle music before results. The first UI regression fails because TARGET remains visible. The implemented screen removes that label and passes its absence, portrait, equal-board, error, and focus assertions.

The first integration run finds two obsolete expectations. The focus test expects silence after recovery, and the presentation test expects removed target narration. Updated assertions require battle music without another opening and the compact target coordinate. The final full suite passes after those test updates. Production code does not change for them.

Native MediaPlayer tests decode and start all five music resources, including the battle loop. Controller tests cover pause, mute, background, audio control loss, stale callbacks, opening completion, final-shot timing, and effect ducking. Directional tests retain all-cell reachability, held-key handling, and focus recovery.

All five music files reproduce byte for byte from recorded source masters. The preparation inspection matches hashes, stereo Vorbis at 44100 Hz, and durations. The new loop preserves the complete 14.489252-second composer-supplied source. Decoded measurement is -21.05 LUFS and -6.08 dBTP before runtime gain. Its 0.28 gain gives approximately -32.11 LUFS before system volume, fades, and impact ducking. These measurements do not establish subjective sound quality.

## Independent review

The source reviewer finds no material defects. The game UI/UX reviewer first rejects the generated concept as authoritative game data. Its labels, ship sizes, focus color, large title, and bottle placement require correction. Native rendering retains exact axes, hidden enemy ships, five friendly ships, cyan focus, and equal stable boards.

The expert reviews native aiming, dense-shot, and computer-turn captures separately. Scores range from 8 to 9. No material visual correction remains. The portrait does not consume a sidebar. The existing distant ship restores the naval atmosphere without the bottle. Generic native captures are bundled beside this report. Physical captures remain outside Git.

## Delivery

The final release installs on the physical TV over the existing app. The package reports version 1.7 and code 8. The installed APK matches the local tested release, SHA-256:

`22109df2d41c6fb70bf02fbfdfd7901fb66bf273ceb015876bc06f843656f1a8`

Continue opens the preserved match with the captain, equal grids, reduced text, and restored naval background. Android reports an active stereo 44100 Hz music player. The emulator also keeps the same battle player beyond one complete loop and releases it on pause. The physical TV also retains the same battle player beyond one loop and releases it on pause. The final match remains paused.
The physical TV capture agrees with the reviewed native layout. Subjective couch readability and speaker balance remain separate from these observations.

## Limits

No subjective listening claim covers musical quality, repetitive feel, loop seams, or speaker balance. The selected source is a dedicated pirate orchestral composition, not the film theme. Screen captures establish visual rendering. Device input and playback reports provide separate functional evidence.
