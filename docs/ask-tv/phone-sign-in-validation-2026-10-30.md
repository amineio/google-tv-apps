# AI Chat 0.3 phone sign-in validation

Date: 2026-10-30. Status: focused checks passed. Integrated release checks, live phone authorization, installation, and delivery remain pending.
This record uses results supplied by the coordinator. The documentation agent ran no build, test, device, browser, network, or Git operation.
The current implementation checkpoint is `63808c11`. Earlier evidence below belongs to its stated revision and scope.

## Release contract

Version `0.3`, version code `3`, uses package `com.amine.asktv` and one signed APK for TV and phone.
The minimum Android version is 14 (`minSdk 34`). Compile and target SDK levels are `37`.
The TV entry is `.MainActivity`. The phone entry is `.phoneauth.PhoneAuthActivity`.
TV, touchscreen, microphone, and camera features are optional.
The existing bundled icon, banner, launcher background, and font license are unchanged.
Release packaging and inspection of the final APK remain pending.

The helper is installed once through the private Mac/ADB route.
The TV renders the active QR at 512 × 512 physical pixels.
The QR connects the installed helper to a temporary local relay. It does not install an APK.
The phone uses a photograph or native deep link, followed by a visible Continue action and the system browser.
There is no public installer website, public relay, APK upload, device-code grant, API-key fallback, or credential import.

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

The API 37 local-network permission method was excluded before execution on API 34.
It was not tested and was not counted as skipped.
The phone lifecycle gate reported nine selected methods, zero failures, zero errors, and zero skips.
The photo/UI gate reported eight selected methods, zero failures, zero errors, and zero skips.
These results do not establish API 37 permission behavior, physical camera quality, network reachability, or browser consent with a real account.

The Mac replacement and rendered-size regressions each failed before their production fix.
Each initial selection contained one test and one failure.
Their passing results above cover the corresponding corrected behavior at `63808c11`.
The final integrated gate must still cover the completed branch.

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

UX round 7 scored 5.5/10 for nine supplied native emulator images.
That score covers only those images and predates the current polish work.
UX round 8 is reviewing 22 actual native images. Its result is pending.
Neither review establishes whole-product polish, state-transition reliability, live sign-in, or physical audio quality.
The [concept record](phone-sign-in-concept.md) records ImageGen previews separately from native evidence.

## Pending integration, installation, and live checks

| Stage | Current status |
| --- | --- |
| Full debug and release unit suites for the final revision | Pending |
| Release lint and debug/release/test APK packaging | Pending |
| Strict repository suite and whitespace check | Pending |
| Final APK metadata, optional features, and unexpected native-library inspection | Pending |
| Fresh UX round 8 result | Pending |
| Physical TV 0.3 installation, package/version, and APK hash | Pending |
| Phone 0.3 installation from the same signed artifact | Pending |
| Live QR acquisition and reachable phone-to-TV LAN | Pending |
| Real system-browser sign-in and callback | Pending |
| Verified account saved by the TV | Pending |
| ChatGPT plan permission | Pending for the live 0.3 route |
| Live subscription model listing | Pending for the live 0.3 route |
| Completed live Responses request | Pending for the live 0.3 route |
| Physical 0.3 remote microphone and audible answer | Unverified |
| Branch push and merge into `origin/main` | Pending |

The user confirmed Android 14 or later for the phone.
The actual phone version and reachable network must still be recorded when the live test starts.
A phone receipt is distinct from the TV's verified account commit.
Account commit, plan permission, model listing, and a completed request must each receive separate live evidence.
Do not use another app's credentials or a synthetic result as evidence of live success.

## Coordinator commands for the remaining gates

These commands describe pending work. Their presence here does not claim that they passed.
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

Install without clearing data and record the same artifact hash for both targets:

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
Validated emulator delivery does not require a physical TV test unless the user requests one.
