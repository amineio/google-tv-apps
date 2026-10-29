# Independent game UI/UX review

Date: 2026-10-29.

## Review scope

I inspected `help-concept.png`, `opponent-source.png`, and `design.md` independently.
This review assesses the generated concept and the proposed native layout.
It does not assess a rendered app, actual D-pad input, measured contrast, or television viewing distance.
Scores describe the supplied images, not release acceptance.

## Decision

Use the three-card structure and the opponent portrait as references.
Do not ship the help concept as a screen image.
Native diagrams must correct the target position, reduce water detail, and preserve the existing game rules.
No new generated concept is necessary because native layout can resolve these defects.

## Material findings

### Target position: correct before implementation

The Aim reticle sits on a vertical grid line in the generated image.
This suggests that a player selects a boundary instead of one square.
Center the native reticle inside one cell.
Use the exact gameplay reticle in the help diagram so that the two screens teach the same action.
Keep its center empty so that a hit cross or miss ring remains visible.
Keep the ring inside the selected cell with four short cardinal ticks.
Use a stable stroke without glow that obscures adjacent cells.

### Diagram noise: simplify before implementation

Bright water ripples compete with the reticle, miss ring, and thin grid lines.
Use a calm board fill and the existing gameplay grid colors.
Keep the distant ship and water texture behind the cards at low contrast.
The white miss ring and red hit cross already differ by shape.
Preserve this distinction rather than relying on color alone.
Do not add particle effects to these static explanations.

### Safe margins: do not copy the generated positions

The left title and Back button begin about 3.5 percent from the image edge.
This differs from the requested five-percent safe area.
Keep every native title, button, label, and portrait inside the five-percent safe area.
Use the same content edge for the title, cards, and Back button.
Remove the bottom-right control sentence because it repeats unrelated gameplay guidance.

### Help copy: shorten and protect game meaning

The Fire sentence promises that the opponent fires after every shot.
That promise does not cover the final winning shot or the visible impact delay.
Use the shorter instruction, “Press OK to fire.”
Keep the hit and miss legend next to its matching shapes.
Keep “Sink all five ships to win” only if five ships remains the actual victory rule.
Show the complete overhead hull only in the Sink card.
Do not reveal intact enemy ships in normal gameplay.

The diagram crops show four columns rather than a complete ten-by-ten board.
Keep them clearly framed as examples without coordinates or invented board labels.
Do not copy their dimensions into the gameplay board.
A short crop avoids shrinking ten rows into an unreadable help illustration.

## Native layout recommendations

Use three equal cards with aligned diagram bounds and consistent internal padding.
Reduce the title height so that the explanations receive more space.
Keep each card title and instruction separate from its diagram.
Give every card body the same fixed space for two lines of translated text.
Allow text to wrap without shrinking the font or changing the card width.
Reserve at least three lines if the final French or Dutch translation requires them.
Test the longest real translation instead of estimating from English.
Keep the Back button below the cards, clear of the final text line.

Use one focusable Back button on this static help screen.
Set initial focus on Back and make the physical Back key return to the previous screen.
Do not make the explanatory cards focusable without an action.
Keep the focus outline outside the label so that it does not reduce text contrast.
Test return focus on How to Play in the main menu.

Remove the requested subtitle and opponent/offline footer from the home screen.
Do not replace them with new promotional copy or control instructions.
Keep the main actions visually separate from decorative scenery.
Use the language names English, Nederlands, and Français in the language control.
Keep the selected value visible and persist it when the app returns.
Make the language control reachable through normal directional input.

## Opponent portrait assessment

The portrait has a clear face, strong light separation, and a restrained naval palette.
Its expression communicates competition without hostility.
The head occupies enough of the square to remain recognizable at small sizes.
The cap is close to the image edge, so a circular crop will remove its upper corners.
That crop is acceptable if the eyes, chin, and central cap emblem remain visible.
Do not stretch the square image.
Use the same portrait container size and treatment as the player captain.
Keep the board dimensions unchanged when adding the second portrait.
Place each portrait beside its board title so that ownership remains clear.
Use concise translated player and opponent labels rather than decorative ranks or names.

Test the final compressed asset at its actual rendered size.
Fine brush detail and the gold cap emblem cannot carry information at that size.
A portrait must not replace the current-turn indicator.
Keep that indicator separate and readable when either portrait is visually quiet.

## Gameplay reticle acceptance

Only the current cursor cell can show the targeting ring.
Moving the cursor must remove the previous targeting ring immediately.
Past shots keep only their hit, miss, or sunk-ship result markers.
No square corner brackets can remain around the last shot.
If the cursor returns to a fired cell, keep the underlying result marker visible.
Keep the ring centered and fully inside the board at every edge and corner.
Do not allow the new opponent portrait to reduce either ten-by-ten board.

## Scores

Scores use a ten-point scale.

| Area | Concept score | Reason |
| --- | --- | --- |
| Help organization | 8 | Three parallel cards create a clear visual sequence. |
| Couch readability | 7 | Large labels help, but busy water and long single lines compete. |
| Rule communication | 6 | The reticle targets a boundary and the Fire sentence overpromises. |
| Safe-area discipline | 6 | The generated left margin misses the requested five-percent boundary. |
| Translation readiness | 6 | English fits, but the concept reserves little space for longer copy. |
| D-pad simplicity | 8 | One Back action fits a static screen, subject to native focus testing. |
| Portrait fit | 8 | Strong face and palette fit the game, subject to a small circular crop. |

## Required rendered review

Review native captures in English, Dutch, and French before delivery.
Inspect all card text at television resolution for clipping and awkward wrapping.
Inspect the home screen after both requested text removals.
Inspect both portraits beside unchanged boards.
Inspect the reticle before a shot, after a shot, after movement, and at board corners.
Actual directional input must establish focus behavior because a screenshot cannot prove it.
The coordinator will provide rendered captures for a separate review.

# Native menu and help review

Date: 2026-10-29.

## Evidence

I inspected nine native captures at 1920 by 1080 pixels.
The coordinator supplied Home, Help, and Settings in English, Dutch, and French.
The files are `/tmp/battleship-language-captures/{home,help,settings}-{en,nl,fr}.png`.
These captures establish visual layout only.
They do not establish actual focus transitions or physical television readability.

## Decision

The native Home, Help, and Settings layouts pass this visual review in all three languages.
I found no clipped labels, overlapping text, missing accents, or unsafe text placement.
No material visual correction is required for these nine captures.
Gameplay, cursor movement, and confirmation screens remain outside this decision until their captures arrive.

## Home

The requested subtitle and offline opponent footer are absent in every language.
The four actions now provide the whole navigation structure without redundant instruction text.
The French title remains on one line without touching the ship or the right edge.
The Dutch action labels fit with ample horizontal space.
The cyan selected button is distinct from the quieter buttons without obscuring its label.
The dark left area keeps the background ship from competing with menu text.
All action labels remain inside the safe content area.
The selected button treatment expands slightly outside the label column but remains clear of the screen edge.

## Help

The native implementation resolves the material defects in the generated concept.
The reticle sits inside a cell rather than on a grid boundary.
Its clear center avoids hiding the selected square.
Calm diagram fills replace the distracting water texture.
The three cards align and provide a stable reading order.
The ship appears as an overhead hull rather than a side-view object.
The diagrams remain examples rather than replacements for the actual ten-by-ten boards.

The English instructions fit without cramped line breaks.
The Dutch final instruction wraps across two lines without leaving the card.
The French final instruction also wraps naturally across two lines.
The French Aim instructions occupy two separate lines without approaching the lower card edge.
All three languages retain space below their final text line.
The French hit and miss legends fit without colliding with each other or the diagram bounds.
Accents in French and Dutch render clearly.

The native Fire instruction avoids the concept's unconditional promise that the opponent fires next.
The additional sentence about one shot per turn explains turn structure without creating a paragraph block.
The Back instruction remains inside the Aim card rather than becoming another screen footer.
The subtitle now appears only on Help, where it introduces the objective.
This does not restore the removed Home subtitle.

The hull alone communicates a revealed ship more directly than the process of sinking.
This is a minor semantic limit, not a release blocker, because the card title and instruction state the objective.
If future play tests expose confusion, show a restrained sunk-state marker rather than adding another sentence.
Do not add visual effects merely to fill the available card space.

## Settings

The sound and language controls remain separate and readable in all three languages.
The selected language value fits on one line, including Nederlands and Français.
The French settings title retains its accent and stays inside the left content area.
The focused sound control has strong visual separation from the background.
The Back button remains clearly separate from the settings controls.
The screenshots do not prove language persistence or directional navigation.
Those behaviors require the coordinator's input tests.

## Native scores

Scores use a ten-point scale and describe these captures only.

| Area | Native score | Reason |
| --- | --- | --- |
| Home restraint | 9 | Four clear actions replace redundant menu copy. |
| Help organization | 9 | Equal cards align diagrams, headings, and short instructions. |
| Couch readability | 8 | Large text and calm panels support readability, subject to television viewing distance. |
| Rule communication | 8 | Aim and shot results are clear, while the static hull depends on its Sink label. |
| Translation layout | 9 | Real Dutch and French copy fits with natural wrapping and visible accents. |
| Safe-area discipline | 9 | All essential text and diagrams remain clear of the screen edges. |
| D-pad presentation | 8 | Selected states are clear, but screenshots cannot prove navigation behavior. |

# Native gameplay and confirmation review

Date: 2026-10-29.

## Evidence and interim decision

I inspected the six supplied gameplay and new-game confirmation captures in English, Dutch, and French.
The files are `/tmp/battleship-language-captures/{game,confirmation}-{en,nl,fr}.png`.
The confirmation dialogs pass visual review in all three languages.
Gameplay requires one roster readability correction before visual approval.
The portrait and turn-label relationship also needs a consistent ownership decision.

## Required roster correction

The additional roster height prevents clipping, but automatic wrapping divides several names at poor positions.
The Dutch labels show Vliegdeksc/hip, Onderzeeë/r, and Torpedobo/ot across their two lines.
The French carrier label shows Porte-avio/ns.
These fragments slow recognition from a couch despite every character remaining visible.
Use intentional word divisions or short roster names without shrinking the font.
Suitable Dutch divisions include Vliegdek-/schip, Onder-/zeeër, and Torpedo-/boot.
Suitable French divisions include Porte-/avions and Sous-/marin.
Keep both boards aligned at their current equal size.
Test the final roster again in all three languages.

## Portraits and turn label

Both portraits retain clear faces at their actual rendered size.
The circular crop preserves the eyes, chin, and central cap emblem.
Both use matching dimensions and treatments without reducing one board more than the other.
The new portrait fits the existing painted naval style.

The existing player portrait remains above Enemy Waters.
The new opponent portrait appears above Your Fleet beside Your Turn.
This arrangement creates an ownership ambiguity because proximity suggests that the new portrait represents the player.
The earlier user preference places the original captain on the left.
Preserve that preference if it remains the intended attack-side layout.
In that layout, associate the turn label consistently with the active captain rather than the opposite portrait.
Do not add a paragraph or decorative ranks to resolve the ambiguity.
The coordinator must settle the relationship before final visual acceptance.

## Reticle and boards

The circular reticle reads as a targeting sight and remains distinct from the grid.
At A1, its cardinal ticks stay inside the board without touching the coordinate labels.
Its clear center preserves the selected square.
Both boards show ten rows and ten columns at equal dimensions.
The translated board headings, counters, and turn labels fit without clipping.
The empty enemy board preserves hidden ship locations.
These screenshots cannot prove that the reticle follows movement or removes the previous highlight.
Those behaviors require the directional input test.

## Confirmation dialogs

The English, Dutch, and French dialogs provide a clear question, one consequence, and two distinct actions.
The French title fits on one line without approaching the dialog border.
The longer French action labels fit without wrapping.
The safe Keep Playing action has the initial visible selection in all three captures.
The darker game surface keeps attention on the dialog without losing context.
No dialog correction is required.

## Interim scores

Scores use a ten-point scale and describe these six captures only.

| Area | Score | Reason |
| --- | --- | --- |
| Portrait rendering | 8 | Faces and crop remain clear without taking board space. |
| Portrait ownership | 6 | Your Turn appears beside the newly added opponent portrait. |
| Reticle presentation | 9 | The sight is clear, cell-centered, and contained at A1. |
| Board balance | 9 | Both ten-by-ten grids remain aligned and equally sized. |
| Roster readability | 6 | All letters fit, but several names split into poor fragments. |
| Confirmation clarity | 9 | All languages fit and emphasize the safe action. |

# Final bounded visual review

Date: 2026-10-29.

## Evidence

I re-inspected the updated human-turn, computer-turn, and confirmation captures in all three languages.
The files use the same capture directory with `game`, `computer`, and `confirmation` prefixes.
This pass reviews only the roster divisions, captain relationship, and effects on the surrounding layout.

## Resolution

The roster correction resolves the required readability finding.
Dutch names now divide as Vliegdek-/schip, Onder-/zeeër, and Torpedo-/boot.
French names now divide as Porte-/avions and Sous-/marin.
These divisions preserve recognizable word parts instead of leaving isolated final letters.
All roster labels remain above their status markers without clipping.
English labels remain legible on one line.
Both boards retain equal size and alignment across all three languages.

The active captain relationship is now consistent.
The human-turn label appears beside the original captain on the left.
The computer-turn label appears beside the opponent captain on the right.
This preserves the user's earlier left-captain preference without associating Your Turn with the opponent portrait.
The translated computer-turn labels fit, including the longer French wording.
Shot feedback occupies a separate area between the captain groups.
The hit cross remains visible inside the reticle without losing its shape.
No old square targeting corners appear in these captures.

The three updated confirmation dialogs retain clear text, safe margins, and distinct buttons.
No regression is visible in these dialogs.

## Final verdict

The native visual change passes this independent game UI/UX review.
No material visual findings remain in the reviewed captures.
The earlier Home, Help, and Settings approval remains unchanged.
The final review covers all three languages and both active-turn presentations.

This decision does not claim actual television viewing-distance acceptance or prove cursor movement.
The coordinator's device and directional-input tests establish those separate behaviors.
The supplied static captures establish layout, state presentation, and visible text only.

## Final gameplay scores

Scores use a ten-point scale and describe the final captures only.

| Area | Final score | Reason |
| --- | --- | --- |
| Portrait rendering | 8 | Both faces retain clear crops and matching presentation. |
| Captain relationship | 9 | Each active-turn label now identifies its captain consistently. |
| Reticle presentation | 9 | The empty sight remains clear around an existing hit marker. |
| Board balance | 9 | Equal grids retain their dimensions through every language. |
| Roster readability | 8 | Intentional divisions preserve readable names within the available footer. |
| Confirmation clarity | 9 | All translated dialogs retain clear consequences and safe initial selection. |

# Incremental tactics page review

Date: 2026-10-29.

## Scope and evidence

I inspected `tactics-en.png`, `tactics-nl.png`, and `tactics-fr.png` in the supplied native capture directory.
I also read `tactics-review.md` for the rule constraints and intended diagram patterns.
This is a visual and instructional review of the added Tactics page.
It does not repeat the previous screen reviews or claim independent source-code verification.

## Decision

The Tactics page passes visual review in English, Dutch, and French.
No material correction is required in these captures.
The separate page keeps advanced advice outside the main menu and the battle surface.
Four equal cards provide a clear reading order without turning the basic Help page into a long paragraph.

## Layout and readability

The two-by-two arrangement gives each diagram enough space beside its explanation.
Card padding separates the diagram, title, and body without squeezing the translated text.
Dutch titles wrap naturally onto two lines where necessary.
The French lower-left title remains on one line with clear padding before the card edge.
The longer French bodies wrap across three lines without colliding with the diagram or card boundary.
I found no clipped letters, missing accents, awkward word fragments, or overlapping content.
The Back button remains outside the cards and inside the safe content area.
Text retains the earlier Help screen's size hierarchy and quiet background treatment.

The cyan candidate dots are smaller than the hit and miss markers, but each remains visible in these full-resolution captures.
Actual couch visibility of these smaller dots still depends on screen size and viewing distance.
No control depends on selecting an individual diagram dot.
These are static examples rather than a miniature playable board.

## Diagram and instruction meaning

The first diagram shows alternating candidate cells across rows and columns.
This matches the checkerboard pattern in the rule review.
The second diagram places four candidates directly above, below, left, and right of one hit.
It does not suggest diagonal targeting.
The third diagram shifts the search pattern on each row.
The separate two-cell sunk symbol does not cover a candidate marker.
The fourth diagram places the sight inside unexplored space and leaves existing shot results elsewhere.
It does not imply a forbidden border around a ship.

The page uses different shapes for candidate cells, hits, misses, and the current sight.
The instructions state that ships can touch, which preserves the placement rule described in the source review.
The two-cell condition appears in the third heading before its reduced search pattern.
This prevents the page from recommending that pattern from the start of the game.
The text describes tactics without claiming a guaranteed win or an extra shot after a hit.

“Search one color” depends on the familiar checkerboard metaphor because the diagram uses dots rather than two colored surfaces.
The visible alternating pattern makes that meaning clear enough for this optional page.
A future revision can replace that phrase with “Search alternate cells” if play tests reveal confusion.
This is a minor wording opportunity rather than a release blocker.

## Scores and limits

Scores use a ten-point scale and describe the supplied captures.

| Area | Score | Reason |
| --- | --- | --- |
| Page organization | 9 | Four equal cards keep optional tactics separate and easy to scan. |
| Translation layout | 9 | Real Dutch and French copy fits without clipping or broken words. |
| Diagram clarity | 8 | Distinct shapes explain all four patterns, subject to small-dot visibility on a television. |
| Rule communication | 8 | The conditions and touching-ship rule remain visible without long explanations. |
| Safe-area discipline | 9 | All cards and the Back action stay clear of the screen edges. |

The coordinator reports passing focused tests for text overflow, both Back paths, and return focus.
That test report is separate from my capture-based visual decision.
I did not operate the device or inspect the test result files in this review.
