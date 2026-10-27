# Original sound sources

Run `python3 apps/chess-math/tools/generate_audio.py` from the repository root.
The script uses Python's standard library. It needs no network access or packages.
It recreates four mono 44.1 kHz, 16-bit PCM WAV resources byte for byte.
The resources in `src/main/res/raw/` also serve as standalone listening previews.

These sounds were created for Schaak & Reken on 2026-10-26 and dedicated to
CC0 1.0 Universal. See the in-app notices. All waveforms and musical gestures
come from original synthesis. No brand clips, recordings, sample libraries,
or third-party music are included.

## Sound design

Move uses rounded wooden modes at 630 Hz, with a quieter 1040 Hz contact.
Capture uses two wooden knocks at 440 Hz and 740 Hz, 93 ms apart.
The second knock gives captures a clear sound, including captures that give check.
This capture sound replaces the original sound on 2026-10-27.
The wood modes have frequency ratios 1, 2.76, and 5.4. Each mode decays
separately. A soft attack replaces the previous noise burst.

Check uses G4 and C5, 105 ms apart. It provides a warm notice without an alarm.
Math success rises through C5, E5, G5, and C6 at 0, 85, 170, and 265 ms.
A quiet harmonic layer supports the final octave. The reward gets its energy
from the rising phrase, note overlap, and resolution.

Musical modes use ratios 1, 2, 3.01, and 4.02, with decreasing amplitudes
and shorter upper-mode decays. All cues include a quiet half-frequency mode.
The script records every frequency, amplitude, onset, envelope, and decay.
Dark reflections at 19, 37, and 61 ms add a small sense of space.
There is no feedback reverb. Cosine fades and 2 ms silent boundaries prevent
abrupt sample edges. A DC removal step removes the constant signal offset.

## Playback and measured export

The full WAV lengths include the reflection tails. `EffectPlayer` uses these
lengths before it releases the stream and audio focus. Its existing 15 ms
cleanup margin remains. Priorities and focus handling remain unchanged.

| Resource | Duration | Bytes | Peak dBFS | RMS dBFS | Playback gain |
| --- | ---: | ---: | ---: | ---: | ---: |
| `wood_move.wav` | 160 ms | 14,156 | -4.44 | -19.89 | 0.50 |
| `wood_capture.wav` | 320 ms | 28,268 | -4.15 | -19.39 | 0.50 |
| `gentle_check.wav` | 420 ms | 37,088 | -6.20 | -17.69 | 0.42 |
| `math_success.wav` | 680 ms | 60,020 | -5.04 | -17.12 | 0.46 |

The four files total 139,532 bytes, including WAV headers. dBFS measures level
relative to the largest digital sample. RMS measures average signal energy.
Different cue lengths affect RMS, so these values do not establish equal
perceived loudness. Conservative gains keep the longer reward comfortable.

The generator reads every exported file back. It asserts PCM format, peak
headroom, low DC offset, silent boundaries, bounded sample steps, and the
250,000-byte budget. These checks establish signal properties. Listening on
the physical TV remains necessary to assess tone and perceived loudness.
