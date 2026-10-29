# Family Home Voice Announcements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add offline spoken child-session announcements with persistent mute and safe background output.

**Architecture:** The controller emits typed events after durable writes. A separate Android speech owner checks session validity, selects an installed offline voice, and owns focus and short-lived foreground service output. Existing timer, visual warning, and expiry effects remain authoritative.

**Tech Stack:** Existing Kotlin, Compose for TV, DataStore, coroutines, Android TextToSpeech, AudioManager, and mediaPlayback foreground service; no new dependency.

**Spec:** `docs/superpowers/specs/2026-10-29-family-home-voice-design.md`

## Global Constraints

- Family Home version `1.10`, version code `11`; min SDK `34`, target SDK `37`.
- Dutch, English, and French follow device language; unsupported languages can use installed offline English only.
- No network permission, network voice, silent downloads, cloud service, or named-audio persistence.
- Preserve duration, balances, profiles, PIN, usage, visual warnings, and helper/Home-role requirements.
- Coordinator owns Git, builds, and device input. Workers own all edits, with one owner per shared file.
- No physical TV, cloud build, APK upload, public release, or personal test data.
- User authorization already covers implementation; no repeated approval gate.

Concurrent main commit `95c590d1` used 1.9 / code 10 for the Home key correction. The combined release uses 1.10 / code 11, superseding the provisional speech version. Keep the merged Home key source and tests unchanged.

## Review Focus

1. Delayed TTS/focus callbacks after standby, profile switch, mute, helper loss, or same-child replacement must never speak stale text.
2. Storage failure must prevent speech events and preserve the existing failure/expiry safety behavior.
3. Missing, network-only, or mismatched voices must remain silent with clear status; unsupported locale fallback must remain offline.
4. Background service/focus denial and focus loss must clean up without disrupting timers or visual warnings.
5. Threshold overlap, recovery, and expiry must avoid duplicates while permitting one expiry sentence at zero time.

---

### Task 1: Deliver durable session announcements and native voice controls

**Files** (all paths below use `apps/family-home/`):
- Modify: `src/main/kotlin/com/amine/familyhome/FamilyController.kt`, `FamilyState.kt`, `FamilyStateCodec.kt` for durable preference and event ordering.
- Modify: `src/main/kotlin/com/amine/familyhome/FamilyAccessService.kt`, `FamilyApplication.kt`, `MainActivity.kt`, `SettingsScreen.kt` for lifecycle, status, and native settings wiring.
- Create: `src/main/kotlin/com/amine/familyhome/VoiceAnnouncements.kt` for typed requests, phrase/voice policy, and cancellable output contract.
- Create: `src/main/kotlin/com/amine/familyhome/FamilySpeechService.kt` for Android TTS, foreground service, focus, and cleanup.
- Modify: `src/main/AndroidManifest.xml`, `build.gradle.kts`, `README.md` for queries, service declarations, version, and usage/limits.
- Test: `src/test/kotlin/com/amine/familyhome/VoiceAnnouncementsTest.kt`, `FamilyControllerTest.kt`, `FamilyStateCodecTest.kt`.
- Test: `src/androidTest/kotlin/com/amine/familyhome/VoiceAnnouncementsPresentationTest.kt`, `SettingsPresentationTest.kt`; add focused Android output/service tests where the emulator supports them.
- Record: `docs/family-home/voice-announcements-validation.md` at repository root.

**Interfaces:**
- Keep `FamilyRepository.load(): FamilyState` and `save(state: FamilyState)` unchanged.
- Add a persisted mute preference defaulting to `false` and an authenticated suspend controller command to change it.
- Preserve `FamilyEffect.Warning(text: String)`, `PauseMedia`, and `OpenHome`. A separate typed voice sink carries profile, runtime session generation, purpose, and remaining time; clearing it cancels pending output.
- Define `VoiceAnnouncement` and `SpeechOutput` in `VoiceAnnouncements.kt`; the output contract accepts a request, cancels pending/current work, and closes resources. Keep Android callback work outside controller locking.
- Use a session identity plus output generation, not global `FamilyState.revision`, for stale-request rejection. Final internal signatures can follow implementation boundaries without changing this contract.

- [ ] **Step 1: Add focused failing tests.** Exercise the real controller with fake storage and a fake speech output. Pin successful-write-before-event ordering; zero speech after failed save; persisted mute and legacy default; start/resume once; no event on app return; exact/late thresholds; five-minute-session suppression; expiry once. Pin delayed callbacks after each cancellation reason, including same-child replacement and helper loss during a suspended save. Verify expiry remains eligible only for its matching zero-time session. Test English/Dutch/French complete sentences, minute rounding, installed-offline selection, missing supported language, and unsupported-locale English fallback.

- [ ] **Step 2: Coordinator runs the focused tests and records the expected failures.**

```sh
apps/family-home/gradlew -p apps/family-home testDebugUnitTest --tests 'com.amine.familyhome.VoiceAnnouncementsTest' --tests 'com.amine.familyhome.FamilyControllerTest' --tests 'com.amine.familyhome.FamilyStateCodecTest' --console=plain
```

- [ ] **Step 3: Implement the contract and Android adapter.** Save state before publishing events; retain visual effects independently of speech. Bound pending work and invalidate old callback generations. Select only installed non-network voices and check Android return codes. Start the nonexported mediaPlayback service from the enabled helper. Add FOREGROUND_SERVICE and FOREGROUND_SERVICE_MEDIA_PLAYBACK permissions and the TTS_SERVICE query. Promote it promptly with a channel/notification before asynchronous initialization. Add transient speech focus, loss/timeout handling, and complete cleanup. Catch denied starts/focus without retries that replay obsolete warnings. Add default-enabled native settings, availability status, and guarded Android speech setup. Update version and app guide.

- [ ] **Step 4: Coordinator runs focused tests, then the integration checks.** All selected tests must pass; inspect XML class names and counts. Include persisted-state size-limit regressions and existing controller/profile/usage suites in the full run.

```sh
apps/family-home/gradlew -p apps/family-home testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease assembleDebugAndroidTest --console=plain
```

- [ ] **Step 5: Coordinator validates the emulator serially.** Set `ANDROID_SERIAL` to the inspected emulator and pass `adb -s "$ANDROID_SERIAL"` for every command. Run one selected instrumentation class per invocation, then inspect its XML. Use actual Up/Down/OK/Back input to test focus, scrolling, mute persistence, setup return, and unchanged profile navigation. Inspect manifest/service/notification behavior and foreground/background output with installed and unavailable voices where available. Exercise cancellation and focus denial/loss. Check the Android 15+ route on an eligible emulator when available; record unavailable coverage explicitly. Install the final release and take fresh generic captures.

```sh
ANDROID_SERIAL="$ANDROID_SERIAL" apps/family-home/gradlew -p apps/family-home connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.amine.familyhome.VoiceAnnouncementsPresentationTest --console=plain
adb -s "$ANDROID_SERIAL" install -r apps/family-home/build/outputs/apk/release/family-home-release.apk
```

- [ ] **Step 6: Obtain fresh independent review and fix material findings.** Review the complete change against the five risks and spec. The reviewer does not own implementation or run competing builds/device sessions. Record tested revision, commands, counts, runtime observations, and audible/physical-TV limits. Repeat only checks affected by any fixes.

- [ ] **Step 7: Coordinator commits, pushes, and merges into `origin/main`.** Preserve unrelated changes and other worktrees. Stage only this feature and related instructions. Verify the merged revision reaches `origin/main`; report installation and merge separately. Use `gh --repo amineio/google-tv-apps` if needed. Do not enable cloud workflows.
