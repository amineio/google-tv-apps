# Battleship 1.5 validation

The final release uses generated ship sprites, a still ocean image, equal grids, overhead impacts, and recorded battle sounds.
The work used two development and design review rounds out of the four authorized rounds.

## Design results

Two independent game design reviewers assessed native emulator captures and ordered frames from a live match recording.
Scores express expert judgment. They do not represent user acceptance or measured frame performance.

| Category | Art director | Independent game UX reviewer |
| --- | ---: | ---: |
| Composition | 8.5 | 8.5 |
| TV readability | 8.3 | 8.0 |
| Ship craft | 8.3 | 8.5 |
| Effect clarity | 8.1 | 8.0 |
| Interaction stability | 8.0 | 8.5 |

Round 1 scored effects at 6.5. Smoke hid the warm fire at 600 ms, and sinking lacked visible burning at 1000 ms.
Round 2 draws smoke behind fire and holds sinking flames until 1400 ms before cooling.
It also replaces uniform splash rings with broken circular arcs and foam.
Both reviewers found no remaining material issue in the reviewed emulator evidence.
Fleet label spacing remains a minor refinement. All labels fit without clipping in the reviewed captures.

See [art direction](art-direction.md), [round 1](review-round1.md), [round 2](review-round2.md), and [independent UX review](ux-review.md).
The [source review](source-review.md) passes specification and code quality after the audio provenance correction.

## Automated evidence

The baseline debug tests passed before implementation.
New timing tests failed against early replacement and final-shot cue ordering before the implementation.
The round 2 regression failed with `HIT needs visible warm fire at 600 ms` before the effect correction.

The final full check passed debug and release unit tests: 50 cases in each variant, zero failures.
The full emulator suite passed 16 tests with zero failures, errors, or skips.
An additional live full-match test passed after the full suite.
Repository checks passed 13 cases, with one existing conditional skip among 14 cases.

Release lint reports zero errors and three existing warnings: two AndroidGradlePluginVersion warnings and one DiscouragedApi warning.
Debug, release, and Android test APK builds passed.
After removal of the unused decorative ship resource, release lint and packaging passed again with those same three warnings.
The only other final source cleanup removed an empty trailing line.

The emulator suite covers equal actual grid bounds and at least 30 dp grid pitch on the standard TV canvas.
It compares bounds at first layout, 500 ms, 2000 ms, long feedback, shots, and pause/resume.
Directional input covers aiming, board edges, dialogs, pause/resume, repeat rejection, activity return, rematch, and results.
Unit tests cover all outcome holds, final sinking before victory, interruption, stale completion IDs, persistence, and hidden information.
Presentation tests cover both boards, center and corner misses, warm fire at 600/1000 ms, and exact completion deadlines.
Static restored boards request no animation frames. Effects do not alter the other board.

The full check used the app's independent Gradle build with explicit emulator serial selection:

```sh
env JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home \
ANDROID_HOME=/opt/homebrew/share/android-commandlinetools \
ANDROID_SERIAL=emulator-5580 apps/battleship/gradlew -p apps/battleship \
testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease \
assembleDebugAndroidTest connectedDebugAndroidTest \
-Pandroid.testInstrumentationRunnerArguments.captureSprites=true --console=plain
```

The source records and preparation scripts pass deterministic audio regeneration comparison.
All seven entries that describe current audio resources match the actual file hashes.
Historical version 1.1 battle recipes explicitly identify their superseded status and link the current record.
Current cues use mono PCM16 at 22050 Hz. They have zero endpoints and a measured peak below 0.72.
The longest new cue lasts 2.5 seconds and uses 110250 decoded bytes, below SoundPool's per-sound limit.

## Rendered evidence

The final staged test saved 35 native screenshots with fixed-clock sample times.
The [capture manifest](captures.json) records selected original and compressed hashes.
The scene uses original generated raster assets, while the UI, grids, markers, focus, and animation remain native Compose.

The live recording runs for 77.53 seconds at 1280 by 720 pixels.
It shows real session timing, directional aiming through a match, complete sinking before later attacks and victory, rematch, and pause/resume.
Reviewers inspected full frames and temporal samples at 250 ms intervals around key transitions.
The recording is silent. It does not establish speaker balance, continuous-playback quality, frame pacing, or physical TV performance.

## Final TV installation

Only the final version installed on the physical TV during this task.
Package inspection reports version 1.5, version code 6, minSdk 26, and targetSdk 37.
The installed APK was pulled back and compared byte-for-byte with the final local release.
Both files have 2305723 bytes and SHA-256:

```text
3f6440fd78227b68e1b193557bdafb1480992e0abf199c68aaec639713d4a3a6
```

The TV initially showed the existing launcher profile chooser.
Normal selection of the parent profile allowed Battleship to open and remain in front.
No launcher code, permissions, access policy, or Cast service changed.
The previous saved match opened successfully after replacement.
Directional input moved the target from J5 to J4, and OK fired an accepted miss.
The TV capture shows the circular splash, matching square grids, ship sprites, readable feedback, and a clear clock overlay area.
Back opened pause. The match remained paused after this bounded physical test.

Physical captures and the short TV recording remain local and outside Git.
This evidence establishes installation, launch, basic remote operation, and rendered layout on the TV.
TV speaker balance and sustained frame performance remain unmeasured. No physical listening claim is made.

## Delivery scope

Changes affect only Battleship and its design, asset, test, and validation records.
No new runtime dependency or network permission was added.
The original launcher artwork and app metadata remain available.
The generated decorative gameplay ship and procedural shader implementation were removed.
The [asset record](assets.json) and [audio record](audio-assets.json) identify sources, prompts, preparation, dimensions, and rights.
