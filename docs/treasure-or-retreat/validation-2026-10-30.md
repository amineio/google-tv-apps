# Treasure or Retreat validation record

Date: 2026-10-30.

## Source and environment

The coordinator tested the task changes on `amine/treasure-memory-games`.
The implementation commit is `1d78253b67bde92d46eba396f64550472460ade6`.
The `apps/treasure-or-retreat/src/main` Git tree is `380ff43c18b739356bf75617eadbeb01d7184002`.
The branch base was `14bfc957` after a fast-forward that included an unrelated Clock change.

The build used Homebrew JDK 17 and the installed Homebrew Android SDK.
Instrumented tests used `emulator-5584`, the `Casual_Games_TV_API_34` Google TV emulator.
The emulator used API 34, a 1920×1080 display, and density 320.
The coordinator also inspected a 720p gameplay capture, then restored 1080p.
No physical TV was tested.

From the repository root, the coordinator operated the app wrapper with this environment and integration command:

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export ANDROID_SERIAL=emulator-5584
apps/treasure-or-retreat/gradlew -p apps/treasure-or-retreat testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease connectedDebugAndroidTest --max-workers=2 '-Dorg.gradle.jvmargs=-Xmx1g -Dfile.encoding=UTF-8' --console=plain
```

## Automated results

The integration command passed after removal of a redundant Back key condition.
Compose `BackHandler` still owns Back navigation.
The coordinator inspected XML test counts and class names.

| Test group | Result |
| --- | --- |
| Debug rules tests | 10 passed, no failures |
| Release rules tests | 10 passed, no failures |
| Full instrumented suite | 8 passed: 6 remote tests and 2 artwork tests |
| `TreasureRemoteTest`, after final geometry corrections | 6 passed, no failures |
| Release lint, after final geometry corrections | 0 errors, 6 warnings |
| Debug APK assembly | Passed |
| Release APK assembly, after final geometry corrections | Passed |
| Repository Python suite | 29 tests, 28 passed, 1 skipped |

The final presentation changes only bounded the ledger and aligned the lid with the chest's rear hinge.
The coordinator repeated `lintRelease assembleRelease` and the focused `TreasureRemoteTest` after those changes.
The earlier full-suite XML remains outside Git in `treasure-suite-eight-tests` within the artifact directory below.

Rules tests cover the deck distribution, guaranteed opening, seeded shuffling, draw guards, banking, traps, progression, restart, and card values.
Remote tests cover directional selection, three expeditions, results, restart, held Select, pause, trap loss, recreation, records, and sound preference.
Artwork tests cover manifest references, resource dimensions, and rendering at launcher sizes.
These tests do not establish animation smoothness or audible quality.

## Packages and installation

The debug APK path is `apps/treasure-or-retreat/build/outputs/apk/debug/treasure-or-retreat-debug.apk`.
The release APK path is `apps/treasure-or-retreat/build/outputs/apk/release/treasure-or-retreat-release.apk`.
The release APK is approximately 2.6 MiB.
The local release build uses the repository's debug signing pattern. It is not a store-signed package.

The coordinator installed the final release APK on the emulator.
The local APK and the installed APK share this SHA-256 hash:

```text
244a5dcb878eae2e59bf3f74b2eac5de3bfd8fceb04b18cfeba0d19f385494d0
```

Cold launch and a real draw passed the final release smoke test.

## Runtime visual review

Status: ACCEPTED by the coordinator and a fresh independent reviewer.

The reviewer compared the final native gameplay captures and source with the supplied Treasure or Retreat concept.
The review found no material issues in the completed change.
The comparison covered the complete 16:9 screen, including chest assembly, artwork, text, focus, panels, safe margins, and visual hierarchy.

Earlier captures exposed neighboring sprite fragments, a detached lid, and an oversized parchment panel.
The final implementation uses clean cutouts, a bounded 205×230 dp ledger, and a lid aligned with the sloping rear hinge.
The coordinator accepted the corrected native decision and trap captures before final release capture.
Screenshots establish assembled appearance. They do not establish audible quality or frame-rate performance.

The local artifact directory is:

```text
/Users/amine/.codex/artifacts/treasure-memory-20261030/
```

| Capture | Evidence |
| --- | --- |
| `treasure-release-gameplay-final.png` | Fresh final release gameplay capture after a real draw |
| `treasure-decision-final-2046.png` | Corrected decision screen and chest assembly |
| `treasure-hinge-corrected-2045.png` | Corrected trap state and chest assembly |
| `treasure-results-manual-final.png` | Final results screen |
| `treasure-720p-gameplay.png` | Gameplay controls fit at 720p |

The final release gameplay screenshot has this SHA-256 hash:

```text
39eda71fae62db5ac3c49fede432eede855d04704fc2260c43ce55b5a14ad8b6
```

Logs, screenshots, and other local artifacts remain outside Git.

## Evidence limits and delivery

Physical-TV tests, audio listening, and a human family playtest remain unperformed.
Emulator tests do not establish physical remote behavior, speaker balance, audio latency, or Chromecast performance.
Successful decoding and SoundPool state do not establish audible quality.
Native 4K rendering is not proved by the 720p and 1080p captures.
Live inspection through Family Home remains unperformed. The launcher resource tests passed.
The app preserves records and sound preference locally. Process death can end the current expedition session.

The implementation is committed. The coordinator records push and merge results separately.
