# Two-player chess and Battleship

Status: Approved by the user on 2026-10-30. Implementation is authorized for both games. This document records design requirements, not runtime verification.

The [execution record](../../two-player-games/validation-2026-10-30.md) records results and current delivery requirements.
The later standing instruction requires physical release installation before delivery to `origin/main`. It replaces the emulator-only delivery rule recorded below.

Source revision: `4aa14ef36a269293fb9361b9fb5ffc50a0595496`, on `amine/two-player-games` in the isolated `two-player-games` worktree.

## Intent and confirmed choices

Add a two-player mode to both existing games. Let each participant choose a generated portrait and use a custom name or the character name. Keep the experience suitable for a TV remote and readable from a couch.

The user explicitly requested these behaviors and art choices:

- Add two-player modes to chess and Battleship.
- Reveal both Battleship fleets once at the start, for ten seconds, then hide them and alternate turns.
- Use generated selectable portraits. Keep the eight animal portraits for chess.
- Give Battleship portraits more naval personality. Include all eight proposed captains, both animals and humans.
- Use chosen names or character names instead of visible numbered player labels.

The approved design uses one shared TV and one remote that participants pass between turns. The user also approved execution through subagents.

## Scope and boundaries

Keep the existing computer mode in each app. Add an explicit choice between the computer and a local opponent when starting a new game. Continue opens the saved game directly, with its saved mode and participants.

Keep chess in Java with its native Views and Canvas board. Keep Battleship in Kotlin and Compose for TV. Keep each app's resources, package, version, persistence, tests, and installer independent.

Do not add networking, accounts, Family Home profile integration, shared storage, online play, statistics profiles, or phone controls. The chosen names are local app data. Do not put real family names or captures in Git.

Retain one current saved session per app. A new match replaces it only after an explicit final confirmation. Keep the current launcher artwork and audio catalog. This change does not create a new app or require new launcher resources.

## Approaches and selected direction

| Approach | Tradeoff | Decision |
| --- | --- | --- |
| Reveal both fleets once, then alternate shots | Adds memory to normal Battleship targeting without repeated interruptions | Selected by the user |
| Reveal both fleets before each round | Reduces memory demands but repeatedly interrupts the match | Not selected |
| Use private phone screens or ask one person to look away | Supports private ship placement but adds devices or an unenforceable screen rule | Outside the approved shared-TV scope |

For chess, the existing rules already support either color. Make both sides human-controlled in local mode instead of replacing the rules engine. Keep White at the bottom so that the board and directional controls remain stable.

## Participants, portraits, and names

Use separate, app-owned portrait catalogs with stable string IDs. Do not save drawable integers, file paths, or bitmap data in a session. Package the final compressed portrait resources inside their respective apps.

The chess catalog contains Fox, Owl, Bear, Cat, Panda, Rabbit, Turtle, and Dragon. The Battleship catalog contains naval Fox, Owl, Bear, Cat, Captain Storm, Commander Reed, Lieutenant Kai, and Chief Mira. The animal captains have supplementary roles: strategist, navigator, engineer, and scout.

Use character names as the default display names. Resolve default names through each app's existing localization resources. Store custom-name metadata separately from the portrait ID so that a language change updates default names but preserves a custom name.

Offer an optional native TV name editor with Save and Cancel. Keep name entry single-line, with a limit of 24 Unicode code points. Trim surrounding whitespace and reject control characters. An empty name returns to the selected character's default name.

Do not open the keyboard when a participant accepts the default name. Changing a portrait updates its default name. It preserves an explicitly saved custom name until the participant edits or clears it.

Require different portraits within a local match. Keep distinct defaults, such as Fox and Owl. Exclude the first participant's selected portrait from the second participant's choices. If a saved ID is unknown, choose a deterministic available fallback without assigning the same portrait twice.

Duplicate custom names are allowed. Keep the portraits visible beside names. Chess also shows White or Black. Battleship board headings, turn prompts, handoffs, and accessible descriptions remain linked to the owner's portrait and full chosen name.

Use the chosen name throughout setup, turns, board ownership, pause summaries, handoffs, results, and accessible descriptions. Internal seat identifiers can remain stable legacy values. Never expose `Player 1`, `Player 2`, `HUMAN`, or `COMPUTER` as participant names in local mode.

Keep full names in saved data and accessible labels. Use a single-line visual label with ellipsis when necessary. Provide enough label width for the 24-code-point limit at the supported font sizes.

## Setup and cancellation

Keep setup as a draft until Start succeeds. Back, Cancel, and leaving setup preserve the existing saved match and its participants. A setup preview must not start a computer task, erase a match, or change saved solo preferences.

Choosing the computer keeps the existing solo setup, portraits, and preferences. It does not require a second participant name or portrait. The local setup sequence is:

1. Choose the computer or a local opponent.
2. Choose the first participant's portrait and optional name.
3. Choose the second participant's portrait and optional name.
4. Review the participants and the game-specific rules.
5. Confirm replacement if a saved session exists.
6. Start the new session through one controller action.

Use a native two-by-four portrait grid. Show the participant currently being edited with a clear instruction and selected portrait. Distinguish D-pad focus from portrait assignment through different outlines and badges. Do not use the same glow for both meanings.

Use actual directional focus movement. Preserve focus after the keyboard, dialogs, Back, app return, and canceled replacement. Keep the lower-right area clear of the separate Clock overlay.

For local chess, the first setup slot plays White and the second plays Black. For local Battleship, the first slot owns the left board and takes the first shot. Rematch keeps these assignments and chosen names. Changing assignments requires an explicit new setup.

## Chess behavior

Preserve the agreed forest-green, ivory, and honey palette, calm mood, existing forest backdrop, existing piece artwork, and board-first layout. Animal portraits complement that design. Do not introduce naval styling or broadly restyle chess.

Local mode starts directly in chess. It does not run arithmetic rounds or apply the forced thinking delay. Preserve the saved solo arithmetic and thinking preferences. Do not change them when entering or leaving local mode.

Allow input only for the color to move. Use one human-turn predicate for selection, ordinary moves, promotion, and intended draw moves. Do not schedule or apply computer replies in local mode, including callbacks that started before replacement.

Keep the existing board orientation, legal-move handling, material totals, animations, sound preferences, and draw rules. Both colors must support all promotions, castling, en passant, draw claims, and saved restoration. Keep checkmate and draw precedence unchanged.

Show small chosen portraits beside the board. Show each chosen name and White or Black. Highlight the active participant with both shape and text. Use a turn message such as “Owl's turn” instead of the shared-screen phrase “Your turn.”

Keep remote focus, selected piece, legal destination, last move, and check visually distinct. Keep input blocked through move presentation. Consume held confirmation repeats before accepting a move for the next participant.

After a result, retain the final board until an explicit rematch or new setup. Local rematch starts another chess game with the same participants. It must not enter arithmetic or revert to the computer mode. Existing solo reset and continuation behavior remains mode-specific.

## Battleship reveal and lifecycle

Explain the memory rule before starting the timed reveal. Show each chosen name and portrait in a sequential Ready step. Both Ready steps must finish before the countdown begins.

Create fresh random fleets using the existing placement rules. Save the new match with its one-time preview eligibility consumed before showing any ship positions. If that save fails, retain the previous match and offer the normal retry or cancellation path. Do not reveal ships before save success.

The countdown belongs to session control and uses a monotonic clock. It lasts ten seconds while the reveal remains active. Show `10` with a full progress bar initially, then reduce the number and bar together. Block targeting and firing during the reveal.

The reveal is transient presentation state. Do not restore it from a saved game. Pause, app background, focus loss, Activity recreation, and process restoration immediately hide both intact fleets and consume any interrupted preview. A translucent pause panel must never leave revealed ships underneath it.

Continue, Resume, relaunch, and restored handoffs never grant another look at the same fleets. If an interruption ends the preview early, explain that the fleets remain hidden. Restart requires confirmation and creates new random fleets with a new ten-second preview.

After the countdown ends, enter the first participant's turn with both intact fleets hidden. The Ready press that preceded the countdown cannot become a shot. The first participant targets the right board.

## Battleship boards and public information

Keep the left board assigned to the first setup slot and the right board assigned to the second. Do not swap boards when the turn changes. Label each board with its owner's portrait and chosen name, for example “Fox's fleet” and “Captain Storm's fleet.”

After the reveal, remove intact ship data from both rendered board projections. An opaque overlay is insufficient. Keep public hit markers, miss markers, and sunk ships. Keep hidden positions out of accessible descriptions and other UI semantics.

Do not show damage segments computed from private unsunk ship cells. They disclose which intact ship a hit belongs to. A local roster can show public sunk outcomes and aggregate ships remaining. Count labels must name the relevant fleet.

Place the targeting reticle only on the opponent's fixed board. Put the selected coordinate beside that board. The owner's other board remains visible but cannot receive an attack. Keep grid labels, focus, hit markers, miss markers, and sunk shapes native.

Preserve the current ten-by-ten boards, five ship sizes, touching-ship rule, edge clamping, and alternate-turn rule. A hit does not grant another shot. An already-targeted cell preserves the current turn.

## Battleship shots and handoff

Use the existing rules engine for both seats. Treat legacy `HUMAN` and `COMPUTER` values as internal side identifiers in local mode. Do not rename serialized enums merely to change visible labels.

The accepted-shot sequence is:

1. Accept one shot from the active human seat.
2. Save the completed move and its next active seat.
3. Show the complete shot effect on its fixed target board.
4. Keep targeting and firing blocked until saving and presentation finish.
5. Show the next participant's portrait, chosen name, and Ready action.
6. Consume the Ready press and its repeat events before enabling targeting.

Saving and visual presentation can overlap, but neither can expose the next turn early. A save error pauses handoff until the write succeeds. A failed or repeated attack never enters this sequence.

Keep the effect's board identity stable while the rules phase changes. Cancel stale countdown, effect, handoff, and computer callbacks on replacement or background activity. Preserve unique effect IDs. Continue must not replay a saved explosion, sound, or countdown.

On restoration of an unfinished local match, open paused and hidden. After Resume, show Ready for the saved active participant. Do not replay the previous shot effect. A held OK button cannot complete Ready and fire a shot in the same press sequence.

On a win, wait for the final sinking effect and save. Then show the winning portrait and chosen name, with both fleets fully visible on the result screen. Use local celebration for either winning seat. Do not play a defeat cue merely because the winner uses the legacy computer seat.

Completed matches can use the full-fleet result projection after restoration. Keep the existing completed-match menu and Continue behavior unless the local result flow requires a named participant label.

Rematch keeps the chosen participants and assignments, generates new fleets, and performs a fresh Ready and reveal sequence. Return to the menu retains the current saved session.

## Persistence and migration

Persist the mode, stable portrait IDs, custom names or default-name metadata, game position, and active seat. Keep the existing app-private atomic storage approach. Keep pending setup outside the committed session.

Chess writes schema 3 and continues to accept schemas 1 and 2. Old saves default to computer mode with default character identities. Preserve their moves, phase, exercise, draw history, pending promotion, and thinking time.

Read the mode before validating a saved selection. Local chess permits either color's selected piece and promotion, provided it belongs to the side to move. Require the chess phase and zero forced thinking time for local sessions. Include all new fields in `Session.copy()`.

Battleship must accept the existing schema-1 saves as computer games. Introduce explicit version support for the added participant metadata. Keep current board and move-history validation. Old saves never gain a reveal. Restoration of unfinished local matches always uses hidden presentation state.

Unknown modes fail through an explicit invalid-save recovery path. Do not silently change a local game into computer mode. Unknown portrait IDs use deterministic distinct fallbacks. Invalid custom-name data uses a valid bounded name or the character default without changing the match rules.

Replacement, reset, retry, Continue, Resume, and rematch must preserve or set mode intentionally. Invalidate old computer work when changing the committed match. A late callback cannot move a piece or fire in a replacement local game.

## Verified code seams

These findings refer to the source revision above. Line numbers identify current code, not proposed implementation positions.

| App and source | Current behavior | Planned responsibility |
| --- | --- | --- |
| Chess `Session.java:8`, `copy():31` | No mode or participant identity | Save and copy the added session metadata |
| Chess `SessionController.java:182`, `212`, `301` | Selection and play require White | Use the shared human-turn predicate |
| Chess `SessionController.java:353` | Every Black move creates computer thinking behavior | Apply it only in computer mode |
| Chess `SessionController.java:435` | Schedules Black computer replies | Guard both scheduling and result callbacks by mode and match generation |
| Chess `SessionController.java:139`, `284` | Continuation and reset use solo assumptions | Preserve local mode and participants deliberately |
| Chess `SessionCodec.java:13`, `34` | Writes schema 2 and accepts schemas 1 and 2 | Write schema 3 and preserve both older migrations |
| Chess `ChessScoreView.java:13`, `MainActivity.java:647` | Uses You/Computer and White-human turn text | Render chosen names, portraits, and active color |
| Chess `MainActivity.java:372`, `548` | Start and chess screens lack participant setup | Add draft setup and both-color presentation |
| Battleship `GameEngine.kt:16` | Accepts either internal attacker seat | Reuse rules and turn alternation |
| Battleship `GameEngine.kt:45` | The viewer's own fleet is fully visible | Add a separate public projection for shared-screen local play |
| Battleship `GameSession.kt:131`, `150` | Fire uses the human seat and resume can schedule the computer | Route the active local seat and prohibit local computer tasks |
| Battleship `GameSession.kt:230`, `SessionState.kt:9` | Effects use relative enemy/friendly board identifiers | Preserve a fixed target identity during local effects |
| Battleship `GameplayScreen.kt:32`, `113` | Solo boards and private per-ship damage roster | Render fixed owner boards from public local data |
| Battleship `GameBoard.kt:75`, `90` | Native keys and initial-press OK filtering | Keep edge navigation and add cross-turn repeat protection |
| Battleship `GameSaveRepository.kt:15` | Schema 1 has one match and no participant metadata | Migrate mode, portraits, and local names |

Chess paths above are relative to `apps/chess-math/src/main/java/com/amine/chessmath/`. Battleship paths are relative to `apps/battleship/src/main/kotlin/com/amine/battleship/`.

## Art and concept evidence

The [concept manifest](../../two-player-games/concepts/assets.json) records exact ImageGen prompts, references, dimensions, and hashes. The final concept files are:

- [Chess portrait chooser](../../two-player-games/concepts/chess-portrait-chooser-concept.png).
- [Chess gameplay](../../two-player-games/concepts/chess-two-player-concept.png).
- [Battleship reveal and hidden boards](../../two-player-games/concepts/battleship-reveal-concept.png).
- [All eight selected naval captains](../../two-player-games/concepts/battleship-captain-directions.png).

The first three images predate the chosen-name requirement. Their numbered labels are superseded. The Battleship gameplay image also predates the themed captain selection. Use those images for composition only, not current labels or final portrait assignments.

The independent game UI reviewer accepted the visual direction with required corrections. These include clear editing identity, separate focus and assignment states, fixed owner labels, memory-rule explanation, Ready handoffs, and held-button protection. This design includes those corrections.

Generate separate portrait assets with ImageGen. Do not bundle a UI screenshot as a portrait sheet. Use the approved character art as references for consistent individual portraits. Preserve ears and caps. Compress resources as WebP in each app.

Keep names, labels, focus outlines, grids, and game geometry native. Preserve the existing font licenses. Record final portrait prompts, source lineage, output dimensions, compression, hashes, and rights metadata before release. The concepts are not production resources or runtime evidence.

## Verification and delivery plan

During implementation, cover changed behavior with focused unit tests. At the integration checkpoint, operate each affected app's full relevant suite once. Keep the final release lint and packaging results. The coordinator alone operates builds and device input.

The required behavior coverage includes:

- Chess alternation, wrong-color rejection, both-color promotion, draw claims, results, and zero local computer tasks.
- Chess schema migration, Black selection restoration, no local arithmetic or thinking delay, and stale callback rejection.
- Battleship public projections, no unsunk damage leak, both target boards, duplicate shots, and local victory for either seat.
- The exact ten-second boundary, blocked reveal input, save-before-reveal ordering, interrupted previews, and hidden restoration.
- Shot-effect completion, save errors, handoff Ready, held OK, rematch, background cancellation, and no resumed computer attacks.
- Portrait and name persistence, default-name localization, custom-name editing, Cancel, invalid-ID fallback, and long or duplicate names.
- D-pad setup, grid movement, dialogs, empty or missing portrait fallback, initial focus, scrolling, and focus after app return.
- Chess Dutch and English layouts, and Battleship English, Dutch, and French layouts, including accessible chosen names.

Use the current app guides for JDK 17 and Android SDK paths. From the repository root, operate the chess integration tasks:

```sh
./gradlew :chessMath:testReleaseUnitTest :chessMath:lintRelease :chessMath:assembleRelease :chessMath:assembleDebugAndroidTest
```

Operate the Battleship integration tasks separately:

```sh
apps/battleship/gradlew -p apps/battleship testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease assembleDebugAndroidTest --console=plain
```

Use an explicit emulator serial for every install, test, screenshot, and input command. Select one focused instrumented class per invocation. Inspect the XML class names and test counts before reporting coverage. Run the relevant emulator suites at integration.

Capture the native chooser, name editor, chess turns for both colors, Battleship reveal, both hidden target turns, handoff, and results. Use fresh filenames and generic character names. Ask a fresh independent reviewer to inspect the completed code and native captures.

Emulator evidence is sufficient for delivery by default. Do not operate the physical TV without an explicit request or reported issue. Concept images do not establish game legality, focus behavior, save recovery, hardware input, or audible quality.

After successful installation, the coordinator follows the repository's commit and push instructions. Fetch origin before integration, preserve concurrent changes, and repeat affected tests after integration. Do not enable cloud APK builds or publish releases.

## Implementation choices left to the builder

The builder can choose class names, bounded portrait decode sizes, layout spacing, and test fixture organization. The builder can choose the explicit new Battleship schema number while retaining schema-1 compatibility. The builder must preserve all behavior and migration constraints above.

The approved design includes the 24-code-point limit, distinct portraits, duplicate-name handling, and fixed assignments. No unresolved art-direction choice remains. The user selected all eight naval captains and confirmed that chess retains its existing visual mood.
