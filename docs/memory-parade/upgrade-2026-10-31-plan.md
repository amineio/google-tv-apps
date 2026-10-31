# Memory Parade upgrade plan

The approved expansion adds 6-card, 12-card, and 16-card boards with eight forest animals. Keep the original forest identity and remove nonessential screen text.

Use a pure board-size-aware rules model. Store records by board size and migrate the original record only to the 12-card board. Remember the selected board and preserve sound preference.

The welcome screen contains three native board choices. The game screen contains the board, a small title, two clear counters, a compact Back/Pause hint, and an eight-place parade. Remove animal captions and decorative instructions. Keep accessibility names.

1. Add rules tests for every board, random subsets, deterministic shuffle, input guards, completion, navigation, and records.
2. Ask the coordinator to record the expected failing compile before rules implementation.
3. Add size-aware rules, preferences, menus, board layout, and animations.
4. Generate separate squirrel, raccoon, and four-place stage artwork. Inspect cutouts and record provenance.
5. Add directional emulator tests for all boards, persistence, pause, held Select, lifecycle return, completion, and real screenshot checkpoints.
6. Ask the coordinator to build, test, capture, review, install the release on the TV, and deliver through Git.

The audio owner edits GameAudio, synthesis, and raw sound files separately. This implementation preserves the existing audio event API. No timer, account, network, currency, or campaign is added.
