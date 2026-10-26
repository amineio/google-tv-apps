# Forest and ivory visual polish

The parent requested fresh screenshots, an independent UX/UI comparison with the original ImageGen concepts, and matching artwork. The parent also requested parallel delegation for independent tasks. This pass implements the existing approved visual direction. It adds no game behavior or workflow.

## Design decisions

Use the independent [visual review](../../chess-math/visual-review-2026-10-26.md) as the implementation brief. Keep the board bounds and 64 cells. Use at most two new raster artifacts: an original transparent piece atlas and a quiet forest backdrop. Preserve native text, high-contrast accessibility behavior, focus, piece selection, legal markers, Dutch copy, math content, timing, and saved progress.

Keep the current vector pieces as a fallback. Decode artwork once. Convert retained PNG artwork losslessly for package size when useful. Preserve source art and prompts. Do not create an emulator or alter TV system settings.

The math screen gets stable heading/equation positions, a separate native answer well, and reserved hint space. The number pad keeps the existing keys and navigation. Refine shared focus corners and add dark rims to board state markers. Parent settings remain scrollable; keep focused Save/Cancel fully visible.

## Parallel ownership

- Capture agent: fresh baseline TV screenshots and capture log only.
- UX/UI designer: comparison report and independent final visual review only.
- Artwork agent: generated piece atlas and its provenance only.
- Layout agent: MainActivity math composition and targeted Android layout tests only.
- Piece integration agent: PiecePainter, ChessBoardView, and focused artwork/marker tests only.
- Coordinator: backdrop asset, TvStyle, shared build metadata, integration, verification, and commits.

## Execution

- [x] Capture fresh baseline screens without resetting the saved game.
- [x] Obtain the designer's comparison and concrete targets.
- [x] Generate and inspect the two artwork assets.
- [x] Apply the bounded visual changes with independent file ownership.
- [x] Test stable math geometry, remote focus, asset bounds, and existing game/session behavior.
- [x] Build, lint, and test the new APK. Preserve the Home and Clock source files.
- [x] Capture the updated TV screens and obtain an independent comparison review.
- [x] Resolve material findings, preserve current progress, and leave the reviewed APK installed.

## Verification

Run the 40 game unit tests, Android lint, release build, and remote instrumentation suite. Add a layout regression that compares heading, equation, and answer bounds before and after Hint. Inspect every atlas tile and alpha bounds. Confirm all board markers remain readable on both square colors. Record APK size, bitmap memory estimates, before/after screenshots, review findings, and remaining limits.
