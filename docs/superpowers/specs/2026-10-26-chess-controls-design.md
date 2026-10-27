# Chess controls update

The user requests a quiet chess screen, a New game action beside Resume, and selectable Easy, Normal, and Hard opponents.

Remove the right text column and hide routine save status on the chess screen. Preserve the centered board, move controls, accessibility descriptions, check highlights, promotion, results, and storage errors.

New game uses the existing reset confirmation. Cancel receives initial focus. Confirmation clears the current session and opens fresh arithmetic directly after the reset saves.

Easy retains its existing deliberate mistakes. Normal searches replies with bounded computation. Hard searches deeper with a larger budget. A parent control selects the level and persists it separately from progress. Easy remains the default. Progress resets preserve the selected level. The selection applies to subsequent computer work. No chess rating is claimed.

## Approaches

The user selected Easy, Normal, and Hard instead of keeping the current fixed opponent. A bounded local search fits the existing offline app and avoids another dependency. An external engine adds packaging and licensing work beyond this update.

The implementation uses the existing preference storage pattern, a controller mode switch, and a shared search implementation for Normal and Hard. The implementation chooses cooperative limits of 250 ms for Normal and 800 ms for Hard, plus node limits. Root preparation and individual library calls can exceed these limits. A safe root fallback accounts for attacks on the queen and moved piece.

Tests cover legal moves, tactics, interruption, preference persistence, mode selection, remote confirmation, board layout, and quiet saves. The user authorizes overwriting current games during development. TV tests use the current app installation.
