# Battle atmosphere design

Date: 2026-10-29. Target: Battleship 1.7.

The interface keeps the two equal boards and adds naval character around them.
A small captain portrait sits at the left of the header.
The scene remains decorative. Native controls draw every board cell, ship, marker, and label.

## Layout

Use the existing five-percent screen margin.
Use a 40 dp header with a 40 dp captain portrait, a 12 dp gap, and compact shot feedback.
Remove the repeated game title.
Place the turn label at the right of this header.
Do not create a portrait column or reduce either board width.

Keep a 24 dp gap between the two board columns.
Keep the columns equal in width and align their board titles.
Remove the separate target row, the last-shot row, and the repeated remote instructions.
Retain the selected coordinate in a compact header item if it helps aim.
Keep shot outcomes in accessibility descriptions and existing effects.
Keep a visible compact message for an invalid repeated target and a save failure.
Keep five-ship health markers under the friendly board because they show game state.

Use ENEMY WATERS and YOUR FLEET as the board labels.
Keep sunk and afloat counts beside these labels.
Reserve at least 18 dp for board labels and 34 dp for friendly fleet health.
Give all remaining safe height to the equal boards.
Do not change board geometry during a shot or effect.

## Background

Restore the existing naval_background image behind gameplay with a dark blue wash.
Use a weaker wash near the outer edges so that the distant ship remains visible.
Keep the existing dark still-water board image and its contrast.
Do not place silhouettes, a bottle, a captain, or scenery inside a board.

The generated concept includes an antique ship bottle at the lower left.
The current background source contains a ship, but no bottle.
A bottle is optional decorative scenery rather than a new panel.
The coordinator can reuse the existing background without adding a new scene asset.

## Native translation limits

The concept is a visual reference rather than a layout or rules source.
Its generated grid labels and ship counts contain errors.
Native code must retain exactly ten rows, ten columns, and five correctly sized friendly ships.
Intact enemy ships remain hidden.
Keep the established cyan focus outline and current Outfit font.
Do not copy decorative gold frames or the concept's large title.

## Generated assets

The built-in ImageGen tool created both images on 2026-10-29.
Both use text prompts without external image references or personal likenesses.
These are AI-generated project assets. No external stock license applies.
The repository can retain and use these generated outputs.
This statement does not claim copyright exclusivity.

The concept is `concept.png`, 1672 by 941 pixels.
It is a preview reference and must not become the application background.
Its original output is `/Users/amine/.codex/generated_images/01a18827-1592-7c10-aaac-853877db5825/exec-a8f551ed-b0a1-4458-bae6-fa8f54f2592d.png`.

The portrait is `captain-source.png`, 1254 by 1254 pixels.
It shows a fictional captain with no name or personal likeness.
The coordinator can resize it to a compact WebP resource.
Its dark square background supports native circular clipping.
Do not add a visible name, role paragraph, or portrait caption.
Its original output is `/Users/amine/.codex/generated_images/01a18827-1592-7c10-aaac-853877db5825/exec-a7b48045-f4cb-4747-a4b4-a3578844412d.png`.

## Concept prompt

```text
Use case: ui-mockup. Asset type: 16:9 television Battleship game screen design concept. Primary request: show an elegant quieter naval game interface with very little text. Dark navy and muted teal with ivory letters, restrained warm highlights. Composition: two equally large square 10 by 10 grids side by side occupy most of screen. Left grid labeled ENEMY WATERS shows only a gold focus square, a few pale miss rings and red hit crosses, absolutely no intact enemy ships. Right grid labeled YOUR FLEET shows five beautifully painted true overhead small gray navy ship sprites on still dark ocean water. Both grids are exactly equal scale and flat orthographic overhead. Small circular illustrated navy captain portrait in upper left header beside compact BATTLESHIP title, not a separate column. Upper right reads YOUR TURN. Very subtle distant warship silhouette at upper right edge and antique glass ship bottle partially visible at lower left edge behind dark wash, outside gameplay grids. This restores maritime character without clutter. No instruction text, no paragraph, no tutorial, no giant sidepanel, no sidebar. Keep small 2/5 and 5/5 counters near board titles, coordinate axes A-J and 1-10 around grids. Premium native TV game aesthetic, not a website. Clean grid lines, readable from couch, enough breathing room. Image is a visual reference, not final rendered application.
```

## Portrait prompt

```text
Use case: illustration-story. Asset type: square captain profile portrait bitmap for a native Battleship television game. A fictional distinguished naval captain, older adult man with silver beard and expressive kind but focused eyes, cream naval peaked cap with a simple anchor insignia, dark navy jacket, head and upper shoulders close up. Semi-realistic finely painted game illustration, subtle warm cinematic sidelight and cool navy fill, muted slate and cream palette. Face centered and large, all of cap inside square with 8 percent headroom. Deep plain navy blue backdrop. No frame, no circle, no text, no letters, no name, no watermark. No real person likeness, no identifiable military rank or country. Strong legible silhouette and facial features at 48 pixels. Square composition.
```


## Bundled portrait

`apps/battleship/src/main/res/drawable-nodpi/captain_portrait.webp` contains a 256 by 256 pixel WebP derived from captain-source.png. The preparation uses cwebp quality 88. Native Compose clips the square to a 40 dp circle. No generated labels or game boards are bundled.
