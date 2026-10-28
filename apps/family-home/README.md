# Family Home

Family Home is a separate Android TV launcher in Kotlin with Compose for TV.
It requires Android 14 or later. It uses no cloud service or network permission.
The existing Java apps keep their own packages, source, and build tools.

## Family use

Select Amine, Elias, or Sofia. Profiles belong to this app and share the existing streaming sign-ins.
Amine has all installed TV apps. Each new child session starts with a duration choice.
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
The parent PIN starts off. When enabled, it protects Amine, family configuration, and new or replacement sessions.
Existing child balances resume without the PIN. The app saves only a salted PIN hash.
The helper reads app and window names. It cannot retrieve screen content.

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
