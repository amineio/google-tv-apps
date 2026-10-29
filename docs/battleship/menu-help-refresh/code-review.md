# Independent source review

Date: 2026-10-29.

## Scope and decision

I reviewed the current Battleship 1.8 patch against `feff842a` and the approved menu and help specification.
I inspected the changed Kotlin source, all three string resources, persistence changes, and added tests.
I did not change source code, operate builds, control devices, or perform Git operations.

Code verdict: pass. No material source defect found.
Specification verdict: pass for the implemented scope, subject to the separate rendered review and final test results.

The user mentioned a future help image and online text but supplied neither item.
Their integration remains pending. The patch does not invent their content or provenance.

## Source findings

The home screen removes the tagline, offline message, and opponent footer without adding replacement instructions.
The menu actions and naval background remain.

`ProvideGameLanguage` uses a copied Android resource configuration scoped to the Compose tree.
It changes native labels without recreating the Activity or resetting the game.
English, Dutch, and French contain matching resource keys and positional format arguments.
The language control shows each language in its native spelling.
The existing control instance retains focus as the label changes.

`SavedSession.language` defaults to English.
Existing schema-one saves without this field remain readable.
The new unit test covers that default and round trips all three languages with a saved match.
The session test covers selection, match, and pause preservation through language changes and restoration.
The game engine, opponent, audio classes, Activity, permissions, minimum SDK, and match timing remain outside the patch.
The session action adds persistence for the language preference without changing attack or turn logic.

Visible gameplay feedback, ship labels, warnings, menus, results, dialogs, and board semantics use translated resources.
Legacy English warning identifiers remain internal and map to translated messages before rendering.
The repeated-target identifier also maps to translated visible text.
Coordinate letters and numbers remain shared game coordinates.

Help uses three native cards with the shared targeting sight, distinct hit and miss shapes, and an overhead ship illustration.
It reserves equal body height for translated copy and keeps the Back action separate.
The new remote test covers card bounds, text overflow, initial Back focus, and directional behavior in all three languages.
It does not claim that external help material was supplied.

The opponent portrait uses the same size and circular crop as the captain portrait.
Both boards retain equal dimensions. The roster fix reserves ten additional dp below each board in every language.
Hidden enemy ships still come from the unchanged restricted player view.

The targeting sight draws only from the current selected coordinate.
The old rectangle and last-shot corner frames are removed from both boards.
Shot markers and sinking effects remain intact.
The added pixel test covers a fired cell, movement through directional input, and removal of cyan and brass highlights from the old cell.

## Evidence and limits

The coordinator owns the active full test run in `/tmp/battleship-menu-help-green.log`.
It was still active at the time of this review. This report does not claim that it passed.
The coordinator also records the expected language API and old-rectangle failures before implementation.

Source inspection does not establish couch readability, portrait crop quality, or translated text fit on the TV.
The independent game UI/UX review must assess final native captures in English, Dutch, and French.
Inspect the long Dutch fleet name and enemy-waters heading because those regions retain fixed space.
Inspect translated pause and confirmation dialogs for readable labels and normal directional navigation.
These are rendered acceptance checks rather than demonstrated source defects.

## Scoped follow-up review

I reviewed the roster layout fix, `MultilingualGameplayTest`, and the Help test input correction.
The coordinator reports that the focused multilingual test reproduced Dutch `Vliegdekschip` overflow before the fix and passed afterward.

The roster now reserves two lines for every ship name without reducing its font size.
Its height increases from 34 dp to 44 dp.
The board calculation reserves the matching ten dp for both boards in every language.
This small uniform size change resolves the demonstrated translation defect and preserves equal grids.
It does not make board size depend on the selected language.

The new test compares board bounds across all three languages and inspects native text layout overflow.
It also covers the restart confirmation layout in each language.
The assertions inspect the actual translated text rather than estimating text width.
They do not establish visual quality, every possible shot message, or every dialog path.

The Help test sends Back through Android instrumentation so that the Activity Back handler receives the key.
Directional navigation still uses actual directional key events.
This correction changes the test input path and does not modify production behavior.

Final code verdict: pass. No material source defect found in the scoped changes.
The full final run at `/tmp/battleship-menu-help-final-green.log` remains active at this review time.
The independent native image review and final test result remain separate acceptance evidence.
The user-supplied help image and online text remain pending.

## Final bounded source review

I inspected the turn-indicator placement, roster resources, resource-provider update, bundle configuration, and expanded multilingual test.
No material source defect found. Final code verdict remains pass.

The active turn label now appears beside the corresponding captain portrait.
The computer indicator also covers the computer impact-resolution interval.
The winner controls its side during the brief final impact interval.
The test covers mutually exclusive player and computer indicators in all three languages.

Dedicated roster resources add deliberate breaks for long Dutch and French names.
Normal ship names and spoken shot feedback still use unbroken resources.
The two-line roster height and equal board calculation remain unchanged from the preceding fix.

The resource provider now observes Compose configuration changes through `LocalConfiguration`.
Its fallback uses `LocalResources`, which also observes configuration changes.
The app bundle retains all language resources for the in-app selector.
The launcher app name remains intentionally untranslated.
Removed help resources have no remaining source references.

I read `/tmp/battleship-roster-turn-fix.log`, which records the focused build and device test as successful in 44 seconds.
The full final run remains separate evidence under coordinator ownership.
The user-supplied help image and online text remain pending.

## Final resource and evidence review

The final resource change moves the incoming-shot count after a translated label in all three languages.
The positional integer argument still matches `GameBoard`.
This form works with zero, one, and multiple shots without an English singular-noun error.
The removed `friendly_board` resource had no remaining reference.
No material source finding arises from this cleanup. Final code verdict: pass.

I inspected the final build logs and current XML reports.
The full suite completed successfully in 2 minutes and 53 seconds.
Its device report contains 23 tests with zero failures, errors, or skipped tests.
After the resource cleanup, both unit variants and release lint completed successfully with debug and release builds in 11 seconds.
Each unit variant contains 75 tests with zero failures, errors, or skipped tests.
Release lint reports zero errors and the three existing warnings: two Android Gradle Plugin version warnings and one discouraged API warning.
The final resource cleanup did not repeat the device suite.
Physical TV installation and rendered acceptance remain coordinator-owned evidence.

## Supplied tactics and workflow review

I reviewed the incremental Tactics screen, Help navigation, three language resources, related tests, and test-workflow guidance.
Code verdict: pass. No material finding in the reviewed increment.

The native diagrams match the approved tactics reference.
The checkerboard uses `(row + column) % 2 == 0`.
The follow-hit diagram marks only the four orthogonal neighbors.
The three-cell hunt uses `(row + column) % 3 == 0` with the two-cell sunk ship drawn separately.
Its title conditions that pattern on the two-cell ship being sunk.
The open-water card explicitly states that ships can touch and does not exclude neighboring cells.
The source photograph remains a reference and is not bundled.

The nested Back handler is enabled only on Tactics and returns to Help.
The visible Back button uses the same return action.
Both paths restore focus to Tactics, while first entry into Help focuses Back.
The tests use directional input and cover both return paths, card bounds, and translated text overflow.
The translations preserve the four conditions and rules from the approved tactics copy.

The updated repository guidance uses focused tests during iteration and broader suites for changed shared behavior.
It retains final release packaging, lint, changed-behavior coverage, installation identity, and scoped delivery.
The ordinary TV pass remains bounded after emulator coverage.
Changed hardware behavior still requires evidence for its affected physical path.
The guidance does not authorize omission of material failures or weaken installation and push requirements.
It also does not require the coordinator to author production code.

I read `/tmp/battleship-tactics-focused.log`, which records a successful build in 27 seconds.
The coordinator owns final device result counts and rendered Tactics acceptance.
The earlier notes that supplied help material was pending describe the earlier review stage.
This increment now incorporates the supplied photograph's four tactics through native diagrams and translated text.
