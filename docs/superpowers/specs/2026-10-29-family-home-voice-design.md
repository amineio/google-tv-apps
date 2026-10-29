# Family Home voice announcements

Date: 2026-10-29. The user selected offline Android text-to-speech and authorized implementation. The implementation details below apply that choice; they are not separate claims of user approval.

## Behavior

Family Home speaks complete sentences for a successful child session start, saved-session resume, five-minute warning, one-minute warning, and expiry. Names come from local profiles. Use the device language for Dutch, English, or French. For another language, use English only when an installed offline English voice is available. A supported language without an installed offline voice shows setup status and keeps visual warnings.

Voice announcements default to enabled. An authenticated Family configuration control saves the enabled state and stops speech when muted. Show clear initializing, ready, muted, or unavailable status. A standard Android speech settings link can open device setup when supported. Family Home must not start downloads, choose a network voice, add network permission, log names, or store synthesized named audio.

Start and resume report the remaining balance, rounded up to whole minutes; use a singular sentence for one minute. Threshold sentences describe “less than five minutes” or “less than one minute” when already below the threshold. At the exact threshold, use the exact minute value. Expiry says that the child's time is up. Translate the complete sentence, including its local name placeholder. Preserve the existing rule that a five-minute session skips its five-minute warning. A late pulse crossing both thresholds emits only the one-minute warning. Do not replay attempted warnings after recovery or app return.

## Ownership and cancellation

`FamilyController` decides when an announcement is due. Publish a typed event only after the corresponding repository write succeeds. Keep existing visual `Warning`, `PauseMedia`, and `OpenHome` effects. Use the real controller with a fake repository to verify write ordering and failed writes.

A separate speech owner handles initialization, locale and offline voice selection, focus, synthesis, completion, and cleanup. It must not hold the controller mutex while waiting for Android callbacks. Each request has a session identity and cancellation generation. Recheck current profile, session, mute state, helper connection, Home role, and interactive state immediately before output. Standby, profile exit/change, session replacement, helper loss, mute, recovery, and teardown invalidate pending work. Expiry cancels old warnings and can speak while the matching zero-time expiry screen remains active. Unrelated settings changes must not invalidate a valid session announcement.

## Background speech

Warnings must work while another app is open when Android permits the required service and audio focus. On Android 15 and later, this target-37 app needs a lawful foreground route for background audio focus. Use a short-lived `mediaPlayback` foreground service, required permissions, a notification channel, and a notification. Start it from the enabled accessibility helper and promote it before asynchronous initialization. The source review supports this route, including target-37 while-in-use checks; newer-OS runtime behavior remains to be tested. Do not bypass a denied start or focus request.

Request transient focus with speech attributes. Cancel on focus loss. Release focus and stop the foreground service after completion, cancellation, or failure. Service start, initialization, and synthesis failures leave visual warnings and expiry behavior working. Do not keep the service alive between announcements. Physical streaming-app ducking and audible quality require observation and are not inferred from callback success.

## Boundaries and delivery

Keep duration selection, saved balances, profiles, PIN protection, usage history, and visual warnings intact. Ship Family Home version `1.10`, version code `11`. Use existing Kotlin/Compose and Android APIs without a new dependency, speech model, or cloud service. Small native settings changes do not require ImageGen. Use generic test names and keep new personal data and captures out of Git.

The coordinator owns builds, device input, integration, and Git. Workers own implementation and documentation edits. Validate focused tests, then relevant full unit suites, lint, release/debug packaging, Android test build, emulator behavior, and fresh independent review. Use explicit emulator serials. Check Android 15+ service/focus restrictions as well as Android 14 compatibility when available; report any missing runtime coverage. No physical TV or cloud build is requested. After successful emulator installation, commit, push, and merge to `origin/main` in this task.

Concurrent main commit `95c590d1` used version 1.9 / code 10 for the Home key correction. The combined release uses 1.10 / code 11 and supersedes the provisional speech release version. Preserve the merged Home key behavior unchanged.

Platform references: [audio focus](https://developer.android.com/media/optimize/audio-focus), [foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types#media), [Android 17 background audio](https://developer.android.com/about/versions/17/changes/bg-audio), [AOSP accessibility binding](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/services/accessibility/java/com/android/server/accessibility/AccessibilityServiceConnection.java), and [AOSP service eligibility](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/services/core/java/com/android/server/am/ActiveServices.java).
