#!/usr/bin/env python3
"""Check bundled music, or reproduce it from the recorded local source masters."""

import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[3]
MANIFEST = ROOT / "docs/battleship/music-polish/assets.json"


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def check(assets):
    for asset in assets:
        path = ROOT / asset["path"]
        if digest(path) != asset["sha256"]:
            raise ValueError(f"Music differs from its recorded hash: {path}")
        probe = json.loads(subprocess.check_output([
            "ffprobe", "-v", "error", "-show_streams", "-show_format", "-of", "json", str(path)]))
        audio = probe["streams"][0]
        if (audio["codec_name"], int(audio["sample_rate"]), audio["channels"]) != ("vorbis", 44100, 2):
            raise ValueError(f"Unexpected music format: {path}")
        if abs(float(probe["format"]["duration"]) - asset["durationSeconds"]) > .05:
            raise ValueError(f"Unexpected music duration: {path}")
        print(f"{asset['role']}: hash, Vorbis stereo format, and duration match")


def reproduce(assets, source_dir):
    with tempfile.TemporaryDirectory(prefix="battleship-music-") as scratch:
        for asset in assets:
            source = source_dir / asset["source_file"]
            if digest(source) != asset["source_sha256"]:
                raise ValueError(f"Original source hash differs: {source}")
            pcm = Path(scratch) / f"{asset['role']}.wav"
            result = Path(scratch) / f"{asset['role']}.ogg"
            subprocess.run([
                "ffmpeg", "-v", "error", "-y", "-t", str(asset["durationSeconds"]), "-i", str(source),
                "-af", asset["filters"] + f",volume={asset['gainDb']}dB", "-ar", "44100", "-ac", "2",
                "-c:a", "pcm_s16le", "-map_metadata", "-1", str(pcm)], check=True)
            subprocess.run([
                "oggenc", "-Q", "-q", "4", "-s", str(asset["oggSerial"]),
                "-t", f"Battleship {asset['role']}", "-a", asset["artist"], "-c", "LICENSE=CC0-1.0",
                "-o", str(result), str(pcm)], check=True)
            if digest(result) != asset["sha256"]:
                raise ValueError(f"Reproduction differs for {asset['role']}; inspect source and tool versions before replacing assets")
            print(f"{asset['role']}: source-based reproduction matches bundled bytes")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Check bundled hashes, format, and duration.")
    parser.add_argument("--source-dir", type=Path, help="Reproduce files from original masters in this directory without changing bundled assets.")
    args = parser.parse_args()
    assets = json.loads(MANIFEST.read_text())["assets"]
    if args.source_dir:
        reproduce(assets, args.source_dir)
    check(assets)


if __name__ == "__main__":
    main()
