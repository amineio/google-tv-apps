# Custom Home with Cast

Chromecast with Google TV needs `com.google.android.tungsten.setupwraith` for Cast device credentials.
Keep that package and `com.google.android.apps.tv.launcherx` enabled.

The optional Home redirect opens TV Home when Google TV Home appears.
It also handles a short Home press and leaves long presses with Android.
Cast, Settings, and other app windows do not trigger the redirect.
The helper reads window package and class names. It cannot read screen content.
A short Google TV Home flash is possible during startup.

## Install

Use a paired ADB connection to the TV. Find its address with `adb devices -l`.
Install JDK 17, Android SDK platform 34, build tools 34.0.0, Python 3, and Android platform tools.
Set `JAVA_HOME` and `ANDROID_HOME` to the installed JDK and SDK directories.
From the repository root, use the TV address:

```sh
TV_SERIAL=192.168.1.121:41137 tools/install-home-with-cast.sh
```

The installer builds TV Home, installs the APK, and adds `stock_home_redirect` to the existing theme file.
It preserves the other theme keys and accessibility helpers.
It enables Google TV Home before setupwraith, selects TV Home for the HOME role, and enables HomeKeyService.
Android still resolves HOME to Google TV Home. The helper opens TV Home over that window.
The installer prints the local backup directory for the previous configuration and helper list.
It does not install or change TV Clock.

Test the physical Home button from YouTube. Test casting from the phone while TV Home is visible.
Restart the TV. Test Home and casting again.
Do not use the generic Home installer on this Chromecast, because it disables Google TV Home.

If you change themes, retain `"stock_home_redirect": "com.google.android.apps.tv.launcherx"` in the theme file.
Restart HomeKeyService after a theme change, or operate the Cast-compatible installer again.

## Restore Google TV Home

Use the connected TV address:

```sh
adb -s 192.168.1.121:41137 shell cmd package set-home-activity --user 0 com.google.android.apps.tv.launcherx/.home.HomeActivity
```

This returns the HOME role to Google TV Home. The helper then stops redirecting stock Home windows and Home presses.
To remove the helper, turn off TV Home in Settings > Accessibility.
Keep setupwraith and Google TV Home enabled to preserve Cast.

## Sources

The implementation adapts upstream's stock-window redirect and boot probe, with an explicit app intent for the custom launcher.
See [PR #698](https://github.com/Gathin23/cleanyourtv/pull/698),
[PR #1121](https://github.com/Gathin23/cleanyourtv/pull/1121), and
[issue #1274](https://github.com/Gathin23/cleanyourtv/issues/1274).
