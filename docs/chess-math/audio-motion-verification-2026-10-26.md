# Audio, motion, and launcher evidence

The parent approved the design proposal and requested a development agent and a separate ImageGen icon agent. A third agent independently reviewed the implementation. The app update retains the existing saved game and uses no new runtime dependency or permission.

## Delivered behavior

Normal moves and captures use original wooden cues. Check and correct math answers use gentle tonal cues. Moves slide for 180 or 220 milliseconds. Castling moves both pieces over 240 milliseconds. Captures, en passant, and promotion use before and after snapshots. The computer waits for landing before its reply. Interrupted slides snap to committed facts and do not replay.

Sound and motion default to enabled. Parents can change them independently. Pause includes immediate mute. Game resets retain these preferences. System animation Off also disables slides. The existing math success delay remains separate from audio.

ImageGen produced an original knight and plus symbol, an adaptive foreground, and a TV banner. Source images, prompts, and export details remain in [icon-v1](icon-v1/README.md). The final wide tile was inspected in the installed custom Home app. Home and Clock source and device configuration were not changed.

## Build and review

The initial new tests failed because the presentation APIs did not exist. The completed release passed all 65 unit tests, release lint, release assembly, and instrumentation compilation. Strict repository tests passed all eight cases.

Independent review found pause paths that did not immediately stop a cue, and a detached board that relied on the watchdog. The engineer fixed both. The final review found no unresolved material source issue. A later sampled-frame review found correct intermediate positions, clear artwork, and no duplicate or missing final piece. The reviewer did not listen to the physical speakers or watch the full recording.

## Real TV execution

The device is a Chromecast with Android API 34 at 1920 × 1080. The first device attempt encountered the TV screensaver and could not establish window focus. That run does not establish an app regression. After wakeup, all 24 selected cases passed in 53.474 seconds.

The final hardware-rendered release passed all 25 selected TV tests in 54.368 seconds. The tests use memory-only sessions and preserve actual progress. They cover ordered child and computer slides, repeated OK during motion, explicit pause, focus loss, real Home interruption, same-task Resume, and exactly one owed computer reply. They also cover disabled motion, detached views, remote preferences, promotions, geometry, math, and marker rendering.

The real Home case logged one saved White move, a paused controller, no presentation gate, and no active sound stream. Resume produced one Black reply without replaying White.

All four PCM assets decoded on the TV. SoundPool returned a nonzero stream for each cue with granted transient audio focus. Stop and close released streams and focus. This proves playback setup and execution. It does not establish physical speaker loudness, perceived quality, or audio-output latency.

## Motion measurement

The first measured software-rendered sample was slow. The implementation removed the board's forced software layer and retained the same Canvas operations. Unit, lint, drawing, and live TV tests passed after that change.

The test collected Window FrameMetrics TOTAL_DURATION values when the board was moving at callback delivery. These are small filtered samples. They do not establish sustained frame rate. No dropped metric reports means no callback reports were lost, not that every frame met its deadline.

| Drawing mode | Samples | Median ms | p95/max ms |
| --- | ---: | ---: | ---: |
| Forced software | 7 | 42.368 | 49.148 |
| Default hardware, comparison | 9 | 17.927 | 82.153 |
| Default hardware, final suite | 8 | 20.826 | 27.322 |

The hardware samples have lower medians. The first hardware sample has a worse tail. No uninterrupted 60 fps claim follows. The [1080p recording](evidence/audio-motion-v5/slides-tv.mp4) shows one White move and one Black reply. Original frames were inspected during both slides. The recording has no sound track. It includes test startup and teardown.

## Artifacts and limits

The original four mono PCM WAV resources total 66,324 bytes. Their deterministic generator and provenance are in the app tools directory. The three exported launcher image resources total 187,194 bytes.

The final v0.5 release APK measures 2,149,937 bytes. Its SHA-256 is `08b49089cb6e10525eac71ed8ef433201d84eb6170da20f9564ee1ca787c6152`. The chess app CI size limit increases from 2 MiB to 2.5 MiB for the generated launcher artwork and audio. Other app limits remain unchanged.

Evidence includes [playback and frame logs](evidence/audio-motion-v5/effects-log.txt), [the installed launcher tile](evidence/audio-motion-v5/launcher.png), and [the final board](evidence/audio-motion-v5/live-effects-after-reply.png). The temporary test package was removed. The reviewed release remains installed.

Assistant interruption, every standby/output-device combination, prolonged play, and every special-move animation on physical hardware remain outside this run. Pure tests cover special-move event snapshots and existing chess rules. Family listening remains necessary to judge comfort and preference.
