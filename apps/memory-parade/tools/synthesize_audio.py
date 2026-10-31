#!/usr/bin/env python3
"""Create original layered mono effects. Use --preview-dir to make an audition WAV."""
import argparse
import hashlib
import json
import math
from pathlib import Path
import random
import struct
import wave

RATE = 44100
ROOT = Path(__file__).resolve().parents[1] / "src/main/res/raw"
TAU = 2 * math.pi
NOTES = {"C4": 261.625565, "E4": 329.627557, "G4": 391.995436,
         "C5": 523.251131, "E5": 659.255114, "G5": 783.990872,
         "C6": 1046.502261, "E6": 1318.510228}
SPECS = {
    "navigate": dict(kind="wood", duration=.052, peak=.16, frequency=465, seed=101),
    "navigate_2": dict(kind="wood", duration=.056, peak=.16, frequency=510, seed=102),
    "flip": dict(kind="flip", duration=.180, peak=.25, frequency=310, seed=201),
    "flip_2": dict(kind="flip", duration=.172, peak=.25, frequency=337, seed=202),
    "flip_3": dict(kind="flip", duration=.188, peak=.25, frequency=294, seed=203),
    "match": dict(kind="match", duration=.470, peak=.30, notes=["C5", "E5", "G5"], seed=301),
    "match_2": dict(kind="match", duration=.480, peak=.30, notes=["E5", "G5", "C6"], seed=302),
    "match_3": dict(kind="match", duration=.460, peak=.30, notes=["G4", "C5", "E5"], seed=303),
    "mismatch": dict(kind="mismatch", duration=.240, peak=.20, seed=401),
    "arrival": dict(kind="arrival", duration=.285, peak=.25, seed=501),
    "complete": dict(kind="complete", duration=1.350, peak=.35, seed=601),
    "confirm": dict(kind="confirm", duration=.220, peak=.25, seed=701),
}
GAINS = {"navigate": .30, "flip": .50, "match": .58, "mismatch": .42,
         "arrival": .46, "complete": .65, "confirm": .50}


def add_tone(out, start, duration, frequency, level, kind="wood"):
    """Decaying modal voices use separate phases and smooth attack/release edges."""
    first = round(start * RATE)
    count = min(round(duration * RATE), len(out) - first)
    modes = [(1, 1, 1), (2.67, .20, .55), (4.13, .055, .30)] if kind == "wood" else [
        (1, 1, 1), (2.002, .15, .65), (3.99, .035, .40)]
    for i in range(count):
        t = i / RATE
        edge = min(1, t / .004, (count - 1 - i) / (RATE * .025))
        value = sum(a * math.sin(TAU * frequency * ratio * t) *
                    math.exp(-t / (duration * .27 * decay)) for ratio, a, decay in modes)
        out[first + i] += level * value * max(0, edge)


def add_brush(out, start, duration, level, seed):
    """A difference of two low-pass noise bands makes a soft leaf/paper texture."""
    rng = random.Random(seed)
    first = round(start * RATE)
    count = min(round(duration * RATE), len(out) - first)
    fast = slow = 0.0
    for i in range(count):
        noise = rng.uniform(-1, 1)
        fast += .18 * (noise - fast)
        slow += .035 * (noise - slow)
        envelope = max(0, math.sin(math.pi * i / max(1, count - 1))) ** 1.5
        out[first + i] += level * (fast - slow) * envelope


def add_pad(out, start, duration, names, level):
    first = round(start * RATE)
    count = min(round(duration * RATE), len(out) - first)
    for i in range(count):
        t = i / RATE
        edge = max(0, min(1, t / .035, (count - 1 - i) / (RATE * .12)))
        value = sum(math.sin(TAU * NOTES[name] * t) +
                    .10 * math.sin(TAU * NOTES[name] * 2 * t) for name in names) / len(names)
        out[first + i] += level * value * edge * math.exp(-t / (duration * .55))


def render(spec):
    out = [0.0] * round(spec["duration"] * RATE)
    kind, seed = spec["kind"], spec["seed"]
    if kind == "wood":
        add_tone(out, 0, spec["duration"], spec["frequency"], .85)
        add_brush(out, 0, .022, .18, seed)
    elif kind == "flip":
        add_brush(out, 0, spec["duration"] * .85, .75, seed)
        add_tone(out, .042, .100, spec["frequency"], .24)
        add_tone(out, .080, .065, spec["frequency"] * 1.42, .045)
    elif kind == "match":
        add_pad(out, .005, spec["duration"] - .005, ["C4", "E4", "G4"], .14)
        for n, name in enumerate(spec["notes"]):
            add_tone(out, n * .070, spec["duration"] - n * .070, NOTES[name], .39, "bell")
        add_brush(out, .050, .230, .075, seed)
    elif kind == "mismatch":
        add_brush(out, .015, .175, .21, seed)
        add_tone(out, .005, .155, 355, .42)
        add_tone(out, .085, .150, 388, .24)
    elif kind == "arrival":
        add_brush(out, .005, .155, .30, seed)
        add_tone(out, .025, .210, NOTES["G5"], .28, "bell")
        add_tone(out, .095, .190, NOTES["C6"], .24, "bell")
    elif kind == "complete":
        add_pad(out, .010, 1.300, ["C4", "E4", "G4"], .21)
        for start, name in zip([0, .120, .240, .390, .550], ["C5", "E5", "G5", "C6", "E6"]):
            add_tone(out, start, min(.650, 1.350 - start), NOTES[name], .34, "bell")
        add_tone(out, .620, .650, NOTES["C5"], .19, "wood")
        add_tone(out, .620, .650, NOTES["G5"], .14, "bell")
        add_brush(out, .300, .750, .08, seed)
    elif kind == "confirm":
        add_tone(out, .002, .180, NOTES["C5"], .42)
        add_tone(out, .065, .155, NOTES["G5"], .28)
    # Remove DC, de-click the outer edges, then normalize to the designed peak.
    mean = sum(out) / len(out)
    for i in range(len(out)):
        fade = max(0, min(1, i / (RATE * .003), (len(out) - 1 - i) / (RATE * .012)))
        out[i] = (out[i] - mean) * fade
    gain = spec["peak"] / max(map(abs, out))
    return [round(value * gain * 32767) for value in out]


def write_wav(path, samples):
    path.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(path), "wb") as out:
        out.setparams((1, 2, RATE, 0, "NONE", "not compressed"))
        out.writeframes(struct.pack("<" + "h" * len(samples), *samples))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output-dir", type=Path, default=ROOT)
    parser.add_argument("--preview-dir", type=Path)
    parser.add_argument("--metadata", type=Path)
    args = parser.parse_args()
    records, audition, timeline = [], [0] * round(.4 * RATE), []
    for name, spec in SPECS.items():
        samples = render(spec)
        path = args.output_dir / (name + ".wav")
        write_wav(path, samples)
        event = name.split("_")[0]
        peak = max(map(abs, samples)) / 32768
        rms = math.sqrt(sum(s * s for s in samples) / len(samples)) / 32768
        records.append(dict(file=f"apps/memory-parade/src/main/res/raw/{path.name}",
                            parameters=spec, playback_gain=GAINS[event], sample_rate=RATE,
                            channels=1, sample_width_bytes=2, duration_seconds=len(samples) / RATE,
                            bytes=path.stat().st_size, sha256=hashlib.sha256(path.read_bytes()).hexdigest(),
                            peak_dbfs=round(20 * math.log10(peak), 2), rms_dbfs=round(20 * math.log10(rms), 2),
                            clipped_samples=sum(abs(s) >= 32767 for s in samples)))
        if args.preview_dir:
            # Auditions use the in-game gain, so navigation stays below rewards.
            preview = [round(s * GAINS[event]) for s in samples]
            write_wav(args.preview_dir / path.name, preview)
            timeline.append(dict(name=name, start_seconds=round(len(audition) / RATE, 3)))
            audition.extend(preview)
            audition.extend([0] * round(.65 * RATE))
    if args.preview_dir:
        write_wav(args.preview_dir / "audio-audition.wav", audition)
        (args.preview_dir / "audio-audition-timeline.json").write_text(json.dumps(timeline, indent=2) + "\n")
    if args.metadata:
        metadata = dict(date="2026-10-31", generator="Original deterministic Python additive/modal/noise synthesis",
                        source="apps/memory-parade/tools/synthesize_audio.py", sample_format="44.1 kHz mono PCM16 WAV",
                        rights="Original synthesized waveforms. No recordings, third-party samples, or generated voices. Repository MIT license.",
                        processing="DC removal, 3 ms entrance and 12 ms exit fades, per-effect peak normalization; no limiting or clipping.",
                        playback="Four independent channels. UI navigation has an 80 ms throttle. Rewards outrank UI. The 1.35-second finale clears other voices and rejects further audio until it ends. Stop/mute/release clear every voice and never replay queued effects.",
                        listening_limit="Source and numeric inspection do not establish audible quality. A local audition sequence is provided outside Git; TV listening remains separate evidence.",
                        reproduce="python3 apps/memory-parade/tools/synthesize_audio.py --metadata docs/memory-parade/audio-upgrade-2026-10-31.json --preview-dir /Users/amine/.codex/artifacts/memory-parade-upgrade-20261031",
                        assets=records)
        args.metadata.parent.mkdir(parents=True, exist_ok=True)
        args.metadata.write_text(json.dumps(metadata, indent=2) + "\n")
    print(f"Generated {len(records)} original effects; no clipped samples.")
    for record in records:
        print(f"{Path(record['file']).name}: {record['duration_seconds']:.3f}s, {record['peak_dbfs']:.2f} dBFS peak")


if __name__ == "__main__":
    main()
