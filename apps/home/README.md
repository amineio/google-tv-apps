# TV Home

For Chromecast with Google TV, use [the Cast-compatible installer](../../docs/CAST-HOME.md). Keep setupwraith and Google TV Home enabled.

TV Home is the native launcher inherited from CleanYourTV.
Its app package remains `com.gathin.tvhome`.
The source and resources live in this directory. The shared Gradle build lives at the repository root.

## Build

From the repository root, use the installed JDK 17 and Android SDK 34:

```sh
./gradlew :home:assembleRelease :home:testReleaseUnitTest :home:lintRelease
```

The APK is `apps/home/build/outputs/apk/release/home-release.apk`.
Edit `src/main/res/values/config.xml` to change the compiled launcher configuration.
Themes can change the appearance without a rebuild.

## Install

The home installer changes the default home app.
It can disable the recognized stock launcher after the new home app starts.
Read [the original guide](../../UPSTREAM.md) for backup, restore, and device-specific behavior.

From the repository root, create the tool environment:

```sh
python3 -m venv tools/venv
tools/venv/bin/pip install -r tools/requirements.txt
```

Connect the TV and complete ADB authorization before installation.
Set `TV_HOST` to the TV address. Then use the home installer:

```sh
TV_HOST=192.168.1.50 tools/install-home.sh
```

The legacy `launcher/build-and-install.sh` entry point remains available.
Windows users can use `launcher/build-and-install.ps1` with the same parameters as before.
This installer builds only `:home`. It does not install TV Clock.
