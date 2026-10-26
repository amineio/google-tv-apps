# Original sound sources

Run `python3 apps/chess-math/tools/generate_audio.py` from the repository root.
The script uses Python's standard library. It needs no network access or package.
It recreates the four mono 44.1 kHz, 16-bit PCM WAV resources byte for byte.

Move and capture use damped wooden resonances with a seeded noise component.
Check uses a soft ascending pair of tones. Math success uses a warmer pair.
Each clip starts with a short attack and ends with a fade. No sample clips.
The four files total 66,324 bytes, including WAV headers.

These sounds were created for Schaak & Reken on 2026-10-26 and dedicated to
CC0 1.0 Universal. See the in-app notices. File checks establish format and
peak limits. Listening and SoundPool decoding still require the physical TV.
