# Fleet tracker concepts

Date: 2026-10-29

## Scope

These images explore fleet trackers above both boards.
They do not change the app.
The current native screen and existing overhead ship artwork provide the visual references.

## Concept A: Integrated strips

[Concept A](concept-a-strips.png) uses five ship pictures in a continuous strip above each board.
Thin separators divide the entries.
The design gives the grids more space and keeps the interface quiet.
The pictures and labels remain smaller than the cards in Concept B.

## Concept B: Mini cards

[Concept B](concept-b-cards.png) gives each ship a small outlined card above its board.
Each card groups the picture, type, and status.
Larger pictures make ship types easier to compare.
The cards use more vertical space and add visual borders.
The cards represent status and do not receive D-pad focus.

## Information rules

Both trackers use Carrier, Battleship, Cruiser, Submarine, and Destroyer in the same order.
Their lengths are 5, 4, 3, 3, and 2 cells.
The enemy tracker shows only Hidden or Sunk.
Hidden means that the ship is not yet publicly sunk.
It does not mean that the player never hit the ship.
The enemy tracker must not expose intact positions, partial damage, or remaining cells.
Enemy sunk status comes from public shot results, not the hidden board.
The own tracker can show damage because the player can already see the whole own board.
Slashes and a crossed thumbnail supplement damage color.

## Native layout requirements

Keep both grids equal, square, and as large as the viewport permits.
Keep labels, status, pips, and board geometry in native code.
Use the five existing bundled ship pictures.
Do not use these full-screen concepts as game textures.
Keep the current captain portraits and circular targeting sight.
Only the selected enemy cell receives the sight.
Remove the old bottom roster after a future implementation.
Do not add control instructions or redundant status sentences.

## Generated image limits

ImageGen produced both concepts with the built-in image tool.
Each output measures 1672 by 941 pixels, approximately 16:9.
Both pictures contain five tracker entries per side.
Neither picture proves exact board geometry or correct gameplay state.
Concept A shows three intact pips under the sunk own Destroyer.
The correct Destroyer has two damaged pips.
Both concepts omit matching own-board damage and sinking evidence.
Their grids appear slightly rectangular.
Native controls must correct these errors.
The generated pictures omit some current feedback and counters.
That omission does not authorize changes to gameplay feedback.

## Recommendation

Prefer Concept A as the starting layout because it preserves more board space.
Use Concept B when larger ship pictures justify its added height.
An independent game UI/UX review must assess both concepts before a future implementation.
No fleet tracker implementation is included in this design task.

## Sources and rights

The current native capture is `docs/battleship/menu-help-refresh/native-game-en.webp`.
The ship references are existing generated assets under `apps/battleship/src/main/res/drawable-nodpi/ship_*.webp`.
The generation used the native capture, Carrier, Battleship, Submarine, and Destroyer as file references.
The tool accepts at most five reference paths, so the Cruiser uses the visible native capture and prompt description.
No third-party ship image was added.
These new AI-generated concepts are project design references.
No third-party license claim or exclusive copyright claim is made.
See `prompts.json` for the exact prompts and generation record.
