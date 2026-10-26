# Forest backdrop v2

Production asset: `apps/chess-math/src/main/res/drawable-nodpi/forest_backdrop_v2.webp`.

The built-in ImageGen tool generated the image on 2026-10-26 with `transparent_background: false`. The style reference was `docs/chess-math/concepts/imagegen/chess-forest-ivory.png`.

The PNG is 1672 × 941 pixels, RGB, and 1,624,633 bytes. Its aspect ratio is approximately 16:9. The file was copied without image processing. The source remains at `/Users/amine/.codex/generated_images/01a17959-f6a0-7cc2-a4d5-a0163e9701b2/exec-145b039f-0152-4d58-8f4b-0862f8673022.png`.

Visual inspection with `view_image` confirmed a broad empty green center, subtle matte texture, and low-contrast branches at the bottom-left and top-right corners. There is no text, interface, board, or piece. The requested hex values guided generation; they are not exact pixel specifications. Read-only inspection measured the center pixel as RGB (22, 74, 40). App contrast and television rendering require integration checks.

## Prompt

> Use case: stylized-concept. Asset type: quiet full-screen background for a television chess app. Create a landscape 16:9 forest-green backdrop only. Use the supplied image exclusively as reference for restrained forest atmosphere and matte texture; remove all its interface content. A very smooth broad radial gradient: softly illuminated center approximately #123e29, deep shaded edges approximately #071a11. Very subtle matte paper grain, nearly invisible at normal viewing size. Sparse delicate organic leafy branch silhouettes ONLY in the extreme bottom-left corner and extreme top-right corner, dark muted green, very low contrast, blended into the background, flat botanical impression rather than photographed plants. Branches confined to outermost corner regions, no center foliage, no vine frame. Keep at least central 80% of canvas broad, clean, and empty for interface content. Refined calm forest-and-ivory chess aesthetic. No text, UI, symbols, board, squares, chess pieces, objects, lights, animals, humans, logos, border, frame, distinct horizon, vignette bands, or high-contrast details. The entire output is only the quiet green background with those two restrained corner branch accents. 16:9 landscape composition.

Production packaging uses lossless WebP encoding of the selected PNG. The source PNG remains in this directory. Pixel dimensions and alpha stay unchanged.
