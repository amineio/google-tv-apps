# Schaak & Reken — icon v1

Selected design: a sculpted warm-ivory chess knight and one honey-gold arithmetic plus sign on a full-bleed forest-green background. No text, frame, or baked rounded corners.

## Files

- `app_icon_original_v1.png`: selected generated original, 1254 × 1254 pixels, PNG.
- `../../../apps/chess-math/src/main/res/drawable-nodpi/app_icon_art_v1.webp`: 320 × 320 WebP delivery asset, losslessly encoded after final size export.

## Source and provenance

Generated on 2026-10-26 with the built-in `image_gen` tool through the imagegen skill. One generation; no manual image edits or follow-up generations. Source: `/Users/amine/.codex/generated_images/01a1799b-d322-7211-8b28-a298a02bdf1a/exec-68badd82-d9aa-4685-b987-abe9cb8cea90.png`.

Existing `app_icon.xml`, `tv_banner.xml`, the centered-v4 chess and arithmetic screenshots, and the forest-ivory chess concept were inspected before generation. They informed palette and sculpted-piece style only. No image was supplied to the generation tool. Prompt requested 1024 × 1024; the tool returned 1254 × 1254. The original dimensions were preserved.

The artwork was inspected with `view_image`. The broad ivory silhouette and separate thick plus sign remain the principal shapes; fine mane and surface detail are secondary. No physical launcher verification has occurred. The original is the legacy square source. A separate transparent foreground and adaptive XML were added below.

Resource export: `cwebp -resize 320 320 -lossless -exact docs/chess-math/icon-v1/app_icon_original_v1.png -o apps/chess-math/src/main/res/drawable-nodpi/app_icon_art_v1.webp`. The generated original remains unchanged.

## Exact generation prompt

```text
Use case: logo-brand.
Asset type: production square launcher icon for Schaak & Reken, a calm premium Dutch chess-and-arithmetic game for a seven-year-old, on Google TV.
Primary request: create ONE polished full-bleed square icon, 1024 x 1024. An immediately readable, sculpted warm-ivory chess knight is the large hero, paired with a bold but smaller honey-gold arithmetic plus sign to its upper right. Both form one balanced centered emblem. The knight must look unmistakably like a classic chess knight on a short broad turned pedestal, with a friendly elegant profile, simple eye, no cartoon face. Clear silhouette.
Scene/backdrop: rich dark forest-green square background, subtle velvety depth, soft restrained central light. Background reaches all four edges; no outer rounded corners or surrounding mockup.
Style/medium: tasteful softly dimensional game-brand illustration, carved ivory or satin ceramic, smooth clean edges, subtle dark-green contour/shadow and warm softly lit bevels, visually related to classic sculpted chess pieces in a forest-green and warm-cream chess game. Premium, calm, child-friendly, precise, timeless.
Composition/framing: compact generous emblem centered inside the middle 64 percent of the square, knight around 56 percent of canvas height, plus sign around 18 percent of canvas width, comfortably separated. Keep all important shapes within a central circular safe region, with generous clear forest-green space on all sides for launcher/adaptive crop. Strong visual mass and recognisable at 48 pixels. Plus sign has four equal arms and thick rounded edges, clearly arithmetic rather than a sparkle.
Color palette: dark forest #0C2B1D, warm ivory #FFF1D0, honey #E9B85C. Use minimal shades to create sculptural volume, no additional hues.
Lighting/mood: warm soft light from upper left, modest soft ground shadow, clean calm surface.
Constraints: no text, letters, numbers, watermark, branding from other apps, board, leaves, UI, frame, rounded outer corners, checker pattern, extra pieces, detailed scenery, glow or excessive ornament. The chess knight and a single plus symbol are the only subjects. Deliver finished original artwork, not an app icon shown on a phone.
```

## Android integration

The manifest selects `@mipmap/app_launcher_v1` and `@drawable/tv_launcher_banner_v1`. API 26 and later select a layered adaptive icon with a solid `#0C2B1D` background and a true-alpha generated foreground. Older versions select the square original.

Version 0.12 removes the banner's fixed-size layer wrapper. The existing 320 × 180 opaque bitmap now fills the bounds that the launcher supplies.
This preserves the 16:9 artwork without transparent space at larger tile sizes. The source artwork and export remain unchanged.

Official references checked on 2026-10-26:

- [TV app icon design guidelines](https://developer.android.com/design/ui/tv/guides/system/tv-app-icon-guidelines): icon and banner resources, 16:9 banner, included app title, and density sizes.
- [Adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive): separate foreground/background, 108 dp layers, central 66 dp safe region, and version-qualified adaptive XML.

### Additional generated assets

- `tv_banner_original_v1.png`: 1672 × 941 RGB opaque banner, with exact title `Schaak & Reken`. Source `.../01a1799b-d322-7211-8b28-a298a02bdf1a/exec-25ce48ac-4058-4457-bee8-334dee911774.png` under `/Users/amine/.codex/generated_images`.
- `app_icon_foreground_original_v1.png`: 1254 × 1254 RGBA foreground. Source `.../01a1799b-d322-7211-8b28-a298a02bdf1a/exec-729929d0-71ad-420f-adf5-b3e5a9f5bee2.png` under the same generation directory.

Both used built-in ImageGen and the square original as the reference image. Both originals were visually inspected with `view_image`. No raster composition or retouch was performed outside ImageGen. Banner text matches `@string/app_name`. The banner ratio differs from exact 16:9 by less than 0.06%; XML maps it to exact 16:9 bounds.

The transparent image has 1,186,081 fully transparent pixels. A read-only PNG alpha inspection measured the subject at alpha > 32: bounding box `(314, 200)` through `(1049, 1088)`, maximum distance from canvas center `0.4192428 × canvas width`. XML insets of 18% on each side reduce that radius to `0.2683154 × layer width`, below the `33 / 108 = 0.3055556` adaptive-safe radius. Subject height after inset is approximately 49 dp. Thin alpha edges are visually secondary. No device or runtime verification is claimed here.

### Banner generation prompt

```text
Use case: logo-brand. Asset: finished Android TV launcher banner, landscape 16:9, target 1280 by 720. Use the supplied icon as a style and brand reference. Recompose into a premium calm forest-green full-bleed banner with the same sculpted warm-ivory chess knight and honey-gold plus on the left 38 percent, and large very readable warm-ivory Dutch title on the right 55 percent. Exact text: "Schaak & Reken". Title on two lines: "Schaak" above "& Reken", aligned left, wide friendly rounded bold sans serif with clean thick strokes. Both lines equally prominent. Keep it simple, elegantly spaced, readable as a small TV launcher tile, 10 percent safe margin. Same carved ivory knight, same thick honey plus, deep forest background #0C2B1D, warm ivory #FFF1D0, honey #E9B85C. Soft modest depth, no dramatic shiny effects. No board, no UI, no extra words, no border, no outer rounded corners, no watermark. Opaque full rectangle. The icon reference is not an edit target: build a wide composition with harmonious left emblem and right title.
```

### Transparent foreground edit prompt

```text
Use case: background-extraction. Edit target: supplied square Schaak & Reken icon. Create its true transparent adaptive-icon foreground. Preserve the exact sculpted warm ivory chess knight and honey-gold arithmetic plus symbol, their visual identity, shape, relative arrangement and material. Remove the forest background entirely. Remove floor, ground shadow, colored backing, frame or opaque square; all pixels outside the knight and plus must have actual alpha transparency. Center the combined emblem on a square canvas, reduce it so the ENTIRE combined knight and plus fits comfortably INSIDE a central circle of diameter 58 percent of canvas width. This is a 108dp Android adaptive-icon foreground; keep the subject inside its central 66dp safe circle. A lot of clear transparent padding is intentional. No text, no new shapes, no glow, no border, no additional content. Retain softly sculpted original ivory and honey look and clean edges. Deliver true transparent RGBA PNG.
```

## Final resource exports

Resize was explicitly authorized for resource packaging only. Generated originals remain unchanged. Each export uses `cwebp -resize WIDTH HEIGHT -lossless -exact SOURCE -o DESTINATION`; lossless applies to the resulting resized pixels, not to resampling.

| Resource in drawable-nodpi | Size | Bytes |
| --- | --- | --- |
| `app_icon_art_v1.webp` | 320 × 320 | 73,784 |
| `app_icon_foreground_art_v1.webp` | 432 × 432 | 67,298 |
| `tv_banner_art_v1.webp` | 320 × 180 | 46,112 |

Combined raster resource cost: 187,194 bytes (about 183 KiB). Sources are the corresponding original PNGs in this directory. All three exported files were inspected with `view_image` at export resolution. The title remains clear at 320 × 180. No art was redrawn, retouched, composited, or semantically changed during packaging. Foreground alpha remains present; XML supplies the forest background. No Gradle, emulator, ADB, or physical-device checks were run by the icon agent.


