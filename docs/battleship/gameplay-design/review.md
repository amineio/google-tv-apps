# Battleship gameplay design review

The designer uses the same rubric for the baseline and three native iterations.
Weights: visibility 30%, remote UX and status clarity 25%, visual craft 25%, naval identity 20%.
Scores express expert judgment from source and native captures. They do not measure user satisfaction or physical viewing distance.

| Round | Visibility | UX | Craft | Identity | Weighted score |
| --- | --- | --- | --- | --- | --- |
| Baseline 1.1 | 7.2 | 7.4 | 6.1 | 5.7 | 6.7/10 |
| Iteration 1 | 7.8 | 8.2 | 8.0 | 8.1 | 8.0/10 |
| Iteration 2 | 8.3 | 8.5 | 8.0 | 8.5 | 8.3/10 |
| Iteration 3 | 8.9 | 8.9 | 8.6 | 8.8 | 8.8/10 |

## Iteration 1

The native command panel separates the selected target from actual shot feedback.
The generated illustration adds identity without covering either board. Five distinct native ship classes replace the shared hull.
The larger fleet labels, board gradients, quiet borders, and consistent spacing improve hierarchy.
The designer found no material issue in the ship geometry or hidden-ship boundary.

The next round addresses three findings: a smaller friendly grid, a weak last-shot marker, and the panel contrast and spacing.
The source review confirms fitted hulls and both rotations. Captures exercise horizontal, vertical, damaged, and sunk ships.
The capture fixtures apply legal engine commands with a fixed seed. They do not control production outcomes.

## Iteration 2

A shorter art header and feedback strip give the friendly board more room.
Larger coordinates and warm corner brackets improve incoming-shot visibility. A darker panel keeps the art clear.
The presentation test passes. The designer rates this round 8.3/10.

The final round addresses target text touching a divider, an extra center cross on attacked cells, and color-only damage bars.
It also adds a legal 72-shot fixture and larger friendly coordinates that meet the documented 13sp target at normal TV dimensions.

## Iteration 3

The target header uses measured content height. The status text clears the divider without clipping.
The selected cell keeps its cyan outline but removes its extra center cross after a shot.
Damaged fleet segments have diagonal slashes as well as coral color. Larger friendly coordinates use a wider gutter.
The designer rates the final screen 8.8/10 and finds no remaining material visible defect in the four native captures.
A fresh independent reviewer accepts the source and corrected 1080p captures with no material finding.

The score covers native screenshots and source. Physical sofa distance, panel contrast, overscan, motion comfort, and native 4K require separate assessment.
The designer states that further scoring needs user observation, not more decoration.

## Native capture record

The previews below are compressed copies of actual 1920 by 1080 emulator captures.
They show deterministic test fixtures generated through legal engine attacks, rather than fixed production outcomes.
The final late-game fixture contains 72 shots per side and three sunk ships per side.

[Iteration 1 overview](iteration-1-overview.webp) and [damaged fleet](iteration-1-dense.webp).
[Iteration 2 overview](iteration-2-overview.webp) and [damaged fleet](iteration-2-dense.webp).
[Iteration 3 overview](iteration-3-overview.webp), [computer turn](iteration-3-computer-turn.webp), and [late game](iteration-3-late-game.webp).

## Evidence notes

The target/result regression failed before implementation because separate target semantics were absent.
Iteration 1 rendered all requested fixtures. One prior test matched both the target Hit label and the last-shot Hit result.
The test now selects the actual last-shot region. Its original compact-feedback requirement remains unchanged.

The final capture caught clipped target text before acceptance. The new TextLayoutResult overflow assertion failed before the measured-height repair and passed afterward.

The [release capture](release-gameplay.webp) shows an actual randomized match after directional aiming, one human shot, and one computer response.
