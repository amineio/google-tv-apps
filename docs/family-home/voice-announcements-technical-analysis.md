# Family Home spoken timer announcements: feasibility analysis

Analysis baseline: `feff842a26103e6543dc0b9794f8168a93ba533a`, 2026-10-29. This is not an approved design or implementation plan. No feature code, assets, builds, installations, or device tests form part of this analysis. Spoken language remains unresolved.

## Current behavior

Family Home shows five-minute and one-minute warnings without speech. Sessions store total time, remaining balance, elapsed-time deadline, and two warning flags. The controller saves a flag before emitting `Warning(text)`. The helper displays a five-second, non-focusable overlay. Sources: [session fields](../../apps/family-home/src/main/kotlin/com/amine/familyhome/FamilyState.kt#L45-L52), [warning decisions](../../apps/family-home/src/main/kotlin/com/amine/familyhome/FamilyController.kt#L255-L280), and [visual output](../../apps/family-home/src/main/kotlin/com/amine/familyhome/FamilyAccessService.kt#L150-L163).

The helper pulses about once per second while the screen is interactive and Family Home holds the Home role. Returning from another app does not create or resume a session. Sources: [helper loop](../../apps/family-home/src/main/kotlin/com/amine/familyhome/FamilyAccessService.kt#L51-L78) and [activity resume](../../apps/family-home/src/main/kotlin/com/amine/familyhome/MainActivity.kt#L57-L61).

Standby and profile exit save the balance and clear the selected profile. Wake requests media pause and opens the chooser. Selecting a child resumes its saved balance. A new session replaces that child's balance and resets warning flags. At expiry, the controller first saves zero remaining time, then requests media pause and opens Home. Sources: [session transitions](../../apps/family-home/src/main/kotlin/com/amine/familyhome/SessionEngine.kt#L23-L64) and [controller transitions](../../apps/family-home/src/main/kotlin/com/amine/familyhome/FamilyController.kt#L144-L195).

Local DataStore stores state. Recovery clears deadlines and pauses sessions. Ten-second checkpoints mean interruption can lose up to ten seconds of measured use. The manifest disables backup and declares no network permission. Sources: [repository](../../apps/family-home/src/main/kotlin/com/amine/familyhome/FamilyApplication.kt#L18-L39), [checkpoint rules](../../apps/family-home/src/main/kotlin/com/amine/familyhome/FamilyController.kt#L255-L280), and [manifest](../../apps/family-home/src/main/AndroidManifest.xml#L1-L21).

## Audio options

Battleship has no speech model. Its battle cues combine CC0 recordings with local synthesis. Its music uses CC0 recordings edited with FFmpeg and encoded with `oggenc`, without an AI model or external runtime. Sources: [battle audio record](../battleship/sprite-polish/audio-assets.json#L1-L6), [music record](../battleship/music-polish/assets.json#L1-L7), and [preparation script](../../apps/battleship/tools/prepare_music.py#L34-L49).

Two approaches merit evaluation. Android offline text-to-speech can construct names and remaining-time phrases from local profile data. Fixed bundled voice clips give consistent delivery. Arbitrary new names require a generic phrase, a local synthesis fallback, or privately provisioned named clips. A clip library requires a defined number vocabulary and rounding rule.

TTS needs a manifest query for `android.intent.action.TTS_SERVICE`. Select the required locale and an installed voice with `isNetworkConnectionRequired == false`. Exclude `notInstalled` and check voice selection results. Do not silently choose a network voice or download during an announcement. Missing data means a visual warning and setup status. Optional setup downloads remain an open choice. The launcher's network permission alone does not constrain an external engine. Device engine and voice availability were not inspected. Sources: [TTS API](https://developer.android.com/reference/android/speech/tts/TextToSpeech), [voice network requirement](https://developer.android.com/reference/android/speech/tts/Voice#isNetworkConnectionRequired()), and [missing data](https://developer.android.com/reference/android/speech/tts/TextToSpeech.Engine#KEY_FEATURE_NOT_INSTALLED).

No audio-generation connector is available. Local tools include `say`, FFmpeg, `ffprobe`, `afconvert`, and `uv`. No speech-model package was found in the inspected default Python environment or matching model cache. Other isolated environments were not inventoried. Availability does not establish voice quality or redistribution rights. Do not base the decision on an uninstalled model.

## Background audio constraint

Family Home targets API 37 and supports API 34 onward. On Android 15 and later, an app targeting API 35 or later cannot request audio focus unless it is the top app or runs a foreground service. Foreground launcher speech and background warnings therefore have different feasibility limits. The existing accessibility helper is not an audio foreground service and does not automatically grant focus. Bundled clips and TTS both need audio coordination. Source: [Android audio focus](https://developer.android.com/media/optimize/audio-focus).

Evaluate transient focus with speech content attributes. Release focus after completion, cancellation, or failure. Denied focus means visual warnings only. Other apps can react differently to ducking, which temporarily lowers volume. TTS audio attributes alone do not establish focus ownership. A media-playback foreground service requires its type, permissions, and notification. Background starts have separate restrictions. Sources: [audio focus](https://developer.android.com/media/optimize/audio-focus), [TTS API](https://developer.android.com/reference/android/speech/tts/TextToSpeech), [service type](https://developer.android.com/develop/background-work/services/fgs/service-types#media), and [background restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start).

AOSP accessibility binding includes `BIND_FOREGROUND_SERVICE_WHILE_AWAKE` and `BIND_ALLOW_BACKGROUND_ACTIVITY_STARTS`. This suggests a possible service route, not guaranteed permission on every TV. An API 35-or-later emulator prototype is a future feasibility test. It is not a requirement for this analysis or a hardware gate. Do not bypass denied focus. Source: [AOSP accessibility binding](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/services/accessibility/java/com/android/server/accessibility/AccessibilityServiceConnection.java).

## Proposed event boundaries

Keep timer decisions in `FamilyController`. Give speech output a separate owner tied to the helper lifecycle. Do not tie announcements to recomposition, app return, or each pulse. A typed event can carry profile identity and purpose.

Candidate utterances are a successful session start or resume, the five-minute warning, the existing one-minute warning, and expiry. These are proposed choices. Start, resume, and expiry currently emit no spoken event. Use generic examples in shared material: “Robin, you have 20 minutes left,” “Five minutes left,” “One minute left,” and “Your time is up.” Language, naming, tone, mute controls, and which events are enabled require a product decision.

Before playback, recheck the selected child, current session, remaining time, screen state, helper connection, and Home role. Cancel pending and active speech on profile exit, standby, session replacement, helper loss, or service destruction. Expiry must take priority over a queued warning. Initialization, synthesis, focus requests, and player failures must not block the controller mutex or interrupt expiry. Keep the visual warning when speech is unavailable.

## Failure modes and persistence

A five-minute session intentionally skips the five-minute warning. If a delayed pulse crosses both thresholds, the controller marks both flags and emits only the one-minute warning. If its first pulse below five minutes sees 4:20 remaining, it still says “5 minutes.” Delayed speech can make that wording less accurate. Define whether to announce a threshold, “less than five minutes,” or the current rounded balance.

The warning flags record an attempted effect, not heard delivery. A crash after saving a flag can lose the announcement. The single callback can also be absent, replaced, or fail. Marking delivery only after playback risks duplicate speech after interruption. Recommended policy: preserve timer state and avoid replaying old threshold warnings automatically. A later explicit resume announcement can report the current balance. This policy remains proposed.

Use a session identity or cancellation generation to reject stale asynchronous work, including a replacement session for the same child. The general state revision also changes for unrelated configuration, so it is not a complete speech identity. Never put actual child names, named audio, synthesis requests, or personal captures in Git. Keep names and any private cache local. Runtime speech must retain the app's offline privacy boundary.

## Proposed validation and open questions

Existing tests cover pause, recovery, expiry, and a warning for a generic dynamic profile. They do not establish speech output. Sources: [controller tests](../../apps/family-home/src/test/kotlin/com/amine/familyhome/FamilyControllerTest.kt#L81-L130) and [dynamic-profile test](../../apps/family-home/src/test/kotlin/com/amine/familyhome/FamilyProfileControllerTest.kt#L130-L157).

Proposed unit coverage includes exact thresholds, delayed pulses, five-minute sessions, duplicate suppression, successful-write ordering, failed storage, session replacement, and stale callbacks. Test speech initialization failure, denied focus, cancellation, and completion with a fake output adapter. Proposed emulator coverage includes helper connection, foreground and background output, standby, profile switching, activity recreation, process recovery, mute controls, and unchanged D-pad focus. Use generic profiles and inspect selected test classes and counts.

The user accepts emulator evidence for this work and does not require a physical-TV gate. Emulator results must still state their limits. Audible quality and behavior with specific streaming apps remain unverified until observed. No tests were run for this analysis.

Recommendation: evaluate offline Android TTS first for arbitrary local names after the language decision. Compare authored clips if pronunciation or quality is unsuitable. If background focus is unavailable, limit speech to the foreground launcher and keep background visual warnings. Language, voice, events, rounding, mute defaults, and interaction with other audio remain open.
