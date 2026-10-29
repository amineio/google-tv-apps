# Independent TV game UX review — final emulator assessment

## Final decision after round 2 and recorded match review

All five categories meet the requested score of at least 8. No material UX or visual blocker remains in the reviewed emulator evidence. This is an expert assessment of the supplied scope, not user acceptance or physical-TV acceptance.

| Category | Final score / 10 | Basis |
| --- | ---: | --- |
| Composition | 8.5 | Equal grids, aligned coordinate gutters, stable board placement across targeting, feedback, sinking, rematch, and pause/resume. The right fleet is no longer squeezed. |
| TV readability | 8.0 | Full-size native frames show legible coordinates, target state, ship silhouettes, damage marks, and populated last-shot feedback. Fleet name spacing remains a minor issue. |
| Ship craft | 8.5 | Five distinct overhead classes render clearly at game scale. Both orientations fit occupied cells. No sprite clipping or ship overlap was observed. Normalized proportions are an explicit game-art choice in the approved design. |
| Effect clarity | 8.0 | Revised orange/yellow fire clearly communicates hits and sustained burning. Fire covers the sunk hull at separated points, followed by cooling, fading hull, and persistent wreck marks. Miss arcs remain distinct from hit crosses. |
| Interaction stability | 8.5 | Recorded state progression shows target movement, complete effect stages, turn handoff, victory, rematch, pause, and resume with stable board placement. The supplied passing real-session test uses directional input. |

### Additional evidence reviewed

Reviewed eight full frames from /tmp/battleship-v15-round2-captures: FRIENDLY-HIT-E5-600, FRIENDLY-SUNK-E5-1000, ENEMY-MISS-J10-500, ENEMY-MISS-A1-160, and ENEMY-SUNK-E5 at 300, 1000, 2000, and 2499ms. These resolve the round-1 fire finding below.

Inspected /tmp/battleship-v15-live-match.mp4 through its contact sheet, full-size frames at 20, 30, 40, and 70 seconds, an additional frame at 42 seconds, and sequential samples extracted at 250ms intervals for 29–35, 67–74, and 73.5–77.5 seconds. The recording is 77.53 seconds at 1280×720 and has no audio stream. This was a temporal sample review of an actual real-time recording, not a claim of continuous playback or measured rendering performance.

The recorded carrier sinking sequence shows ignition, distributed fire, cooling, and a dim hull before incoming attack feedback replaces the player's result. The final destroyer sequence reaches dim wreck marks before the separate victory screen appears. Victory then leads to a fresh A1 board; pause and resume retain the board layout and return to A1.

Live frames show YOUR SHOT with Carrier sunk, and the additional 42-second frame shows incoming shot feedback during the actual match. The target coordinate and state remain in the left strip while result ownership remains in the right strip. The Ready text in earlier effects fixtures is confirmed as fixture construction, not a product defect.

The coordinator reports 50 debug unit tests, 50 release unit tests, 16 device tests, lint, builds, and the actual-session test passing. This reviewer did not run those checks. Source inspection and the recording provide complementary evidence, not a separate test execution.

### Remaining limits and minor polish

- Fleet labels have tight spacing, especially Battleship/Cruiser and Submarine/Destroyer. No clipping was observed. This is nonblocking polish.
- Sampled video frames support sequence order and stable placement. They do not establish frame rate, frame pacing, continuous smoothness, or hardware remote latency.
- Silent emulator video does not establish sound quality, synchronization, volume, or TV speaker behavior.
- Physical-TV viewing distance, panel contrast, hardware input, and user acceptance remain separate.

---

# Round-1 historical review

Reviewer role: senior game UX designer for TV and remote strategy games.
Date: 2026-10-29. Review only; no builds, edits to the app, or device input.

## Evidence and limits

Reviewed the root README, Battleship README, approved sprite design, GameplayScreen.kt, GameBoard.kt, BoardGeometry.kt, and relevant presentation test source. Checkout: /Users/amine/Developer/google-tv-apps, main, HEAD bb99134a21c03cae20a791be318f2e12c4df39c2, with active uncommitted implementation.

Viewed these full 1920×1080 captures from /tmp/battleship-v15-round1-captures:

- battleship-sprites-pause-resume.png
- battleship-sprites-FRIENDLY-HIT-E5-completed.png
- battleship-sprites-ENEMY-SUNK-E5-completed.png
- battleship-sprites-ENEMY-MISS-J10-500.png
- battleship-sprites-FRIENDLY-SUNK-E5-1000.png
- battleship-sprites-FRIENDLY-HIT-E5-600.png

The coordinator reports 16 passing emulator tests and 50 passing unit tests. I inspected relevant test coverage, but did not execute or independently inspect the test report. Still frames establish appearance at sampled times. They do not establish smooth motion, real-time effect duration, audio synchronization, remote latency, or physical-TV readability.

## Provisional scores

| Category | Score / 10 | Assessment |
| --- | ---: | --- |
| Composition | 8.5 | Both grids have equal visual size and aligned edges. The right fleet has enough space. The fixed status strip and fleet roster preserve board area. |
| TV readability | 8.0 | Coordinates, cyan target, shot symbols, and ship silhouettes are distinct in the full capture. Fleet names have tight spacing. Physical viewing-distance review remains open. |
| Ship craft | 8.5 | Five visible classes have distinct overhead silhouettes and useful deck detail. Hulls fit their cells without overlap in the reviewed fleet. The narrow submarine is recognizable, although its dark hull has the least contrast. |
| Effect clarity | 6.5, round 1 only | Miss rings and permanent hit marks are legible. The sampled hit and sunk stages look mostly like gray smoke; burning is not clearly communicated. Round-2 fire/splash changes require new capture review. |
| Interaction stability | 8.0, provisional | Source uses one board focus target, explicit directional input, nonrepeating fire, and focus restoration after pause. The resumed B2 capture agrees with this model. Supplied tests cover directional input and fixed bounds. Live interaction and motion review remain open. |

These are expert judgments, not user acceptance. The target of at least 8 in every category is not yet met because effect clarity remains open.

## Findings

### Material: round-1 burning lacks a clear fire stage

The friendly sunk frame at 1000ms shows a dim hull and gray smoke over pale damage crosses. The hit frame at 600ms is similarly muted. The visual cue does not clearly communicate ignition and sustained fire at full-screen scale. A distinct warm fire stage on the exact hull is needed. The coordinator reports this is already being revised in round 2. Reassess the revised sampled frames and real-time clip before acceptance.

### Minor: fleet roster labels need more separation

In the pause/resume fleet, Battleship and Cruiser nearly join, as do Submarine and Destroyer. There is no observed clipping. Each group retains separate health blocks, so the information remains usable. Slight spacing or a compact abbreviation would improve scanning, but this does not block the layout score.

### Evidence gap: these effects fixtures cannot establish live last-shot clarity

The supplied effects captures show LAST SHOT — Ready while containing shot markers. Test source confirms that the fixtures replace board shots without populating lastAttack. This is a fixture limitation, not an observed product defect. A native long-feedback or ordinary-gameplay capture with populated lastAttack is needed to judge the full target/result hierarchy visually. Source separates TARGET from YOUR SHOT/INCOMING, which is appropriate. The source also includes the shot coordinate in accessibility semantics; the visible result strip omits it.

## Stable layout and artwork conclusion

No material layout or ship-art blocker appears in the reviewed native captures. The user-reported right-side squeeze is resolved visually. Both grids use the same coordinate gutter. The cyan target remains above ship effects and marks in the reviewed images. Enemy hulls remain hidden in the unsunk fixture; the sunk fixture reveals a dim hull. The status strip is well separated from the boards, and long feedback has explicit fixed-bounds and text-overflow test coverage.

## Still required

- Review revised fire/splash captures and replace the round-1 effect score.
- View a native frame with YOUR SHOT or INCOMING and long sunk feedback.
- Review the real-time clip for complete effect stages, turn pacing, and stable target position. Do not infer motion acceptance from staged stills.
- Keep physical-TV reading distance, hardware remote behavior, and audio listening acceptance separate from emulator evidence.
