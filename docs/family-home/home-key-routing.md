# Home key routing on Chromecast

Keep Google TV Home (`com.google.android.apps.tv.launcherx`) and `com.google.android.tungsten.setupwraith` enabled.
The setup package supplies Cast device credentials on the tested Chromecast firmware.
This change updates Home key handling in Family Home. It does not remove Google packages.

## Observed behavior

The baseline source is `b0298413ed3bda9f43b8f14a103c8a211eed1f55`.
On 2026-10-29, the device assigned the Home role to Family Home.
The Home intent resolver still selected Google TV Home.
The role identifies the selected Home app. The resolver chooses an activity for a Home intent.

In the baseline, `FamilyAccessService.onKeyEvent()` opens Family Home on a short Home release, then returns `false`.
Android can therefore process the same key after the helper opens Family Home.
Device logs show Google TV Home opening, followed by the helper's redirect to Family Home.
This sequence explains the reported stock Home flash.

## Intended key behavior

When Family Home holds the Home role, its helper consumes both Home press and release events.
A short press opens Family Home once. A held press opens Android Settings for the parent profile.
For other viewers, a held press opens Family Home.
The helper leaves unrelated keys with Android. If Family Home does not hold the Home role, the helper does not consume Home.

Android documents that returning `true` from `onKeyEvent()` consumes an event.
The press and release events must remain consistent.
See the [Android accessibility key API](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#onKeyEvent(android.view.KeyEvent)).

## Developer access and package changes

Developer options allow authorized ADB debugging. They do not grant root access, which gives administrative system privileges.
The observed device uses a production `user` build with `ro.debuggable=0`.
Its ADB shell runs as user ID `2000`, not root.
See the [ADB guide](https://developer.android.com/tools/adb) and [AOSP root documentation](https://android.googlesource.com/platform/packages/modules/adb/+/HEAD/docs/dev/root.md).

Package operations have different effects:

- `pm disable-user --user 0` disables a package for that Android user. It retains the APK and app data.
- `pm uninstall --user 0` removes a system app for that user. It can delete user data, but retains the firmware copy.
- Deleting firmware files requires separate system access. Developer options do not provide that access.

See [AOSP system app deletion handling](https://android.googlesource.com/platform/frameworks/base/+/3ac6aa1e4daeb646bdb40813e988d1013d72150c/services/core/java/com/android/server/pm/DeletePackageHelper.java).
Firmware storage, writable app storage, RAM, and CPU use are separate measurements.
Removing a package does not establish a general speed gain or a fixed RAM saving.
The resource snapshot records one device state. It does not measure an improvement.

Do not apply an old package list without reviewing it.
Preserve both Google packages. Test key handling before considering a separate package or component experiment.
No package removal or component disable is part of this change.

## Evidence and validation

Keep the current device backup local in the ignored `backup/` directory.
Do not commit APK backups, logs, device identifiers, account details, or family screenshots.
The private baseline evidence is in `backup/home-handoff-20261029/` in the coordinator checkout.

The tested working tree uses base `643f4aec9e6e1c124abbb9b452537880aec0e07d` plus the Home key changes.
The change was then rebased onto `b7c663b9791d52e248dda4419109fe4c23c1d0b4` with the same Family Home source and tests.
Family Home is version `1.9`, version code `10`.
The SHA-256 hash of `FamilyAccessService.kt` is `65cb7623f5dd6b4c1d83d989812730debacae796ebec9fff340ad17cd6022928`.
The release APK hash is `79a3b3dd76bbb5bc13fba23e5439d6caa71fd941965535db164e567656c7ba9e`.

The focused regression suite reproduced the baseline problem with five failures in nine tests.
After the change, all nine focused tests passed.
The full integration run passed 73 debug unit tests, 73 release unit tests, and 62 emulator tests across nine classes.
The emulator run includes the nine Home key tests. Lint and release packaging passed.
All 16 strict repository tests passed after integration. Independent code review found no blockers.

The coordinator used this environment and these commands from the repository root:

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export ANDROID_SERIAL=emulator-5554

apps/family-home/gradlew -p apps/family-home connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.amine.familyhome.FamilyHomeKeyTest --console=plain
apps/family-home/gradlew -p apps/family-home testDebugUnitTest testReleaseUnitTest lintRelease assembleRelease connectedDebugAndroidTest --console=plain
CLEANYOURTV_STRICT=1 python3 -m unittest discover -s tests -v
```

The strict repository run includes `tests/test_examples.py`.
Its list format test reads active package entries and ignores comments.
It does not establish that a package is safe to disable or that Cast works.

The coordinator installed version `1.9`, code `10`, on the Chromecast with the official installer.
The installed APK hash matched the release APK hash above.
The device address is omitted from this example:

```sh
TV_SERIAL='<connected address>' tools/install-family-home.sh
```

Five ADB-injected Home presses still opened Google TV Home five times and Family Home nine times.
This result does not establish the behavior of the actual remote.
In Android 14, `notifyKey()` sends hardware events through the enabled input filter.
This filter delivers eligible keys to accessibility services before normal handling.
`injectInputEvent()` queues injected keys without that filter call.
See [Android 14 input dispatch](https://android.googlesource.com/platform/frameworks/native/+/refs/heads/android14-release/services/inputflinger/dispatcher/InputDispatcher.cpp).

An emulator experiment with raw `EV_KEY` events opened neither Home app. It provides no navigation evidence.
The production shell denied raw device input. No root access or device permission change was attempted.
The actual remote's Home flash remains unverified. Physical Cast, wake, and reboot were not tested in this task.
The software checks passed, but they do not establish these physical results or a general speed improvement.
