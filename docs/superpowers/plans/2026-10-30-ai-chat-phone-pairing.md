# AI Chat Android Phone Pairing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task by task. Steps use checkbox syntax for tracking. The main agent coordinates. The user waived further design approval waits.

**Goal:** Complete AI Chat's own SIWC login through an Android phone and a temporary pinned TLS relay.

**Architecture:** One `com.amine.asktv` APK adds a phone activity and foreground callback service. The TV owns OAuth, PKCE, code exchange, credential storage, and refresh. The phone opens the official browser flow and forwards only the callback over an authenticated local connection.

**Tech Stack:** Existing Kotlin 2.4.21, AGP 9.4.1, JDK 17, Compose, Android Keystore and TLS sockets, ZXing core 3.5.4, and Android foreground services.

**Spec:** [Android phone pairing](../specs/2026-10-30-ai-chat-phone-pairing.md).

## Global constraints

- Continue in `/Users/amine/Developer/worktrees/ask-tv-sign-in/google-tv-apps`, branch `amine/ai-chat-sign-in`. The coordinator reconciles fresh `origin/main` before integration.
- Preserve all existing task edits. Assign each shared file to one implementer at a time. Do not create another worktree inside this checkout.
- Keep package `com.amine.asktv`, `minSdk = 34`, `compileSdk = 37`, and `targetSdk = 37`. Use one APK, version `0.3`, code `3`, for this feature.
- Keep the current credential file, Keystore session alias, TV host ID, issued/pending registration IDs, and Mac helper route.
- Keep exact callback `http://127.0.0.1:1455/auth/callback`. The phone receives no verifier, ID token, access token, or refresh token.
- No public backend, Codex binary, imported credentials, API-key fallback, native ABI dependency, cloud build, APK upload, or public release.
- Use the spec's exact `600_000` ms attempt limit, 30-second terminal grace, socket bounds, request forms, status values, and retry limits.
- The coordinator alone runs builds, test sessions, devices, live browser authorization, Git integration, and delivery. Implementation agents edit files only.

## Review focus

- Opening the browser backgrounds the phone activity. The foreground listener must survive. Task 3 tests this transition.
- A callback receipt or browser write can fail after TV acceptance. Task 1 tests replay and Task 2 tests reset sockets without a second exchange.
- Old expiry/start jobs race a replacement session. Tasks 1 and 4 test generation ownership before closure and UI updates.
- A canceled attempt races credential commit. Task 4 preserves the existing guarded commit and durable account-completion tests.
- An untrusted QR or stalled peer can send secrets to the wrong host or retain sockets. Tasks 1 and 2 test strict addresses, pin-before-bytes, and absolute deadlines.

## File ownership

All Kotlin paths below start at `apps/ask-tv/src/main/kotlin/com/amine/asktv/` unless stated otherwise.
Keep Postbox unchanged. Adapt its small app-local patterns without adding a shared module.

| Owner/task | Files |
| --- | --- |
| Protocol, Task 1 | New `phoneauth/PhonePairingLink.kt`, `phoneauth/PhoneRelayProtocol.kt`, `phoneauth/AuthCallbackParser.kt`, `phoneauth/PhoneAuthorization.kt`; existing `LoopbackAuthServer.kt` only for shared callback parsing; related JVM tests |
| Transport, Task 2 | New `phoneauth/RelayHttp.kt`, `phoneauth/RelayTlsIdentity.kt`, `phoneauth/PhoneRelayServer.kt`, `phoneauth/PhoneRelayClient.kt`, `phoneauth/PhoneLoopbackServer.kt`; transport JVM and native tests |
| Phone, Task 3 | New `phoneauth/PhoneAuthSession.kt`, `PhoneAuthService.kt`, `PhoneAuthActivity.kt`, `PhoneAuthScreen.kt`, `PhonePairingPhotoReader.kt`; app manifest, Gradle file, `res/xml/phone_pairing_paths.xml`, notification vector; phone tests |
| TV, Task 4 | New `PhoneSignInCoordinator.kt`, `PhoneSignInPanel.kt`; existing `AskViewModel.kt`, `AskScreen.kt`, `MainActivity.kt`; TV and integration tests |
| Documentation, Task 5 | `apps/ask-tv/README.md`, new `docs/ask-tv/phone-sign-in-validation-2026-10-30.md`, `tests/test_ask_tv_install.py` only for changed release metadata/contracts |

### Task 1: Define the one-attempt protocol

**Interfaces:**

- `PhonePairingLink(host: String, port: Int, pin: String, token: String)` provides `encode(): String` and `companion object parse(value: String): PhonePairingLink`.
- `AuthCallbackParser.parseTarget(target: String): Map<String, String>` parses only the bounded origin-form `/auth/callback` target and rejects duplicate fields.
- `RelayClaim(relayToken: String, authorizationUrl: String, expiresInMs: Long)` and `RelayReceipt(receipt: String)` define the wire response values.
- `RelayState` has `WAITING`, `VERIFYING`, `CONNECTED`, `PLAN_REQUIRED`, `FAILED`, and `CANCELED`, encoded as the spec's lowercase strings.
- `PhoneRelayProtocol(protocol: OAuthProtocol, monotonicMs: () -> Long, onGrant: (AuthGrant) -> Unit)` provides `open(durationMs: Long = 600_000): String`, `claim(bootstrap: String): RelayClaim`, `callback(relayToken: String, target: String): RelayReceipt`, `status(relayToken: String): RelayState`, `terminal(state: RelayState): Unit`, `cancel(relayToken: String): Unit`, and `close(): Unit`.
- `open()` calls `OAuthProtocol.begin()` exactly once per new relay instance. `PhoneRelayServer.start()` calls `open()` once. Claim, status, callback retry, and terminal grace never restart OAuth or its deadline.
- `RelayRejected(status: Int)` carries only safe HTTP status information. No exception message contains input data.
- `PhoneAuthorizationValidator.validate(value: String): PhoneAuthorization` returns a validated URL plus expected callback state. Define `PhoneAuthorization(url: String, state: String)` in `phoneauth/PhoneAuthorization.kt` for Task 3 to consume.

- [ ] Add `PhonePairingLinkTest` and `PhoneRelayProtocolTest` with fake clocks and a counted grant callback. Run them through the coordinator and establish a failing baseline.

Pin these assertions, using an actual `OAuthProtocol` where possible:

```kotlin
assertEquals(link, PhonePairingLink.parse(link.encode()))
assertThrows(IllegalArgumentException::class.java) { PhonePairingLink.parse(duplicateTokenLink) }
assertThrows(IllegalArgumentException::class.java) { PhonePairingLink.parse(publicHostLink) }
assertEquals(403, rejected { relay.claim("wrong") }.status)
assertEquals(600_000L, relay.claim(bootstrap).expiresInMs)
assertEquals(403, rejected { relay.claim(bootstrap) }.status)
assertEquals("accepted", relay.callback(relayToken, callback).receipt)
assertEquals("accepted", relay.callback(relayToken, callback).receipt)
assertEquals(1, exchangeDispatchCount.get())
assertEquals(409, rejected { relay.callback(relayToken, differentCallback) }.status)
assertEquals(410, rejected { atElapsed(600_000) { relay.status(relayToken) } }.status)
```

- [ ] Add `AuthCallbackParserTest` for wrong paths, absolute-form targets, fragments, duplicate encoded field names, malformed percent encoding, invalid UTF-8, and byte limits. Keep a preconnected/incomplete request from consuming a grant.
- [ ] Add URL validation tests for the official origin/path, exact callback, allowed current client IDs, scope set, challenge/state/nonce, and rejected external redirects or `id_token_hint`.
- [ ] Implement the interfaces. Dispatch one accepted grant, retain its digest for identical retries, and compare tokens in constant time.
- [ ] Invoke `onGrant` outside the relay lock. Never call OAuth cancellation under that lock. Add a barrier-based test for commit/cancel lock ordering and preserved terminal state.
- [ ] Switch only the Mac server's callback parsing to `AuthCallbackParser`. Preserve `/start`, `/status`, acknowledgement-failure behavior, and fixed diagnostics.
- [ ] Ask the coordinator to run `testDebugUnitTest` filtered to the three new protocol classes plus `OAuthProtocolTest`, `LoopbackAuthServerTest`, `AuthorizationRecoveryTest`, and `AuthorizationCompletionTest`.
- [ ] Commit the scoped protocol change after the coordinator confirms the results.

### Task 2: Add bounded native TLS and phone loopback transport

**Interfaces:**

- `RelayTimeouts(connectMs: Int = 5_000, handshakeMs: Long = 5_000, readMs: Int = 5_000, totalMs: Long = 30_000)` is injectable for tests.
- `PhoneRelayServer(context: Context, protocol: PhoneRelayProtocol, timeouts: RelayTimeouts = RelayTimeouts())` provides `start(): PhonePairingLink` and `close(): Unit`.
- `PhoneRelayTransport : AutoCloseable` defines `claim(): RelayClaim`, `callback(relayToken: String, target: String): RelayReceipt`, `status(relayToken: String): RelayState`, and `cancel(relayToken: String): Unit`. `PhoneRelayClient(link: PhonePairingLink, timeouts: RelayTimeouts = RelayTimeouts())` implements it.
- `PhoneLoopbackListener : AutoCloseable` defines `start(): Unit` and `expectState(value: String): Unit`. `PhoneLoopbackServer(onCallback: (String) -> Unit, onFailure: (String) -> Unit, timeouts: RelayTimeouts = RelayTimeouts())` implements it. Bind before claim, then install the expected state before browser readiness. Reject callbacks while no state is installed.
- Test-only constructor options can override the connect/bind address, port, clock, and timeouts. Production cannot select public addresses or bypass the pin.

- [ ] Add `RelayHttpTest` before implementation. Assert byte bounds before allocation, duplicate-header/form rejection, unsupported methods/paths, empty bodies, and fixed response schemas.
- [ ] Add `PhoneRelayTlsTest` under `src/androidTest/kotlin/com/amine/asktv/phoneauth/`. Adapt Postbox's real Keystore/TLS fixtures, not a fake trust manager.
- [ ] Assert that the wrong certificate receives zero application bytes. Test correct claim, wrong token, identical callback retry, changed callback rejection, and listener close on expiry/cancel.
- [ ] Test dripping handshake, partial headers/body, blocked output, queue overload, and a queued socket deadline. Inject a 250 ms deadline and assert closure within 1,500 ms.
- [ ] Add `PhoneLoopbackServerTest` for exclusive `127.0.0.1:1455` binding, occupied-port failure, exact callback path/state, callback write reset, and quiet preconnection timeout.
- [ ] Implement the transport with the spec's fixed worker/queue limits and per-socket absolute deadlines. Do not use unbounded executors or `readLine()` before a size bound.
- [ ] Use an AI Chat-only Keystore identity alias `ai-chat.phone-relay.v1`, with certificate subject `CN=AI Chat` and Postbox's compatible RSA/TLS configuration.
- [ ] Keep a terminal status receipt for at most 30 seconds within the original deadline, then close. Do not retain an authorization code after dispatch.
- [ ] Ask the coordinator to run the focused JVM classes, then the single native class on the emulator. Inspect XML selection and keep its report before another run.
- [ ] Commit the scoped transport change after the coordinator confirms the results.

### Task 3: Add the Android phone helper to the same APK

**Interfaces:**

- `PhoneAuthSession(clientFactory: (PhonePairingLink) -> PhoneRelayTransport, listenerFactory: ((String) -> Unit, (String) -> Unit) -> PhoneLoopbackListener, monotonicMs: () -> Long)` owns in-memory attempt state and implements `start(link: PhonePairingLink): Unit`, `takeBrowserLaunch(): PhoneAuthorization?`, `cancel(): Unit`, and `close(): Unit`.
- `PhoneAuthSession` exposes `StateFlow<PhoneAuthUi>`. Define `PhoneAuthPhase` values `IDLE`, `STARTING`, `READY_FOR_BROWSER`, `WAITING_FOR_CALLBACK`, `VERIFYING`, and `FINISHED`. Define `PhoneAuthUi(phase: PhoneAuthPhase, message: String, deadlineElapsedMs: Long?, canOpenBrowser: Boolean)` without secrets.
- `takeBrowserLaunch()` returns the validated URL once and moves readiness to waiting. Rotation or rebinding cannot launch it twice. A failed browser launch cancels that session.
- `PhoneAuthService` owns that session and exposes a local Binder for `start(link: PhonePairingLink): Unit`, `snapshot: StateFlow<PhoneAuthUi>`, `takeBrowserLaunch(): PhoneAuthorization?`, and `cancel(): Unit`.
- `PhoneAuthActivity` binds from visible UI, passes the parsed link through the local Binder, and consumes browser readiness while resumed. No token-bearing start-service extras are allowed.

- [ ] Add `PhoneAuthSessionTest` with fake relay/listener boundaries. Assert bind-before-claim, claim-before-browser readiness, no browser on bind/pin failure, bounded callback retries, remaining-lifetime use, canceled/replaced generation isolation, and no credential endpoint calls.
- [ ] Add `PhoneAuthLifecycleTest` under Android tests. Launch the actual activity/service, move the activity to stopped state, deliver a synthetic callback through a real loopback socket, and assert one relay dispatch.
- [ ] Also test rotation/rebind, denied local-network permission, denied notifications, service start failure, notification Cancel, task removal, process/session loss, and stale notification actions against a replacement generation.
- [ ] Implement a non-exported `connectedDevice` foreground service, started while the helper is visible, with `START_NOT_STICKY`, low-importance channel, fixed notification text, explicit immutable Open/Cancel actions, and cleanup on completion/expiry/destruction.
- [ ] Add manifest permissions `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_CONNECTED_DEVICE`, `CHANGE_NETWORK_STATE`, `POST_NOTIFICATIONS`, and `ACCESS_LOCAL_NETWORK`. Request local-network access on API 37+ before LAN work. Notification denial does not block service launch.
- [ ] Make leanback optional. Add an exported `PhoneAuthActivity` with a phone `MAIN/LAUNCHER` filter and `VIEW/DEFAULT/BROWSABLE` for `aichat://pair`. Leave the TV `LEANBACK_LAUNCHER` and authenticate action intact.
- [ ] Parse/clear incoming links immediately, require a visible Continue action before network work, and reject conflicting new links during an active flow unless the user cancels it.
- [ ] Add Compose Material 3 and ZXing core `3.5.4` as Postbox uses them. Keep the current build tools and ABI independence.
- [ ] Add the private FileProvider path, delegated camera flow, bounded decoder, paste fallback, and phone screen with explicit colors, Continue/Cancel, countdown, and safe results. Store no relay secrets in saved state or clipboard.
- [ ] Add `PhonePairingUiTest`: decode a genuine generated QR from a private fixture photo, confirm photo deletion on success/error/cancel, open a deep link, reject malformed input, and inspect phone text contrast.
- [ ] Set feature version `0.3`, code `3`. Ask the coordinator to run focused unit tests and each phone instrumented class separately on a phone emulator, followed by release lint/packaging when integrated.
- [ ] Commit the scoped phone change after the coordinator confirms the results.

### Task 4: Integrate TV phone sign-in and retain Mac diagnostics

**Interfaces:**

- `SignInRoute` has `PHONE` and `MAC`. `AskViewModel.chooseSignInRoute(requestConsent: Boolean)` opens the route chooser. `AskViewModel.signIn(route: SignInRoute, requestConsent: Boolean = false)` starts exactly one route.
- Preserve no-argument `AskViewModel.signIn()` for the existing authenticate intent and helper. It starts `MAC`. Ordinary TV sign-in buttons call `chooseSignInRoute`.
- `PhoneSignInCoordinator(context: Context, onGrant: (OAuthProtocol, AuthGrant, Long) -> Unit, onUi: (Long, PhonePairingUi) -> Unit)` provides `start(record: CredentialRecord, requestConsent: Boolean, operationToken: Long): Unit`, `terminal(state: RelayState): Unit`, and `close(): Unit`. It creates the protocol/relay and connects one grant to the existing guarded exchange/commit path. Its generation is the existing `AskController` operation token.
- `PhonePairingUi(link: String?, expiresAtElapsedMs: Long, state: String)` carries TV presentation state only. Do not expose it to logs or diagnostic snapshots.
- `PhoneSignInPanel` renders native QR and countdown with tags `phone-sign-in-qr`, `phone-sign-in-cancel`, and `phone-sign-in-retry`.

- [ ] Add `PhoneSignInCoordinatorTest` with fake transport/exchange. Assert same TV host ID and saved issued/pending client ID, guarded pending-client persistence, identity/scope rejection, cancel-before-commit, accepted commit-before-cancel, and stale generation closure.
- [ ] Preserve the current durable-first account behavior. Both routes call one extracted grant-completion function rather than duplicating code exchange and installation.
- [ ] Show **Use Android phone** and **Use Mac** after Continue or Enable plan. Keep Enable plan's explicit consent flag and ordinary sign-in's existing behavior.
- [ ] Start LAN listening only with permission and while the TV sign-in panel stays active. Back/background/Cancel/replacement stops it. Claim hides the QR without extending the deadline.
- [ ] Add a real ZXing QR bitmap with a quiet zone and current palette. Keep focus on Cancel while waiting. Retry creates fresh bootstrap/state/nonce/verifier and restores the correct focus.
- [ ] Add `PhoneSignInNavigationTest`: actual D-pad selection, initial focus, route choice, QR visibility, denied permission, Cancel, Back, retry, terminal plan-required state, and focus after app return. Do not force destination focus.
- [ ] Run the existing `AskTvNavigationTest` and all new TV integration classes one class per invocation on the TV emulator. Keep phone and TV sessions sequential.
- [ ] Commit the scoped TV integration after the coordinator confirms focused checks.

### Task 5: Review, validate, install, and deliver

- [ ] Update the app guide with the Android 14+ phone requirement, same signed APK, phone/system-browser sequence, temporary relay, permissions, network limits, and retained Mac route.
- [ ] Record test evidence in `docs/ask-tv/phone-sign-in-validation-2026-10-30.md`. Start it as unverified and replace each status only with actual evidence.
- [ ] Ask a fresh independent reviewer to inspect the complete patch, native phone/TV captures, trust boundary, foreground lifecycle, cancellation races, and upgrade preservation. Fix material findings and repeat affected checks.
- [ ] Have the coordinator run the integrated app gate once after material fixes:

```sh
apps/ask-tv/gradlew -p apps/ask-tv testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease assembleDebugAndroidTest --console=plain
CLEANYOURTV_STRICT=1 python3 -m unittest discover -s tests -v
git diff --check
```

- [ ] Run each required Android class separately with an explicit serial. Inspect XML class names and counts before claiming coverage:

```sh
ANDROID_SERIAL='<tv emulator serial>' apps/ask-tv/gradlew -p apps/ask-tv connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.asktv.phoneauth.PhoneRelayTlsTest' --console=plain
ANDROID_SERIAL='<phone emulator serial>' apps/ask-tv/gradlew -p apps/ask-tv connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.asktv.PhoneAuthLifecycleTest' --console=plain
ANDROID_SERIAL='<phone emulator serial>' apps/ask-tv/gradlew -p apps/ask-tv connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.asktv.PhonePairingUiTest' --console=plain
ANDROID_SERIAL='<tv emulator serial>' apps/ask-tv/gradlew -p apps/ask-tv connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.asktv.PhoneSignInNavigationTest' --console=plain
ANDROID_SERIAL='<tv emulator serial>' apps/ask-tv/gradlew -p apps/ask-tv connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.asktv.AskTvNavigationTest' --console=plain
```

- [ ] Install the same release artifact without data clearing on the authorized TV emulator and phone target. Record its SHA-256 and installed package/version. Do not invoke instrumented tests against the user's live authenticated installation.

```sh
TV_SERIAL='<tv emulator serial>' tools/install-ask-tv.sh
adb -s '<phone test serial>' install -r apps/ask-tv/build/outputs/apk/release/ask-tv-release.apk
adb -s '<phone test serial>' shell am start -W -n com.amine.asktv/.PhoneAuthActivity
shasum -a 256 apps/ask-tv/build/outputs/apk/release/ask-tv-release.apk
```

- [ ] Preserve existing TV data, issued registration, Home role, stock Home, Cast credentials, and all other apps. Inspect optional-feature declarations and the APK for unexpected native libraries.
- [ ] Confirm the user's phone OS and reachable network when the live phone test starts. Use actual phone/browser consent for AI Chat's own registration. Do not replace this with a synthetic success or an existing app's credentials.
- [ ] Record live callback, validated account commit, granted subscription permission, model listing, and a completed Responses request as separate results. Keep account/QR captures private and out of Git.
- [ ] If the live phone/network is unavailable, report that limit and deliver validated emulator work without claiming phone-only login success. Keep Mac diagnostics available.
- [ ] Fetch `origin` before delivery, integrate concurrent changes without force push, repeat affected checks, commit only the app and related documents, and merge the validated branch into `origin/main` under repository instructions.

## Self-review

The five review conditions above each have named tests in their owning task.
The plan covers the spec's QR, URL validation, trust, HTTP bounds, replay, expiry, foreground lifecycle, data preservation, and separate live evidence.
The phone helper is Android 14+ because the existing APK's source floor remains unchanged.
No app code, credential action, build, installation, or live test is established by these planning documents.
