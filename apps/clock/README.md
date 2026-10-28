# TV Clock

TV Clock shows white `HH:mm` text in the top-right corner over other apps.
The background is transparent. The clock uses the TV time and time zone.
Open TV Clock from the app grid. Select Clock appearance to turn Show clock on or off.
The clock remembers this choice and starts after boot when enabled.
Focus Position, then use Left or Right to move the clock along the right edge.
Focus Text size, then use Left or Right to change the text size.
Use Up and Down to move between settings rows.
Changes appear immediately and stay saved. Reset position and size restores the original layout.
Changing the layout while the clock is off keeps it off.

This separate app keeps the existing home screen.
It requires Android TV 14 or later and permission to appear over other apps.
It uses no network, accessibility service, or remote button mapping.
Some apps and protected system screens can hide overlays.

## Viewing limits

The main page contains a session timer and a daily bedtime.
Select Timer duration, choose 30, 60, or 90 minutes, then select Start timer.
The timer counts elapsed time, including standby. It does not measure playback.
Select Cancel timer to stop it.

Bedtime starts off. Select its time to edit it with the remote.
Use Left and Right to select hours or minutes. Use Up and Down to change the value.
Press OK to save. Press Back to cancel.
Turn bedtime on to repeat it each day. Skip next skips one occurrence after confirmation.
If the selected time already passed, bedtime starts tomorrow.

The app shows a brief warning five minutes before the deadline.
The warning does not take remote focus.
Select At the limit, then choose Stop screen for a full-screen reminder at the deadline.
Choose Remind me for a brief message instead.
The stop screen offers Go to Home and one five-minute extension after confirmation.
Another active deadline can prevent an extension. An extension never moves the other deadline.
Back and Home dismiss the occurrence without repeated interruptions.

Limits continue when the clock is hidden. Both overlay permission and exact alarm access are required.
The app shows a permission action if access is missing.
Bedtime follows the TV time zone. The app does not wake the display.
Expired events do not open a stop screen when the TV wakes later.

These limits support adults who manage their own viewing time.
They do not lock the TV or guarantee that playback stops in another app.
Some apps can hide overlays. Android can stop the app.

## Build and install

Install JDK 17, Android SDK platform 34, Android build tools 34, and ADB on the computer.
Connect the TV through ADB. Find its address with `adb devices`.
From the repository root, operate this command with the TV address:

```sh
TV_SERIAL=192.168.1.121:34927 bash tools/install-clock.sh
```

The script builds the app, installs it, grants overlay and exact alarm access, and opens the main page.
It does not replace the launcher or disable packages.
Set `JAVA_HOME` and `ANDROID_HOME` if the tools use different paths on your computer.

## Local tests

From the repository root, use JDK 17 and the installed Android SDK:

```sh
./gradlew :clock:testDebugUnitTest :clock:lintRelease :clock:assembleRelease :clock:assembleDebugAndroidTest
```

The unit tests cover deadline order, standby, clock corrections, reboot recovery, and the single extension.
They also cover daylight saving time changes.
Device tests must separately exercise Android alarms, remote focus, permissions, and background presentation.
The Android tests restore the saved app preferences after each case.

## Test on the TV

The test changes the switch, opens Settings, and returns to the current home screen.
It restores the clock layout and on/off state. Operate this command from the repository root:

```sh
TV_SERIAL=192.168.1.121:34927 python3 apps/clock/tests/device_test.py
```

The clock uses a foreground service, an Android task with a persistent notification.
It updates once per minute and pauses updates while the screen is off.
The app uses native Android controls and no additional runtime libraries.
Android can stop the app. Open TV Clock to start it again.
Reinstalling the app preserves its on/off choice. Force stop prevents automatic startup until you open the app again.

## Remove

To hide the clock, turn Show clock off.
To remove the app, use Settings > Apps > TV Clock > Uninstall.
You can also operate this command on the connected computer:

```sh
adb -s 192.168.1.121:34927 uninstall com.amine.tvclock
```

Removal also removes overlay permission. The existing home screen stays active.
