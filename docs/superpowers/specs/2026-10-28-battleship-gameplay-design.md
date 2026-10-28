# Battleship gameplay visual design

The user requests naval artwork and three scored UI/UX improvement iterations. The coordinator chooses this direction under that authorization.

## Direction and approaches

A photographic interface adds atmosphere but cannot own controls or board state. A technical chart stays clear but lacks naval character.
The selected direction combines a restrained naval command layout, one generated battleship illustration, and five distinct native ship designs.
The generated concept is inspiration. Its invented specifications, controls, and incorrect fleet content are excluded.

The enemy board remains the largest element. It keeps one focus target, all 100 cells, bold coordinate labels, and a cyan cursor.
The command panel contains the selected coordinate, battleship illustration, friendly board, and fleet damage.
Selected-target information and last-shot feedback have separate labels. No verbose shot narration returns.
Boards use calm navy surfaces. Coral crosses, pale rings, and revealed sunk hulls preserve feedback through shapes.
Grid strokes, axis highlights, subtle material depth, and distinct ship classes improve finish.
The illustration never covers interactive cells. All text and controls remain native Compose.

## Three iterations

Baseline: designer score 6.7/10. Fixed weights: visibility 30%, remote UX and status clarity 25%, visual craft 25%, naval identity 20%.
Iteration 1 implements the composition and ships. The designer reviews an actual native capture and assigns a score.
Iteration 2 addresses the most valuable visible findings. The designer reviews new captures, including a busy game state.
Iteration 3 addresses remaining polish and clarity findings. The designer rates the final native presentation with evidence limits.
A fresh independent source review covers the complete change. Scores are expert judgments, not measured user satisfaction.

## Constraints and acceptance

Keep Kotlin, Compose for TV, engine rules, AI, saves, sound, and remote input. Add no libraries, networking, or speculative features.
Maintain approximately five percent safe margins. Preserve the enemy board size at the same viewport where practical.
Use at least 13sp for fleet and secondary game labels at the standard TV density.
Do not reveal intact enemy ships. Show friendly ships from the authoritative player view.
Do not stretch one generated sprite across ship classes. Native hulls fit exact cells in either orientation.
Build debug and release APKs. Operate unit tests, release lint, and D-pad emulator tests.
Install the tested update when physical TV use permits. Preserve app data, Home, helpers, and Cast credentials.
Commit and merge installed changes into origin/main under the repository delivery instructions.
Physical viewing-distance acceptance and native 4K remain separate from 1080p emulator evidence.
