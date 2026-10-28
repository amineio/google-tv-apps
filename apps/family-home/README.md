# Family Home

Family Home is a separate Android TV launcher in Kotlin with Compose for TV.
It requires Android 14 or later. It uses no cloud service or network permission.
The existing Java apps keep their own packages, source, and build tools.
For reusable patterns and development lessons, read [the Kotlin TV guide](../../docs/kotlin-tv-development.md).
Build and test locally. GitHub APK builds are paused until the user requests them again.

## Interface

Version 1.4 adds native TV settings and optional child profiles.
The profile screen uses circular illustrated portraits, Outfit text, and a gold focus outline.
The home screen keeps the profile at the upper right and gives the countdown fixed bounds and equal-width digit cells.
Select an app tile with the remote center key to open it.
Chess keeps its illustrated background. Apps can supply their own text-free background through application metadata.
Family Home loads only the selected background and limits its decoded dimensions to 1920 by 1080 pixels.
Apps without usable artwork use a subdued gradient based on their icon color.
Landscape banners fill landscape tiles. Square icons fill square tiles.
Children see installed permitted apps in the approved order. Amine keeps all installed TV apps.

The launcher restores the selected app after returning from it.
It refreshes the catalog after resume or a package change.
Profile and session controls stay below the app row, clear of the independent Clock overlay.
The artwork is bundled locally. The launcher does not fetch recommendations or viewing history.

## Family use

Select Amine, Elias, or Sofia. Profiles belong to this app and share the existing streaming sign-ins.
Amine has all installed TV apps. Each new child session starts with a duration choice.
The centered duration screen shows the child's portrait and name above equal-size step buttons.
Start session is centered and has initial focus. Buttons turn gold only when focused.
Use the remote Back key to leave the duration screen without starting a session.
End session and Switch profile show one centered Continue button. Back dismisses the question without applying it.
Switch profile does not require a PIN. End session keeps the parent PIN check when the PIN is enabled.
PIN dialogs start on the first digit. Continue stays disabled until all four digits are entered.
The default is 20 minutes, with five-minute steps from 5 through 60 minutes.
The maximum applies to each session. There is no daily quota.

The timer counts all screen-on time, including menus and paused videos.
Standby and profile changes pause the outgoing child. Wake shows the chooser before playback continues.
Select the child to resume the saved balance. Home and Back do not reset it.
After an interruption, the app restores the latest checkpoint in a paused state.
Checkpoints occur every ten seconds. An interrupted process can lose up to ten seconds of measured use.

At expiry, the helper requests media pause and opens Time is up.
Continuing requires an explicit new session or profile change.
Pause support depends on the app that receives Android's media button request.
The helper redirects excluded apps. It requires an enabled accessibility service and the Home role.
Android can stop or disable the helper. These family controls do not provide managed-device security.

Both children initially get Chess & Math, MasterClass, VTM GO, VRT MAX, Spotify, and Prime Video.
Amine can change each child's app list in Family configuration.
Settings has three rows: Profiles, Parent PIN, and Device setup.
Open a child under Profiles to switch allowed apps on or off.
Unavailable default apps appear after the installed apps and cannot receive focus.
Each row has one focus target. Up and Down move through rows. OK opens or changes a row.
Remote Back returns to the previous page and restores its row and scroll position.
Device setup opens Android controls for the Home helper and default Home app.
The parent PIN starts off. When enabled, it protects Amine, family configuration, and new or replacement sessions.
Existing child balances resume without the PIN. The app saves only a salted PIN hash.
The helper reads app and window names. It cannot retrieve screen content.

To create a child, open Profiles and select Add child profile.
Enter a unique name with 1 through 24 characters, then select Save.
The Android keyboard uses the remote. Dismiss the keyboard to keep editing the same form.
Only Save creates a profile. Remote Back leaves the form without creating one.
A new child uses an initials portrait, the six default apps, and no saved session.
Additional profiles remain reachable by scrolling right on the chooser.
This release does not add rename, deletion, portrait selection, or additional parent roles.
Upgrades preserve existing profiles, balances, allowed apps, and the parent PIN.
Enabling the helper can return to the profile chooser under the existing wake privacy rule.

## Build and install

Use JDK 17, Android SDK platform `android-37.0`, build tools `36.0.0`, and ADB on the computer.
This app uses its own Gradle wrapper. From this directory, operate:

```sh
./gradlew testDebugUnitTest testReleaseUnitTest lintRelease assembleRelease assembleDebugAndroidTest
```

The release APK is `build/outputs/apk/release/family-home-release.apk`.
Connect and authorize the Chromecast before installation. From the repository root, set its ADB address:

```sh
TV_SERIAL='<connected address>' tools/install-family-home.sh
```

The installer builds only Family Home and captures the current Home role and accessibility helpers.
It keeps Google TV Home and setupwraith enabled for Cast credentials.
It removes the competing Home helper from the active helper list and preserves unrelated helpers.
It does not erase data or reinstall the other apps. The installer prints the backup directory.

## Local TV emulator

The development Mac has a Google TV emulator named `Family_TV_API_34`.
It uses Android 14, ARM64, a 1920 × 1080 screen, and keyboard input.
Family Home owns its Home role and uses its accessibility helper.

On this Mac, set the SDK and JDK paths in your terminal:

```sh
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
```

Start the emulator:

```sh
"$ANDROID_HOME/emulator/emulator" -avd Family_TV_API_34 -port 5554 -gpu auto -scale 0.7
```

From this app directory, build and open a local preview in a second terminal:

```sh
./gradlew assembleDebug --console=plain
adb -s emulator-5554 install -r build/outputs/apk/debug/family-home-debug.apk
adb -s emulator-5554 shell am start -W -n com.amine.familyhome/.MainActivity
```

The explicit emulator address keeps these commands separate from the Chromecast.
Use the arrow keys and Enter to navigate. Capture a full-resolution image with:

```sh
adb -s emulator-5554 exec-out screencap -p > /tmp/family-home.png
```

Run the Android UI tests against this emulator:

```sh
ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest --console=plain
```

The emulator supports layout, text contrast, and remote navigation tests.
Use the Chromecast for Cast, streaming playback, wake behavior, and final visual acceptance.
The emulator does not contain the family's streaming accounts or all installed TV apps.

For manual helper tests, use `adb input` and `screencap`.
Android's default UI automation suppresses accessibility services, which interrupts the helper during a hierarchy dump.
See [Android UI automation flags](https://developer.android.com/reference/android/app/UiAutomation#FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).

## Recovery

If the helper fails, select Amine and open Family configuration to enable it again.
If the PIN is lost, clear Family Home data through ADB on the authorized computer.
This removes all Family Home balances, app lists, and the parent PIN. Other apps retain their data.

```sh
adb -s '<connected address>' shell pm clear com.amine.familyhome
```

To restore the previous Home routing, read the installer's `backup/family-home-install.*/original.json`.
Use its captured user, Home role, helper list, and accessibility state. For the usual Google TV Home routing:

```sh
adb -s '<connected address>' shell cmd package set-home-activity --user 0 com.google.android.apps.tv.launcherx/.home.HomeActivity
```

Disable Family Home in Android Accessibility after changing the Home role.
Keep Google TV Home and setupwraith enabled so that Cast credentials remain available.

## Later releases

Usage reports, program titles, and development shortcuts are separate planned releases.
This release does not provide viewing history or identify who watches without selecting their profile.
