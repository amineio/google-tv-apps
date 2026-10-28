# Family Home design

Family Home is a new Kotlin app with Compose for TV.
Its package is `com.amine.familyhome` and its source lives in `apps/family-home`.
The existing Java apps remain independent. Existing Home is a reference, with no fallback role.
The app uses a separate Gradle build so that its tools do not change the Java builds.

## First release

Start with Amine, Elias, and Sofia. Profiles belong to this launcher and share the existing streaming sign-ins.
Amine has unrestricted app access and no compulsory timer.
Each new child session requires a duration choice. The default is 20 minutes.
Offer five-minute steps from 5 through 60 minutes. The maximum applies to each session, with no daily quota.
An unfinished session resumes its saved balance. Home and Back never reset it.

All screen-on time counts. Standby and profile changes pause the outgoing child.
Wake and boot show the chooser before apps continue. Persist transitions and bounded checkpoints.
Recover interrupted sessions paused, with a visible recovery message. Never silently grant a new balance.
Keep the timer controller outside the screen lifecycle. Use elapsed time, independent of civil clock changes.
Block new child sessions if required access or storage fails.

Both children initially get Chess & Math, MasterClass, VTM GO, VRT MAX, Spotify, and Prime Video.
Amine can edit each child's allowed packages. New apps remain excluded until allowed.
Apply one policy to the grid, launch requests, remote shortcuts, and foreground window events.
Permit narrow system dialogs for volume, app login, and permission prompts.
Redirect excluded apps, including YouTube, RTBF Auvio, alternate launchers, and Cast receivers during child sessions.
The helper reads package and class names only. It cannot retrieve screen content.

Show remaining time on child Home. Give brief five-minute and one-minute warnings without taking focus.
At expiry, save the expired state, request media pause, and open Time is up.
Home and Back cannot resume an expired session. Continuing requires an explicit new session or profile change.
Pause child media at standby and profile handoff. Prove actual playback behavior on the TV.

The parent PIN starts off. When enabled, it protects Amine, family configuration, and new or replacement sessions.
Existing child balances resume without the PIN. Store a salted hash, never the PIN.
Adult authorization ends on profile exit and standby. Document local ADB recovery.

## Implementation choices

Use stable Kotlin, coroutines, immutable session records, and one serialized controller.
Use local DataStore storage and await successful writes before publishing changes.
Compose reads state and sends actions. Android's accessibility service owns deadlines and screen events.
Use TV Material controls, large text, clear focus, and focus restoration after dialogs and app returns.
The app requires Android 14 for this household Chromecast. It uses no network permission or cloud service.

Recreate only the necessary Home and Cast behavior from the existing reference.
Keep `com.google.android.apps.tv.launcherx` and `com.google.android.tungsten.setupwraith` enabled.
The installer captures Home roles and accessibility helpers, then prevents competing launcher helpers.
It preserves unrelated helpers and existing app data. It installs only Family Home.

## Delivery

Complete unit tests, remote flows, lint, release builds, and independent review.
Test wake, reboot, app shortcuts, playback pause, system dialogs, Cast, and the independent Java apps.
Record release size and device captures. Preserve the existing app size limits.
After each successful installation, deliver the installed source to `origin/main` in the same task.
Report installation and source delivery separately.

## Later releases

Usage reports follow acceptance of the first release. Record total and per-app time by selected profile.
Keep unknown observations separate from measured time. Design retention and report layout in that release.
Program titles require a separate media-session feasibility test. Never invent missing titles.
Development shortcuts and the Java migrations are separate work.

## Approaches

The discussion considered extending Java Home, converting Home, and creating a new app.
Amine selected a new Kotlin app with Compose for TV.
This gives the family app its own design and permits separate migrations of the other apps.
