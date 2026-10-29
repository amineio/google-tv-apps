# Battle music source research

Date: 2026-10-29. Primary composer upload pages only. Scores measure documented source fit, not listening quality. I did not audition these recordings. Source licenses describe the uploader grant, not independent clearance of all composition or sample rights.

## Recommendation

Use **Pirate Indenture**, by Eldritch Grim, for the first battle loop. The composer upload explicitly identifies an epic, fast pirate track, orchestra, battle, and drums. It supplies a dedicated loop in lossless WAV. This directly matches the requested adventurous naval mood. It does not require the film recording, title, characters, or melody.

The supplied loop lasts only 14.489252 seconds. Keep its level quiet and duck it under explosions. A short loop can become repetitive during a long match. Do not claim that it sounds like the film or that its transition is audibly seamless without listening.

Primary page: https://opengameart.org/content/pirate-indenture

Composer uploader: Eldritch Grim. Uploaded February 14, 2024. License: CC0, with attribution explicitly optional. License link on page: https://creativecommons.org/publicdomain/zero/1.0/

Direct loop: https://opengameart.org/sites/default/files/pirate_indenture_loop.wav

Direct complete track: https://opengameart.org/sites/default/files/pirate_indenture.wav

Downloaded research copy: /tmp/battleship-pirate-research/pirate_indenture_loop.wav

Measured with ffprobe: 14.489252 seconds, PCM signed 16-bit stereo, 44100 Hz, 2555982 bytes.

Original SHA-256: ae0d1c43a11575237e17923c56aa7f4eaeb91d7d57de738a478083c534007b96

## Ranked alternatives

| Rank | Track and composer | Score | Evidence and limit |
| --- | --- | --- | --- |
| 1 | Pirate Indenture, Eldritch Grim | 9/10 | Explicit pirate, orchestra, battle, drums, dedicated lossless loop. Short 14.49-second loop. |
| 2 | Last Journey, Crocdent | 8/10 | Composer says orchestral OST for Sail of Dreams. Dedicated 36.92-second Loop1 plus multiple transitions and endings. No lossless source supplied. |
| 3 | Pirate’s Orchestra, Dizzy Crow | 7.5/10 | Explicit strings, violins, drums, trombone, trumpet, accordion. Adventurous and silly description fits family play. No explicit loop and no lossless recording. |
| 4 | Pirate Adventure, Eldritch Grim | 7/10 | Explicit pirate, adventure, epic, orchestra, drums, fun. Lossless 61.99-second WAV, but no explicit loop, so do not loop blindly. |

All four individual source pages explicitly show CC0. None of these source pages identifies the film franchise as its source. This is source-page evidence, not a melodic originality comparison.

### Last Journey

Page: https://opengameart.org/content/last-journey

Direct loop archive: https://opengameart.org/sites/default/files/last_journey_loops.zip

Composer-uploaded FL Studio project: https://opengameart.org/sites/default/files/project_1.flp

The archive contains Vorbis Intro1 (25.846145s), Intro2 (40.615374s), Loop1 (36.923084s), Loop2 (14.769229s), Loop3 (12.923084s), Loop3End (18.461542s), and Transi2to3 (14.769229s). These durations were measured from downloaded files with ffprobe. This is the strongest fallback for a longer, varied game score. The uploader page says the music is for their own game. The page also contains an old reader comment about attribution, but the current grant is CC0 without a current extra notice.

### Pirate’s Orchestra

Page: https://opengameart.org/content/pirates-orchestra-208

Direct recording: https://opengameart.org/sites/default/files/Pirate%27s%20Orchestra.mp3

Direct MIDI: https://opengameart.org/sites/default/files/Pirates.mid

Duration in source title: 2:08. Not measured. A MIDI arrangement could be rendered with a documented soundfont, but that introduces extra instrument licensing and audio authoring work.

### Pirate Adventure

Page: https://opengameart.org/content/pirate-adventure

Direct recording: https://opengameart.org/sites/default/files/pirate_adventure.wav

Measured duration: 61.990839 seconds. Size: 10935228 bytes. The uploader describes an epic pirate quest. No supplied loop is listed.

## Integration guidance

Use one locally bundled Vorbis stream. Keep existing sound muting, activity lifecycle, and audio focus behavior. Start battle music after the opening. Restore it after Resume when sound and audio focus permit. Stop it on pause, results, background, and mute. A foreground restart resumes a background loop, but must not replay opening or result cues. Keep asset provenance and preparation hashes alongside the current music record.
