# AI Chat Android phone pairing

## Goal and status

An Android phone signs AI Chat into its own ChatGPT registration through a real TV QR code.
The TV keeps the PKCE verifier, exchanges the code, validates identity, and stores and refreshes all OAuth tokens.
The phone receives the browser callback and forwards it over a temporary, authenticated TLS connection to the TV.
The normal phone route needs no Mac, ADB tunnel, public backend, or separately hosted page.

This is a new app-specific relay design. It is not an official OpenAI device-code flow, and it has no runtime evidence yet.
The user selected Android and authorized work without another design approval pause.
Keep the existing Mac route available for the current live diagnostic and subsequent recovery.

## Source constraints and choice

Use one APK and the existing `com.amine.asktv` package. Add a phone launcher and `PhoneAuthActivity` alongside the TV activity.
Keep `minSdk = 34`, `compileSdk = 37`, and `targetSdk = 37` from the current Gradle file.
This route therefore requires Android 14 or later on the phone. The user's phone version is not established.
Do not lower the Android floor or add a second APK as incidental work.
If the phone runs an older version, that compatibility requirement needs a separate decision.

Keep the existing credential file, Keystore alias, TV host ID, issued client binding, and pending-registration recovery.
Use pure Kotlin/JVM and Android framework APIs. The Chromecast exposes only `armeabi-v7a,armeabi` for app binaries.
Do not add a Codex executable, native QR library, account migration, API-key entry, or paid API fallback.

Postbox is the source reference, not a new dependency. Adapt these patterns into AI Chat's package:

| Existing source | Reuse |
| --- | --- |
| `apps/postbox/src/main/AndroidManifest.xml` | Optional leanback feature, separate phone launcher, pairing deep link, private FileProvider |
| `core/PairingLink.kt`, `core/TlsIdentity.kt`, `core/PostboxClient.kt` | Strict private IPv4 link, Android Keystore certificate, SHA-256 certificate pin before application bytes |
| `core/TransferProtocol.kt`, `core/CaptureReceiver.kt` | Bounded HTTP parsing, two workers, bounded queue, socket deadlines, current-session cleanup |
| `PairingPhotoReader.kt`, `PhoneActivity.kt`, `TvScreens.kt` | ZXing `3.5.4`, native QR bitmap, delegated photograph, private bounded decode, photo deletion |
| `ReceiverLifecycleTest.kt`, `core/PinnedTransferTest.kt` | Delayed-start races, stale cleanup, real Android TLS, stalled-handshake tests |

Do not copy Postbox's persistent phone token or its phone `onPause` cancellation policy.
AI Chat pairing lasts for one authorization attempt. Opening the browser must leave the phone listener running.

## Official protocol boundary

The TV creates the official SIWC authorization URL through the existing `OAuthProtocol`.
Use `dynamic_agent_client` only for first registration, `agent_name_hint=AI Chat`, and the TV's stable `ext_agent_host_id`.
Reuse an existing issued or pending client ID exactly as the current app does.
Keep the exact callback `http://127.0.0.1:1455/auth/callback` in authorization and exchange.
The listener for this route runs on the phone, so the phone browser can reach that address.
The TV does not need to bind port 1455 for the phone route.

The TV retains the state, nonce, and verifier. The phone sees the authorization URL and its public PKCE challenge.
Do not include retained `id_token_hint` on the phone route. The phone must receive no ID, access, or refresh token.
The phone never calls the token endpoint, creates an OpenAI client, or changes the TV host ID.
The TV uses its existing `OAuthClient`, `SessionStore`, and `AccountCompletion` after relay delivery.
Models and inference continue through public `/v1/models` and `/v1/responses` with the granted subscription permission.
Do not read or copy Mango, ChatGPT, or Codex credentials.

OpenAI documents loopback OAuth and a separate Codex-managed device-code login.
Its documentation does not establish a dynamic-client device grant or export of Codex device credentials into this direct integration.
The relay keeps the documented OAuth messages unchanged, but OpenAI does not document or endorse this cross-device callback transport.

Sources read on 2026-10-30: [SIWC sign-in](https://developers.openai.com/siwc/token-sharing-open-source/sign-in),
[SIWC sessions](https://developers.openai.com/siwc/token-sharing-open-source/profiles-and-sessions),
[Models and inference](https://developers.openai.com/siwc/token-sharing-open-source/models-and-inference),
[Loopback behavior on remote hosts](https://developers.openai.com/siwc/token-sharing-open-source/self-hosted-vms),
and [Codex device-code login](https://learn.chatgpt.com/docs/app-server#3b-log-in-with-chatgpt-device-code-flow).

## User flow

1. On TV, select **Continue with ChatGPT**, then **Use Android phone**. Keep **Use Mac** as the other route.
2. The TV requests local network permission when required, starts a fresh attempt, and displays a native QR code and countdown.
3. On the phone, open AI Chat and select **Photograph TV code**, or open the pairing link through the system camera.
4. The phone requests local network permission when required. It connects only after the user selects **Continue with ChatGPT**.
5. The phone starts its foreground service and binds `127.0.0.1:1455` before it claims the TV attempt.
6. The phone verifies the pinned TV certificate, claims the attempt, and opens the official authorization URL in the system browser.
7. The browser returns to the phone listener. The phone forwards the callback to the TV over pinned TLS.
8. The TV validates the callback, exchanges the code, validates identity and scopes, and commits the encrypted record.
9. The TV displays **Connected through your ChatGPT subscription.** only after that commit and permission check.

For an account without plan permission, show the existing explicit **Enable ChatGPT plan** action with the same phone/Mac choice.
Phone success copy says **Authorization sent. Check AI Chat on your TV.** until the TV reports its terminal result.
Keep a visible **Cancel** action on both screens. The phone notification opens the helper and also offers **Cancel**.
If system QR handling fails, the photograph route remains available. A full-link paste field is the final fallback.
Delete temporary photographs after decode, cancellation, or failure. Bound decoded images to 2,048 pixels on the longest edge.
Do not save pairing links to the clipboard automatically or display them in routine diagnostics.

Reuse the current TV palette, type, margins, and button treatment. Render the QR at 512 by 512 pixels with a white quiet zone.
Keep the QR square and uncropped. Keep its controls clear of the separate Clock overlay.
Use native Compose controls and a native ZXing bitmap. Keep the current icon, banner, and launcher background.
Use the [private concept record](../../ask-tv/phone-sign-in-concept.md) as a visual reference only. Review actual phone and TV emulator captures independently.

## Pairing and relay contract

The TV binds an available TCP port on one selected private IPv4 LAN interface.
Bind only that address, not `0.0.0.0`. The phone and TV must share a reachable local network.
Do not use DNS, mDNS discovery, public addresses, cleartext LAN HTTP, or router changes.
IPv6-only networks are outside the first implementation. Display a useful unavailable-network message.

The QR contains `aichat://pair?v=1&host=<private-ipv4>&port=<port>&pin=<sha256-cert-hex>&token=<bootstrap>`.
Allow exactly those five fields. Reject duplicates, unknown fields, paths, fragments, user information, noncanonical IPv4, and invalid encodings.
Use Postbox's RFC1918 ranges, port `1..65535`, lowercase 64-hex pin, and 43-character base64url tokens from 32 random bytes.
Limit the full pairing link to 2,048 characters. Treat the bootstrap token as a secret visible to people who can see the screen.
The TV uses an AI Chat Keystore alias distinct from Postbox's alias. The phone pins the certificate before sending any request body or authorization header.
Never fall back to a trust-all client, a different pin, or cleartext after a failure.

Use HTTP/1.1 over native TLS sockets. Requests use bounded, duplicate-rejecting form fields and explicit `Content-Length`.
Responses use fixed JSON schemas with no credentials or account identifiers. Do not follow redirects on the LAN transport.

| Request | Authorization and body | Result |
| --- | --- | --- |
| `POST /claim` | No Authorization header. Form contains only `token=<bootstrap>` | Consume bootstrap once. Return fresh `relay_token`, `authorization_url`, and remaining `expires_in_ms` |
| `POST /callback` | `Authorization: Bearer <relay_token>`. Form contains only `target=<raw-loopback-callback-target>` | Return `202 {"receipt":"accepted"}` after one validated callback dispatch. Retry of identical bytes returns the same receipt without another exchange |
| `POST /status` | Same bearer header. Empty body | Return one state: `waiting`, `verifying`, `connected`, `plan_required`, `failed`, or `canceled` |
| `POST /cancel` | Same bearer header. Empty body | Cancel before commit and return `200 {"state":"canceled"}`. If already committed, return its existing terminal state without discarding the grant |

A lost `/claim` response requires a new TV QR. Do not replay a bootstrap to recover a relay token.
A callback receipt confirms delivery only. Poll status at most once per second until a terminal result or the shared deadline.
Keep the phone's identical callback in memory for up to three relay attempts within that deadline, with 250 and 500 ms backoff.
Cache only the accepted callback digest and receipt on TV after dispatch. Reject a different callback for the consumed attempt with `409`.
Invalid tokens return `403`. Closed or expired attempts return `410` while the listener is still available.
Malformed or unsupported requests return `400` or `405`. An oversized body returns `413`.
Untrusted requests do not consume or cancel a valid attempt.

Before browser launch, validate the fetched URL against the existing official authorization origin, path, required scope set, and exact loopback redirect.
Reject fragments, user information, unexpected authority ports, missing state/challenge/nonce, and client IDs outside current protocol rules.
Do not accept an arbitrary redirect URL from an intent or LAN response.
The phone accepts only a bounded `GET /auth/callback?...` origin-form request with the expected state.
Reject duplicate query fields before forwarding. The TV repeats parsing and OAuth validation through the same callback parser used by the Mac route.

## Lifetimes and bounds

Use a monotonic ten-minute deadline (`600_000` ms) from TV attempt creation. Claiming or retrying does not extend it.
The phone adopts the remaining lifetime returned by the TV and never exceeds ten minutes locally.
Remove the QR after claim. Keep relay status available for at most 30 seconds after a terminal result, within the original deadline.
Close all listeners, accepted sockets, worker queues, and timers on expiry, cancellation, or replacement.
Keep pairing tokens, callback codes, URLs, and service state in memory only. A process restart requires a fresh QR.
Do not persist them in saved instance state, intents, notifications, files, or analytics.
An initial deep-link Intent must be parsed and cleared immediately. The exported activity does not start network work until the user selects Continue.

Both HTTP listeners have an 8,192-byte request-line limit, a 16,384-byte total header limit, and a 7,168-byte callback-target limit.
The relay request/response body limit is 16,384 bytes. Bound bytes while reading, before allocating or decoding larger input.
Reject transfer encoding, duplicate `Content-Length` or Authorization headers, and request pipelining. Close each connection after its response.
Use two request workers, a queue of two, 5-second connect/handshake/read limits, and a 30-second absolute socket deadline.
Apply the absolute deadline to queued sockets and both input and output. Close excess sockets immediately.
Treat browser preconnections and incomplete-request timeouts as quiet closes. They must not consume the authorization attempt.

On TV, leaving the sign-in surface, backgrounding, Back, Cancel, expiry, or a new attempt invalidates the old generation before cleanup.
Old start jobs, completion callbacks, and timer jobs cannot close or update a newer generation.
Cancellation before the existing guarded commit prevents credential installation. Cancellation after a completed commit cannot discard that grant.
Do not call grant callbacks or acquire OAuth/store locks while holding the relay state lock.
Cancellation must release relay locks before it waits for OAuth cancellation. Preserve a committed terminal result when that wait finishes.

On phone, the service owns the listener and transport. Activity `onPause` and `onStop` do not cancel it.
Start the service from the visible helper after permissions and before browser launch. Wait for listener readiness before launching the browser.
Use `connectedDevice` with `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CONNECTED_DEVICE`, and qualifying `CHANGE_NETWORK_STATE` declarations.
Show a low-importance notification with immutable explicit actions and no pairing or OAuth data.
Request `POST_NOTIFICATIONS` to make that notification visible. Denial does not prevent foreground-service launch under Android's documented rules.
Use `START_NOT_STICKY`. Cancel on explicit Cancel, task removal, service destruction, expiry, or completion. Do not restart at boot.
Do not launch activities from the background callback. The browser page and notification guide the user back to AI Chat.

Declare `ACCESS_LOCAL_NETWORK` and request it before LAN operations on API 37 or later, on both devices.
Keep `usesCleartextTraffic=false`; the browser's official HTTP loopback redirect is separate from the pinned LAN client.
The browser and helper must run in the same Android user/profile. Cross-profile loopback requires separate compatibility investigation.

Android sources: [Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types#connected-device),
[Foreground service launch](https://developer.android.com/develop/background-work/services/fgs/launch),
[Notification permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission),
and [Local network permission](https://developer.android.com/privacy-and-security/local-network-permission).

## Acceptance and limits

Pure tests cover malformed links and requests, wrong state/pin/token, duplicate fields, expiry, replay, denial, cancellation, and replacement races.
Native Android tests establish real pinned TLS, bounded sockets, loopback callback handling, foreground-service survival, phone photo decoding, and TV D-pad focus.
Keep the Mac callback tests, issued-client persistence tests, and account-completion race tests passing.
A synthetic callback test must use a fake exchange boundary. It cannot prove live OpenAI authorization or subscription inference.

Install the same signed release APK on the TV emulator and Android phone test target without clearing data.
Record exact revision, commands, XML class names/counts, APK hash, versions, generic captures, and a fresh independent review.
Keep account screens, QR secrets, addresses, identifiers, and credentials out of Git and shared captures.
Phone camera, home Wi-Fi, system browser callback behavior, and live SIWC remain separate evidence until tested on the user's actual phone/network.
The phone OS version and local-network reachability are prerequisites to confirm. They are not established failures.
No known source constraint blocks the single-APK design. Actual browser-to-phone callback and service behavior remain unverified engineering risks.
Do not report a working phone-only sign-in until an own-app live flow completes on the phone and the TV validates and commits it.
Keep the Mac route and current diagnostic candidate usable until then.
