# Forest and ivory visual review

Date: 26 October 2026. Independent UX/UI review. Scope: visual refinements within the approved design. No game, session, or navigation changes.

## Evidence and limits

I visually inspected both original ImageGen references in `concepts/imagegen/`, and the seven TV screenshots in `evidence/`: start, chess, selection, math, hint, settings, and promotion. I also read `TvStyle`, `ChessBoardView`, and the corresponding `MainActivity` layouts to make the advice practical.

Fresh captures in `evidence/ux-review-v1/` confirm the same visual findings: [01-promotion-current.png](evidence/ux-review-v1/01-promotion-current.png) shows promotion, [04-start.png](evidence/ux-review-v1/04-start.png) shows Continue, [05-chess.png](evidence/ux-review-v1/05-chess.png) shows the saved board, and [06-selected.png](evidence/ux-review-v1/06-selected.png) shows the selected f2 pawn. I opened these images after the capture agent verified and replaced the board captures. The fresh selected state confirms that mint dots cross both square colors, making a dark rim useful.

I also visually inspected the fresh fixture captures: [fixture-math-hint.png](evidence/ux-review-v1/fixture-math-hint.png), [fixture-settings-default.png](evidence/ux-review-v1/fixture-settings-default.png), [fixture-promotion.png](evidence/ux-review-v1/fixture-promotion.png), and [fixture-chess-selection.png](evidence/ux-review-v1/fixture-chess-selection.png). The math answer remains inline with the equation; the default settings viewport still partly cuts off the bottom action row. These are fixture states captured on the TV, not proof of navigating every state in a real session. There is no fresh plain-math pair or bottom-action focus capture. Therefore, the layout-shift observation uses the older pair, and bottom-action visibility remains a remote verification item.

The TV captures are 1920 × 1080 at density 320. Therefore, 2 physical pixels equal 1 dp. Coordinates below use a 960 × 540 dp canvas. Text uses sp and must retain Android font scaling. Coordinates are target layout bounds, not fixed text baselines.

The black strokes around text come from the TV high-contrast text setting. The clock at the top right comes from the external Clock overlay. Neither is an app styling defect. Preserve support for both. The original images are mood and composition references, not exact interaction specifications.

## Assessment

The app already matches the main composition: a large left board, a quiet right instruction column, a dark green backdrop, and an ivory palette. It does not need a redesign. The largest difference is material quality. The reference pieces look like small sculptures. The live pieces are flat, outlined symbols. The live board is already large enough; enlarging it would provide little benefit. Its current bounds are approximately x 42–504 dp and y 41–503 dp. Retain this square and its 64 equal cells for the first pass.

The background also differs. The references use fine texture and small, irregular corner branches. The current background uses a clean gradient and a chain of large, repeated leaf shapes. The keypad keeps the correct structure, but its muted borders, flat focus surface, and inline answer make the result feel more utilitarian than the reference.

The text outline increases perceived weight in the captures. Keep the native text and accessibility behavior. Improve visual hierarchy through size, spacing, and placement instead of rasterizing text or weakening contrast.

## Five changes, in priority order

### 1. Give the pieces material and depth

**Impact: highest. Cost: medium.** Keep the current board geometry and replace only the piece rendering.

- Produce one original transparent atlas containing the six piece types in both colors. Use a consistent three-quarter relief style, light from the upper left, warm ivory bodies, and charcoal bodies with soft edge highlights.
- A 4 × 3 atlas with 256 × 256 px tiles is sufficient. Keep transparent padding within each tile. Align every base to the same visual baseline. Keep the piece body within about 78% of the square height and 68% of its width; allow a small soft shadow within those bounds.
- At the current board size, a cell is about 57 dp. The pieces should occupy roughly 43–46 dp vertically. Compare rook, knight, king, and pawn at actual TV size before accepting the atlas.
- Render the bitmap inside the existing Canvas board. Keep game state, piece names, focus handling, and content descriptions independent of the artwork. Draw legal markers and focus indicators after the bitmap.

Do not crop pieces directly from the full concept: colored squares and baked shadows would leave artifacts. Create a matched transparent set, then inspect all 12 tiles. A generated atlas may need manual alignment and silhouette corrections before production. Confirm its provenance and retain the source file.

**Lean fallback:** retain the existing paths, add restrained vertical body gradients, one narrow highlight, a darker base, and a soft base shadow. This keeps the smallest package and easiest maintenance. It will improve depth, but will remain more graphic than the sculpted reference. Do not spend the first pass rebuilding 12 detailed vector sculptures.

### 2. Replace the repeated leaf chain with one quiet backdrop

**Impact: high across every screen. Cost: low to medium.** Prefer one background asset over many decorative assets.

Use one original 1920 × 1080 backdrop bitmap in `drawable-nodpi`. Draw it with aspect-preserving center cropping. It should contain only the forest gradient, fine low-amplitude texture, and two sparse branches. Keep all text, buttons, board squares, and focus indicators native.

Suggested color tokens: background center `#123E29`, background edge `#071A11`, board dark `#3E6347`, board light `#F1E5C9`, ivory foreground `#FFF1D0`, muted text `#B5CEB0`. These are starting targets, not sampled color claims. Keep existing mint and amber semantic colors until state contrast is checked on both square colors.

Place branches mostly inside the lower-left 110 × 170 dp and upper-right 100 × 150 dp corners. Avoid visible leaves behind the math prompt, hint paragraph, or keypad labels. Texture must remain subtle at viewing distance. Do not apply a textured overlay to foreground text or controls.

A compressed bitmap adds package and decoded memory cost; a 1920 × 1080 RGBA decode is about 7.9 MiB. Decode once and reuse it. A native path version is smaller and scales freely, but needs varied leaves, a visible thin stem, and more restrained placement. Either approach is offline and needs no rendering library.

Keep board squares native and flat in the first pass. Small frame depth and improved pieces provide most of the reference character without reducing marker contrast.

### 3. Give math a stable equation and answer area

**Impact: high. Cost: low.** Keep the exact copy, keypad order, hint behavior, and answer rules.

The older math and hint captures show the left column moving upward when hint content appears. Reserve a hint area so the heading, progress, and equation keep their positions. Empty feedback should not create variable layout shifts.

Suggested targets on the 960 × 540 dp canvas:

| Element | Target |
| --- | --- |
| Left column | x 42–548 dp; top anchor around y 82 dp |
| Math title | 44–48 sp bold; one line where possible |
| Progress | 21–23 sp; 10–14 dp below title |
| Equation row | y 220–310 dp; 54–60 sp bold |
| Answer area | Native ivory outline, 2 dp; 10–12 dp radius; minimum 84 × 80 dp |
| Hint and feedback area | Approximately y 326–416 dp; width bounded by left column |
| Paper reminder | Below the reserved hint area; 19–21 sp |
| Keypad group | Approximately x 590–914 dp; y 66–478 dp |

The answer area should show the same current input, including the empty state. It is a visual container, not an additional focus target. Let long expressions fit through normal layout constraints and a sensible type-size range; do not truncate the exercise or hint.

Keep at least the existing control sizes. Use 6–8 dp clear gaps between keys. A slightly shorter keypad block can recover the reference's calmer proportions, but do not reduce useful text size merely to match the artwork. Maintain the existing remote instructions.

### 4. Refine focus without weakening it

**Impact: medium across every screen. Cost: low.** Keep focus distinct from selection and legal moves.

The board already uses the right corner-bracket language. Buttons use full borders plus square corner strokes, producing conspicuous ivory blocks in the captures. Refine that shared drawable once:

- Unfocused border: 1.5–2 dp ivory with enough opacity to remain visible against the forest background. Avoid the current low-contrast gray-green appearance.
- Focus: muted green fill near `#52734C`, 2 dp base border, and 3–4 dp rounded corner brackets about 10–12 dp long. Inset the brackets enough to avoid clipping.
- Keep corner brackets visible without relying on fill color. Keep the amber selected border and selected marker separate from focus.
- On the board, retain the dark under-stroke behind ivory brackets. Preserve legal destination dots and capture markers above the new piece art. Add a 1–1.5 dp dark rim beneath mint dots and amber selection outlines where necessary; the pale square otherwise gives these colors little separation. Keep their shapes, meaning, and positions. Check each state on both light and dark squares.

Do not add focus animations, enlargement, glow effects, or moving backgrounds. The static focus must remain legible with high-contrast text enabled.

### 5. Apply the same spacing discipline to utility screens

**Impact: medium. Cost: low.** Preserve all existing actions and focus order.

The start screen is clear but top-heavy. Keep the 42 dp left margin, group the title and instructions, then place the two existing actions below with 10–12 dp separation. Use the new backdrop to carry the empty area. No extra hero illustration is necessary.

The promotion screen already has four readable choices. Preserve them. After the piece atlas is accepted, a small matching piece image can accompany each existing label if it fits the current controls. This is optional and lower priority than board pieces.

The settings capture shows the bottom action row partially below the viewport. Verify this with remote focus before calling it a defect. Keep each focused action fully visible. Improve group spacing with 8–12 dp between a label and its row, then 16–20 dp between groups. Keep the form scrollable and retain every setting. Do not compress the full form into one screen at the expense of readable labels.

## Asset decision

Use **two production artifacts at most** for the first visual pass: one transparent piece atlas and one quiet backdrop. These address the largest gaps and keep Android integration simple. The piece atlas has the highest value. If the atlas cannot pass silhouette, alignment, or transparency review, ship the native gradient refinement first and defer the atlas.

Do not bake a full app screen into an image. Do not bake text, board cells, buttons, focus, selection, or legal markers into decorative assets. Do not add online image loading, a 3D engine, a new UI framework, or a font dependency for this pass. The current sans-serif family is suitable; stronger hierarchy and spacing matter more than a new font.

## Acceptance checks

1. Capture chess, selected piece, legal destination, capture destination, check, math, hint, start, promotion, and settings with remote focus on the bottom actions.
2. Keep all 64 cells visible. Verify artwork never hides the cell boundary, focus brackets, selected marker, or legal marker.
3. Compare black and white pieces on both square colors. Each type must remain identifiable at TV viewing distance.
4. Keep the native high-contrast text behavior and current Dutch copy. Check long hint text without clipping or layout movement.
5. Use the same screenshot dimensions and TV settings for before/after comparisons. Ignore the external clock when judging app alignment.
6. Verify remote navigation, content descriptions, and existing semantic states remain unchanged. No touch path or animation is required.

This report is visual evidence and design advice. It does not establish game-rule correctness, accessibility certification, or final device acceptance.

## Independent review of the second visual pass

I visually inspected `evidence/ux-review-v2/chess-selection.png`, `math-hint.png`, `promotion.png`, `settings-default.png`, and `start.png`. I compared these with the original ImageGen references and the first TV captures. I reopened `math-hint.png` after its replacement from the final APK; the final normal-size layout remains clear. Test results belong in the implementation verification record; this section reports visual observations only.

**Assessment: the main visual gaps are substantially reduced. No new visual blocker is visible at the captured text size.** The result is much closer to the approved forest-and-ivory concept. Keep this direction.

- The pieces now have the reference's sculpted bodies, consistent bases, and ivory/charcoal materials. All displayed types are identifiable. Black pieces remain distinct on green squares at the screenshot scale. The source atlas's fine edge noise is not a material issue at the displayed size.
- The backdrop now has fine texture, natural branches, and clear reading regions. It replaces the repeated leaf chain successfully. The branches remain secondary to text and controls.
- The board retains all 64 cells and its useful size. The mint dots now have dark rims on both square colors. Focus brackets and the amber selected state remain distinguishable over the artwork.
- Math now has a larger equation and a distinct native answer well. The shown hint and paper reminder fit. The answer well remains separate from keypad focus. Rounded button focus also removes the conspicuous square corner blocks from the first pass.
- Start and promotion use the shared visual language cleanly. Their simple layouts need no additional illustration or controls.

### Remaining actions and minor departures

**Separate implementation evidence:** the coordinator reports that the final release tests passed, including remote reachability of overflow content at font scale 1.6. I did not run those tests. The default settings screenshot shows the bottom action row partly outside the viewport, with focus on Addition. This is a scrollable form, so that screenshot does not establish an inaccessible control. No settings visual blocker is demonstrated. Keep the test evidence separate from this screenshot assessment.

**Small optional refinement:** the short math expression and its answer are farther apart than in the reference. In `math-hint.png`, the equals sign ends near x 246 dp while the answer well begins near x 446 dp. The intervening blank space weakens their visual grouping. For short expressions, let the expression use its natural width and place the well 12–20 dp after it. Keep a maximum expression width and the current fit behavior for longer operands. This is a minor composition improvement, not a functional blocker.

**Acceptable departures:** the board squares remain flat, the keypad borders remain more muted than the concept, and the app omits the decorative remote illustration. These do not prevent the approved look or clear remote use. Further texture on the board, stronger keypad borders, or extra decorative art has low value for this pass. Do not expand the work to pursue pixel equality with the reference.

The TV high-contrast text strokes and external clock remain supported and unchanged. Neither is included as a remaining design issue.
