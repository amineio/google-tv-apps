# Battleship water and impact design

The user approved this design on 2026-10-29 and requested implementation.

## Visual behavior

Keep the current flat, top-down boards, layout, remote controls, and ship artwork.
Both boards gain dark blue moving water with soft reflections. Keep grid lines and markers clear.
Misses show a splash crown, droplets, and expanding ripples. Hits show a compact flash, sparks, and fading smoke.
Sinking uses a stronger localized burst and the existing ship reveal. Each impact lasts at most 480 milliseconds.
Keep the cyan cursor above impacts. Clip effects to the playable board and scale them to cell size.
Preserve shot rings, crosses, last-shot corners, audio, and turn timing.

## Rendering

Use native AGSL water on API 33 and later. Use bounded procedural waves and reflected light, without an FFT simulation.
Use a cached procedural ripple texture and Canvas drawing on API 26 through 32 or after shader failure.
Separate water and impact layers from static board drawing. Share board geometry and a presentation clock.
Animate only while gameplay is active, resumed, unpaused, and without a confirmation dialog.
Freeze water and discard impacts when gameplay becomes inactive.

## Impact contract

Add nonserialized `ImpactPresentation(id: Long, board: ImpactBoard, coordinate: Coordinate, outcome: ShotOutcome, startedAtNanos: Long)`.
`ImpactBoard` contains `ENEMY` and `FRIENDLY`. `SessionState.activeImpact` is nullable.
IDs increase during the session. The start time uses an injectable monotonic nanosecond source, defaulting to `System.nanoTime()`.
Create the value only when the engine accepts an attack. Human attacks target `ENEMY`; computer attacks target `FRIENDLY`.
`GameAction.CompleteImpact(id)` clears only the matching event. This action produces no sound or save operation.
Clear impacts on pause, background activity, dialogs, Settings, Home, restart, and results.
Restoration and Continue do not create effects from shot history. Presentation never applies moves or gates turns.
Render enemy information through the existing redacted player view.

## Approaches

The user selected native shaders over pre-rendered Blender frames and an embedded Three.js WebView.
The user selected calmer water than the generated concepts. Ultra-Water remains a visual reference, without copied source.
Use generated images only as design references. Keep native text, controls, ships, grid, and focus outlines.
Retain Android 8 support, current build tools, launcher artwork, and bundled audio.

## Acceptance

Test accepted and rejected attacks, stale completion, cancellation, restoration, and both target boards.
Capture repeatable start, peak, and completed effects for misses, hits, and sinking, including corners and dense boards.
Test the shader and forced fallback, directional navigation, focus return, and lifecycle interruption.
Run local unit tests, lint, APK builds, and emulator tests. Assess physical TV visuals and performance separately.
Use focused implementation tasks and a fresh independent reviewer. Keep builds and device input under one coordinator.
Release as version 1.4, version code 5. After installation, commit scoped changes and push to origin/main.
