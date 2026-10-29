# Ask TV validation, 2026-10-30

The coordinator tested source commit `783491dc85d2548658d9e8d4699fcdfb5a4c6580` and installed its release APK on the authorized Google TV.
Independent final code review approved this revision.
Installation and software tests passed. Live account authorization remains incomplete.

## Software evidence

The unit tests used synthetic credentials and generated test keys.
The navigation tests used actual Android D-pad and Back input.
The coordinator inspected the XML results to confirm the selected classes and test counts.

| Check | Result |
| --- | --- |
| Debug unit tests | 67 tests in eight classes, zero failures |
| Release unit tests | 67 tests in eight classes, zero failures |
| Emulator navigation | Four tests in `com.amine.asktv.AskTvNavigationTest`, zero failures |
| Release lint | Zero errors, nine warnings |
| APK packaging | Debug, release, and instrumented-test APKs built |
| Repository tests | 23 tests passed |
| Whitespace | Full diff from the task base to the tested commit passed |
| Final code review | Approved, including both authorization recovery fixes |

The final regressions cover cancellation after a durable sign-in commit but before UI delivery.
They also cover browser acknowledgement failure after an accepted loopback callback.
Account display reads the current stored record, and an acknowledgement failure does not suppress the accepted grant.

## Environment and commands

The coordinator used macOS, JDK 17, SDK platform `android-37.0`, and build tools `36.0.0`.
The independent build uses Gradle `9.6.0`, AGP `9.4.1`, and Kotlin `2.4.21`.
Layout and navigation tests used the Android 14 Google TV emulator.

Set `JAVA_HOME` to JDK 17 and `ANDROID_HOME` to the Android SDK.
Run these commands from the repository root.
Set `TV_SERIAL` to the authorized emulator for the instrumented tests, because the runner can uninstall the tested app.

```sh
apps/ask-tv/gradlew -p apps/ask-tv testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease assembleDebugAndroidTest --console=plain
CLEANYOURTV_STRICT=1 python3 -m unittest discover -s tests -v
TV_SERIAL='<authorized emulator serial>'
ANDROID_SERIAL="$TV_SERIAL" apps/ask-tv/gradlew -p apps/ask-tv connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.asktv.AskTvNavigationTest' --console=plain
git diff --check e433c5d23bacc621ea178879459aa7066a4a4d96..783491dc85d2548658d9e8d4699fcdfb5a4c6580
```

## Native interface review

A principal TV UX reviewer examined only actual emulator captures across four rounds.
The overall scores were `7.8 → 9.2 → 9.8 → 10/10`.
The final round scored Ready, Type, Configuration, and Example at `10/10` each.
This rating applies only to the visible layouts in those captures.
It does not establish live inference, microphone capture, or audible quality.

The coordinator also examined the selected launcher banner and background in the emulator.
The review included app entry and return to the launcher.
Personal captures and device identifiers remain outside Git.

## Physical installation

The release package installed successfully as version `0.1`, code `1`.
The installed APK hash matched the built release APK.
The coordinator privately inspected the actual TV ready screen.

Release APK: `apps/ask-tv/build/outputs/apk/release/ask-tv-release.apk`.
SHA-256:

```text
26006cf8526d16fee212a6f95ac4d9cd484f8ef8bcbcef6c9fbbcf93e8f60662
```

The separate installer targets one explicit device:

```sh
TV_SERIAL='<authorized TV serial>' tools/install-ask-tv.sh
```

## Sign-in evidence and limits

The Mac ADB tunnel reached the Android listener at `/status`.
Opening `/start` redirected the browser to the official `auth.openai.com` account selection for Ask TV.
No account was selected. The review did not establish who owned the visible browser account.
The app received no authorization grant or credentials. Live inference did not run.
The coordinator canceled the flow and removed only the task's tunnel.

The helper uses an explicit device serial:

```sh
TV_SERIAL='<authorized TV serial>' tools/sign-in-ask-tv.sh
```

Built-in examples remain available while signed out.
The app has no API key entry, paid API fallback, or ChatGPT history import.
Live inference, physical remote microphone capture, and audible quality remain unverified.
This record does not claim delivery to `origin/main`.
