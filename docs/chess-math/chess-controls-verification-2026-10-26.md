# Chess controls verification, 2026-10-26

The working tree on `main` implements version 0.8. Its starting commit is `20548c4034aa0f14fa964fd7dbcee412c29f6b72`. A fresh fetch shows no commit difference from `origin/main`. The implementation remains uncommitted.

## Changes

The chess board stays centered without the right text panel. Routine saving remains silent during chess. Storage errors, promotion, draw claims, check highlights, and results remain available.

A saved session offers Continue and New game. New game requires confirmation with Cancel focused. Confirmation opens fresh arithmetic directly after persistence succeeds.

Parent controls offer Easy, Normal, and Hard. The choice persists separately from game progress. Easy keeps its existing deliberate random choices. Normal and Hard use local search with respective depth limits of three and five half-moves. Their search budgets are 250 ms and 800 ms. Root preparation and individual library calls can exceed those budgets. The search uses a legal fallback that considers immediate attacks.

## Software evidence

The final Gradle command passes:

```sh
./gradlew :chessMath:testReleaseUnitTest :chessMath:lintRelease :chessMath:assembleRelease :chessMath:assembleDebug :chessMath:assembleDebugAndroidTest
```

All 88 unit tests pass, with no failures, errors, or skips. Release lint reports zero errors and 25 warnings. Warnings include existing resource and platform guidance, newly unused chess text, and intentional synchronous preference persistence.

Independent subagents review the interface and engine changes. Review catches the extra Pause step after New game. Device tests catch a cold-search random fallback and a queen sacrifice at a shallow search depth. The final code addresses each finding. A subsequent independent core review finds no blocking issue.

An initial combined device run passes 18 of 19 tests. The existing promotion test reports board focus instead of promotion focus. Its unchanged isolated rerun passes. The final fixtures freeze time during promotion, wait for board focus, and complete activity teardown before restoring shared references. This removes unrelated checkpoint timing from promotion assertions. The original log does not prove the exact timing cause.

## Chromecast evidence

The test device is Chromecast `sabrina`, Android 14, build `UTTC.250917.004`. The user authorizes replacement of development games. Testing uses the existing application ID, `com.amine.chessmath`.

The final tactical device tests pass for both levels. Normal finds immediate mate in 202 ms, preserves its queen in 254 ms, and returns a legal opening reply in 254 ms. Hard takes 155 ms, 800 ms, and 807 ms for those fixtures. These observations do not establish a chess rating or general playing strength.

Screenshots show the quiet board, Continue beside New game, and all three difficulty choices. See [the board](evidence/controls-v8/chess.png), [the start screen](evidence/controls-v8/start.png), and [the difficulty dialog](evidence/controls-v8/difficulty.png).

The final combined TV run passes all 19 tests in 74.651 seconds. It covers board layout and quiet persistence, new-session confirmation, preference persistence and failures, both stronger opponents, promotion, parent controls, and remote board navigation. See [the device test output](evidence/controls-v8/device-tests.txt).

The version 0.8 release APK replaces the existing TV installation. Development app data is cleared before launch, as the user requests. No separate review app remains.
