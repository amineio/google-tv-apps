# Memory Parade 1.1 validation — 2026-10-31

The coordinator built and tested version 1.1, version code 2, then installed the tested release APK on the physical TV. The installed APK and local release artifact share SHA-256 `2bd9dff6aaf51f63deeb8dc5044b8defae56d65786deb7cb729e375b8dcbe357`. Installation preserved app data. This record does not claim physical TV gameplay, remote navigation, or listening quality.

The task branch is `amine/memory-parade-upgrade`, based initially on `1aa862ef`. Before delivery, the coordinator integrated fresh `origin/main` at `505e3772`; those concurrent changes affect AI Chat, not Memory Parade. The installed Memory Parade source and resources stayed unchanged. The delivery commit records the final revision.

## Checks

- Debug and release each passed 11 pure rules tests.
- The 1080p emulator suite passed 14 instrumented tests: 9 remote/game tests, 2 preference tests, and 3 launcher-art tests.
- The 720p emulator run passed the same 9 remote/game tests. The emulator was then restored to 1080p.
- Release lint reported 0 errors and 8 warnings. Debug and release APK builds passed.
- Rules coverage includes every board size, seeded shuffle, pair distribution, fair animal subsets, navigation bounds, input guards, completion, and records.
- Device coverage includes actual directional navigation to all cells, held Select, mismatch blocking, pause, Home/return, Activity recreation, sound-event consumption, preference migration, separate records, all three completions, and launcher resources.

The expected failing checkpoint is `rules-red.log`: the new board-size references failed compilation before implementation. The coordinator inspected test XML counts rather than inferring coverage from build success.

## Local evidence

All logs, APK copies, and captures are outside Git at `/Users/amine/.codex/artifacts/memory-parade-upgrade-20261031/`.

- `build.log`: build, unit-test, and lint output.
- `device-tests.log` and `full-1080p-tests/debug/TEST-Casual_Games_TV_API_34(AVD) - 14.xml`: full emulator suite.
- `720p-tests.log`: lower-resolution remote suite.
- `memory-parade-v1.1-debug.apk` and `memory-parade-v1.1-release.apk`: tested artifact copies.

Fresh native captures follow real directional play and matching. Each size has progress, full-parade, and results captures. Files were copied into `/sdcard/Download` before test cleanup, then pulled locally.

| Board | Full-parade 1080p capture |
| --- | --- |
| 6 cards | `memory-v11-easy-full-parade-1793439305936.png` |
| 12 cards | `memory-v11-classic-full-parade-1793439322269.png` |
| 16 cards, all eight animals | `memory-v11-challenge-full-parade-1793439344119.png` |

The `720p/` folder contains the equivalent fresh lower-resolution captures. Full-parade filenames end in `1793440259686`, `1793440274903`, and `1793440296403` for easy, classic, and challenge respectively. Concept artwork and assembled native captures remain distinct. Physical installation does not establish audible quality, animation smoothness on the TV, or family enjoyment.

## Commands

Use JDK 17 and the Android SDK paths documented in the app README. The coordinator ran local Gradle checks with two workers and explicit emulator serial `emulator-5584` for instrumented tests. The commands below reproduce these checks. Logs above record the completed coordinator runs.

```sh
apps/memory-parade/gradlew -p apps/memory-parade testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease assembleDebugAndroidTest --max-workers=2 --console=plain
ANDROID_SERIAL=emulator-5584 apps/memory-parade/gradlew -p apps/memory-parade connectedDebugAndroidTest --max-workers=2 --console=plain
ANDROID_SERIAL=emulator-5584 apps/memory-parade/gradlew -p apps/memory-parade connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.memoryparade.MemoryRemoteTest' --max-workers=2 --console=plain
```
