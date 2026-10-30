# Memory Parade validation: 2026-10-30

## Tested environment

The coordinator used the isolated `amine/treasure-memory-games` branch. Its base is `14bfc957` after integration of the unrelated Clock fast-forward. The validated implementation commit is `1d78253b67bde92d46eba396f64550472460ade6`. The app's `src/main` Git tree is `66d3eb2753f7ecaecbdad9d480b02635497d1b26`.

The local build used JDK 17 and the Homebrew Android SDK. The test target was `emulator-5584`, the `Casual_Games_TV_API_34` emulator. It used Android API 34, a 1920 by 1080 display, and density 320.

## Build and test results

The coordinator operated the app's own wrapper with these arguments:

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
ANDROID_SERIAL=emulator-5584 apps/memory-parade/gradlew -p apps/memory-parade testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease connectedDebugAndroidTest --max-workers=2 '-Dorg.gradle.jvmargs=-Xmx1g -Dfile.encoding=UTF-8' --console=plain
```

Both variants passed all six rules tests. The emulator passed nine instrumented tests: six in `MemoryRemoteTest` and three in `LauncherArtworkTest`. The coordinator inspected the XML counts and class names. The complete reports remain outside Git because the focused screenshot run replaces the current test output.

The remote tests covered all twelve board positions, pause and Activity recreation, mismatch input guards, held Select input, Home and app return, sound preference persistence, full-game completion, saved best moves, and restart. The launcher tests covered icon bounds, banner bounds, and the separate 1920 by 1080 hero resource.

Lint reported zero errors, six warnings, and four hints. Debug and release APK assembly passed. The release APK is approximately 2.3 MiB and uses the local debug signing key. This build does not establish store distribution signing.

The repository suite ran 29 tests. It passed 28 tests and skipped one test.

## Runtime screenshots and visual review

The first actual emulator screenshot exposed weak card materials and floating stage bars. The implementation replaced them with a separate embossed card skin and a connected mossy stage. Native code retains the card geometry, labels, counters, and focus borders.

After the panda resource correction, the coordinator repeated `lintRelease assembleDebug assembleRelease` and the focused `com.amine.memoryparade.MemoryRemoteTest#fullGameWithRealDirectionalInputCompletesAndRestarts` test. Its XML reports one test and zero failures. The earlier complete nine-test XML remains outside Git in `memory-suite-nine-tests`.

The screenshot checkpoints follow three actual matches and the complete game. The test reads the shuffled positions and uses directional input to reach and select cards. It does not alter the game state to create the captures.

The test copies timestamped screenshots to `/sdcard/Download/` before app removal. The coordinator pulled and inspected the final captures outside Git:

```text
/Users/amine/.codex/artifacts/treasure-memory-20261030/memory-three-pairs-1793392717213.png
/Users/amine/.codex/artifacts/treasure-memory-20261030/memory-results-1793392728914.png
```

The three-pair capture has SHA-256 `e2b29cc3ff47467b7110b1ffe2c63812cbbd57e0b67df7d0eb6c64bcc47e21d0`.

The first populated capture exposed a detached neighboring fragment below the panda's feet. The corrected sprite preserves the full panda and removes the fragment. All six sprites received visual inspection. No other detached fragments were visible. The coordinator accepts the final Memory Parade composition. A fresh independent reviewer accepted the final code and complete screen captures against the supplied concept. The review found no material issues.

The release build also received a 1280 by 720 capture. All controls fit. A 4K logical override still produced a 1920 by 1080 screenshot. This does not establish native 4K output. The coordinator restored the 1080p emulator configuration.

## Release installation

The coordinator installed the release APK on `emulator-5584`. The local APK and installed APK both have SHA-256 `aecbdebdcba25ee47509f8e6ea4951b452942a1b244cae6c4c57cab8bcd17afb`.

The resource rendering tests passed. Live artwork inspection through Family Home remains unperformed. The tests do not establish the launcher's complete presentation.

## Evidence limits

The coordinator did not test the physical TV. Audio listening and a family playtest remain unperformed. Successful playback calls and emulator tests do not establish physical speaker balance, audible quality, or human enjoyment.

The installed release is identified by its APK hash above and corresponds to the validated implementation commit. The coordinator will report delivery to `origin/main` after the push. This record does not claim that a push occurred.
