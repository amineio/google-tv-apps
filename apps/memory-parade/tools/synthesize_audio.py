#!/usr/bin/env python3
"""Create original quiet mono effects with a smooth envelope. No external samples."""
import math
from pathlib import Path
import struct
import wave

ROOT = Path(__file__).resolve().parents[1] / 'src/main/res/raw'
ROOT.mkdir(parents=True, exist_ok=True)
RATE = 22050
SOUNDS = {
    'navigate': ([660, 880], .055, .12),
    'flip': ([420, 630, 840], .18, .18),
    'match': ([523.25, 659.25, 783.99, 1046.5], .42, .22),
    'mismatch': ([440, 392], .24, .15),
    'arrival': ([783.99, 1046.5], .22, .18),
    'complete': ([523.25, 659.25, 783.99, 1046.5, 1318.5, 1568], .95, .23),
    'confirm': ([523.25, 783.99], .17, .16),
}
for name, (notes, duration, peak) in SOUNDS.items():
    samples = []
    for i in range(round(RATE * duration)):
        t = i / RATE
        step = min(int(t / duration * len(notes)), len(notes)-1)
        local = (t / duration * len(notes)) % 1
        envelope = math.sin(math.pi * local) ** .7
        freq = notes[step]
        tone = .8 * math.sin(2 * math.pi * freq * t) + .15 * math.sin(2 * math.pi * freq * 2 * t)
        fade = min(1, t / .008, (duration-t) / .02)
        samples.append(int(32767 * peak * envelope * tone * fade))
    with wave.open(str(ROOT / (name + '.wav')), 'wb') as out:
        out.setparams((1, 2, RATE, 0, 'NONE', 'not compressed'))
        out.writeframes(struct.pack('<' + 'h'*len(samples), *samples))
