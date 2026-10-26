# Simplification evidence

The parent requested less screen text, a smaller keypad, automatic math answers, promotion images, and cleaner focus colors. Three agents implemented separate controller, UI, and palette changes. A fourth agent independently reviewed the code and TV captures.

Buttons now use one continuous rounded ivory outline. Chess selection uses sage, legal dots use forest or ivory for contrast, and capture targets use continuous rings. Promotion choices show the matching artwork above the Dutch name.

Math uses a 240 dp keypad with ten digits and delete. It has no submit or Next action. An exact answer counts once. The screen shows Goed! before the next question or phase handoff. The controller measures 1200 milliseconds of active visible time and excludes time spent saving. Pause and background use reset the interval. Restored solved exercises receive a fresh interval. Wrong entries remain editable. Explanations appear through the Hint action.

The saved-session schema, chess rules, round requirements, and artwork remain unchanged. The update retains actual saved progress. The tests use temporary memory sessions.

## Build and unit evidence

The new controller regression tests failed before implementation. The final release suite passed 50 tests. Release lint, release assembly, and device-test compilation passed. Strict repository tests passed all eight cases. Home and Clock source files remain unchanged.

The independent reviewer found an incidental-save timer reset at the five-minute boundary. The controller now accumulates visible time across those saves and excludes save latency. Regression tests cover this case, final handoff, failed advancement, duplicate input, partial input, zero, one-digit and multi-digit answers, pause, and restore. The reviewer found no remaining material source issue after the fix.

## TV evidence

The app runs on the physical Chromecast, Android 14, at 1920 × 1080. No emulator was used. The existing high-contrast text setting and external Clock overlay stay unchanged.

The first device run passed 11 of 12 tests. Its sole failure was an obsolete fixture assumption: text at font scale 1.6 now fits the compact layout. The corrected fixture proves that the text fits at 1.6 and that overflow at 2.0 supports remote scrolling. All three layout tests passed after that correction.

The initial success capture accidentally paused its memory fixture. It was rejected because it showed Pause. The corrected capture keeps the foreground success state visible.

The final complete device run passed all 12 tests in 34.531 seconds against the release APK. Correct numeric entry advanced without submit or Next. Promotion image tests compared each rendered choice with its named piece. The final screenshots are in [simplification-v3](evidence/simplification-v3/math-compact.png). The success capture now shows Goed!, the completed answer, and no keypad. The temporary test package was removed. The release app remains installed.

The independent reviewer inspected the replacement success capture and found no remaining material issue. The final APK is 1,883,690 bytes, below the unchanged 2 MiB limit. Its SHA-256 is `415de0d6ad19d56cca01f1b8f6e9e7a858fb8b7647fef42a8777f379a0035dad`.
