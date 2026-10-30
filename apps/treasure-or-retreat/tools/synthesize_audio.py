"""Create original quiet mono PCM effects with deterministic additive synthesis."""
from pathlib import Path
import math, struct, wave
OUT = Path(__file__).resolve().parents[1] / 'src/main/res/raw'
RATE = 22050
# Notes in Hz, note spacing in seconds, duration, synthesis flavor.
SOUNDS = {
    'navigate': ([660], .04, .07, 'soft'),
    'confirm': ([523.25, 783.99], .065, .23, 'bell'),
    'chest_open': ([130.81, 196, 261.63], .07, .34, 'wood'),
    'coin': ([1046.5, 1318.5, 1568], .06, .38, 'bell'),
    'jewel': ([659.25, 987.77, 1318.5, 1975.5], .07, .5, 'bell'),
    'bank': ([523.25, 659.25, 783.99, 1046.5], .1, .68, 'bell'),
    'trap': ([220, 196, 164.81, 130.81], .08, .62, 'wood'),
    'expedition': ([392, 523.25, 659.25], .1, .55, 'soft'),
    'finale': ([523.25, 659.25, 783.99, 1046.5, 1318.5], .12, .95, 'bell'),
}
OUT.mkdir(parents=True, exist_ok=True)
for name, (notes, spacing, duration, flavor) in SOUNDS.items():
    data = []
    for i in range(round(RATE * duration)):
        t = i / RATE
        value = 0
        for j, freq in enumerate(notes):
            age = t - j * spacing
            if age < 0:
                continue
            attack = min(1, age / .008)
            env = attack * math.exp(-age * (9 if flavor == 'wood' else 7))
            tone = math.sin(2 * math.pi * freq * age)
            tone += .22 * math.sin(2 * math.pi * freq * 2 * age)
            if flavor == 'bell':
                tone += .12 * math.sin(2 * math.pi * freq * 3.01 * age)
            value += tone * env / max(1, len(notes) ** .5)
        tail = min(1, max(0, duration-t) / .04)
        data.append(int(max(-.22, min(.22, value * .14 * tail)) * 32767))
    with wave.open(str(OUT / f'{name}.wav'), 'wb') as f:
        f.setparams((1, 2, RATE, len(data), 'NONE', 'not compressed'))
        f.writeframes(struct.pack('<' + 'h' * len(data), *data))
