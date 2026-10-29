# Independent game UI/UX review

Date: 2026-10-29

## Verdict

Rank Concept A first at 7/10 and Concept B second at 6/10.
Use A's compact layout with B's clearer grouping and damage symbols.
Neither image is ready for implementation without the corrections below.
This review covers the two generated concepts and `design.md`, not a rendered native implementation.

The compact strip serves the user's priorities better because it leaves more room for the boards and adds fewer borders.
The card layout makes each ship easier to inspect, but ten outlined cards compete with the playing field.
Keep the portraits and current turn label in a shallow header.
Place one fleet strip directly above each board.

## Scores

Scores describe the images as shown, before native corrections.

| Criterion | A: strips | B: cards | Reason |
| --- | ---: | ---: | --- |
| TV readability | 5/10 | 7/10 | B has larger mixed-case labels and stronger grouping. A has small status text and faint sunk entries. |
| Board space | 8/10 | 6/10 | A's tracker occupies less height. B pushes the boards down and visibly flattens their cells. |
| Ship recognition | 6/10 | 6/10 | Pictures help, but Cruiser and Battleship remain similar. The generated Destroyer picture changes between concepts. |
| State accuracy | 3/10 | 5/10 | A has the wrong Destroyer pip count. Both omit board damage that their trackers claim. |
| Quiet visual hierarchy | 8/10 | 5/10 | A keeps the board dominant. B resembles a row of selectable buttons. |
| Dutch and French fit | 5/10 | 6/10 | Neither image demonstrates longer translated names. B has more grouping but little spare width. |
| Overall recommendation | 7/10 | 6/10 | A provides the better starting structure, with mandatory corrections. |

## Required native corrections

### Preserve square boards

Both images show rectangular boards and rectangular cells.
A's board measures approximately 626 by 592 image pixels.
B's board measures approximately 642 by 578 image pixels.
These measurements describe the concept images, not device layout measurements.
Draw equal square boards with ten equal rows and ten equal columns.
Size both boards from the available height after a fixed, compact header and tracker strip.
Do not shrink only one board when labels or turn feedback change.
Do not copy generated coordinates, grid lines, or ship placements.

### Use exactly five correct ships

Use Carrier, Battleship, Cruiser, Submarine, and Destroyer in that order on both sides.
Their lengths must be 5, 4, 3, 3, and 2 cells.
The own tracker must have exactly those pip counts.
Concept A incorrectly gives the sunk Destroyer three intact-looking pips.
Replace them with two damaged pips.
Concept B shows the correct pip counts, but its claimed board state still needs correction.
Both concepts show an apparently intact Destroyer on the own board while the tracker marks it sunk.
Both also omit the Cruiser damage that the tracker shows.
Derive the tracker and board appearance from the same match state.
Show the damaged cells on the board and preserve the existing sinking feedback.

### Protect hidden enemy information

Show only Hidden or Sunk for each enemy ship.
Do not add enemy damage pips, health bars, remaining cells, or partial-hit status by ship type.
A hit alone must not identify an enemy ship through its tracker.
Read enemy sunk status from public shot results, never directly from hidden ship damage.
Reveal enemy ship art on the board only after the public sinking result permits it.
Generic reference thumbnails in the tracker are safe because they describe the standard fleet, not its positions.
Keep both three-cell types separate and update only the publicly identified sunk type.

### Make status readable without color

Keep every label readable from the normal TV viewing distance.
Do not dim sunk entries until their names disappear into the water background, as Concept A does.
Use an opaque dark strip behind small text and pips.
Use a slash on each damaged own pip and a cross on each sunk thumbnail.
Use the same damage symbols and colors across both trackers and boards.
Keep all pips visible for a sunk own ship so that the player can still identify its length.
Use native text and icons for every label, pip, slash, and cross.

### Keep the actual ship artwork consistent

Use each existing bundled ship asset beside its matching name.
Do not extract replacement ships from either full-screen concept.
The generated Destroyer in B resembles the orange Battleship instead of a distinct matching thumbnail.
The crossed and dimmed thumbnails also hide identifying details.
Keep a visible silhouette under the sunk cross.
Preserve each image's aspect ratio and use consistent thumbnail bounds.
Do not scale every ship to the same hull length, which removes a useful recognition cue.
Use the type label as the final identity cue because Cruiser and Battleship can remain visually similar at TV size.

### Keep focus on the selected target

Make tracker entries informational and exclude them from D-pad navigation.
Avoid bright card outlines that imply a selectable control.
Use one circular targeting sight on the currently selected enemy cell.
Move the sight with the selection and clear selection corners from the previous cell.
Keep permanent hit and miss marks distinct from the targeting sight.
Retain the left captain and right opponent portraits without increasing the header height.
Retain existing turn and shot feedback that the concepts omit.
Do not reintroduce control instructions or the old bottom roster.

### Allow translated labels

Test English, Dutch, and French at the same board size.
Use mixed-case labels because small uppercase ship names are harder to scan in A.
Allow a consistent two-line label region when a translated ship name needs it.
Do not reduce the font size or clip French and Dutch labels to preserve the strip.
Keep state labels in a separate, fixed-height line.
Use existing localized ship names and state labels rather than image text.
If the strip cannot fit readable names, shorten the localized names deliberately and test them with the art.
Do not invent abbreviations independently on each screen.

## Acceptance evidence for a future implementation

Review fresh emulator captures in all three languages, including a damaged Cruiser and a sunk two-cell Destroyer.
Include a capture with both three-cell types present and only one sunk.
Include an enemy hit that does not yet identify or sink its ship.
Move the targeting sight with actual D-pad input and inspect the previous cell.
Confirm equal square boards and readable tracker text in the final TV layout.
Use one focused physical TV visual pass after emulator coverage.
Do not treat either generated image as proof of geometry, translation fit, correct state, or working navigation.

## Brief rationale

Concept A gives both fleets a clear place above the boards while keeping the game spacious.
Use larger labels, consistent ship pictures, and explicit damage symbols from Concept B.
Correct the ship counts, board shapes, and damage states in native code.
