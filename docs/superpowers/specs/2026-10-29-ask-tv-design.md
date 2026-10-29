# Ask TV design

Date: 2026-10-29. Implementation is authorized without another approval wait.

## Goal and scope

Build a clean, standalone TV app for spoken or typed questions. Use the user's existing ChatGPT subscription through the official open-source token-sharing flow. Do not require a separately paid API, API key, ChatGPT history, or ChatGPT audio service.

The app lives in `apps/ask-tv`, with package `com.amine.asktv`, title `Ask TV`, version `0.1`, and version code `1`. It has an independent Kotlin and Compose for TV build. Minimum SDK is `34`; target and compile SDK are `37`. Preserve the existing Java builds and all other apps.

## Native interface

Use the warm gold concept selected by the coordinator. Use background `#0D0E12`, ivory `#F8F5F0`, and gold `#F6CD83`. Keep text and controls native. Use large TV text, clear focus outlines, and ample spacing. Keep the lower-right area clear for TV Clock.

Ready opens with focus on **Ask**. The microphone is off. Configuration sits in the upper right. Two prompt chips offer clearly labeled **Example** previews. Each preview shows a deterministic sample answer and never claims to be connected. A typed-question dialog works without microphone access. A signed-out real question leads to configuration and **Continue with ChatGPT**.

Configuration shows actual account status, an explicit subscription label, sign-in, sign-out, the current server-listed model, and speech controls. Version 0.1 supports one saved registration, without an account chooser. Never infer connection from a saved label or an unfinished browser flow. Keep usable examples available while signed out.

An answer has a readable scrolling area, **Stop**, **Repeat**, a follow-up action, and **New conversation**. Partial text remains visibly incomplete if a request fails. Do not present a partial response as successful. New conversation clears session context. Retain bounded context in memory only; limit it to the latest six completed question/answer pairs and 24,000 characters. Drop oldest complete pairs first. Reject a single question above 4,000 characters with a readable message.

Back cancels recording, inference, or speech first. Back returns from configuration or a dialog to its prior screen. Back from the idle main screen exits. Restore the previous useful focus after dialogs and app return.

## Speech and lifecycle

Use Android `SpeechRecognizer` for one utterance and Android `TextToSpeech` for answer playback. Request microphone permission only for an explicit recording action. Denial, unavailable recognition, no result, and recognition errors offer typed input. No continuous listening, wake word, background recording, or separately billed audio endpoint is included.

Starting a new action cancels the previous action. Activity stop cancels recognition, network work, and audio. Ignore callbacks from canceled operations. Stop speech before recording. Release speech resources when their owner ends. If speech synthesis is unavailable, keep the answer readable and explain the unavailable action.

## Authentication and credentials

The Android app owns authorization and credentials. The Mac only opens a browser through an explicit ADB tunnel. This tunnel is a local implementation assumption to test, not an OpenAI-endorsed arrangement. Bind a `ServerSocket` to `127.0.0.1:1455`, never a wildcard address. One attempt lasts at most ten minutes. Close the listener after success, cancellation, or timeout.

`tools/sign-in-ask-tv.sh` accepts an explicit serial, creates `adb -s "$SERIAL" forward tcp:1455 tcp:1455`, starts `com.amine.asktv.AUTHENTICATE` or the activity with `--ez authenticate true`, and opens `http://127.0.0.1:1455/start`. `/start` redirects to OpenAI. `/auth/callback` receives the result. The helper writes no credentials to Mac files or logs. Clean up its owned forwarding when the attempt ends.

Follow the official [registration and sign-in contract](https://developers.openai.com/siwc/token-sharing-open-source/sign-in). Persist a stable `urn:uuid` host ID. Use fresh state, nonce, and PKCE S256 for every attempt. Start with `dynamic_agent_client`; reuse the issued client ID later. Use `agent_name_hint=Ask TV` for registration only. Use callback `http://127.0.0.1:1455/auth/callback` consistently. Request `openid profile email offline_access resource.invoke chatgpt.tokens.use.direct` and resource `https://api.openai.com/v1`. Authorize at `https://auth.openai.com/api/accounts/authorize`; exchange at `https://auth.openai.com/api/accounts/oauth/token`. Reject invalid state, missing initial client ID, changed returning client ID, or mismatched returning identity. Validate RSA signature through published JWKS, issuer, audience, expiry, and nonce. Require the granted subscription scope before inference.

Use Android Keystore encryption and an atomic credential record. Set `android:allowBackup="false"`. Store account identity, issued client ID, tokens, scopes, expiry, and host identity together consistently. Never log tokens, authorization URLs, prompts, or raw error bodies.

Follow [accounts and sessions](https://developers.openai.com/siwc/token-sharing-open-source/profiles-and-sessions) for active account display, serialized refresh, rotated credentials, and sign-out. Keep one issued client/verified subject mapping for reauthorization. Commit refreshed credentials atomically. Scope downgrade disables inference. Invalid grant requires sign-in. Sign-out stops requests, attempts revocation through OpenID discovery, and clears local tokens. If remote revocation fails, state that it was not confirmed. Retain the registration mapping and host ID without retained token hints after sign-out.

## Subscription inference

Follow [models and inference](https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference). Fetch `https://api.openai.com/v1/models` for the active account. Display `visibility: "list"` entries in server order with `display_name`; send their `slug`. Persist the selected model and revalidate it against a fresh account catalog.

Send the subscription OAuth bearer token to `https://api.openai.com/v1/responses`, with `store:false`, `stream:true`, and bounded completed session context. Handle SSE text deltas, explicit errors, incomplete responses, cancellation, and disconnects. Success requires `response.completed`. Subscription usage unavailable or limit errors stop the request and show recovery guidance. No API key fallback exists.

The [preview limitations](https://developers.openai.com/siwc/token-sharing-open-source/preview-limitations) remain an external dependency. Installation and examples can be complete while live account authorization remains unfinished. Report those outcomes separately.

## Build and artwork

Reuse the inspected Family Home versions: JDK 17, Gradle `9.6.0`, AGP `9.4.1`, Kotlin and Compose compiler `2.4.21`, Compose BOM `2026.09.00`, TV Material `1.1.0`, and build tools `36.0.0`. Copy only the required independent build structure. Keep permissions limited to network and explicit microphone use.

Bundle a simple Android vector chat icon with a 512-square viewport. Render a separate 640×360 WebP banner in code with the chat icon and **Ask TV** title, using licensed Outfit if needed. Do not depend on vector XML text support. Bundle a separate generated text-free 1920×1080 dark gold background as compressed WebP. Keep all three resources in Ask TV. Declare the background through application metadata `com.amine.familyhome.HERO_IMAGE` with `android:resource`. Record prompt, source, dimensions, compression, hash, and rights under `docs/ask-tv`. Preserve the OFL if a bundled font is copied.

## Acceptance and delivery

Test OAuth validation, credential updates, refresh races, scope downgrade, subscription-only payloads, streaming completion, cancellation, and session bounds. Test actual D-pad movement, initial focus, dialogs, scrolling, and return focus in the emulator. Fixtures must remain distinguishable from connected behavior.

The coordinator owns all builds, device input, integration, and Git delivery. Obtain fresh independent code and UI review. Run local unit tests, lint, release packaging, installer tests, and diff checks. Keep APK workflows paused.

Install on the emulator, then install the release build on the explicitly authorized physical TV. Verify the installed version and APK hash. Attempt Mac-assisted authentication if possible; report any remaining user sign-in step accurately. Do not place account captures, device identifiers, or personal data in Git. After installation, fetch `origin`, integrate concurrent changes, repeat affected checks, commit, push the task branch, and deliver the validated revision to `origin/main`. Preserve other tasks and device configuration.
