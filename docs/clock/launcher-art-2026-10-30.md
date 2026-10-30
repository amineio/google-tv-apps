# Clock launcher artwork, 30 October 2026

Clock 1.6 (version code 7) uses two Android vector drawables and a separate WebP background.
The navy field, pale ticks, and blue and white hands match the dashboard.
Each dial has 12 major hour ticks and flat-ended hands at 10:10.
The icon keeps its dial inside the central 70 percent for small-size recognition.
The banner retains the existing Clock title paths.
The text-free background places its dial on the upper right.
Its left half stays empty, and its lower quarter stays dark.
The uniform navy field removes the previous rectangular color seam.

| Resource | Nominal size | Viewport |
| --- | --- | --- |
| `clock_icon.xml` | 96 by 96 dp | 96 by 96 |
| `clock_banner.xml` | 192 by 108 dp | 320 by 180 |
| `clock_background.webp` | 1920 by 1080 pixels | SVG viewBox 320 by 180 |

Source: original native vector geometry, with the existing repository title paths.
The background source is `designs/clock/launcher-background.svg`.
Family Home uses ImageDecoder for the background, so it requires a raster resource.
No generated images, external artwork, or font files were added.
Prompt: none; deterministic native paths suit these simple clock symbols.
License: MIT, under the repository LICENSE.
Compression: WebP quality 90 for the background; not applicable to XML vector sources.
The stable resource names and HERO_IMAGE manifest registration remain unchanged.
Rendered launcher checks and packaging belong to the delivery validation record.

## Export commands

```sh
rsvg-convert -w 1920 -h 1080 designs/clock/launcher-background.svg -o /tmp/clock-launcher-background-20261030.png
cwebp -quiet -q 90 /tmp/clock-launcher-background-20261030.png -o apps/clock/src/main/res/drawable-nodpi/clock_background.webp
```

## Source SHA-256

- `clock_icon.xml`: `da8c7186565846165ece4a4981486f67319327daee1c4402cff7f40855974162`
- `clock_banner.xml`: `23ebb0ceb0a4647ad98918b96893686e07cea8278b282b9983e92fa6c324366c`
- `clock_background.webp`: `321b63da6586f6d88e2f8cc692ee942fd851f4e3221b9ebb547674add4c7bd52`
- `launcher-background.svg`: `a92256d37d0eaf76d4bb694ce9a45b92a1a9652d3b8a5fbed09f2864cce8e1a4`
