# Battle atmosphere game design review

Reviewer: independent game UI/UX subagent. Date: 2026-10-29.

## Scope and evidence

I read the root README, the Battleship guide, and the battle atmosphere design.
I inspected `concept.png` at its supplied resolution and inspected `captain-source.png` separately.
This first review covers the generated concept and the proposed native translation.
It does not establish D-pad behavior, measured contrast, TV distance readability, audio quality, or native rendering.
A later section records a separate review of an actual application capture.

## Verdict

The concept supports the requested quieter naval atmosphere, but it is not a correct game screen.
Use its scene treatment and small portrait idea. Retain the native game geometry, markers, typography, and focus system.
The proposed native translation avoids its main defects if it preserves the conditions below.
The scores describe this image, not the finished application.

| Area | Score / 10 | Evidence and consequence |
| --- | --- | --- |
| Information hierarchy | 7 | Boards dominate, but the oversized title takes height from the boards. |
| Board balance | 8 | Both boards appear equally prominent. Their generated cell geometry still requires native replacement. |
| TV readability | 6 | Fine serif axes, thin grid lines, and textured water weaken distant reading. |
| Focus clarity | 7 | The gold square is visible, but the scene repeats gold highlights. Focus must retain the established cyan outline. |
| Rules and hidden information | 5 | Enemy ships remain hidden, but the counters and friendly ship placement do not establish valid game state. |
| Text restraint | 9 | Repeated instructions disappear. Turn and board identity remain visible. |
| Background restraint | 7 | The distant ship adds atmosphere. The bottle and compass compete with the lower left board edge. |
| Captain suitability | 8 | The portrait has a clear face and cap silhouette. The generated concept portrait differs from the separate source. |
| Native implementation suitability | 6 | The image provides direction, but its labels and decorative frames cannot become game controls. |

These scores do not form a release approval. The concept has several explicit corrections.

## Required native corrections

### Preserve valid boards and state

Draw exactly ten columns and ten rows with native code.
The friendly column labels read A, C, C near the left of the generated image. They must read A through J once each.
The enemy bottom row label is obscured or absent in the concept. Keep all labels, including 10, clear of scenery.
Keep friendly ship lengths exactly 5, 4, 3, 3, and 2 cells.
The image paints ships with inconsistent cell spans and offsets. Its attractive ship arrangement is not placement evidence.
Keep each native sprite aligned with its occupied cells so that its footprint does not imply attacks on adjacent cells.
Keep intact enemy ships hidden. Reveal only sunk enemy ships through the existing game rules.

Use the real sunk or afloat counts with consistent semantics on both boards.
The concept combines a red crossed symbol with 2/5 and a ship symbol with 5/5.
Those symbols suggest different meanings, which makes the two fractions hard to compare.
A compact `3 afloat` or `2 sunk` label is clearer than an unexplained fraction.
If space requires fractions, use the same meaning and visible symbol on both boards.

### Protect board space and focus

Use the proposed compact 40 dp header, not the concept's large decorative title.
Do not introduce a portrait sidebar, ornamental board borders, or extra panels.
Keep both boards equal in size during aiming, shots, effects, computer turns, and pause return.
The scene does not prove this behavior. It needs capture comparison and directional interaction evidence.

Retain the native cyan focus outline. The warm sunset and metallic trim reduce the uniqueness of gold focus in this concept.
Keep the focus outline above ships, hit markers, miss markers, and all effect frames.
A chosen target must remain identifiable without motion, sound, or color alone.
Retain a compact selected coordinate if the outline and axes do not support comfortable couch-distance aiming.
Do not repeat the coordinate in several panels.

### Remove tutorials without removing feedback

Remove repeated arrow, OK, and Back instructions from gameplay.
Keep the turn state visible, including the computer turn and pause state.
Keep a short visible response for a repeated target, unavailable action, or save failure.
Do not put those responses only in accessibility descriptions. A sighted player also needs to know why an action did nothing.
Keep named sunk-ship feedback only if it occupies a reserved region without changing board size.
The full-hull sinking effect and the fleet state can carry most ordinary hit feedback.

### Keep scenery outside the game area

Keep the water inside each board dark and visually still.
The concept contains bright wave detail that competes with pale miss markers and thin grid lines.
The native scene must preserve marker contrast through every position, not only the center.

Reuse the earlier naval background under a dark wash.
Allow the distant ship to remain visible around the upper outer edge without crossing labels or turn text.
The bottle is optional. Its lower-left placement in the concept crowds row labels and the board corner.
Do not add a bottle if the safe margin cannot show it without overlap.
The user asked to retain some of the earlier atmosphere, so a visible distant ship already satisfies part of that direction.

### Keep the captain small and decorative

Use the separate generated portrait, not a crop from the concept.
The separate source has a readable face, recognizable cap, plain dark background, and no text.
Its bright cream cap supports recognition at small scale.
Its fine skin and embroidery details will disappear at 36 dp, which is acceptable for decorative art.
Keep the portrait at the left of the compact header with a modest gap before the title.
Do not give it focus, a name block, a rank paragraph, or a separate column.
Check the circular crop at the bundled size. It must retain the face and avoid an awkward crop through the cap insignia.
A close crop is preferable to reducing the face to preserve all shoulders.

## Native capture review criteria

The later capture review must compare equal board bounds and unchanged geometry during a shot.
It must inspect all ten axes, focus contrast, five correct friendly ships, hidden enemy information, and the compact turn label.
It must inspect the portrait at actual header scale and the background at the outer screen edges.
Directional tests must establish initial focus, edge stops, targeting, pause return, and settings return.
A still image cannot establish those behaviors or actual physical TV readability.

## Native capture review

I inspected three current 1920 by 1080 application captures: overview, dense play, and computer turn.
The coordinator supplied `/tmp/battleship-atmosphere-overview.png`, `/tmp/battleship-atmosphere-dense.png`, and `/tmp/battleship-atmosphere-computer-turn.png`.
I also read the presentation fixture to distinguish its deliberate feedback substitution from real game state.
I did not operate a device or run a build during this independent review.

### Native verdict

The native interface corrects the concept's material visual defects.
It meets the requested quieter layout without sacrificing the main targeting information.
No material UI defect requires another visual revision from these three captures.
This verdict covers rendered composition and visible states, not speaker balance or physical TV readability.

| Area | Score / 10 | Native evidence |
| --- | --- | --- |
| Information hierarchy | 9 | Two large boards dominate. The small portrait and turn state remain secondary. |
| Board balance and stability | 9 | Both grid interiors span about 680 pixels. Their bounds match across all three supplied captures. |
| TV readability | 8 | Large sans-serif axes and strong markers read clearly in the captures. Physical viewing distance remains untested here. |
| Focus clarity | 9 | The cyan square, central cross, selected axis colors, and compact coordinate identify the target. |
| Rules and hidden information | 9 | All ten axes appear. Five friendly ships use cell-aligned lengths. No intact enemy ship appears. |
| Text restraint | 9 | Gameplay instructions disappear. Coordinate, turn, short feedback, counts, and fleet health remain. |
| Background restraint | 8 | The earlier ship scene is visible around the boards. It does not hide labels or markers. |
| Captain suitability | 8 | The face and cream cap remain recognizable at the small header size. It takes no separate board column. |
| Dense-state clarity | 8 | Pale miss rings, red hits, white ship damage, and fleet health remain distinct under heavy occupancy. |

### Specific observations

The grid interiors extend approximately from x195 to x875 and from x1083 to x1763 in each capture.
Their top and bottom edges remain near y235 and y916.
This visible consistency supports the stable-layout requirement across the supplied states.
It does not prove every animation frame, although the presentation fixture also asserts stable board bounds.

Both axes show A through J and 1 through 10 without the concept's duplicate letters or missing bottom label.
The five friendly ships visibly occupy lengths 2, 3, 5, 4, and 3 in the overview.
The intact enemy ships remain hidden. Dense play reveals a fully sunk ship with damage crosses.
The dense fixture deliberately changes the latest feedback to `Battleship sunk` without changing the board's sunk carrier.
That substitution tests the longer text. It is not evidence of a production ship-name mismatch.

The native counters resolve the concept's ambiguous symbols through explicit `SUNK` and `AFLOAT` labels.
These labels use different perspectives, but each denominator and meaning are visible.
The compact selected coordinate supports aiming without the previous instruction panel.
The repeated-target response remains visible below the enemy board without moving either grid.
The fleet health display retains useful state rather than tutorial text.

The portrait remains at the far left of a compact header. Its circular crop preserves the face and cap insignia.
Removing the large game title gives the boards more visual priority.
The portrait is decorative and does not resemble another targeting or navigation control.
The earlier warship background restores atmosphere without adding a bottle beside the row labels.
A bottle is not necessary for this revision because the preserved naval scene already restores recognizable character.

### Minor recommendations and limits

The water texture is still the busiest surface in the interface.
A later polish pass can reduce its brightness slightly if physical TV review shows that thin grid lines compete with the waves.
The supplied captures do not justify a required change because the axes, focus, and markers remain readable.

The visible incoming sunk message does not explicitly name the affected side.
Accessibility descriptions retain that distinction, and fleet damage provides visible context.
If playtesting reveals confusion, use a short message such as `Your carrier sunk` for an incoming loss.
This is a usability refinement rather than a blocker from these captures.

The computer-turn capture retains the targeting outline while the turn label clearly reads `COMPUTER TURN`.
This preserves the selected location. Interaction tests must separately establish that input cannot fire during that state.
A still capture cannot establish D-pad edge stops, pause return, settings return, timing, or input gating.
The coordinator reports those behaviors through separate tests. This review does not replace their execution evidence.

No capture establishes music quality, comfortable volume, physical overscan, or couch-distance legibility.
Those limits remain separate from this positive native composition review.
