# Edge cleanup trials

Both candidates were rejected on 2026-10-26. The app still uses `chess_pieces_v2`. No v3 production resource was added.

The built-in ImageGen tool edited the original `docs/chess-math/art-v2/chess_pieces_v2.png` twice. Both calls used `transparent_background: true`. Neither candidate was processed after generation. The source and v2 production resource remain unchanged.

## Inspection

Both outputs retain twelve recognizable pieces in six columns and two rows. Their dimensions are 2172 × 724 pixels, RGBA, with 362 × 362 cells. All alpha > 8 bounds remain inside their cells. Both edits slightly change the shapes and positions. The first edit moves the queens farther right within their cells. The second retains the general arrangement more closely.

Original-resolution previews show a colored dotted fringe. Read-only pixel inspection limits this observation: examples of red RGB values have alpha 2/255. The original contains no strongly red pixels at alpha > 8, using red > 150 and red > twice both green and blue. Therefore the bright preview fringe does not establish that Android displays bright colored debris. Android draws decoded bitmaps with premultiplied alpha.

Read-only eight-neighbor connected-component inspection counted pixels in disconnected components smaller than 20 pixels within each cell:

| Asset | Alpha > 8 | Alpha > 128 |
| --- | ---: | ---: |
| Original v2 PNG | 11 | 0 |
| Candidate 1 | 25 | 0 |
| Candidate 2 | 32 | 0 |

These counts do not measure perceived edge quality. They show that the generated edits do not demonstrate removal of disconnected alpha speckles. Together with shape drift, this is insufficient evidence to replace production art.

Both candidates have the same decoded RGBA budget as v2: 6,290,112 bytes, approximately 6 MiB. A complete mip pyramid adds approximately 2 MiB if the renderer honors the hint. No candidate is loaded by the app. Expected displayed piece heights remain approximately 66–86 pixels in a 100-pixel board cell. Actual TV edge appearance still requires a hardware rendering comparison.

## Prompts

Candidate 1, `rejected-edge-cleanup-1.png`:

> Use case: background-extraction. Asset type: production chess sprite atlas. Input image 1 is the EDIT TARGET, not a loose style reference. Make one narrowly targeted edge cleanup of this exact atlas. Preserve all twelve original sculpted piece designs, their shapes, sizes, positions, shading, ivory and charcoal materials, fine dark outlines, highlights, and their exact order. Change only the transparent contours: remove the colored red/yellow fringe, isolated edge speckles and dirty halos. Replace those defects with clean, smooth antialiased coverage at the existing silhouette boundary. Keep the fine modeled shading and fine features sharp. Do not blur, soften away outlines, wash out details, add glow, change colors, or redesign any piece. Preserve holes and interior transparent cutouts. The output is a genuinely transparent RGBA PNG. Preserve the wide 3:1 six equal columns by two equal rows layout, preferably the original 2172x724 dimensions (both dimensions must support exactly 6 equal columns and 2 equal rows). Top row ivory pawn, knight, bishop, rook, queen, king; bottom row charcoal pawn, knight, bishop, rook, queen, king. Each complete silhouette wholly within its own equal rectangular cell, with substantial clean fully transparent gutters all around. All twelve must remain distinct and complete, kings retain crosses, queens retain round-tipped open crowns, bishops retain diagonal slits. No extra objects, labels, chessboard, grid, backdrop, checkerboard or floor. Keep every pixel outside the pieces genuinely transparent with no residual colored specks. The only intended change is clean transparent alpha contours.

Candidate 2, `rejected-edge-cleanup-2.png`:

> Use case: precise-object-edit. This image is an EDIT TARGET. Repair ONLY the damaged silhouette edges of these twelve exact chess sprites. The current transparent silhouettes have a ragged dotted one-pixel colored fringe, especially bright red and yellow pixels around the ivory queen crown gaps, knight chin and bases. REMOVE that fringe COMPLETELY. Reconstruct each very thin dark outside contour as a clean CONTINUOUS smooth near-black ink contour with antialiased alpha coverage. No disconnected pixels or colored dotted halos can remain. Outside each continuous contour every pixel must be fully transparent. The piece shading inside the contour must retain the current ivory/charcoal materials, highlights and exact designs. Keep king crosses, queen balls, bishop slits and knight eyes. DO NOT merely copy the damaged outline. DO NOT blur the pieces. Do not add a thicker outline, glow, border, color edging, floor or shadow. Transparent interior gaps, including ivory queen crown holes and under knight chin, must be cleaned of ALL red/yellow debris. This is an edge restoration, not a redesign. Preserve the exact twelve piece identities and order, top row ivory p n b r q k, bottom row charcoal p n b r q k, six equal columns by two equal rows, each piece wholly within its cell with generous transparent gutters. Preserve 2172x724 RGBA PNG canvas and equal362x362 cells. Real transparency, no checkerboard. No text. Production sprite asset intended to remain clean at80px displayed height.

The prompts describe the initial preview interpretation. The pixel checks above corrected that interpretation. Neither prompt is evidence that its requested cleanup succeeded.
