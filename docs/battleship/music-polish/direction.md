# Battleship music selection and design record

Updated 2026-10-29. Scope: music selection and primary-source verification. No app implementation, builds, device input, or audio audition performed by this research task. Fit scores below describe source-based suitability. They are not listening or quality ratings.

## Selected music

| Event | Source / creator | Selected treatment | Source-based fit |
| --- | --- | --- | --- |
| Main menu | Determined Pursuit (epic orchestra loop), Emma_MA | Keep the complete approximately 108-second composer loop. Parent supplied the measured duration. This replaces the initial 30–60 second excerpt proposal and avoids inventing a new loop seam. | 9/10. Composer explicitly describes loopable strings, horns, and drums. |
| New game | Determined Pursuit, Emma_MA | Parent-selected first six seconds, with a fade. The shared source preserves the menu's sound. This is an edited excerpt, not a verified complete musical phrase. | 8/10 for source and orchestration; cut quality unreviewed. |
| Victory | Victory Theme for RPG, cynicmusic | Parent-selected approximately 12-second opening, with a fade, from a 56.74-second source. Durations are parent supplied. This supersedes the initial 6–10 second target. | 8/10. The source explicitly targets victory. The composer warns that the full track's ending is awkward. Opening cut quality unreviewed. |
| Defeat | Game Over, Kistol | Use the complete short track, preserving its supplied ending. Prefer the FLAC master. Measure duration during asset preparation; file size is not a duration measurement. | 9/10. Source explicitly identifies sad strings and game-over use. Composer describes exaggerated tragedy, so confirm the intensity suits the app during listening review. |

The preferred defeat track is now **Game Over by Kistol**. Its explicit game-over purpose and sad-string tags provide stronger evidence than the title-based fit of Laments of the War. Keeping its complete source preserves the supplied ending; this does not establish that its cadence or production quality has been auditioned.

Use prerecorded orchestral music. These sources do not establish that a live orchestra recorded the tracks; sampled instruments may be used. Bundle rendered audio with no model runtime. Keep launch, victory, and defeat as one-shot cues. Keep music controlled by the existing sound setting and app lifecycle. No continuous gameplay score is selected here.

## Verified primary sources and downloads

### Determined Pursuit (epic orchestra loop)

- Creator: Emma_MA.
- Source: https://opengameart.org/content/determined-pursuit-epic-orchestra-loop
- WAV: https://opengameart.org/sites/default/files/determined_pursuit_loop.wav
- Verified HEAD: HTTP 200; 19,052,830 bytes; application/octet-stream.
- Page lists CC0. Creator explicitly states public-domain dedication in January 2017 and no required attribution.
- Composer identifies strings, horns, drums, and loopability. Preserve the full source loop without new internal edits.

### Victory Theme for RPG

- Creator: cynicmusic.
- Source: https://opengameart.org/content/victory-theme-for-rpg
- MP3: https://opengameart.org/sites/default/files/Victory1_1.mp3
- Verified HEAD: HTTP 200; 1,361,839 bytes; audio/mpeg.
- This individual upload lists CC0. The page also promotes other Pixelsphere music under separate permission language. Do not extend this upload's license to the whole soundtrack.
- Creator identifies a victory theme and warns about its ending. The selected opening avoids relying on that ending, but requires listening review for a natural fade.

### Game Over

- Creator: Kistol.
- Source: https://opengameart.org/content/game-over
- FLAC: https://opengameart.org/sites/default/files/Game%20Over.flac
- Verified HEAD: HTTP 200; 1,292,390 bytes; application/octet-stream.
- Primary page lists CC0, tags the music as game over, strings, and sad, and says credit is not required.
- Composer calls it an exaggerated, tragic game-over theme. The source also provides an OGG version; edit from the FLAC.
- A user-created collection named Non-Commercial Music appears on the page. That collection name does not replace the individual upload's explicit CC0 license.

## Editing and evidence limits

Record original source hashes, source URLs, creator names, licenses, retrieval date, output hashes, exact cut times, fade shapes, and encoding parameters. Decode the victory MP3 once. Prefer the WAV and FLAC masters for the other sources.

Keep the menu's complete loop and test repeated playback. Source loopability is verified; runtime gapless playback is not established by the source page. Listen for a gap, beat disruption, or reverb discontinuity over at least three loops.

The first-six-second launch and approximately 12-second victory edits are parent design selections. A fade prevents an abrupt waveform stop, but cannot establish musical phrase completion. Do not claim those cuts preserve complete phrases without audition. Keep the complete defeat ending and avoid truncating its decay.

Initial mastering targets: menu around -23 to -21 LUFS integrated; cues around -20 to -18 LUFS; true peak at or below -2 dBTP. These are proposed starting points, not verified TV loudness acceptance. Compare perceived loudness with existing impacts. Defeat can be slightly quieter than victory.

Required later listening review: pleasant menu repetition, clear launch transition, complete-sounding victory fade, distinct defeat emotion, no clicks or clipping, and comfortable TV volume. Source research alone does not prove these properties.

## Licenses

Repository code remains MIT. Imported music remains separately CC0 1.0. Do not relabel imported works as MIT. Keep a music source/credits file with the creator, title, source URL, license URL, and edit description. Optional credits are useful even when not required.

CC0 permits copying, modification, and distribution: https://creativecommons.org/publicdomain/zero/1.0/.

## Alternatives investigated, not selected

- A Legend Will Rise (Orchestral), CodeManu: CC0, alternate heroic launch/menu. Source https://opengameart.org/content/a-legend-will-rise-orchestral ; direct MP3 https://opengameart.org/sites/default/files/A%20Legend%20Will%20Rise.mp3 ; verified HTTP 200, 2,785,768 bytes. Loopability is not promised.
- Laments of the War, Cethiel: CC0 orchestral/war/choir/drum track. Its defeat fit was weak without listening, so Kistol's dedicated game-over track replaces it. Source https://opengameart.org/content/laments-of-the-war ; direct FLAC https://opengameart.org/sites/default/files/Lament%20of%20the%20War%20-%20Lossless%20FLAC.flac ; verified HTTP 200, 26,173,527 bytes.
- Won! (Orchestral winning jingle), spuispuin: primary page lists CC-BY 4.0, despite third-party CC0 claims. https://opengameart.org/content/won-orchestral-winning-jingle
- Military Shooter Music Pack, Augusto Mazzoli: coherent loops and stingers, but the retrieved composer page does not establish CC0. Free price does not establish license rights. https://augustomazzoli.com/index.php/military-shooter-music-pack/
