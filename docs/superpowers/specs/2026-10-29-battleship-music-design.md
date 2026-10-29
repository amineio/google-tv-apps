# Battleship music design

The user requests dramatic music on the menu and at match start, with better victory and defeat music. They authorize autonomous implementation. Use prerecorded orchestral CC0 works, bundled offline. The new request supersedes earlier no-background-music scope.

## Playback contract

Use one MediaPlayer music channel and the existing SoundPool for short battle effects. Keep minSdk26, package, rules, persistence and controls unchanged. No runtime dependency or network permission is needed. Music follows the existing sound control, and stops immediately on pause, background, mute and destruction.

Loop music on the loaded HOME screen and its Help/Settings pages when no paused game menu is active. Preserve the same menu loop while changing those pages. Fade in quietly. Start a separate finite dramatic cue only for a newly accepted match or confirmed restart/rematch. Continue, Resume, canceled confirmation and restored state must not replay the opening.

Keep ordinary gameplay quiet after the opening. Duck an active opening beneath battle effects. Pause and background discard the opening; do not resume it later. Results use distinct finite victory/defeat tracks after the full2500ms final sinking. Those tracks replace the previous synthetic fanfares. Do not loop result tracks or replay them because target/state/UI recomposes. Leaving results stops its music. App return does not replay old result/opening cues. Menu music can restart after app return.

Use asynchronous local preparation with stale callback protection. Release replaced, failed and destroyed players. Do not allow two music tracks to play together. Handle focus loss and ducking without restarting old one-shot cues. Preserve sound-effect cancellation and leave saved-match timing unchanged. Expose pure policy/controller seams for unit tests, not production-only test flags. Use ordinary native MediaPlayer lifecycle APIs; long music must not pass through SoundPool's decoded-size limit.

## Assets and levels

Coordinator supplies music_menu.ogg, music_start.ogg, music_victory.ogg, music_defeat.ogg in raw resources. Prefer a complete composer-authored loop for menu rather than arbitrary cuts. Keep start and result cues finite with musical arrivals and fades. Retain source URLs, author, CC0 terms, original hashes, edit decisions, durations and levels. Stream compressed stereo audio. Keep menu quieter than battle cues and do not mask missile, splash or explosion sounds.

## Validation and delivery

Add regression tests before implementation for menu stability, new-match opening once, no opening on continue/resume, result ordering, mute/background and rapid changes. Test native decoding/preparation of all four bundled tracks on the emulator and playback state/lifecycle transitions. Use a specialist audio design/source review and a fresh independent code review. Report measured decoding and levels separately from subjective listening. Build/test locally; install only the final version on the physical TV. After installation commit scoped app changes and push origin/main. Version1.6 code7.
