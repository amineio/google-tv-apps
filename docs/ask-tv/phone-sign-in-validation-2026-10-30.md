# AI Chat 0.3 phone sign-in validation

Initial record: 2026-10-30. Latest coordinator update: 2026-10-31.
Status: implementation, release checks, physical TV installation, and application Git delivery are complete.
The user deferred personal-phone helper installation and live QR authorization until later.
This deferral is not an implementation blocker or a passing result.
This record uses results supplied by the coordinator. The documentation agent ran no build, test, device, browser, network, or Git operation.
The delivered runtime is `27e6e29b11cd0eda0b5401f0cf2468cb16338224`.
Runtime `81cd4db1`, test-only baseline `57bd1b03`, and stable native TV source `a094ba94` are earlier checkpoints.
The task integrated freshly fetched `origin/main` at `1aa862ef` through merge `7321183e`.
Final debug/release unit suites, release lint, and APK packaging passed at `27e6e29b`.
The last runtime change adds the active guide countdown. Its focused native check and refreshed captures passed.
The full TV navigation and appearance baseline remains `81cd4db1` with test-only correction `57bd1b03`.
Physical installation is hash-verified. New physical visual, account-screen, microphone, and audio behavior remain unverified because the TV was asleep.
Earlier evidence belongs to its stated revision and scope.

## Release contract

Version `0.3`, version code `3`, uses package `com.amine.asktv` and one signed APK for TV and phone.
The minimum Android version is 14 (`minSdk 34`). Compile and target SDK levels are `37`.
The TV entry is `.MainActivity`. The phone entry is `.phoneauth.PhoneAuthActivity`.
TV, touchscreen, microphone, and camera features are optional.
The existing bundled icon, banner, launcher background, and font license are unchanged.
Release packaging, signature verification, and the APK inspection recorded below passed at `27e6e29b`.

The helper is installed once through the private Mac/ADB route.
The TV renders the active QR at 512 × 512 physical pixels.
The QR connects the installed helper to a temporary local relay. It does not install an APK.
The phone uses a photograph or native deep link, followed by a visible Continue action and the system browser.
There is no public installer website, public relay, APK upload, device-code grant, API-key fallback, or credential import.

The current native labels are **Use Mac helper**, **Allow plan access**, and **Phone setup guide**.
Built-in answers show **Built-in example · no live model used**.
The setup guide QR opens the app guide's `#install-the-phone-helper-once` section.
It is separate from the active pairing QR and does not install the helper.
The guide offers **Back to pairing**, **Back to sign-in choices**, or **Back to sign-in status**, according to the current sign-in route and status.
That return action updates after an external Mac transition while the guide remains open.
Opening it during pairing retains the same host and original deadline.
The active guide shows live **Pairing expires in m:ss** text from that original deadline.

The browser now says “Authorization received on this phone. Return to AI Chat to check delivery.”
That message confirms phone-local callback receipt only.
The helper's “Authorization sent” status follows the TV's callback receipt.
Verified account storage on the TV remains a separate result.

## Verified delivered release artifact at `27e6e29b`

The coordinator verified the release APK signature and inspected its metadata and libraries.
The APK is 2,580,521 bytes and uses package `com.amine.asktv`, label AI Chat, version `0.3`, and version code `3`.
Its minimum SDK is `34` and target SDK is `37`.
Its SHA-256 is:

```text
5b044dbc1bae51ac525ce77307abc70b4aa7001e5adbd2f26df6b77ba9e3df56
```

The APK includes AndroidX graphics-path native libraries for `arm64-v8a`, `armeabi-v7a`, `x86`, and `x86_64`.
The required Chromecast ARM32 library is present.
Camera, microphone, touchscreen, and leanback features are optional.
The manifest retains the icon, banner, and `com.amine.familyhome.HERO_IMAGE` metadata.
The earlier `81cd4db1` artifact had SHA-256 `29340a11af5e3e43bef1e126d444975a4a7b8244922bcc2e170369200d1a256b`.
That earlier artifact was installed on the dedicated TV emulator, with version `0.3`, code `3`, and a successful cold MainActivity start.
Its release Ready image and gold initial focus were inspected. That is emulator evidence for the earlier checkpoint.

## Physical TV installation and application delivery

The authorized physical installation used the immutable, verified `27e6e29b` artifact.
`adb install -r` succeeded and preserved app data.
The installed package reports version `0.3`, code `3`, minSdk `34`, and targetSdk `37`.
The coordinator pulled the installed `base.apk` privately. Its exact SHA-256 matches the delivered artifact above.
`RECORD_AUDIO` permission entries compare identically before and after installation.
The MainActivity start command completed.

The TV reported power state Asleep, no resumed activity, and a black screenshot.
No wake command was sent.
This establishes installation and package identity, not a visible physical 0.3 Ready screen or verified stored-account UI.
No new physical microphone or audio test was performed.
The historical 0.2 Mac sign-in and user-confirmed voice result below remain separate.

Before delivery, fresh `origin/main` was `1aa862ef` and its ancestor relationship was verified.
An atomic push updated the task branch and `origin/main` to application delivery commit `27e6e29b11cd0eda0b5401f0cf2468cb16338224`.
A fresh fetch confirmed exact equality of HEAD, `origin/main`, and `origin/amine/ai-chat-sign-in` at that commit.
This document is a follow-up to that application delivery commit.
No cloud build, APK upload, or public release was triggered.
The shared local `main` checkout and existing worktrees were preserved.

## Earlier installed 0.2 evidence

The [0.2 validation record](validation-2026-10-30-ai-chat.md) records the earlier release and its Mac authorization result.
That installed release completed its own Mac sign-in, identity verification, subscription permission, and server model listing.
The user reported a successful physical remote question and spoken answer on 0.2.
The coordinator saw Listening and the microphone indicator but did not hear the answer.
These results do not establish 0.3 phone authorization, 0.3 physical microphone routing, or 0.3 audible quality.

## Focused implementation evidence

The coordinator inspected test XML names and counts for the selections below.
All passing selections in this table reported zero failures and zero errors.
Rows are separate gates. Do not add their counts as a single current full-suite result.

| Stage and revision | Supplied result | Evidence limit |
| --- | --- | --- |
| Protocol fix, `3d398a27` | Eight JVM classes, 52 tests passed | Strict links, callback validation, one-attempt behavior, replay, expiry, cancellation, and guarded completion |
| Transport, `317fee77` | `RelayHttpTest` 5 and `PhoneLoopbackServerTest` 4 passed | Local JVM transport and callback behavior |
| Native TLS, `317fee77` | `PhoneRelayTlsTest` 8 passed | Android Keystore, pinned sockets, deadlines, bounds, replay, cancellation, and expiry in an emulator |
| Snapshot producer, `5e3d0c15` | `PhoneRelayProtocolTest` 18 passed | Safe relay snapshots and protocol state behavior |
| Phone fix gates accepted at `f04b57c0` | `PhoneAuthSessionTest` 11 and `PhoneHelperMemoryTest` 3 passed | Phone session and memory ownership |
| Phone lifecycle covering gate, `044b5958`, accepted at `f04b57c0` | `PhoneAuthLifecycleTest` 9 passed on API 34 | Real activity/service, synthetic browser callback, return, notifications, and cleanup |
| Phone photo/UI, `f04b57c0` | `PhonePairingUiTest` 8 passed on API 34 | Genuine synthetic QR photographs, bounded decode, private-file deletion, visible confirmation, deep-link clearing, and rendered text contrast |
| TV coordination, `848b63bd` | `GrantCompletionTest` 11 and `PhoneSignInCoordinatorTest` 10 passed | Guarded account completion, route ownership, cancellation, and replacement |
| Initial TV sign-in navigation, `848b63bd` | 14 selected, 13 passed, 1 capture-edge assertion failed | The QR decoded. A one-pixel capture fringe failed the exact-white edge assertion |
| Corrected QR assertion, `848b63bd` | Exact rendered-QR method, 1 passed | Source bitmap and inner white margin checked. This was not a full 14-case rerun |
| Real TV main activity, `848b63bd` | `AskTvNavigationTest` 4 passed | Configuration return, typed input, actual keyboard Back behavior, and directional answer scrolling |
| TV fix round, `63808c11` | `GrantCompletionTest` 11 and `PhoneSignInCoordinatorTest` 10 passed | Focused 21-case JVM gate after the replacement and rendered-size fixes |
| Real Mac replacement, `63808c11` | `MacSignInReplacementTest` 1 passed | Exclusive real loopback port 1455, delayed old cleanup, and successful replacement |
| Rendered QR, `63808c11` | Exact rendered-QR method, 1 passed | 512 physical pixels, genuine decode, source margin, and interior white strips |
| Native capture workflows, `63808c11` | `TvFlowCaptureTest` 2 passed | Synthetic native capture workflows, separate from live authorization |
| Committed guide baseline, `0ea7e6e7` | Coordinator reported all 31 repository tests passed | Retained log: `task5-repository-tests.log`. This is not the final strict repository gate |
| Stable native TV source, `a094ba94` | `AskTvNavigationTest` 4/4, `TvInitialFocusAppearanceTest` 4/4, and `PhoneSignInNavigationTest` 15/15 passed on API 34 | Exact selected classes. All zero failures, errors, and skips |
| Native capture workflows, `a094ba94` | `TvFlowCaptureTest` 2/2 passed, 25 fresh native images | Zero failures, errors, and skips. Synthetic capture workflows, separate from live authorization |
| Full debug JVM suite, `a094ba94` | 18 classes, 143 tests passed | Zero failures, errors, and skips in XML. Includes `PhoneLoopbackServerTest` 5 |
| Full release JVM suite, `a094ba94` | 18 classes, 143 tests passed | Zero failures, errors, and skips in XML. Later lifecycle observer fix still requires final gates |
| Strict repository gate after merge `7321183e` | 31 tests passed | After integration of fresh `origin/main` at `1aa862ef` |
| Changed-file privacy and whitespace checks after integration | Zero changed files matched the personal-identifier scan. `git diff --check` passed | These checks do not establish final APK contents or live behavior |
| Initial full release lint before `4a173345` | Failed with one `RepeatOnLifecycleWrongUsage` error and 15 warnings | Corrected through proper `onCreate` ownership of the RESUMED observer, without suppression |
| Phone lifecycle full selection, `4a173345` | 9 selected, 8 passed, 1 initial `canOpenBrowser` startup timeout | Timeout occurred before the task-removal handler. Not a clean full nine-case run |
| Phone lifecycle isolated retry, unchanged `4a173345` | The same failed method passed, 1 test | Zero failures, errors, and skips. Exact 8+1 scope, not a full-class rerun |
| Final app gate, `81cd4db1` | Passed in 1 minute 9 seconds. Debug and release each selected 18 XML classes and 143 tests | Each suite reported zero failures, errors, and skips |
| Final lint and packaging, `81cd4db1` | Release lint: zero errors, 15 warnings. Debug, release, and AndroidTest APK packaging passed | Resolves the earlier lifecycle lint error. Warnings remain |
| Final phone photo/UI, `81cd4db1` | `PhonePairingUiTest` 8/8 passed on API 34 | Phone presentation and synthetic photograph checks, separate from personal-phone installation and live authorization |
| Final TV MainActivity, `81cd4db1` | `AskTvNavigationTest` 4/4 passed | Final selections and capture results are recorded in the following row |
| Final TV native selections, runtime `81cd4db1`, test-only `57bd1b03` | MainActivity 4/4, sign-in 17/17, appearance 4/4, and capture workflows 2/2 passed | Each selection reported zero failures, errors, and skips. Captures produced 27 fresh PNGs |
| Final repository delivery gate | 31 tests passed again in 8.808 seconds. `git diff --check` clean | Log: `final-repository-delivery-tests.log` |
| Dedicated TV emulator release installation | Version `0.3`, code `3`, cold MainActivity start OK | Release Ready image inspected with gold initial focus. No physical TV claim |
| Delivered runtime full app gate, `27e6e29b` | Debug and release each passed 18 classes and 143 tests | Zero failures, errors, and skips in each suite |
| Delivered runtime lint and packaging, `27e6e29b` | Lint zero errors and 15 warnings. All APKs built. Signature verified | Same package/version/SDK levels and native ABIs as recorded above |
| Active guide check and refreshed captures, `27e6e29b` | Exact native guide method 1/1 and capture workflows 2/2 passed, with 27 fresh images | Each selection reported zero failures, errors, and skips. Last runtime change only adds the guide countdown |
| Physical TV installation, `27e6e29b` | `install -r` succeeded, installed package metadata confirmed, installed APK hash matched exactly | Data preserved. TV asleep. No new physical visual, account-screen, voice, or audio result |
| Application Git delivery | Atomic task-branch and `origin/main` push succeeded at `27e6e29b11cd0eda0b5401f0cf2468cb16338224` | Fresh fetch confirmed exact SHA equality. Documentation follow-up is separate |

The API 37 local-network permission method was excluded before execution on API 34.
It was not tested and was not counted as skipped.
The earlier phone lifecycle gate at `044b5958` reported nine selected methods, zero failures, zero errors, and zero skips.
The earlier photo/UI gate at `f04b57c0` reported eight selected methods, zero failures, zero errors, and zero skips.
The current photo/UI eight-case rerun passed at `81cd4db1` as recorded above.
These results do not establish API 37 permission behavior, physical camera quality, network reachability, or browser consent with a real account.

The coordinator retained an earlier transient MainActivity timeout while waiting for the keyboard window.
An isolated one-case retry, a full four-case MainActivity run, and an actual keyboard capture then passed on unchanged source.
The later clean four-case result is recorded above. The first timeout is not discarded.
At `4a173345`, the phone lifecycle startup timeout preceded the task-removal handler.
The same method passed in isolation without source changes.
No concrete lifecycle source defect was established. The cause of that startup timeout remains unknown.

The final external-Mac guide test initially failed because it sent Back before the canceled state appeared.
Test-only correction `57bd1b03` waits for the observable canceled-guide message and asserts the current guide focus before sending actual Back.
The final retry-focus assertion remains unchanged.
The exact method then passed once, followed by the complete 17-case sign-in selection.
That test-only correction made no production change from `81cd4db1`, and its artifact hash remained unchanged at that checkpoint.
The later `27e6e29b` guide-countdown change produced the new delivered artifact hash recorded above.
The initial test-order failure remains recorded. It is not presented as a clean first-run pass.

The Mac replacement and rendered-size regressions each failed before their production fix.
Each initial selection contained one test and one failure.
Their passing results above cover the corresponding corrected behavior at `63808c11`.
The final full app gate covers delivered runtime `27e6e29b`.
The full TV navigation and appearance baseline remains runtime `81cd4db1` with test-only correction `57bd1b03`.
The affected active-guide method and refreshed capture workflows passed at `27e6e29b`.

The coordinator retains private logs and XML archives outside Git.
Relevant logs include `phone-cancel-race-green.log`, `phone-transport-jvm-green.log`, `phone-transport-native.log`, and `phone-fix-round1-jvm.log`.
Phone logs include `phone-lifecycle-round1-green.log` and `phone-pairing-ui-green.log`.
TV logs include `tv-sign-in-navigation.log`, `tv-qr-quiet-green.log`, `tv-main-navigation.log`, and `tv-round1-unit-green.log`.
The fix logs are `tv-mac-replacement-green.log` and `tv-qr-size-green.log`.
No personal capture, live QR, account name, device identifier, or credential is included here.

## Review and visual evidence

Scoped independent code reviews for phone Task 3 and TV Task 4 passed after their material fixes.
These reviews cover their stated source scopes. They do not replace final integrated checks or live authorization.
The snapshot addition also received a fresh scoped review with no findings.
The whole change through `4a173345` received a fresh independent source review with no open material source findings.
That review accepted the proper lifecycle observer fix for lint. Final lint and build reruns passed at `81cd4db1`.
Fresh whole-change review also accepted the final `81cd4db1` UI delta with no open material findings.

UX round 7 scored 5.5/10 for nine supplied native emulator images.
That score covers only those images and predates the current polish work.
UX round 8 scored 7.5/10 for 22 actual native images.
UX round 9 reviewed 25 actual native images and scored 8.5/10.
Three further UI fixes are implemented at `81cd4db1`.
The final capture selection passed two workflows and produced 27 fresh native images, including a settled-dialog capture.
UX round 10 reviewed 27 actual native images and scored 9.5/10, with no material blockers.
One minor guide-countdown issue was then fixed.
The same final round included bounded verification of two new actual guide/return captures and received a final scoped score of 10/10.
The 10/10 score covers that two-capture verification, not a new full concept iteration or whole-product acceptance.
Static images do not prove continuous countdown behavior.
These image reviews do not establish whole-product polish, state-transition reliability, live sign-in, or physical audio quality.
The stable native TV gates and 25-image capture result at `a094ba94` are complete as recorded above.
They do not establish acceptance of later UI changes.
The [concept record](phone-sign-in-concept.md) records ImageGen previews separately from native evidence.

## Final status and live-test limits

| Stage | Current status |
| --- | --- |
| Full debug and release unit suites at delivered runtime `27e6e29b` | Passed. Each suite: 18 XML classes, 143 tests, zero failures, errors, and skips |
| Final release lint and debug/release/test APK packaging at `27e6e29b` | Passed. Lint: zero errors and 15 warnings. Signature verified |
| Strict repository suite and whitespace check | Passed: 31 tests in 8.808 seconds and clean `git diff --check`, before this metadata-only documentation update |
| Final APK signature, metadata, optional features, and native-library inspection | Passed for the artifact recorded above. AndroidX graphics-path libraries include the required ARM32 ABI |
| UX round 10 | Full 27-image review: 9.5/10, no material blockers. Final bounded two-capture guide/return verification: scoped 10/10 |
| Final TV MainActivity at `81cd4db1` | Passed, four methods |
| Final TV sign-in navigation at runtime `81cd4db1`, test-only `57bd1b03` | Passed, 17 selected methods. Earlier test-order failure and correction recorded above |
| Final TV appearance at `81cd4db1` | Passed, four selected methods |
| Final native capture workflows at `81cd4db1` | Passed, two workflows and 27 fresh PNGs |
| Affected active-guide check at `27e6e29b` | Passed, exact one-method selection, zero failures, errors, and skips |
| Refreshed capture workflows at `27e6e29b` | Passed, two workflows and 27 images, zero failures, errors, and skips |
| Phone lifecycle at `4a173345` | Full selection: 8 passed, 1 startup timeout. Same method passed isolated unchanged. No clean nine-case rerun claimed |
| Current phone presentation/photo rerun at `81cd4db1` | Passed, eight selected cases on API 34 |
| Dedicated TV emulator release installation | Passed: version `0.3`, code `3`, cold MainActivity start OK, Ready image and initial focus inspected |
| Physical TV 0.3 installation and installed package/version/hash | Complete. Data preserved. Installed APK hash matches the verified artifact. TV asleep, so new visual/account-screen behavior is unverified |
| Personal-phone 0.3 helper installation from the same signed artifact | Deferred by the user until later. Not tested |
| Live QR acquisition and reachable phone-to-TV LAN | Deferred with the user's live phone test. Not tested |
| Real system-browser sign-in and callback | Deferred with the user's live phone test. Not tested |
| Verified account saved by the TV through the live 0.3 phone route | Unverified |
| ChatGPT plan permission through the live 0.3 phone route | Unverified |
| Live subscription model listing through the 0.3 phone route | Unverified |
| Completed live Responses request through the 0.3 phone route | Unverified |
| Physical 0.3 remote microphone and audible answer | Unverified |
| Application task-branch and `origin/main` delivery | Complete at `27e6e29b11cd0eda0b5401f0cf2468cb16338224`, confirmed by fresh fetch |

The user confirmed Android 14 or later for the phone.
The user chose to connect the personal Android phone later.
The deferral applies to personal-phone USB connection, helper installation, and live QR authorization.
Personal-phone installation and live QR authorization do not block completion of the remaining authorized implementation and release work.
The actual phone version and reachable network must still be recorded when the live test starts.
A phone receipt is distinct from the TV's verified account commit.
Account commit, plan permission, model listing, and a completed request must each receive separate live evidence.
Do not use another app's credentials or a synthetic result as evidence of live success.

## Reference commands

These commands are references for the recorded checks and installation procedures.
The status table above is the authority for completed, pending, and user-deferred work.
Replace every serial placeholder with the explicit authorized target.
Run instrumented tests only on dedicated test installations because the runner can uninstall the app.

```sh
apps/ask-tv/gradlew -p apps/ask-tv testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease assembleDebugAndroidTest --console=plain
CLEANYOURTV_STRICT=1 python3 -m unittest discover -s tests -v
git diff --check
```

Select each native class separately and retain its XML before the next class:

```sh
ANDROID_SERIAL='<TV emulator serial>' apps/ask-tv/gradlew -p apps/ask-tv connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.asktv.phoneauth.PhoneRelayTlsTest' --console=plain
ANDROID_SERIAL='<phone emulator serial>' apps/ask-tv/gradlew -p apps/ask-tv connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.asktv.phoneauth.PhoneAuthLifecycleTest' --console=plain
ANDROID_SERIAL='<phone emulator serial>' apps/ask-tv/gradlew -p apps/ask-tv connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.asktv.phoneauth.PhonePairingUiTest' --console=plain
ANDROID_SERIAL='<TV emulator serial>' apps/ask-tv/gradlew -p apps/ask-tv connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.asktv.PhoneSignInNavigationTest' --console=plain
ANDROID_SERIAL='<TV emulator serial>' apps/ask-tv/gradlew -p apps/ask-tv connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.asktv.AskTvNavigationTest' --console=plain
```

Install without clearing data and record the artifact hash for each target.
The personal-phone commands below are for the later user-deferred phone test:

```sh
TV_SERIAL='<authorized TV serial>' tools/install-ask-tv.sh
adb -s '<phone serial>' install -r apps/ask-tv/build/outputs/apk/release/ask-tv-release.apk
adb -s '<phone serial>' shell am start -W -n com.amine.asktv/.phoneauth.PhoneAuthActivity
shasum -a 256 apps/ask-tv/build/outputs/apk/release/ask-tv-release.apk
```

Preserve the existing TV registration, account data, Home role, stock Home, Cast credentials, and other apps.
The coordinator must fetch `origin` before integration and repeat affected checks after concurrent changes.
Installation and merge results must be reported separately.
If the phone or network is unavailable, record that limit and retain Mac diagnostics.
Physical installation is authorized for this task. Record its actual result separately from emulator and artifact checks.
