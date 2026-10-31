# AI Chat

Ask a spoken or typed question on Google TV. AI Chat uses the ChatGPT subscription authorization flow.
It has no API key entry, paid API fallback, or ChatGPT history import.
Version `0.3` uses one signed APK and package `com.amine.asktv` for the TV and Android phone helper.
Both devices require Android 14 or later. The release version code is `3`.

## Remote controls

The app opens with its primary action focused and the microphone off.
After sign-in and plan permission, select **Ask a question** to start the microphone.
Microphone permission is requested only when you choose Ask.
Select **Type a question** to use the keyboard. If Android speech recognition is unavailable, use typed input.
The separate Assistant button keeps its normal system behavior.

The two example buttons show built-in sample answers.
Each answer is labeled **Built-in example · no live model used** and does not contact OpenAI.
Live questions require sign-in and ChatGPT plan permission.
Configuration shows the account, plan status, current server model catalog, and speech controls.
Only listed subscription models can be selected. No default model name is bundled.

In Configuration, select **Model** to open the model chooser.
Select **Refresh models** to request the current catalog.
Use **Read answers aloud** to turn automatic speech on or off.

Use Up and Down inside an answer to scroll. **Stop** cancels the current action.
**Read aloud** uses an installed offline Android voice. **Follow-up** opens typed input.
**New conversation** clears the conversation in memory.
Back first stops recording, inference, or speech. Back then returns from a page or exits the ready screen.
Leaving the app stops all recording, inference, and audio. The displayed text remains available on return.

## Install the phone helper once

Install the same release APK privately from the Mac through ADB, the Android device connection tool.
There is no helper download website or public APK upload.
The TV QR connects an installed helper to one sign-in attempt. It does not install the helper.
On the TV route screen, **Phone setup guide · install once** opens a guide QR.
That QR opens this installation section. It does not install the APK or start a pairing attempt.
Enable the phone's developer connection and authorize the Mac before installation.
Replace each serial placeholder with the target shown by `adb devices`.
Build the APK with the commands in **Build and install**, then run:

```sh
adb -s '<phone serial>' install -r apps/ask-tv/build/outputs/apk/release/ask-tv-release.apk
adb -s '<phone serial>' shell am start -W -n com.amine.asktv/.phoneauth.PhoneAuthActivity
```

The same package exposes a TV launcher activity and a separate phone helper activity.
Do not clear app data when you update either installation.
The phone helper needs no microphone access. It delegates photographs to the installed camera app.
Camera, microphone, touchscreen, and TV features are optional in the package manifest.

## Sign in with an Android phone

Keep the TV and phone on the same reachable local network.
The screens say “same Wi-Fi”, but matching network names alone do not establish connectivity.
Guest networks, client isolation, VPN routing, or firewalls can block the phone from reaching the TV.
The pairing route accepts private IPv4 addresses only. It does not support public addresses, IPv6, or remote pairing.

1. Keep AI Chat visible on the TV.
2. Select **Continue with ChatGPT**, then **Use Android phone**.
3. Open AI Chat on the phone.
4. Select **Photograph TV code** and photograph the complete TV QR, including its white margin.
5. When the helper shows **TV code ready**, select **Continue with ChatGPT** on the visible phone screen.
6. Allow local network access if Android requests it.
7. Complete sign-in in the phone's system browser and allow ChatGPT plan use.
8. Return to the TV and make sure that AI Chat shows the connected account and plan status.

The TV renders a genuine 512 × 512 pixel QR for the active attempt.
A camera or scanner that supports the `aichat://pair` deep link can open the installed helper directly.
The helper also offers **Paste pairing link instead** for a link obtained privately.
All routes require the visible **Continue with ChatGPT** action before network work or browser launch.
Opening a link alone does not authorize sign-in.

On Android API 37, the helper requests local network permission before connecting.
The TV also requests that permission when required.
The helper requests notification permission for its foreground service, which keeps the temporary callback listener active while the browser is visible.
Notification denial does not prevent that service from running.
When available, its notification opens the helper or cancels the attempt.
Do not force-stop the helper or remove its task during authorization.
If the helper stops, get a new code on the TV.

“Authorization sent” on the phone means that the callback reached the TV.
It does not mean that the TV saved a verified account.
The TV must verify the identity and save the account before it reports **Your TV is connected**.
If the TV saves the account without plan permission, select **Allow plan access** and complete consent again.
That action reuses the saved registration. Ordinary sign-in does not force consent.

Select **Cancel** or press Back on the TV to cancel an active attempt.
The phone helper also has Cancel.
After expiry, failure, or cancellation, select **Try again** on the TV for a new code.
Codes expire after ten minutes. Retries do not extend that deadline.
Cancel cannot erase an account that the TV already saved. Read the final TV status before starting again.
To renew authorization for a connected account, select **Reconnect account** in Configuration.
You do not need to select **Sign out** first.

## Mac fallback

Select **Use Mac helper** on the route screen or the active phone panel.
Keep AI Chat visible and run this command from the repository root:

```sh
TV_SERIAL='<TV serial>' tools/sign-in-ask-tv.sh
```

The helper opens the Mac browser. Complete **Continue with ChatGPT** and allow ChatGPT plan use.
Make sure that Configuration on the TV shows the verified account and subscription status.
An unfinished browser flow does not connect the account.
For **Allow plan access**, run the helper while that screen remains open.
The helper preserves the active consent attempt.

The Mac helper forwards only local port 1455, opens the browser, and waits.
It does not read ChatGPT or Codex credentials or save tokens on the Mac.
It refuses an occupied listener or ADB forward and removes only its own forwarding.
The temporary Android listener binds to `127.0.0.1:1455` and expires after ten minutes.
Switching routes retires the previous attempt before starting its replacement.

## Authorization and data

The phone route uses a temporary TLS relay on the TV's private network address.
TLS pinning checks the TV certificate against the fingerprint in the QR before sending pairing data.
Each attempt accepts one phone claim and one authorization result.
Identical delivery retries reuse that result. Changed callbacks and replayed claims are rejected.
The listener closes after cancellation, expiry, or a final-status grace period of at most 30 seconds.
HTTP sizes, connection timeouts, and socket lifetimes are bounded.
Pairing links and temporary photographs stay private. The helper deletes photographs after reading or cancellation.

The phone receives no OAuth verifier, ID token, access token, or refresh token.
The TV owns authorization, identity verification, and encrypted token storage.
Both routes retain the exact browser callback `http://127.0.0.1:1455/auth/callback`.
The local relay and ADB tunnel are app implementation assumptions, not an official device-code grant or provider endorsement.
No public relay backend is used.

The app retains one registration. Reauthorization must use the same account and issued client ID.
An issued registration ID is saved before code exchange.
If exchange fails or the process stops, the next attempt reuses that ID.
A pending registration does not become a connected account until its signed identity is verified.
Sign-out clears local access, refresh, and ID tokens, and removes login hints.
It retains the opaque host ID and verified registration binding for the next sign-in.
If remote revocation cannot be confirmed, the app says so.
You can disconnect AI Chat in ChatGPT Settings.

Subscription availability depends on the current OpenAI preview and account permissions.
Use ChatGPT Settings → Usage to review plan limits. This app never substitutes a paid API call.
Conversation context stays in memory. It retains at most six completed pairs and 24,000 characters, dropping oldest complete pairs first.
Questions are limited to 4,000 characters. Partial answers are marked incomplete and do not enter conversation context.
The app logs no account, question, answer, or token.

## Build and install

Use JDK 17, SDK platform `android-37.0`, and build tools `36.0.0`.
The app has its own Gradle build:

```sh
apps/ask-tv/gradlew -p apps/ask-tv testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease assembleDebugAndroidTest --console=plain
```

Install only AI Chat on an authorized TV emulator with:

```sh
TV_SERIAL='<TV emulator serial>' tools/install-ask-tv.sh
```

The installer tests the release unit suite, runs release lint, builds the APK, installs it, and opens the TV activity.
It prints the artifact hash and installed version.
It leaves launcher routing, Cast packages, other apps, and existing app data unchanged.
The release APK uses the repository's local debug signing configuration. Store signing is separate work.
Physical TV installation requires a specific user request or a reported TV issue.

Run each native test class separately with an explicit emulator serial. For example:

```sh
ANDROID_SERIAL='<TV emulator serial>' apps/ask-tv/gradlew -p apps/ask-tv connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.asktv.AskTvNavigationTest' --console=plain
```

Use dedicated test installations. Instrumented tests can uninstall the tested package.
Inspect XML class names and counts before claiming coverage.
The emulator establishes only the behavior exercised by its tests.
It does not establish physical microphone routing, audible quality, camera quality, LAN reachability, or live subscription authorization.
Android recognition services vary by device.
Answer speech requires an installed offline voice and can be muted in Configuration.
Answers longer than the speech engine limit remain fully readable. **Read aloud** reads the first segment.

Artwork and rights are recorded in [the asset record](../../docs/ask-tv/assets.md).
The [phone sign-in concept record](../../docs/ask-tv/phone-sign-in-concept.md) records private ImageGen previews. Existing bundled artwork is unchanged.
The [0.3 phone sign-in validation record](../../docs/ask-tv/phone-sign-in-validation-2026-10-30.md) separates tests, reviews, pending release checks, and live authorization.
The [0.2 validation record](../../docs/ask-tv/validation-2026-10-30-ai-chat.md) records the earlier installed release and completed Mac sign-in.
The user reported a successful remote question and spoken answer on installed 0.2.
That result does not establish phone authorization or physical voice behavior for 0.3.
The [0.1 validation record](../../docs/ask-tv/validation-2026-10-30.md) remains historical evidence.
