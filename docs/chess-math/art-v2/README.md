# Chess piece atlas v2

The app owns the production copy at `apps/chess-math/src/main/res/drawable-nodpi/chess_pieces_v2.webp`.

The built-in ImageGen tool generated this original atlas on 2026-10-26. The style reference was `docs/chess-math/concepts/imagegen/chess-forest-ivory.png`. The reference supplied the ivory and charcoal materials, sculpted forms, and dark contours. No third-party chess piece set was copied.

## Atlas contract

- PNG, 2172 × 724 pixels, RGBA, 622,344 bytes.
- Six equal columns and two equal rows. Each cell is 362 × 362 pixels.
- Top row: ivory pawn, knight, bishop, rook, queen, king.
- Bottom row: charcoal pawn, knight, bishop, rook, queen, king.
- Column keys: `p`, `n`, `b`, `r`, `q`, `k`.
- The renderer must crop each cell separately, trim alpha within that cell, and apply the desired piece height.
- The renderer uses alpha > 8 for trim bounds. All twelve tiles retain clear margins at this threshold. Preserve the source pixels when drawing.

## Verification

The selected output was inspected visually with `view_image`. All twelve types are distinct and in the required order. Kings have cross finials. Bishops have diagonal mitre slits. Knights have clear horse profiles. No board, labels, borders, or floor are present.

Read-only Pillow inspection confirmed alpha values from 0 to 255 and 1,289,697 fully transparent pixels. Every cell has fully transparent outer edges. At alpha ≥ 16, every piece remains well inside its cell. The largest visible piece bounds are 207 × 232 pixels. The PNG was copied without image processing or recompression.

The first generation was rejected because the lower king reached the row boundary. A targeted ImageGen edit reduced and centered the pieces. No Gradle build, app rendering, or television validation forms part of this asset check. Small-scale contrast and visual height need verification in the app.

## Generation prompts

Initial prompt:

> Use case: stylized-concept. Create one production game sprite atlas, a genuinely transparent PNG, with EXACTLY twelve isolated original chess pieces arranged as precisely 6 equal columns by 2 equal rows. Wide landscape 3:1 canvas preferred. The supplied image is STYLE REFERENCE ONLY: match its elegant sculpted ivory and charcoal chess forms, softly modeled volume, clear dark contours, refined tactile matte material. Do not reproduce its board, UI, words, foliage, or background. ROW 1 left to right: ivory PAWN, ivory KNIGHT, ivory BISHOP, ivory ROOK, ivory QUEEN, ivory KING. ROW 2 left to right: charcoal PAWN, charcoal KNIGHT, charcoal BISHOP, charcoal ROOK, charcoal QUEEN, charcoal KING. Each piece centered in its equal cell with generous completely transparent gutters (minimum 12% clear margin all sides); no artwork may cross a cell edge. All bases aligned consistently inside each row. All pieces share a frontal slightly elevated view with elliptical bases and soft realistic shape shading. Classic recognizable Staunton-inspired silhouettes: pawn round ball head; knight clear horse head in side profile; bishop mitre with clearly visible diagonal slit; rook crenellated tower; queen open pointed crown with small round tips; king cross finial. Keep all twelve recognizable, distinct, elegant, and coherent. Ivory pieces have a fine dark ink rim for readability on light board squares. Charcoal pieces have muted warm edge highlights for readability on green squares. Pawn shorter, king tallest. Strong readable silhouettes at small television size. No toy characters, faces, decorative fantasy features, text, border, grid, labels, checkerboard pattern, chessboard, extra objects, background fill, floor, or large cast shadows. Preserve actual transparent alpha around and between every piece, including empty interior cutouts. This image is a precise two-row six-column sprite sheet, not a scene.

Final layout edit prompt:

> Precise sprite atlas layout edit. Preserve these exact twelve piece designs, materials, colors, and order. Change ONLY scale and placement of the twelve pieces to give them large fully transparent cell margins. The canvas has exactly SIX equal columns and TWO equal rows. EVERY individual piece MUST lie wholly inside its own equal rectangular cell. Shrink each piece so its full opaque bounding box uses at most 70% of its cell height and at most 70% of its cell width. Center each piece BOTH vertically and horizontally inside its cell. This means there is a LARGE empty transparent horizontal gap between the two rows, occupying at least 15% of total image height. The cross on the lower right black king must be well BELOW the horizontal midpoint, and the base of the upper right ivory king well ABOVE the midpoint. Put zero visible or near-transparent specks outside the pieces. Exact row one order ivory pawn knight bishop rook queen king; exact row two order black pawn knight bishop rook queen king. Do NOT add or remove or redesign pieces. No labels, no background, no checkerboard, no border, no grid. Fully transparent background and generous clean transparent gutters. Wide 3:1 canvas.

Both calls used `transparent_background: true`. The final edit used the first generation as its reference.

Original selected generation: `/Users/amine/.codex/generated_images/01a17959-f6a0-7cc2-a4d5-a0163e9701b2/exec-1f7dfedc-423a-4d92-9bd7-f967d260daaf.png`.

Production packaging uses lossless WebP encoding of the selected PNG. The source PNG remains in this directory. Pixel dimensions and alpha stay unchanged.
