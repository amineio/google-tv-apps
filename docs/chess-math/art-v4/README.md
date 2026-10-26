# Higher-detail piece atlas v4

The root coordinator accepted this candidate for production on 2026-10-26. The app now loads the lossless WebP resource `apps/chess-math/src/main/res/drawable-nodpi/chess_pieces_v4.webp`. This resource replaces v2 and contains 679,160 bytes.

The built-in ImageGen tool generated `chess_pieces_v4_candidate.png` on 2026-10-26. It used the original v2 PNG as its design and material reference. The exact prompt is in `prompt.txt`. The call used `transparent_background: true`. The output was copied unchanged. No scripted resizing or image editing occurred.

## Actual dimensions

The prompt requested 3840 × 1280 pixels. The tool returned **2172 × 724 pixels**, RGBA. This is the same canvas size as v2. It must not be described as a 3840-pixel or 4K image.

The pieces occupy more of each cell. Their silhouettes therefore have more source pixels than v2, despite the unchanged canvas size. Read-only alpha > 8 measurements:

| Piece | V2 width × height | Candidate width × height |
| --- | --- | --- |
| Ivory pawn | 129 × 173 | 171 × 232 |
| Ivory knight | 150 × 196 | 201 × 265 |
| Ivory bishop | 140 × 208 | 186 × 277 |
| Ivory rook | 150 × 181 | 198 × 243 |
| Ivory queen | 199 × 204 | 254 × 271 |
| Ivory king | 167 × 227 | 213 × 297 |
| Charcoal pawn | 134 × 174 | 179 × 238 |
| Charcoal knight | 159 × 205 | 215 × 278 |
| Charcoal bishop | 142 × 213 | 191 × 287 |
| Charcoal rook | 154 × 192 | 210 × 260 |
| Charcoal queen | 207 × 212 | 263 × 285 |
| Charcoal king | 172 × 232 | 220 × 307 |

Each silhouette is approximately 30–37% taller in source pixels. The renderer sizes each piece from its alpha bounds, so this does not require larger displayed pieces. Existing target display heights remain approximately 66–86 pixels in a 100-pixel board cell.

## Visual and alpha inspection

Original-resolution inspection confirms twelve recognizable sculpted pieces, ivory above charcoal, in six columns and two rows. The order remains pawn, knight, bishop, rook, queen, king. Ivory contours retain a thin dark line. Charcoal forms retain restrained warm highlights. The output broadly follows the original materials and style, with new fine texture and small shape differences. It is not an exact pixel-preserving enlargement.

Every alpha > 8 silhouette fits inside its 362 × 362 cell. The smallest visible gutter is 13 pixels above the lower king. This is narrower than requested. No visible silhouette crosses a cell boundary. Two cell boundaries contain alpha 1 pixels. There are 1,080,706 fully transparent pixels in the atlas.

The tool preview again shows colored near-transparent fringe. The earlier v3 checks explain why preview fringe alone does not establish an Android rendering defect. No claim of smoother TV output follows from this source inspection.

Decoded RGBA memory remains 6,290,112 bytes, approximately 6 MiB. A complete mip pyramid adds approximately 2 MiB if used. The renderer requests mipmaps, smaller prefiltered bitmap levels, and uses bilinear filtering during scaling. Mipmap use depends on the rendering backend. Larger source silhouettes and dark contours match the requested direction. Physical TV comparison is still required before claiming cleaner displayed edges.
