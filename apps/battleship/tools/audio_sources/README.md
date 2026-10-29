# Battle sound sources

These canonical recordings use CC0-1.0. The source pages permit redistribution and modification.
The current manifest records their URLs, creators, selected files, hashes, and conversion.
See `docs/battleship/sprite-polish/audio-assets.json` from the repository root.

`naval_explosion.wav` and `naval_launch.wav` derive from version 2 of Tiny Naval Battle Sounds Set by Iwan Gabovitch.
Its archive README identifies the replacement ExplosionMetal source as public domain.
`ocean_splash.wav` derives from Ocean splash by Thimras.
`water_bubbles.wav` derives from bubble_01.ogg by rubberduck.

Source dedication: https://creativecommons.org/publicdomain/zero/1.0/

The files contain mono PCM16 audio at 22050 Hz. The preparation script uses no network access.
Run `python3 apps/battleship/tools/generate_audio.py` from the repository root to rebuild the bundled cues.
