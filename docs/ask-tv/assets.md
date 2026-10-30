# AI Chat artwork

All three launcher resources belong to AI Chat. The launcher controls title contrast, focus, and cropping.

## Generated launcher background

- Source: built-in ImageGen output `exec-f606f53b-9e87-4c43-abe8-b30ad68cf774.png`, generated for this project on 2026-10-29. Source dimensions: 1672x941.
- External reference: none. The original text-free Ask TV background is reused unchanged for AI Chat.
- Prompt: “Text-free 16:9 Google TV launcher background for Ask TV. Near-black #0D0E12 dominates, one abstract amber circular light/eclipse form at rightmost third upper middle, thin golden rim restrained soft glow, left half and lower quarter quiet near-black for native launcher controls and clock. Minimal smooth low-noise ambient artwork, no stars/grain/landscape/mic/text/logos/buttons/UI.”
- Preparation: `cwebp -q 85 -resize 1920 1080 SOURCE.png -o ask_hero.webp`.
- Bundled size: 1920 × 1080 WebP. Metadata: `com.amine.familyhome.HERO_IMAGE` → `@drawable/ask_hero`.
- Rights: AI-generated project artwork. No external image source or exclusive-rights claim.

## Native symbol and rendered banner

- Icon: original Android vector chat symbol, 512 × 512 viewport. Source: `ask_icon.xml`. Repository MIT license.
- Banner: separate 640 × 360 WebP, rendered in code with two outlined chat boxes and the native title “AI Chat”. No generated interface text.
- Code-render brief: near-black 640 × 360 field; small gold double-chat outline at left; white AI Chat title at right; generous safe margins; no focus ring.
- Render source: [`render-banner.sh`](render-banner.sh). The version 0.2 correction keeps the composition and uses a brighter semibold title.
- Banner render: ImageMagick `-size 640x360 xc:#0D0E12`, unchanged gold chat outlines, Outfit Semibold (`wght=600`) at 72 px, white `#FFFFFF` title at `+258+207`; `cwebp -q 90`.
- Variable font handling: ImageMagick `-weight 600` produced the same thin glyphs as the default font. FontTools therefore creates a static semibold instance in a temporary directory. The script deletes that instance after rendering. No extra font or dependency is bundled in the app.
- Render prerequisites: ImageMagick (`magick`), WebP tools (`cwebp`), and Python 3 with FontTools. The command below uses `uv` to supply the pinned build-time package.
- Reproduce with the render-time tool: `uv run --no-project --with fonttools==4.68.0 bash docs/ask-tv/render-banner.sh`.
- Font: Outfit copied from Family Home. Its SIL Open Font License is preserved in `apps/ask-tv/FONT-LICENSE-OFL.txt`. The app sets explicit variable font weights.

## Hashes

| File | Bytes | SHA-256 |
| --- | ---: | --- |
| `ask_hero.webp` | 16552 | `ab80756b1b5f5d9f374108ee60f397683ba9075df62daf21fda2b16ea0317c6a` |
| `ask_banner.webp` | 4434 | `ef61438d1bf9245e63bd6d1ef2f4fb0a81e5860dbe05e3694e8f498012261528` |
| `ask_icon.xml` | 839 | `c55cf3ecbc8b5674b4d594d1cbdc3064c3f04ae64a69240f3ad2992a47a4e828` |
| `outfit.ttf` | 110884 | `fc7287273e66929776e2ba54f144fe699080bec29f61bf649d70d871468aeade` |

Generated source SHA-256: `e5828d644fbde4050310d6bb93974e78267e721151f7505dab2e054ee0ba573b`.

Device and account captures are private review evidence and must not be committed.
