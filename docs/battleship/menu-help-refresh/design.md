# Menu and help refresh design

Date: 2026-10-29.

## Scope

Remove the main menu subtitle and the offline opponent footer.
Keep the main menu actions and existing naval background.
Add English, Dutch, and French through the native language control.
Use native text for every label so that translations remain readable.

## Help layout

Replace the paragraph block with three equal visual cards: Aim, Fire, and Sink.
Use one short instruction below each diagram.
Keep Back and Tactics buttons below the cards.
Keep the existing five-percent safe margins.
Use the current Outfit font and cyan focus treatment.

The Aim card shows a circular reticle over an empty cell.
The Fire card shows a pale miss ring and a red hit cross.
The Sink card shows one full overhead hull.
Native code draws the diagrams and uses the current ship assets.
Do not bundle generated labels or grid diagrams as the help interface.
Keep decorative water and scenery faint behind the cards.
Do not copy the concept footer because it repeats a control instruction.

The user later supplied `30213.jpg`, a photograph with four Battleship tactics.
Use this photograph as a content reference only.
Do not bundle the photograph or its surrounding personal scene.
No online URL, original publisher, or external license was supplied.

A separate Tactics page follows the basic Help cards.
Four equal cards pair native square diagrams with short translated tips.
The diagrams show checkerboard hunting, orthogonal neighbors after a hit, shifted every-third-cell hunting, and unexplored water.
Use the third-cell pattern only after the two-cell ship sinks.
Ships can touch, so the diagrams must not exclude cells around sunk ships.
The [tactics review](tactics-review.md) records the placement rules and exact translated copy.
Hardware Back and the Tactics Back button return to basic Help.
Focus returns to the Tactics button.

## Gameplay translation

Add the second captain portrait without reducing either board.
Keep both portraits compact and visually balanced.
Keep all hidden enemy ships hidden.

Draw a circular military reticle with cardinal ticks around the current selected cell.
Move the reticle with the current cursor.
Keep past hits and misses as their standard markers.
Do not retain old square corner brackets as a last-shot highlight.
The reticle must remain clear beside a hit cross or miss ring.

## Generated images

The built-in ImageGen tool created both images from text prompts.
The concept is a preview reference, not an application screen bitmap.
The portrait shows a fictional captain without a personal likeness.
No external stock image, character, logo, or licensed artwork was used.
These are generated project assets with no external stock license.
This record does not claim copyright exclusivity.

The concept is `help-concept.png`, 1672 by 941 pixels.
Its original output is `/Users/amine/.codex/generated_images/01a1884f-bc4c-72c0-ac1a-1fa03b151ad8/exec-5373e096-042a-4776-a31c-dce201ee4057.png`.
The portrait is `opponent-source.png`, 1254 by 1254 pixels.
Its original output is `/Users/amine/.codex/generated_images/01a1884f-bc4c-72c0-ac1a-1fa03b151ad8/exec-33607673-f899-43f2-a882-10a90bec5806.png`.
The parent coordinator owns compression and native integration.

## Concept prompt

```text
Use case: ui-mockup. Asset type: polished 16:9 native television game How to Play concept, landscape 1920 by 1080 proportions. Primary request: organize a Battleship help screen into three large equal visual cards, short labels and no paragraphs. Dark navy background with very subtle still ocean water and a faint distant naval ship at far right edge. Use cyan, ivory, restrained red, no gold ornament. Wide TV safe margins of 5 percent. Small title HOW TO PLAY at top left, then three horizontally aligned equal cards taking most screen width and about half screen height. Cards have no heavy borders, navy blue panel fill, clear room between them. Each card shows a large flat overhead schematic navy-water square board crop above its brief text. Card 1 has a bright circular military targeting reticle centered on one empty square, thin cyan concentric ring with four cardinal ticks, no square corner brackets. Card 2 shows two distinct cells with pale miss ring and red hit cross, small exact legend MISS and HIT. Card 3 shows the full overhead hull of one sunk warship spanning four grid cells, quiet red embers, a simple visual of ship revealed. Under each image, typography exact text: card 1 title "Aim", body "Use arrows to choose a target." Card 2 title "Fire", body "Press OK. Then your opponent fires." Card 3 title "Sink", body "Sink all five ships to win." At bottom left a compact native cyan-focused button exact text "Back". Optional small footer aligned right exact text "Back pauses a battle." All text large, short, readable from couch. Only the supplied exact text, no extra prose, no numbers on board axes, no step numbers, no external remote illustration. Premium calm native TV game aesthetic, diagrams simple and accurate, no browser chrome, no logos. This is an interface design reference, not a bitmap UI to ship.
```

## Portrait prompt

```text
Use case: illustration-story. Asset type: square opponent captain profile portrait bitmap for a native Battleship television game. A distinct fictional adult woman naval captain, about forty-five years old, medium brown skin, dark brown eyes, dark curly hair neatly tucked under cream naval peaked cap with a tiny generic anchor symbol. Calm confident competitive expression, no hostile caricature. Dark navy wool jacket and cream knitted collar. Head and upper shoulders close-up, face centered large, whole cap contained with 8 percent headroom, deep plain navy blue backdrop. Finely painted semi-realistic game illustration with visible subtle brush texture, warm cinematic side lighting from left and cool blue fill, muted slate cream and navy palette, strong readable facial silhouette at 40 pixels. Natural human facial proportions. No real person likeness, no recognizable celebrity, no rank badges, no country insignia. No frame, no circle, no text, no name, no watermark, no brand. Square composition, no weapon.
```

## Initial design review

The concept provides a readable sequence and clear miss and hit examples.
Its water texture is too active for small native diagrams.
Native code must use the current quieter board surface.
Its large header and extra footer consume space without explaining game rules.
Use a smaller header and remove the footer.
The reticle has a central dot that can obscure the underlying hit marker.
Native code can omit that dot and keep a clear center.
The concept contains four-column crops, which are examples rather than full game boards.
Do not change the gameplay board dimensions.

The portrait matches the current captain's palette and painted texture.
Its distinct face remains centered with adequate room for a circular crop.
A fresh game UI/UX reviewer must assess the concept and rendered implementation.
This initial assessment does not replace that independent review.


## Separate exploration

Fleet tracker concepts remain a design exploration only.
This release does not implement them.
