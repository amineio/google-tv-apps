#!/usr/bin/env python3
"""Prepare finite battle cues from bundled CC0 recordings and original synthesis."""

from __future__ import annotations

import hashlib
import io
import json
import math
from pathlib import Path
import random
import struct
import wave

ROOT = Path(__file__).resolve().parents[3]
SOURCES = Path(__file__).resolve().parent / "audio_sources"
RATE = 22050
RAW = ROOT / "apps/battleship/src/main/res/raw"
MANIFEST = ROOT / "docs/battleship/sprite-polish/audio-assets.json"


def recording(name: str) -> list[float]:
    with wave.open(str(SOURCES / f"{name}.wav"), "rb") as source:
        assert (source.getnchannels(), source.getsampwidth(), source.getframerate()) == (1, 2, RATE)
        data = source.readframes(source.getnframes())
    return [x / 32768 for x in struct.unpack(f"<{len(data)//2}h", data)]


def layer(track: list[float], samples: list[float], start: float, gain: float) -> None:
    offset = round(start * RATE)
    for i in range(min(len(samples), len(track) - offset)):
        track[offset + i] += samples[i] * gain


def finish(samples: list[float]) -> tuple[bytes, dict]:
    # A short attack prevents clicks without losing the impact transient.
    peak = max(abs(x) for x in samples) or 1
    gain = .72 / peak
    for i, value in enumerate(samples):
        fade = min(1, i / (RATE * .003), (len(samples) - 1 - i) / (RATE * .055))
        samples[i] = value * gain * max(0, fade)
    pcm = [round(max(-1, min(1, x)) * 32767) for x in samples]
    data = io.BytesIO()
    with wave.open(data, "wb") as target:
        target.setnchannels(1)
        target.setsampwidth(2)
        target.setframerate(RATE)
        target.writeframes(struct.pack(f"<{len(pcm)}h", *pcm))
    encoded = data.getvalue()
    return encoded, {"durationSeconds": len(pcm) / RATE, "sampleRateHz": RATE,
                     "channels": 1, "decodedBytes": len(pcm) * 2,
                     "peak": max(abs(x) for x in pcm) / 32768,
                     "rms": math.sqrt(sum((x / 32768) ** 2 for x in pcm) / len(pcm)),
                     "firstSample": pcm[0], "lastSample": pcm[-1],
                     "sha256": hashlib.sha256(encoded).hexdigest()}


def generate() -> dict[Path, bytes]:
    explosion = recording("naval_explosion")
    splash = recording("ocean_splash")
    bubbles = recording("water_bubbles")
    launch = recording("naval_launch")
    files = {}
    measurements = {}
    for name, duration in {"fire": .22, "splash": 1.1, "impact": 1.5, "damage": 1.5, "sunk": 2.5}.items():
        track = [0.0] * round(duration * RATE)
        if name == "fire":
            layer(track, launch, 0, .65)
        elif name == "splash":
            layer(track, splash, 0, 1)
        else:
            layer(track, explosion, 0, .80 if name == "sunk" else 1)
            # Quiet low body adds weight without a piercing tonal accent.
            for i in range(len(track)):
                t = i / RATE
                track[i] += .10 * math.sin(2 * math.pi * (56 * t + .8 * (1 - math.exp(-8*t)))) * math.exp(-4*t)
            if name == "damage":
                layer(track, explosion, .10, .14)
            if name == "sunk":
                noise = random.Random(291029)
                low = 0.0
                for i in range(round(.3 * RATE), round(1.95 * RATE)):
                    t = i / RATE
                    low += .20 * (noise.uniform(-1, 1) - low)
                    envelope = min(1, (t - .3) / .15) * min(1, (1.95 - t) / .4)
                    crackle = noise.uniform(-1, 1) if noise.random() < .012 else 0
                    track[i] += envelope * (.075 * low + .035 * crackle)
                layer(track, bubbles, 1.75, .38)
                layer(track, bubbles, 2.05, .16)
        encoded, measurement = finish(track)
        path = RAW / f"{name}.wav"
        files[path] = encoded
        measurements[name] = {"path": str(path.relative_to(ROOT)), **measurement}
    manifest = {
        "date": "2026-10-29", "generator": "apps/battleship/tools/prepare_scene_audio.py",
        "regeneration": "python3 apps/battleship/tools/generate_audio.py",
        "comparison": "python3 apps/battleship/tools/generate_audio.py --check",
        "rights": "Recorded sources are CC0-1.0. Original synthesis and edits use the repository MIT license. These terms permit redistribution of the bundled source and edited assets.",
        "sources": [
            {"files": ["naval_explosion.wav", "naval_launch.wav"], "creator": "Iwan Gabovitch (qubodup)", "url": "https://opengameart.org/content/tiny-naval-battle-sounds-set", "download": "https://opengameart.org/sites/default/files/qubodup-NavalBattleSoundSet-cc0_0.7z", "license": "CC0-1.0", "note": "Version 2 archive. Its README identifies replacement ExplosionMetal recordings with public-domain sources."},
            {"files": ["ocean_splash.wav"], "creator": "Thimras", "url": "https://opengameart.org/content/ocean-splash", "download": "https://opengameart.org/sites/default/files/cannon_miss_0.ogg", "license": "CC0-1.0"},
            {"files": ["water_bubbles.wav"], "creator": "rubberduck", "url": "https://opengameart.org/content/40-cc0-water-splash-slime-sfx", "download": "https://opengameart.org/sites/default/files/water-splash-slime-sfx.zip", "selectedFile": "bubble_01.ogg", "license": "CC0-1.0"},
        ],
        "preparation": "Canonical source WAVs are FFmpeg-decoded 22050Hz mono PCM16 with metadata removed. Generator mixes recordings with original low body and finite fire crackle. Cues use 3ms attacks, 55ms release fades, peak0.72, zero endpoints. Runtime gain remains independently controlled by SoundPool.",
        "assets": measurements,
        "reviewLimits": "Duration, PCM format, decoded memory, peaks, RMS, endpoints and reproducibility are measured. These do not establish listening quality or TV speaker balance. Physical listening remains separate.",
    }
    for source in manifest["sources"]:
        source["canonicalFiles"] = [{"path": str((SOURCES / n).relative_to(ROOT)), "sha256": hashlib.sha256((SOURCES / n).read_bytes()).hexdigest()} for n in source["files"]]
    files[MANIFEST] = (json.dumps(manifest, indent=2) + "\n").encode()
    return files
