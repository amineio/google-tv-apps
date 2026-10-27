#!/usr/bin/env python3
"""Original CC0 cues: deterministic, offline synthesis using only standard Python."""
import math
from pathlib import Path
import struct
import wave

RATE = 44100
ROOT = Path(__file__).resolve().parents[1] / 'src/main/res/raw'
TAU = 2 * math.pi
# Full resource lengths, including the quiet room tail. Keep EffectPlayer in sync.
DURATIONS = {'wood_move': .160, 'wood_capture': .320,
             'gentle_check': .420, 'math_success': .680}


def add_mallet(out, start, frequency, level, decay=.13, wooden=False):
    """A rounded striker excites individually damped, slightly inharmonic modes."""
    modes = ((1, 1, 1), (2.76, .24, .46), (5.4, .07, .25)) if wooden else (
        (1, 1, 1), (2, .23, .65), (3.01, .105, .43), (4.02, .045, .28))
    first = round(start * RATE)
    for i in range(first, len(out)):
        t = (i - first) / RATE
        # Raised-cosine attack prevents a hard click; wood has no noise burst.
        attack = .5 - .5 * math.cos(math.pi * min(1, t / (.003 if wooden else .007)))
        body = sum(weight * math.sin(TAU * frequency * ratio * t)
                   * math.exp(-t / (decay * damping))
                   for ratio, weight, damping in modes)
        # Soft sub-resonance gives small TV speakers a warm body without bass rumble.
        body += .13 * math.sin(TAU * frequency * .5 * t) * math.exp(-t / (decay * .65))
        out[i] += level * attack * body


def add_air(out, start, frequency, level, length):
    """A quiet, rounded harmonic layer sustains the reward's last note."""
    first = round(start * RATE)
    for i in range(first, min(len(out), first + round(length * RATE))):
        t = (i - first) / RATE
        envelope = math.sin(math.pi * t / length) ** 2
        out[i] += level * envelope * (math.sin(TAU * frequency * t)
                                     + .18 * math.sin(TAU * frequency * 2 * t))


def room(out):
    """Sparse, dark early reflections; no long or metallic feedback tail."""
    dry = out[:]
    for delay, level in ((.019, .11), (.037, .075), (.061, .045)):
        offset = round(delay * RATE)
        filtered = 0.0
        for i in range(offset, len(out)):
            filtered += .22 * (dry[i - offset] - filtered)
            out[i] += filtered * level


def finish(samples, peak):
    # Remove the tiny finite-wave DC offset before the boundary fade.
    mean = sum(samples) / len(samples)
    samples = [s - mean for s in samples]
    scale = peak / max(abs(s) for s in samples)
    pcm = []
    for i, sample in enumerate(samples):
        # Exact digital silence for 2 ms at either edge; 24 ms cosine tail fade.
        start = max(0, min(1, (i / RATE - .002) / .003))
        end = max(0, min(1, ((len(samples) - 1 - i) / RATE - .002) / .024))
        fade = (.5 - .5 * math.cos(math.pi * start)) * (.5 - .5 * math.cos(math.pi * end))
        pcm.append(round(sample * scale * fade * 32767))
    return pcm


def render(name):
    out = [0.0] * round(RATE * DURATIONS[name])
    if name == 'wood_move':
        add_mallet(out, .002, 630, .8, .025, wooden=True)
        add_mallet(out, .008, 1040, .16, .014, wooden=True)
        peak = .60
    elif name == 'wood_capture':
        # Two separated wooden knocks distinguish a capture from the single move tap.
        add_mallet(out, .002, 440, .68, .032, wooden=True)
        add_mallet(out, .095, 740, .72, .042, wooden=True)
        peak = .62
    elif name == 'gentle_check':
        add_mallet(out, .002, 392, .50, .083)
        add_mallet(out, .105, 523.251, .43, .090)
        peak = .49
    else:
        # Original C-major gesture: C5, E5, G5, C6. The final octave resolves softly.
        for start, frequency, level, decay in (
                (.002, 523.251, .43, .095), (.087, 659.255, .44, .105),
                (.172, 783.991, .46, .115), (.267, 1046.502, .44, .105)):
            add_mallet(out, start, frequency, level, decay)
        add_air(out, .245, 523.251, .060, .275)
        add_air(out, .265, 1046.502, .028, .230)
        peak = .56
    room(out)
    return finish(out, peak)


def write_and_inspect(name, pcm):
    path = ROOT / (name + '.wav')
    with wave.open(str(path), 'wb') as clip:
        clip.setparams((1, 2, RATE, 0, 'NONE', 'not compressed'))
        clip.writeframes(struct.pack('<' + 'h' * len(pcm), *pcm))
    # Read back the actual export, not only the in-memory signal.
    with wave.open(str(path), 'rb') as clip:
        assert (clip.getnchannels(), clip.getsampwidth(), clip.getframerate()) == (1, 2, RATE)
        assert clip.getcomptype() == 'NONE'
        actual = struct.unpack('<' + 'h' * clip.getnframes(), clip.readframes(clip.getnframes()))
    peak = max(abs(s) for s in actual) / 32768
    rms = math.sqrt(sum(s * s for s in actual) / len(actual)) / 32768
    assert peak < .65, (name, 'peak headroom')
    assert all(s == 0 for s in actual[:88] + actual[-88:]), (name, 'silent boundaries')
    assert abs(sum(actual) / len(actual) / 32768) < .001, (name, 'DC offset')
    assert max(abs(b - a) for a, b in zip(actual, actual[1:])) < 8500, (name, 'sample discontinuity')
    print(f'{name}: {len(actual)/RATE:.3f}s, {path.stat().st_size} bytes, '
          f'peak {20*math.log10(peak):.2f} dBFS, RMS {20*math.log10(rms):.2f} dBFS; boundaries silent')
    return path.stat().st_size


def main():
    ROOT.mkdir(parents=True, exist_ok=True)
    total = sum(write_and_inspect(name, render(name)) for name in DURATIONS)
    assert total <= 250_000
    print(f'Total: {total} bytes. Original synthesis; no external samples or dependencies.')


if __name__ == '__main__':
    main()
