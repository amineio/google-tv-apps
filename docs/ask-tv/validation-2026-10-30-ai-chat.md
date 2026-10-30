# AI Chat 0.2 validation, 2026-10-30

AI Chat `0.2`, code `2`, is installed on the authorized TV.
Independent review approved runtime repair `f76fbae88555f321f7998d4ea819011503969fae`.
Test checkpoint `4212ed4c63aa0a0b474cf84acd547d3b87dbe7a1` also includes native keyboard synchronization in the navigation test.

## Software results

The coordinator inspected XML class names and counts.
Unit tests use synthetic credentials and generated keys.
Navigation tests send actual Android D-pad and Back events.

| Check | Result |
| --- | --- |
| Debug unit tests | 69 tests in eight classes passed |
| Release unit tests | 69 tests in eight classes passed |
| Typed-dialog test alone | One selected test passed |
| Full `AskTvNavigationTest` | Four selected tests passed |
| Release lint | Zero errors, nine warnings |
| APK packaging | Debug and release builds passed |
| Repository tests | 29 tests passed |
| Runtime code review | Independently approved |

The timeout regressions cover an idle connection and incomplete headers.
A timeout closes only that socket, sends no HTTP response, and preserves pending authorization.
A subsequent valid callback still delivers its grant exactly once.
The navigation test waits for native keyboard visibility and idleness between real Back events.

The environment uses JDK 17, SDK platform `android-37.0`, build tools `36.0.0`, and the Android 14 Google TV emulator.
Run the app and repository checks from the repository root:

```sh
apps/ask-tv/gradlew -p apps/ask-tv testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease assembleDebugAndroidTest --console=plain
CLEANYOURTV_STRICT=1 python3 -m unittest discover -s tests -v
TV_SERIAL='<authorized emulator serial>'
ANDROID_SERIAL="$TV_SERIAL" apps/ask-tv/gradlew -p apps/ask-tv connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.asktv.AskTvNavigationTest' --console=plain
```

## Installed release and visible interface

The installed APK matches the built release hash:

```text
5acf0707ec7c9d2cc035dc82d8ad36d7f678b6b7c808d617c178c8e22ee906af
```

A screenshot-only TV UX reviewer rated the renamed Ready screen `10/10`.
The launcher banner reached `10/10` in review round six after the title weight correction.
These scores cover only the visible native layouts in emulator captures.
They do not establish microphone capture, live answer quality, or audible quality.
The user reports that the interface is not polished and needs substantial iteration.
Static screenshot grades do not establish full-flow polish or product acceptance.
Work on the complete user flow continues.

## Sign-in evidence and remaining limits

The actual Mac browser callback completed.
The TV validated the account identity and subscription scope.
It also loaded a nonempty live catalog of available models.
This establishes the existing Mac-assisted sign-in path for the installed build.

USER-REPORTED: “Question and spoken answer worked” in the actual TV remote test.
This records user-reported success for remote microphone input, live inference, and spoken output.
The coordinator observed “Listening…” and the Android microphone indicator.
The user returned to Ready before the coordinator captured the final answer.
The coordinator did not independently hear or assess the spoken output.
Built-in examples remain available without live inference.
There is no API key entry, paid API fallback, or ChatGPT history import.

Phone QR sign-in is not implemented in this installed build.
A separate approved plan covers that work.
The user confirmed an Android 14 or later phone for the planned work.
This record contains no account details, device serials, credentials, pairing values, or personal captures.
Git delivery is a separate coordinator step.
