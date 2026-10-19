# Viewing limits implementation plan

> For agentic workers: Use superpowers:executing-plans. Complete each task and its tests before device delivery.

Goal: Add the approved timer, daily bedtime, five-minute warning, and stop screen to TV Clock.

Architecture: A small Java state machine owns deadlines and the single extension. Shared preferences persist its state. Android alarms schedule events independently of clock visibility. Native Android views provide the interface.

Tech Stack: Java 17, Android SDK 34, native views, AlarmManager, JUnit. No new runtime libraries.

Spec: designs/viewing-limits/concept-v1.json and the user's approval in this chat.

## Global constraints

Keep com.gathin.tvhome as Home. Preserve clock appearance preferences. Use a dark, spacious interface with one clear focus indicator. Do not add accessibility access, playback monitoring, network access, or a device lock. Never wake the display. Bedtime starts disabled until the user sets it.

## Review focus

Clock-off state must not cancel limits. Simultaneous deadlines must show one stop screen. Extensions must not move another deadline. Dismissed events must not reopen. Reboot, time changes, and standby must not produce stale morning interruptions.

## Task 1: Deadline state machine

Files: TimeLimits.java, TimeLimitsTest.java.
Interface: startTimer, cancelTimer, setBedtime, next, evaluate, canExtend, extend, dismiss.

- [x] Write failing tests for elapsed timing, overlap, one extension, DST, and stale events.
- [x] Implement a pure Java state machine and run its tests.

## Task 2: Android scheduling and presentation

Files: LimitsStore.java, LimitsController.java, LimitsReceiver.java, ClockService.java, BootReceiver.java, StopActivity.java, AndroidManifest.xml.
Interface: Controller operations load, change, save, and reschedule state atomically. Alarm receivers evaluate persisted state. The existing service presents brief warnings. StopActivity validates an event token before actions.

- [x] Add device tests for real alarms, clock-hidden warnings, background stop presentation, confirmation, and dismissal.
- [x] Add exact alarm access with a visible inactive state if access is missing.
- [x] Keep one lightweight foreground service while clock or limits are active. Avoid continuous polling for limits.
- [x] Test warnings without focus capture and one extension on the TV. Test standby suppression in the deadline unit tests.

## Task 3: Clean TV controls and delivery

Files: MainActivity.java, ClockSettingsActivity.java, TvUi.java, test instrumentation, README.md, install-clock.sh.

- [x] Keep appearance controls on a separate page. Add timer presets, bedtime, and the end action to the main page.
- [x] Use remote-friendly time editing and immediate saved configuration.
- [x] Build, lint, run unit and viewing-limit device tests, and obtain independent review.
- [x] Save the prior APK and device state. Install the tested update and inspect screenshots.
- [x] Leave the clock on, timer stopped, and bedtime disabled. Report limits and evidence.

## Delivery status, 2026-10-19

The release build and lint pass. Clock unit tests pass: 14 tests.
The existing launcher unit tests pass: 42 tests. Repository tests pass: five tests, one skip.
All five viewing-limit device tests pass against the installed non-debuggable 1.2 release.
The tests cover real alarms, background presentation, warning focus, one confirmed extension, stale confirmation, bedtime scheduling, and remote time editing.

Independent code review found no remaining blockers after the timing and confirmation fixes.
The design sub-agent reviewed actual TV screenshots. The selected preset, confirmation, and bedtime editor now include its fixes.
The final screenshot review found no remaining visual blockers.
The warning remains readable over Home. Readability over every video background remains untested.

The original home, disabled packages, and accessibility service configuration match the saved device state.
The prior APK, final APK, screenshots, review, and logs remain in the external backup folder.
Both original clock regression tests pass on the release. The clock remains on at its saved Top position and size 20.
The timer is stopped and bedtime is off. The test app is removed.
