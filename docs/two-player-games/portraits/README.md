# Player portrait assets

The two apps use sixteen individual ImageGen portraits. Each portrait was generated separately from an approved concept reference. No portrait was extracted from a sheet.

Chess uses natural creatures, forest-green backdrops, warm ivory light, and honey highlights. Battleship uses the approved naval animals and human captains, midnight navy, teal, and brass light. Both sets use a mature painterly style.

The prompts requested 1024 × 1024 pixels. ImageGen returned square 1254 × 1254 PNG files. The original files remain under the recorded Codex generated-images paths. Only the selected 512 × 512 WebP resources are bundled. Preparation used `cwebp -q 86 -resize 512 512`; it did not crop or edit image content.

`assets.json` records the exact tool arguments and prompts, reference hashes, original paths and hashes, and bundled sizes and hashes. It also records the generation rights statement. No fonts were added. Existing font licenses remain unchanged.

All sixteen source images were inspected for character identity, palette, expression, costume, signature props, square framing, and absence of text or UI frames. Native code adds labels, borders, focus, and circular clipping. Review the final app captures for clipping and small-size readability before delivery.
