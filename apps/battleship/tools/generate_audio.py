#!/usr/bin/env python3
"""Create original Battleship effects with Python's standard library.

From the repository root:
    python3 apps/battleship/tools/generate_audio.py
    python3 apps/battleship/tools/generate_audio.py --check

The original score and synthesis use the repository MIT license.
Version 1.5 battle cues also use CC0 recordings with source records.
"""

from __future__ import annotations

import argparse
import hashlib
import io
import json
import math
from pathlib import Path
import struct
import wave


RATE = 22050
TAU = 2.0 * math.pi
ROOT = Path(__file__).resolve().parents[3]
RAW = ROOT / "apps/battleship/src/main/res/raw"
MANIFEST = ROOT / "docs/battleship/audio-polish-assets.json"
PEAK = 0.82
IMPACTS = {
    "impact": {
        "duration": 0.44,
        "seed": 2402,
        "resonances": ((119.0, 0.30, 14.0), (229.0, 0.18, 17.0), (387.0, 0.08, 23.0)),
        "rumbleDecay": 11.0,
        "debrisDecay": 9.0,
    },
    "damage": {
        "duration": 0.58,
        "seed": 2403,
        "resonances": ((83.0, 0.34, 8.0), (137.0, 0.19, 11.0), (211.0, 0.11, 15.0)),
        "rumbleDecay": 11.0,
        "debrisDecay": 9.0,
    },
    "sunk": {
        "duration": 0.68,
        "seed": 2404,
        "resonances": ((65.0, 0.38, 5.8), (103.0, 0.22, 8.0), (173.0, 0.12, 13.0)),
        "rumbleDecay": 7.0,
        "debrisDecay": 5.0,
    },
}


class Noise:
    """Fixed 32-bit xorshift sequence, independent of Python's random module."""

    def __init__(self, seed: int) -> None:
        self.state = seed

    def sample(self) -> float:
        value = self.state
        value ^= (value << 13) & 0xFFFFFFFF
        value ^= value >> 17
        value ^= (value << 5) & 0xFFFFFFFF
        self.state = value & 0xFFFFFFFF
        return self.state / 2147483647.5 - 1.0


def smooth(value: float) -> float:
    value = min(1.0, max(0.0, value))
    return value * value * (3.0 - 2.0 * value)


def add_at(track: list[float], sound: list[float], start: float, gain: float = 1.0) -> None:
    offset = round(start * RATE)
    for index in range(min(len(sound), len(track) - offset)):
        track[offset + index] += sound[index] * gain


def low_drum(duration: float, strength: float, seed: int) -> list[float]:
    """Soft mallet attack and falling resonant drum modes."""
    noise = Noise(seed)
    sound = []
    phase = 0.0
    low = 0.0
    for index in range(round(duration * RATE)):
        t = index / RATE
        phase += TAU * (56.0 + 69.0 * math.exp(-t * 31.0)) / RATE
        low += 0.22 * (noise.sample() - low)
        body = math.sin(phase) + 0.34 * math.sin(phase * 1.61)
        head = low * math.exp(-t * 78.0)
        envelope = smooth(t / 0.003) * math.exp(-t * 12.0)
        sound.append(strength * envelope * (body * 0.62 + head * 0.5))
    return sound


def snare(duration: float, strength: float, seed: int) -> list[float]:
    """Band-shaped wire noise over two quiet drumhead modes."""
    noise = Noise(seed)
    sound = []
    low = 0.0
    high = 0.0
    for index in range(round(duration * RATE)):
        t = index / RATE
        sample = noise.sample()
        low += 0.10 * (sample - low)
        high += 0.64 * (sample - high)
        wire = high - low
        head = 0.12 * math.sin(TAU * 177.0 * t) + 0.07 * math.sin(TAU * 289.0 * t)
        envelope = smooth(t / 0.002) * math.exp(-t * 26.0)
        sound.append(strength * envelope * (wire * 0.75 + head))
    return sound


def brass(frequency: float, duration: float, strength: float, seed: int) -> list[float]:
    """Rounded brass with a breathed attack and softer upper partials on release."""
    noise = Noise(seed)
    sound = []
    phase = 0.0
    breath = 0.0
    partials = (1.0, 0.46, 0.25, 0.13, 0.075, 0.035)
    for index in range(round(duration * RATE)):
        t = index / RATE
        attack = smooth(t / 0.035)
        release = smooth((duration - t) / min(0.15, duration * 0.45))
        # Very small pitch settling and slow movement avoid a static oscillator.
        pitch = frequency * (1.0 - 0.004 * math.exp(-t * 48.0) + 0.0015 * math.sin(TAU * 4.6 * t))
        phase += TAU * pitch / RATE
        brightness = 0.72 + 0.28 * math.exp(-t * 6.0)
        tone = sum(
            amplitude * (brightness ** harmonic) * math.sin(phase * harmonic + harmonic * 0.045)
            for harmonic, amplitude in enumerate(partials, 1)
        )
        breath += 0.18 * (noise.sample() - breath)
        envelope = attack * release * (0.88 + 0.12 * math.exp(-t * 8.0))
        sound.append(strength * envelope * (0.62 * tone + breath * 0.022))
    return sound


def ambience(track: list[float], taps: tuple[tuple[float, float], ...]) -> None:
    """Quiet early reflections, without recursive echoes or a long tail."""
    dry = track.copy()
    for delay, gain in taps:
        add_at(track, dry, delay, gain)


def impact(name: str) -> list[float]:
    spec = IMPACTS[name]
    noise = Noise(spec["seed"])
    track = [0.0] * round(spec["duration"] * RATE)
    low = 0.0
    dark = 0.0
    rumble_phase = 0.0
    for index in range(len(track)):
        t = index / RATE
        white = noise.sample()
        low += 0.19 * (white - low)
        dark += 0.045 * (white - dark)
        ring = sum(gain * math.sin(TAU * hz * t) * math.exp(-decay * t) for hz, gain, decay in spec["resonances"])
        rumble_phase += TAU * (40.0 + 39.0 * math.exp(-t * 24.0)) / RATE
        rumble = math.sin(rumble_phase) * math.exp(-t * spec["rumbleDecay"])
        burst = (low * 1.75 + (white - low) * 0.13) * math.exp(-t * 29.0)
        debris = dark * math.exp(-t * spec["debrisDecay"])
        track[index] = smooth(t / 0.003) * (burst + ring + rumble * 0.30 + debris * 0.8)
    if name == "damage":
        # The short second hull creak makes friendly damage recognizable.
        add_at(track, brass(164.81, 0.23, 0.075, 2451), 0.11)
    if name == "sunk":
        add_at(track, low_drum(0.35, 0.31, 2452), 0.095)
        add_at(track, snare(0.16, 0.15, 2453), 0.17)
    ambience(track, ((0.038, 0.10), (0.081, 0.055)))
    return track


SCORES = {
    "victory": {
        "duration": 2.30,
        "style": "Original bright major naval brass fanfare with snare pickup and low drum accents.",
        "notes": [
            [0.12, "G4", 392.00, 0.17, 0.34],
            [0.30, "C5", 523.25, 0.17, 0.36],
            [0.48, "E5", 659.25, 0.20, 0.37],
            [0.70, "G5", 783.99, 0.29, 0.36],
            [1.03, "E5", 659.25, 0.16, 0.30],
            [1.21, "G5", 783.99, 0.19, 0.33],
            [1.43, "C5", 523.25, 0.64, 0.36],
            [1.43, "E4", 329.63, 0.65, 0.15],
            [1.43, "G4", 392.00, 0.65, 0.16],
            [1.43, "C4", 261.63, 0.66, 0.19],
        ],
        "snare": [[0.00, 0.22], [0.065, 0.17], [0.12, 0.27], [0.48, 0.22], [0.70, 0.26], [1.21, 0.18], [1.43, 0.31]],
        "drum": [[0.12, 0.23], [0.70, 0.21], [1.43, 0.32]],
    },
    "defeat": {
        "duration": 2.05,
        "style": "Original restrained descending minor bugle phrase with muted low drum, without speech or sirens.",
        "notes": [
            [0.06, "E4", 329.63, 0.31, 0.35],
            [0.40, "C4", 261.63, 0.31, 0.34],
            [0.74, "A3", 220.00, 0.39, 0.33],
            [1.17, "E3", 164.81, 0.64, 0.32],
            [1.17, "A2", 110.00, 0.65, 0.12],
        ],
        "snare": [[0.00, 0.11], [0.74, 0.09]],
        "drum": [[0.06, 0.23], [0.74, 0.20], [1.17, 0.24]],
    },
}


def result_fanfare(name: str) -> list[float]:
    score = SCORES[name]
    track = [0.0] * round(score["duration"] * RATE)
    seed = 2800 if name == "victory" else 2900
    for index, (start, _note, hz, duration, strength) in enumerate(score["notes"]):
        add_at(track, brass(hz, duration, strength, seed + index), start)
        # A quiet detuned voice rounds the held chord without a sawtooth wave.
        if duration > 0.5:
            add_at(track, brass(hz * 0.9985, duration, strength * 0.16, seed + 30 + index), start + 0.007)
    for index, (start, strength) in enumerate(score["snare"]):
        add_at(track, snare(0.16, strength, seed + 60 + index), start)
    for index, (start, strength) in enumerate(score["drum"]):
        add_at(track, low_drum(0.33, strength, seed + 80 + index), start)
    ambience(track, ((0.041, 0.12), (0.089, 0.08), (0.151, 0.045)))
    return track


def encode(samples: list[float]) -> tuple[bytes, dict]:
    # The slow release and common final fade suppress clicks at track boundaries.
    for index in range(len(samples)):
        t = index / RATE
        remaining = (len(samples) - 1 - index) / RATE
        samples[index] *= smooth(t / 0.003) * smooth(remaining / 0.055)
    # Remove any DC component through a single-pole 20 Hz high-pass filter.
    coefficient = math.exp(-TAU * 20.0 / RATE)
    previous_input = 0.0
    previous_output = 0.0
    for index, sample in enumerate(samples):
        filtered = coefficient * (previous_output + sample - previous_input)
        previous_input, previous_output = sample, filtered
        samples[index] = filtered
    # The high-pass memory needs a final small fade as well.
    for index in range(max(0, len(samples) - round(0.015 * RATE)), len(samples)):
        samples[index] *= smooth((len(samples) - 1 - index) / (0.015 * RATE))
    gain = PEAK / max(abs(sample) for sample in samples)
    pcm = [round(sample * gain * 32767) for sample in samples]
    if any(abs(value) >= 32767 for value in pcm):
        raise ValueError("Generated audio clips")
    output = io.BytesIO()
    with wave.open(output, "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(RATE)
        wav.writeframes(struct.pack(f"<{len(pcm)}h", *pcm))
    data = output.getvalue()
    rms = math.sqrt(sum((sample / 32768.0) ** 2 for sample in pcm) / len(pcm))
    return data, {
        "durationSeconds": round(len(pcm) / RATE, 6),
        "sampleRateHz": RATE,
        "channels": 1,
        "bitsPerSample": 16,
        "encoding": "PCM signed little-endian WAV",
        "peak": round(max(abs(sample) for sample in pcm) / 32768.0, 6),
        "rms": round(rms, 6),
        "rmsDbfs": round(20.0 * math.log10(rms), 2),
        "decodedBytes": len(pcm) * 2,
        "fileBytes": len(data),
        "sha256": hashlib.sha256(data).hexdigest(),
        "firstSample": pcm[0],
        "lastSample": pcm[-1],
    }


def generate() -> dict[Path, bytes]:
    sounds = {
        "impact": impact("impact"),
        "damage": impact("damage"),
        "sunk": impact("sunk"),
        "victory": result_fanfare("victory"),
        "defeat": result_fanfare("defeat"),
    }
    descriptions = {
        "impact": "Enemy hit: short cannon impact, steel resonances, and filtered debris.",
        "damage": "Friendly hit: lower hull thud, metallic decay, and a brief lower brass creak.",
        "sunk": "Ship sunk: deeper impact with a second drum accent and a longer debris decay.",
        "victory": SCORES["victory"]["style"],
        "defeat": SCORES["defeat"]["style"],
    }
    manifest = {
        "origin": "Current victory and defeat cues use original deterministic local synthesis. Historical version 1.1 battle recipes remain below for reference.",
        "source": "apps/battleship/tools/generate_audio.py",
        "generationCommand": "python3 apps/battleship/tools/generate_audio.py",
        "comparisonCommand": "python3 apps/battleship/tools/generate_audio.py --check",
        "references": "User feedback: stronger impact on the friendly fleet, an enthusiastic military victory sound, and a somber military defeat sound.",
        "rights": "The current victory and defeat cues and historical synthesis recipes use the repository MIT license. Current recorded battle cues use separate CC0 sources documented in sprite-polish/audio-assets.json.",
        "licensePath": "LICENSE",
        "sourceNotes": "Only assets.victory and assets.defeat describe current bundled files. Historical battle entries describe version 1.1 bytes, not the current files at those paths. Version 1.5 replaces fire, splash, impact, damage, and sunk; sprite-polish/audio-assets.json is authoritative for those cues.",
        "currentBattleManifest": "docs/battleship/sprite-polish/audio-assets.json",
        "historicalBattleAssetsVersion1_1": {},
        "commonParameters": {
            "sampleRateHz": RATE,
            "peakTarget": PEAK,
            "noise": "Fixed xorshift32 sequence with a separate seed for each voice.",
            "brassPartials": [1.0, 0.46, 0.25, 0.13, 0.075, 0.035],
            "brassAttackSeconds": 0.035,
            "brassReleaseSecondsMaximum": 0.15,
            "brassVibratoHz": 4.6,
            "brassVibratoFraction": 0.0015,
            "highPassHz": 20.0,
            "endFadeSeconds": 0.055,
            "postFilterFadeSeconds": 0.015,
            "finalSample": 0,
            "reverb": "Quiet finite early reflections only. No recursive echo.",
        },
        "scores": {
            name: {
                "noteFields": ["startSeconds", "pitch", "frequencyHz", "durationSeconds", "gainBeforeNormalization"],
                "notes": score["notes"],
                "percussionFields": ["startSeconds", "gainBeforeNormalization"],
                "snare": score["snare"],
                "lowDrum": score["drum"],
            }
            for name, score in SCORES.items()
        },
        "impactParameters": {
            "resonanceFields": ["frequencyHz", "gainBeforeNormalization", "exponentialDecayPerSecond"],
            "cues": IMPACTS,
            "attackSeconds": 0.003,
            "burstDecayPerSecond": 29.0,
            "rumbleFrequencyHz": "40 + 39 * exp(-24 * t)",
            "reflectionTaps": [[0.038, 0.10], [0.081, 0.055]],
            "friendlyAccent": {"frequencyHz": 164.81, "startSeconds": 0.11, "durationSeconds": 0.23, "gain": 0.075, "seed": 2451},
            "sunkDrumAccent": {"startSeconds": 0.095, "durationSeconds": 0.35, "gain": 0.31, "seed": 2452},
            "sunkSnareAccent": {"startSeconds": 0.17, "durationSeconds": 0.16, "gain": 0.15, "seed": 2453},
        },
        "scoreParameters": {
            "baseNoiseSeeds": {"victory": 2800, "defeat": 2900},
            "reflectionTaps": [[0.041, 0.12], [0.089, 0.08], [0.151, 0.045]],
            "heldNoteDouble": {"frequencyRatio": 0.9985, "relativeGain": 0.16, "delaySeconds": 0.007},
            "snare": {"durationSeconds": 0.16, "noiseFilterCoefficients": [0.10, 0.64], "drumheadFrequenciesHz": [177.0, 289.0], "decayPerSecond": 26.0},
            "lowDrum": {"durationSeconds": 0.33, "frequencyHz": "56 + 69 * exp(-31 * t)", "secondModeRatio": 1.61, "decayPerSecond": 12.0},
        },
        "assets": {},
        "review": "PCM format, reproducibility, finite duration, peak/RMS, zero endpoints, and SoundPool decoded-size limits are measured locally. Listening through TV speakers remains separate.",
    }
    files = {}
    for name, samples in sounds.items():
        encoded, measured = encode(samples)
        path = RAW / f"{name}.wav"
        files[path] = encoded
        entry = {
            "path": str(path.relative_to(ROOT)),
            "description": descriptions[name],
            **measured,
        }
        if name in IMPACTS:
            entry["historicalPath"] = entry.pop("path")
            entry["status"] = "Historical version 1.1 recipe; superseded by sprite-polish/audio-assets.json."
            manifest["historicalBattleAssetsVersion1_1"][name] = entry
        else:
            manifest["assets"][name] = entry
    files[MANIFEST] = (json.dumps(manifest, indent=2) + "\n").encode("utf-8")
    return files


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Compare generated bytes with bundled files without changing files.")
    args = parser.parse_args()
    files = generate()
    # Keep the original score record, then replace the battle cues with the
    # current source-recorded mix. Both paths are deterministic and offline.
    from prepare_scene_audio import generate as generate_scene_audio
    files.update(generate_scene_audio())
    different = []
    for path, data in files.items():
        if args.check:
            if not path.exists() or path.read_bytes() != data:
                different.append(str(path.relative_to(ROOT)))
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)
        print(f"{path.relative_to(ROOT)}: {len(data)} bytes")
    if different:
        print("Generated files differ: " + ", ".join(different))
        return 1
    print("All generated files match." if args.check else "Wrote current WAV files and their asset records.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
