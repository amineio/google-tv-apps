# Independent review: Battleship 1.6 music

Reviewed 2026-10-29 against the current working tree based on `06a31ffe9678dca5108c7ff868cf0968e0b20287`.

## Final verdicts

- Specification compliance: PASS for the reviewed implementation and recorded asset requirements.
- Code quality: PASS. The original P2 finding is resolved. No outstanding material findings.
- Validation status: final debug/release unit suites, full emulator suite, builds, and release lint pass.

These verdicts do not establish subjective listening quality, gapless loop perception, musical phrase quality, physical TV balance, installation, or delivery.

## Original finding and correction

The first review found a P2 defect in the former `GameAudio` focus bookkeeping. A denied focus request or permanent focus loss left the stored request present. Later user actions could not request focus again while the app remained foreground with Sound enabled. This could leave all music and effects silent until an app-return or Sound-toggle cycle.

The correction introduces `AudioFocusController` and `NativeAudioFocusBackend`. Each native request has one lease. Denial and permanent loss clear the lease and invalidate its callbacks before abandonment. Transient loss retains its lease for the later gain callback. Repeated state updates do not retry. A later explicit SELECT/FIRE cue, or a foreground/sound-enable eligibility transition, can request focus again. Automatic impacts and results do not retry.

The scoped second review checked the controller, native adapter, `GameAudio` integration, six new focus regressions, and the worker's report. The correction resolves the original finding. Old gain/loss callbacks cannot change a newer lease. Focus recovery does not recreate a discarded opening or result track. Main-thread native callbacks preserve the controller's request/assignment ordering.

This approach follows the [Android audio-focus guidance](https://developer.android.com/media/optimize/audio-focus): permanent loss requires a new request, while transient loss can receive a later gain callback.

## Playback and asset review retained from the first pass

The reviewed implementation preserves the menu loop across Home, Help, and Settings. Only accepted new matches emit the opening. Continue and Resume do not replay it. Ordinary play is silent after the opening. Results remain finite and follow the full existing 2500 ms sinking delay. Replacement releases the previous player before preparing the next. Tokens reject stale callbacks. Mute, pause, background, and destruction stop playback. Short effects remain on SoundPool and duck the opening.

The current manifest and runtime gains agree. The four assets retain a complete 108-second menu loop and finite 6/12/15-second start/victory/defeat tracks. The first review's read-only asset check confirmed hashes, Vorbis stereo 44100 Hz format, and durations. Current source pages agreed with creator names and CC0 labels: [Determined Pursuit](https://opengameart.org/content/determined-pursuit-epic-orchestra-loop), [Victory Theme for RPG](https://opengameart.org/content/victory-theme-for-rpg), and [Game Over](https://opengameart.org/content/game-over). Credits and records preserve edits, source/output hashes, and terms. The historical generator record is not the current result music. Unchanged assets were not re-audited during the focus correction review.

## Validation evidence and limits

The coordinator reproduced the focus defect before its fix: six tests compiled, and four failed for denial recovery, permanent-loss recovery, stale callbacks, and recovery without opening replay. Evidence: `/tmp/battleship-focus-red.log`.

After the correction, the reviewer inspected the final log and current result XML. `/tmp/battleship-music-final-green.log` ends with `BUILD SUCCESSFUL in 2m 18s`. The XML confirms 69 debug unit tests, 69 release unit tests, and 18 emulator tests, with zero failures, errors, or skips. All six focused regressions pass. Debug, release, and Android test APK builds completed. Release lint reports zero errors and three existing warnings: two `AndroidGradlePluginVersion` warnings and one `DiscouragedApi` warning.

Before the focus correction, the final-level build and two native music tests passed. An earlier complete emulator run passed 18 tests. The completed full run after the correction now supplies final validation evidence.

This review made no production/test changes and ran no builds or device commands. Native tests establish decoding and playing/released state, not audibility. Physical playback and listening acceptance remain separate observations.
