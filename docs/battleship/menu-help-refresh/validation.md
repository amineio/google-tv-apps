# Battleship 1.8 validation

Date: 2026-10-29. Base: feff842a. Scope: menu copy, language, visual Help and Tactics, opponent portrait, targeting sight, and faster development guidance.

## Behavior

The main menu removes its tagline, offline label, and bottom footer. Settings adds a language row with English, Nederlands, and Français. Selection changes the app text without restarting the Activity. The saved match retains its board, turn, selection, and pause state. Existing saves default to English. All language resources stay bundled for offline use.

How to Play uses three native illustrated cards: Aim, Fire, and Sink. The user later supplies `30213.jpg` with four tactics. A separate Tactics page adapts those tips into native diagrams and short English, Dutch, and French text. The photograph remains outside the repository and app. No online URL or external license was supplied.

The Tactics page shows checkerboard hunting, orthogonal neighbors after a hit, shifted third-cell hunting after the two-cell ship sinks, and open water. Ships can touch. Both Back paths return to Help and restore focus to Tactics.

A fictional opponent portrait joins the existing captain. The human stays on the left. Each turn label appears beside the active captain. The selected coordinate uses an open-center circular sight. Old shots retain their hit or miss markers without corner frames. Ship captions use intentional word-based line breaks where needed. Both grids retain equal size across languages and turns.

## Test evidence

The language regressions first fail because the language fields and action do not exist. They then cover old saves, round trips, and changing language without changing the paused match. The cursor regression first finds 45 cyan pixels in a rectangular corner that must remain clear. It then covers directional movement, an empty previous target frame, and a visible sight over a previously attacked cell.

The first Help test exposed a test-input issue. Compose key injection did not reach the Activity Back handler. The test now sends Android Back input. The multilingual gameplay test finds the long Dutch carrier caption clipped. A two-line roster fixes overflow. A separate visual reviewer then finds awkward word breaks and ambiguous turn-label ownership. Intentional caption breaks and active-side labels resolve those issues.

The complete emulator suite passes all 23 tests. Debug and release each pass 75 unit tests. The full build produces debug, release, and device-test APKs. The new Back, language, cursor-pixel, translated text, captain-side, and stable-grid tests all pass. Native audio tests remain green.

Release lint initially finds two integration errors. The language provider reads Configuration through LocalContext, and the fixed app name lacks translation metadata. Reactive configuration/resource reads, an untranslated brand name, and disabled language splitting resolve them. Unused resource cleanup and a count-at-end spoken label remove two minor resource warnings. The release inspection passes with zero lint errors and the three existing warnings. Both 75-test unit variants and debug/release packaging pass after resource cleanup. The full 23-test emulator run precedes that wording-only cleanup.

## Incremental Tactics evidence

The tested source is the working tree based on `feff842a`. The earlier integration checkpoint passes 75 debug unit tests, 75 release unit tests, and 23 emulator tests. That full suite precedes the Tactics addition.

The later interface-only addition uses focused tests. The initial combined filter runs `HelpScreenTest`, which passes one test across all three languages. A subsequent single-class `TacticsScreenTest` invocation passes one test across all three languages. It covers card geometry, text overflow, directional focus, hardware Back, footer Back, and focus restoration. The coordinator inspects XML class names and counts for each invocation.

An initial comma-separated class filter selects only Help in this environment. Its result does not establish two-class coverage. The separate Tactics invocation establishes its one passing test. Future focused invocations select one class each.

The initial combined-filter invocation also completes `lintRelease` and `assembleRelease` in 27 seconds. The separate Tactics invocation completes in 16 seconds. Release lint reports zero errors and three existing warnings. The workflow does not repeat the unaffected full suite after this interface-only addition.

The host uses JDK 17 and the app Android SDK. Each device command sets the emulator serial explicitly. The initial command is:

```sh
env JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home ANDROID_HOME=/opt/homebrew/share/android-commandlinetools ANDROID_SERIAL=emulator-5580 apps/battleship/gradlew -p apps/battleship connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.battleship.HelpScreenTest,com.amine.battleship.TacticsScreenTest' -Pandroid.testInstrumentationRunnerArguments.captureHelp=true lintRelease assembleRelease --console=plain
```

The following commands show the corrected one-class pattern. The later Tactics invocation uses this pattern. The separate Help command is guidance, not additional test evidence.

```sh
ANDROID_SERIAL=emulator-5580 apps/battleship/gradlew -p apps/battleship connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.battleship.HelpScreenTest' '-Pandroid.testInstrumentationRunnerArguments.captureHelp=true' --console=plain
ANDROID_SERIAL=emulator-5580 apps/battleship/gradlew -p apps/battleship connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.battleship.TacticsScreenTest' '-Pandroid.testInstrumentationRunnerArguments.captureHelp=true' --console=plain
apps/battleship/gradlew -p apps/battleship lintRelease assembleRelease --console=plain
```

Generic Tactics captures are stored as `native-tactics-en.webp`, `native-tactics-nl.webp`, and `native-tactics-fr.webp`. They use `cwebp -q 84`. Their dimensions and hashes appear in `assets.json`. The independent native Tactics UI/UX review passes in all three languages, with scores from 8 to 9. No material fix remains. Small candidate dots still require actual viewing-distance judgment.

Repository guidance now separates focused iteration, integration checkpoints, and final delivery checks. Ordinary interface changes receive one focused physical TV smoke test after emulator acceptance. Changed hardware paths retain their own evidence requirements. The guide records fresh captures, real Back input, one-class filters, and XML coverage inspection.

## Independent review

The game UI/UX expert reviews the ImageGen concept before native implementation. The reviewer rejects its grid-boundary sight, crowded margins, busy water, and over-specific Fire rule. Native code corrects those details.

The same independent expert reviews native Home, Help, Settings, gameplay, computer turn, and restart confirmation in all three languages. Final scores range from 8 to 9. No material visual issue remains. The independent source review also passes after the layout and resource fixes. Its incremental review accepts the Tactics rules, navigation design, and revised workflow. Static captures do not prove navigation or persistence. Separate tests cover those behaviors.

Generic emulator captures are compressed beside this report. Physical TV captures and device identifiers stay outside Git.

## Delivery

The initial version1.8/code9 release installs successfully. Its installed APK matches the tested local release:

`e5b68b0d947377b734ba9e0cb20b4968a6c8d45347dbdde5567334bc65a80530`

Physical captures show the clean main menu, visual Help, both captains, and a sight moved to B1. Dutch and French Settings appear through actual D-pad input. English is restored. The final game is paused. Physical UI dumps coincide with Family Home redirects, so later inspection uses screenshots and normal profile selection without changing Home or accessibility configuration.

The user then supplies the tactics photograph and requests a faster repository workflow. The incremental work adds a Tactics page and applies focused emulator tests plus release lint and packaging. The initial installed hash above does not identify that final addition.

The final version 1.8/code9 release reinstalls successfully on the TV. The installed APK matches the local final release:

`9eee15272244b57e2bfc2e9e6b5ddc1e1c7fcd1ae75c5de220293306fcde3a09`

The final focused capture shows the Family Home profile chooser. The coordinator stops physical input under the bounded test policy. This capture does not establish physical Tactics readability or navigation. The earlier base-screen observations remain separate evidence. The final Tactics page has emulator visual and navigation coverage in all three languages.

Fleet tracker concepts remain design-only. Git delivery follows final documentation.
