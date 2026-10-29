# Battleship music implementation plan

> For agentic workers: Use superpowers:subagent-driven-development.

**Goal:** Add orchestral menu, match-start, victory and defeat music.
**Architecture:** A finite playback policy drives one local MediaPlayer, separate from short SoundPool effects.
**Tech Stack:** Kotlin, native MediaPlayer/AudioManager, Ogg Vorbis, existing coroutines.
**Spec:** docs/superpowers/specs/2026-10-29-battleship-music-design.md

## Global Constraints

Keep minSdk26 and offline operation. No new runtime dependency or network permission. Coordinator owns all assets, builds, device input and Git. Existing sound control mutes all audio. Pause/background stops all audio without replaying one-shot music. Only new matches trigger opening music. Final sinking completes before result music. Use version1.6 code7. No changes to game rules, visual layout or persistence.

## Review Focus

Rapid state changes cannot revive stale prepared players. Menu page changes do not restart the loop. Muted or background activity stays silent. Continue/resume does not play the opening. Victory/defeat music starts once after sinking, then stops on exit.

### Task 1: Implement music routing and playback

**Owned files:** apps/battleship/src/main/kotlin/com/amine/battleship/audio/, MainActivity.kt, viewmodel/GameSession.kt for MATCH_START cue, related unit/Android tests and app version only. Coordinator owns raw files, audio generators and documentation.
**Asset interface:** R.raw.music_menu, music_start, music_victory, music_defeat; all Ogg Vorbis stereo. Remove victory/defeat from SoundPool mapping but retain SoundCue.VICTORY/DEFEAT event ordering. Coordinator deletes old raw victory.wav/defeat.wav.

- [ ] Read spec and current audio/session/lifecycle code.
- [ ] Add meaningful failing unit tests for music policy, one-shot starts, result handling, mute/background and stale callbacks before implementation. Notify coordinator for RED run. Do not build or operate devices yourself.
- [ ] Implement compact testable policy and native asynchronous MediaPlayer adapter. Ensure no stale preparation or completion can restart canceled music. Keep ordinary gameplay silent after intro. Use safe foreground/audio-focus handling and quiet fades/ducking. Avoid complex generalized audio frameworks.
- [ ] Add MATCH_START only in accepted start(), not Continue/Resume/canceled confirmation. Adapt sound tests if needed. Keep final result cue after2500ms sinking.
- [ ] Connect loaded state/sound control and Activity lifecycle. Preserve short effects and immediate interruption. Update version.
- [ ] Add emulator tests for decoding all four bundled resources and observable playback/lifecycle transitions, without asserting audible quality. Coordinator runs builds and tests.
- [ ] Write task report with changes, red/green evidence, limitations. No subagents, Git, builds or device commands.

### Task 2: Prepare music, review and deliver

Coordinator selects CC0 orchestral assets with specialist direction, preserves phrase/loop structure, masters local compressed files and records sources/hashes/levels. Run full app checks and emulator audio tests. Fix material findings from one fresh reviewer. Install finalAPK on physical TV, verify version/hash and foregroundmusicstate, then commit/push origin/main. Report subjective TV listening limits without claiming a listening review.
