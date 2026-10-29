# Ask TV artwork

All three launcher resources belong to Ask TV. The launcher controls title contrast, focus, and cropping.

## Generated launcher background

- Source: built-in ImageGen output `exec-f606f53b-9e87-4c43-abe8-b30ad68cf774.png`, generated for this project on 2026-10-29. Source dimensions: 1672x941.
- External reference: none.
- Prompt: “Text-free 16:9 Google TV launcher background for Ask TV. Near-black #0D0E12 dominates, one abstract amber circular light/eclipse form at rightmost third upper middle, thin golden rim restrained soft glow, left half and lower quarter quiet near-black for native launcher controls and clock. Minimal smooth low-noise ambient artwork, no stars/grain/landscape/mic/text/logos/buttons/UI.”
- Preparation: `cwebp -q 85 -resize 1920 1080 SOURCE.png -o ask_hero.webp`.
- Bundled size: 1920 × 1080 WebP. Metadata: `com.amine.familyhome.HERO_IMAGE` → `@drawable/ask_hero`.
- Rights: AI-generated project artwork. No external image source or exclusive-rights claim.

## Native symbol and rendered banner

- Icon: original Android vector chat symbol, 512 × 512 viewport. Source: `ask_icon.xml`. Repository MIT license.
- Banner: separate 640 × 360 WebP, rendered in code with two outlined chat boxes and the native title “Ask TV”. No generated interface text.
- Code-render brief: near-black 640 × 360 field; small gold double-chat outline at left; ivory Ask TV title at right; generous safe margins; no focus ring.
- Banner render: ImageMagick `-size 640x360 xc:#0D0E12`, gold stroke chat outlines, Outfit at 72 px, ivory title at `+258+207`; `cwebp -q 90`.
- Font: Outfit copied from Family Home. Its SIL Open Font License is preserved in `apps/ask-tv/FONT-LICENSE-OFL.txt`. The app sets explicit variable font weights.

## Hashes

| File | Bytes | SHA-256 |
| --- | ---: | --- |
| `ask_hero.webp` | 16552 | `ab80756b1b5f5d9f374108ee60f397683ba9075df62daf21fda2b16ea0317c6a` |
| `ask_banner.webp` | 4080 | `69f1dd3c335aac471e68019339d1f5995bd274bd18ffa9b0e05693edd0a800b9` |
| `ask_icon.xml` | 839 | `c55cf3ecbc8b5674b4d594d1cbdc3064c3f04ae64a69240f3ad2992a47a4e828` |
| `outfit.ttf` | 110884 | `fc7287273e66929776e2ba54f144fe699080bec29f61bf649d70d871468aeade` |

Generated source SHA-256: `e5828d644fbde4050310d6bb93974e78267e721151f7505dab2e054ee0ba573b`.

Device and account captures are private review evidence and must not be committed.
