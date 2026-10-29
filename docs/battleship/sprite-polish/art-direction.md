# Battleship art direction — 29 October 2026

Read-only review of GameplayScreen.kt, GameBoard.kt, BoardGeometry.kt, ImpactEffects.kt, BoardPresentation.kt, the app guide, the supplied generated concept, and native-hit.webp. Checkout: /Users/amine/Developer/google-tv-apps, main, bb99134a21c03cae20a791be318f2e12c4df39c2. No repository changes or device input performed.

## Decision

Use two equally important square sea charts on a quiet navy canvas. Preserve the enemy board on the left and the player's fleet on the right. Bring the friendly grid to at least 90% of the enemy grid side, preferably 100%. Use realistic overhead ship cutouts and a static ocean photograph-style texture. Keep the cyan targeting outline as the strongest persistent accent.

The generated concept provides the right hierarchy and photographic material direction. It is not a pixel specification: its friendly chart is slightly rectangular, one ship class is absent from the chart, and the water highlights compete with markers. Correct those issues in native rendering.

## Measured problem and layout contract

In native-hit.webp, the enemy grid is about 520 px square and the friendly grid about 252 px square. The friendly grid has only 23% of the enemy grid area. The decorative command ship, large coordinate block, side roster, padding, and legend consume the available space.

1. Remove the decorative angled command ship from gameplay. Its perspective conflicts with the overhead fleet and it occupies essential board space.
2. Put Target, coordinate, selected-cell state, and Last shot into one reserved-height status row. Keep selected-cell state separate from last-shot result. Reserve enough space for "Battleship sunk" and "Already targeted" before either string occurs.
3. Put the fleet roster below the friendly board in five equal columns. Each column has its ship name and five-or-fewer damage segments. A small ship thumbnail is optional only when it does not shrink the grid or labels.
4. Use two equal-width columns with a 20–24 dp gap. Derive both board sizes from the same available-height calculation. Use the same coordinate gutter for both boards; the current compact gutter changes actual grid size even with equal containers.
5. At the usual 960×540 dp logical canvas with the existing 5% outer safe area, budget approximately: app heading 30 dp; status 32 dp; board headings 18 dp; square board containers 340–350 dp; footer/roster 30–34 dp; remaining height for fixed gaps. These are starting dimensions, not independent minimums that must overflow. Keep actual grid pitch at least 30 dp. At 1920×1080 this produces approximately 60 px cells.
6. Align both grid top edges and bottom edges. Center each square in its column. Do not stretch the water or grid into a rectangle.
7. The left footer holds remote hints. The right footer holds the roster. Keep these equal, fixed heights. Reserve any save-warning slot; a warning must not recompute board size.
8. Fix status-row height, title-row height, and board bounds from the first gameplay frame. Do not use content-driven sizing, entry scale animation, or focus scale on the board. Reserve turn-label width for COMPUTER TURN and SHOT RESOLVING.

Review exact grid bounds on first visible frame, 0.5 s, 2 s, after the first shot, and after pause/resume. Bounds must not change. The parent may choose a slightly different fixed layout if these measurable contracts hold.

## Static water art

Generate a square, seamless-looking open-ocean image viewed exactly from above: deep Prussian blue, restrained teal troughs, small irregular waves, subtle silvery reflections, no horizon, shore, objects, wakes, fog, lettering, vignette, or baked grid. Use a high-resolution master and a compressed local drawable. Crop square; never stretch. Do not animate the image, its sampling, or its brightness.

Apply a uniform navy tint sufficient to keep the water subdued. In a grayscale capture, the grid, miss rings, and ship outlines must separate from the brightest wave highlights. No near-white crest should resemble a shot marker. Start with a 20–30% navy overlay and adjust from native capture. Keep the full-page background flatter and darker than the charts. Avoid a bright center spotlight.

## Five ship sprites

Generate separate, transparent RGBA cutouts with the same lighting and scale language. Use a true 90-degree overhead orthographic view. Bow points up in the source. No visible hull sides, horizon, perspective convergence, water, drop shadow outside the silhouette, baked damage, flames, labels, flags, or UI. Soft neutral illumination comes from the upper-left; keep the edge readable on dark water. Record generation prompt, model/tool, dimensions, output, compression, and rights in the asset manifest.

- Carrier / 5 cells: recognisable full flight deck, runway markings, island on one side, a few small parked aircraft. Broadest hull, roughly 0.72–0.80 cell wide.
- Battleship / 4 cells: tapered steel hull, warm muted deck, three large gun turrets and central superstructure; about 0.62–0.72 cell wide.
- Cruiser / 3 cells: narrower hull with smaller bow/stern turrets and compact superstructure; about 0.55–0.65 cell wide.
- Submarine / 3 cells: dark streamlined capsule hull, small central sail, no surface-ship deck clutter; about 0.38–0.48 cell wide.
- Destroyer / 2 cells: short slender grey hull with one clear bow gun and compact bridge; about 0.52–0.62 cell wide.

Crop transparent margins tightly and consistently. Fit each hull into length×pitch minus approximately 0.15 pitch total, centered on its occupied cells. Rotate the entire sprite by exactly 90 degrees for horizontal ships. Do not stretch one universal sprite to make all five classes. Keep shadow/subtle edge treatment separate and within about 0.03 pitch of the hull. Draw grid below sprites, then damage marks, then effects, then focus. A ship's silhouette must stay inside its own cell strip, including when ships touch.

The friendly board always shows all five ships. The enemy board reveals only ships already sunk. Individual hits must not expose an intact enemy hull. Retain a dim, desaturated sunk silhouette after animation so learned board information remains clear.

## Overhead effects and timing

Use native Canvas effects above the static water. A generated raster water asset does not require raster animation.

MISS — 1000–1200 ms total:
- 0–160 ms: small dark circular impact hole with white broken foam rim.
- 120–500 ms: irregular radial crown seen from above, white arcs and compact droplets distributed in all directions.
- 300–1100 ms: two or three near-circular ripples expand to a radius of 0.8–1.0 pitch and fade.
- X and Y radii should agree within 5%. Do not use the current 0.61 ellipse, upward crown polygon, or gravity-driven screen-Y droplets. These imply an oblique camera.

HIT — 1300–1600 ms total:
- 0–120 ms: small bright warm ignition.
- 120–420 ms: orange fire lobes expand around the exact cell, with short radial sparks.
- 350–950 ms: warm embers and soft charcoal smoke persist.
- 900–1500 ms: smoke disperses and fades; the hit cross remains clear.
- No generic sunburst with long equal spikes. Keep the hottest core within about 0.45 pitch radius. Allow softer smoke to reach 0.8 pitch. Avoid vertical smoke columns or camera shake.

SUNK — 2500 ms total:
- 0–350 ms: slower ignition at the final hit.
- 300–1400 ms: 2–3 small fires spread to different parts of the known sunk hull; do not ignite unrelated cells.
- 1100–2100 ms: cool and darken the hull, add subtle local foam, gradually reduce hull opacity to suggest submersion. Do not slide the hull into another cell or squash it as if the camera tilted.
- 2000–2500 ms: fade smoke and fire to a stable dim wreck silhouette. Keep hit markers visible.
- Apply the sequence to the exact sunk ship on either board, not only a larger burst on the last-hit cell. Use sunkCells/ship identity for the bounds.

Use outcome-specific durations in the shared presentation clock and retirement logic. Merely slowing drawing progress is insufficient when the active impact is cleared early. Prevent the next attack from replacing an unfinished impact; keep the full 2.5-second sinking sequence visible before the next attack or result overlay. Preserve Back/pause responsiveness, existing saved-state rules, and cancellation when backgrounded. Existing markers should return without replay when continuing a saved match. Controls may move the aim during resolution if that remains existing behavior, but must not fire a second shot.

## Review rubric

Use native captures and recorded directional interaction. Generated concepts cannot establish a shipped score. Score each category independently; all must reach at least 8/10. A listed critical failure caps its category at 6/10.

| Category | 8/10 minimum evidence | Critical failure | What earns 9–10 |
|---|---|---|---|
| Composition | Friendly grid side >=90% of enemy; aligned square grids; full five-column roster; no decorative panel crowding | Friendly board remains under 80%, clipped or rectangular grids | Balanced negative space; full board and roster read immediately at normal TV distance |
| TV readability | Both grids have >=30 dp pitch at 960×540; coordinate/status labels >=13 sp; visible focus and markers over every sampled water region | Any clipped label; water mistaken for markers; inaccessible target coordinate | Ships, markers, sunk state, and selected cell all distinguishable in grayscale and at half-size capture |
| Ship craft | All five classes use consistent true-overhead art; silhouettes fit occupied cells and remain distinct; no matte halos | Side-view/perspective sprites, missing class, wrong cell span, revealed intact enemy ship | Carrier deck, large turrets, cruiser proportions, submarine sail, and destroyer bridge survive native scaling |
| Effect clarity | Circular overhead miss; visible 1.3–1.6 s hit; full 2–3 s hull-specific sinking; persistent correct markers | Side-view splash, effect cut off by next attack, sinking only a flash | Natural asymmetric foam/smoke; effect origin obvious; no distraction from grid or focus |
| Interaction stability | Identical board bounds from initial gameplay through shots/pause/resume; directional aiming and repeat rejection verified; pause remains responsive | Gameplay starts large then squeezes, focus lost, duplicate fire, timing overlay obscures sink | Stable across fresh game/continue/restart and long feedback; no visible redraw jitter |

Required review captures: fresh match; target on busy water; miss at center plus edge; hit at about 0.2/0.6/1.2 s; sunk at about 0.3/1.0/2.0/2.5 s; after pause/resume; a long "Battleship sunk" result. Review a short clip for startup size stability and effect rhythm. Keep emulator evidence separate from physical TV readability and audio acceptance.
