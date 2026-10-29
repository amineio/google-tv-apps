# Missile flight integration audit

This report recommends a native presentation sequence above the existing two boards. It does not propose a game engine migration.
This is a read-only source audit with a documentation output. No app source, build, or device changed.

## Direct source observations

- `GameSession.fire()` accepts, commits, and persists the complete attack immediately. It emits FIRE and the impact cue in the same action.
- `GameSession.scheduleComputer()` also commits the incoming attack before its impact finishes. It waits for the current impact before another attack.
- `SessionState` has one `activeImpact`, a `resolving` input gate, and no flight state. `cancelTurn()` invalidates delayed work through a generation number.
- Current impact durations are 1,100 ms for MISS, 1,500 ms for HIT, and 2,500 ms for SUNK. These share one monotonic deadline.
- `GameplayScreen` reads current shots, sunk ships, last attack, winner, roster damage, and accessibility descriptions directly from the committed game.
- Therefore, simply adding a flying missile would reveal its outcome before arrival. Suppressing only the explosion would leave several early result signals.
- `MusicController` ends battle music when the authoritative game becomes GAME_OVER. A final-shot flight needs an explicit presentation policy for that transition.
- `GameBoard` owns one focus target. The selected cell can move while a turn resolves, but `resolving` prevents another accepted shot.
- `BoardGeometry` converts cells into board-local pixels. Each board currently clips its impacts to its own grid bounds.
- `BoardPresentation` requests frames only during active impacts. Its completion callback clears a matching impact. Session delays govern turn progression independently.
- `MainActivity.onPause()` disables audio and backgrounds the session. Pause, Home, replacement, and background actions discard impacts and cancel turn work.
- `GameAudio` streams music separately from SoundPool effects. It supports mute, focus loss, background cancellation, and temporary music ducking.
- `Board.shipSunk()` identifies surviving friendly ships. `GameEngine.playerView()` exposes only sunk enemy hulls to the board renderer.

Relevant source locations are `GameSession.kt:131`, `GameSession.kt:150`, `GameSession.kt:192`, `GameplayScreen.kt:32`, and `MusicController.kt:77`.
Other inspected files are `SessionState.kt`, `MainActivity.kt`, `GameAudio.kt`, `ImpactEffects.kt`, `BoardGeometry.kt`, `BoardPresentation.kt`, and `GameBoard.kt`.

## Recommended minimum architecture

Use one `AttackPresentation` with an event ID, attacker, fixed destination, launch anchor, launch time, arrival time, and presentation stage.
Its stages are FLIGHT and IMPACT. Keep the outcome unavailable to the flight renderer.
Keep the accepted attack atomic in the existing game model and persistence path. Keep transient flight data out of saved matches.
During FLIGHT, render a pre-attack `PlayerView` snapshot for all result-dependent presentation. Use the current committed model after arrival.
This snapshot prevents early markers, damage counts, sunk hulls, winner labels, last-shot feedback, and spoken result descriptions.
It must also guide music eligibility until arrival. Do not pass an invented game model back into rules or persistence.

Use one session-owned sequence and monotonic clock for launch, arrival, impact start, and turn completion.
The renderer samples that sequence. It must not independently trigger attack acceptance, sound, or the next computer turn.
At arrival, expose the committed result, emit its impact sound once, and create the existing impact with a fresh start time.
Then retain the complete existing impact duration before the next attack or result theme.
Generation checks must cover arrival callbacks and impact completion. Stale renderer callbacks must never advance turns.

This adds a presentation projection and one phase before existing impacts. It avoids a second rules implementation and a new engine dependency.
The main engineering cost is synchronization and interruption behavior, rather than drawing the missile.

## Launch source and hidden information

Choose a surviving friendly ship before the accepted shot changes presentation. Use a stable selection rule or rotate among surviving ships.
Choose a plausible deck anchor from that ship's own cells. A damaged ship remains eligible until all its cells are hit.
Bind the launch anchor and target to the event. Moving the sight during flight must not redirect the missile.
Treat all surviving ship types as presentation launch platforms for this game. Avoid a new weapon-capability rule unless separately requested.

Never launch enemy return fire from a hidden enemy ship cell. That would reveal protected placement information.
Use an incoming screen-edge launch point independent of enemy ship coordinates. An off-board wake and smoke entry can imply a distant attacker.
Its trajectory must not depend on hidden hulls. A known sunk ship must never appear to launch.

## Coordinates, focus, and drawing

Add one decorative overlay above the shared gameplay board container. It spans both boards and the gap between them.
Measure both board rectangles relative to that container. Convert `BoardGeometry.center(cell)` through the measured board offset.
Do not use screen coordinates, fixed pixel offsets, or assumed equal board positions. The overlay and board geometry must share one coordinate space.
Do not place the flight inside either board's clipped impact layer.
Keep layout coordinates separate from event data. Resolve cell anchors against current measured rectangles.
If geometry becomes unavailable during a layout transition, cancel presentation safely instead of drawing toward stale coordinates.

Keep the overlay outside the focus graph and pointer handling. Preserve the existing targeting board as the only aiming focus target.
Freeze the launched target while allowing the selected sight to move. Keep repeated Fire rejected during resolution.
Keep reticle and text above low-opacity smoke where needed. Smoke must not cover both boards for the full flight.

## Visual sequence and realism

Use 600 to 850 milliseconds for the minimum prototype. Compare an optional cinematic flight of 900 to 1,300 milliseconds separately. Neither range is measured, and no timing decision is accepted yet.
Use a brief ignition flash, a rising departure, a fast traverse, and a terminal descent toward the exact cell center.
Use trajectory tangent for missile orientation. Use size, shadow separation, exhaust brightness, and smoke decay to imply height.
Keep the existing overhead impact plane. Do not tilt the entire splash or board to suggest missile height.
Use bounded, pooled smoke particles or a small sprite sequence. Cache textures and avoid allocations on each frame.
Keep the overall direction stable between repeated shots. Small deterministic trail variation can prevent identical-looking exhaust.

For this fixed overhead view, a guided trajectory is an art-directed path, not a gravity-only projectile simulation.
A parabolic cannonball path communicates a different weapon. A full rigid-body simulation adds little visible value for a fixed launch and target.
Depth cues and timing can provide strong apparent volume without adding world-scale collision physics.
These are design recommendations. This audit does not measure physical accuracy or compare engines through benchmarks.

## Sound sequence

Separate the launch transient from the current impact sounds. Add a short exhaust or flyby layer only while the flight remains active.
Arrival triggers MISS, HIT, DAMAGE, or SUNK once. The existing result theme follows the full final impact.
Keep battle music audible at a reduced gain during launch and impact. Keep one Sound control for all audio.
The current SoundPool limit is three streams. Budget launch and exhaust overlap so that arrival does not steal an essential impact sound.
Store stream handles by event or stage. Stop canceled exhaust immediately rather than waiting for its recording to finish.
Avoid promising positional audio realism from left-right panning alone. Preview modest stereo movement and test mono clarity.

## Interruption policy

On pause, background, Home, confirmed replacement, or restore, discard flight and impact presentation and stop its sound streams.
Keep the already committed attack. On Resume or Continue, show saved markers without replaying launch or impact.
For a non-final human shot, resume the remaining computer turn once. For an interrupted computer shot, return to the committed player turn.
For a final shot, retain existing result behavior without delayed stale explosions or duplicate result themes.
Make this policy explicit because committed game state can precede visible impact during uninterrupted play.

Mute cancels current flight audio without canceling its visual sequence. Re-enabling sound must not replay an old launch or result cue.
Audio focus loss also stops active flight streams. Visual flight can finish if the activity remains foreground.
New matches invalidate all old event IDs and jobs. Rapid restart must never finish an earlier missile on the new board.

## Minimal acceptance spike

Use the emulator first. This research task does not require a build or physical TV session.
For implementation, first use a simple missile shape and bounded smoke to prove event timing and geometry before final art.

Focused session tests must cover these behaviors:

1. One accepted shot commits once. No outcome-dependent view or sound appears before arrival.
2. Arrival exposes the exact target once. Each outcome retains its existing full impact duration.
3. Final result text and music wait until the defined sequence stage. The next turn does not overlap flight or impact.
4. Pause, background, restart, and restored saves discard transients without losing or duplicating attacks.
5. Stale arrival and completion callbacks cannot change a newer match. Mute and focus loss cannot replay canceled audio.
6. Friendly launch uses an unsunk hull. Enemy entry is invariant to hidden enemy ship placement.

Focused renderer and emulator evidence must cover both board edges, screen-size changes, actual D-pad movement, and focus after pause.
Capture launch, mid-flight, arrival, and impact from the current build. Compare the target cell center with the arrival position.
Record allocations, frame timing, and peak particle count over a short repeated-shot scene. Compare against the unchanged scene on the same emulator.
Emulator GPU timing cannot establish Chromecast performance or audible quality.
Do not make physical tests an automatic gate. If the user requests hardware review or reports a problem, profile only this changed scene.
A bounded Chromecast session can compare frame timing, audio overlap, and foreground/background cancellation. It does not require another deep TV suite.

## Material risks

The highest risk is premature disclosure through secondary presentation, especially roster state, spoken labels, winner text, and music transitions.
The next risk is two independent clocks advancing the turn twice or starting impact after cancellation.
Other risks are hidden enemy launch origins, board-local clipping, stale measured coordinates, excessive transparent smoke overdraw, and SoundPool stream pressure.
The recommended projection, one sequence owner, event IDs, shared coordinates, particle bounds, and focused tests directly address these risks.
