# AI Chat

Ask a spoken or typed question on Google TV. AI Chat uses the official open-source ChatGPT subscription flow.
It has no API key entry, paid API fallback, or ChatGPT history import.

Version `0.2` uses package `com.amine.asktv`. Android 14 or later is required.

## Remote controls

The app opens with **Ask a question** focused and the microphone off.
Select **Type a question** to use the keyboard. Microphone permission is requested only when you choose Ask.
If Android speech recognition is unavailable, use typed input.
The separate Assistant button keeps its normal system behavior.

The two **Example** buttons show built-in sample answers. Each answer is labeled as an example and does not contact OpenAI.
Live questions require sign-in and ChatGPT plan permission.

Use Up and Down inside an answer to scroll. **Stop** cancels the current action.
**Read aloud** uses an installed offline Android voice. **Follow-up** opens typed input.
**New conversation** clears the conversation in memory.
Back first stops recording, inference, or speech. Back then returns from a page or exits the ready screen.
Leaving the app stops all recording, inference, and audio. The displayed text remains available on return.

## Continue with ChatGPT

1. Install AI Chat on the authorized TV or emulator.
2. Connect the Mac through ADB and use the explicit device serial.
3. Keep AI Chat visible and run this command from the repository root:

```sh
TV_SERIAL='<connected serial>' tools/sign-in-ask-tv.sh
```

The helper opens the Mac browser. Complete **Continue with ChatGPT** and allow ChatGPT plan use.
Check Configuration on the TV for the verified account and subscription status.
An unfinished browser flow does not connect the account.
If sign-in succeeds without plan permission, select **Enable ChatGPT plan** in Configuration.
That explicit action requests consent again with the saved registration. Run the Mac helper while that screen remains open; it preserves the active consent attempt. Ordinary sign-in does not force consent.

The Android app owns authorization and encrypted tokens. The Mac helper forwards only local port 1455, opens the browser, and waits.
It does not read existing ChatGPT or Codex credentials or save tokens on the Mac.
It refuses an occupied listener or ADB forward and removes only its own forwarding.
The temporary Android listener binds to `127.0.0.1:1455` and expires after ten minutes.
The ADB loopback tunnel is an app implementation assumption, not an OpenAI-endorsed device flow.

Configuration shows the current server model catalog. Only listed subscription models can be selected.
No default model name is bundled. Subscription availability depends on the current OpenAI preview and account permissions.
Use ChatGPT Settings → Usage to review plan limits. This app never substitutes a paid API call.

Version 0.2 retains one registration. Reauthorization must use the same account and issued client ID.
An issued registration ID is saved before code exchange. If exchange fails or the process stops, the next attempt reuses that ID.
This pending registration does not become a connected account until its signed identity is verified.
Sign-out clears local access, refresh, and ID tokens, and removes login hints.
It retains the opaque host ID and verified registration binding for the next sign-in.
If remote revocation cannot be confirmed, the app says so. You can disconnect AI Chat in ChatGPT Settings.

Conversation context stays in memory. It retains at most six completed pairs and 24,000 characters, dropping oldest complete pairs first.
Questions are limited to 4,000 characters. Partial answers are marked incomplete and do not enter conversation context.
No account, question, answer, or token is logged by the app.

## Build and install

Use JDK 17, SDK platform `android-37.0`, and build tools `36.0.0`.
The app has its own Gradle build:

```sh
apps/ask-tv/gradlew -p apps/ask-tv testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease assembleDebugAndroidTest --console=plain
```

Install only AI Chat with:

```sh
TV_SERIAL='<connected serial>' tools/install-ask-tv.sh
```

The installer builds and tests the release APK, installs it, opens it, and prints its hash and installed version.
It leaves launcher routing, Cast packages, other apps, and existing app data unchanged.
The release APK uses the repository's local debug signing configuration. Store signing is separate work.

Run the focused emulator navigation class:

```sh
ANDROID_SERIAL=emulator-5554 apps/ask-tv/gradlew -p apps/ask-tv connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.asktv.AskTvNavigationTest' --console=plain
```

The emulator establishes layout and D-pad behavior only. It does not establish physical microphone routing, audible quality, or live subscription access.
Android recognition services can vary by device. Answer speech requires an installed offline voice and can be muted in Configuration.
Android speech engines limit one spoken segment. Answers longer than the engine limit remain fully readable; Read aloud reads the first segment.

Artwork and rights are recorded in [the asset record](../../docs/ask-tv/assets.md).

The [AI Chat 0.2 validation record](../../docs/ask-tv/validation-2026-10-30-ai-chat.md) records the installed release, completed Mac sign-in, and current test results.
The user reported that a remote question and spoken answer worked on the actual TV. The coordinator did not independently assess spoken output.
The user also reports that the interface needs substantial polish. Work on the complete user flow continues.

The [version 0.1 validation record for 2026-10-30](../../docs/ask-tv/validation-2026-10-30.md) separates test results, visual review, installation, and unfinished sign-in.

The [current diagnostic scope](../../docs/superpowers/specs/2026-10-30-ai-chat-sign-in.md) keeps the existing Mac loopback flow. No QR flow is included.
