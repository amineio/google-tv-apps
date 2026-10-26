# Home and Cast compatibility

The user requests a working custom Home screen and Samsung casting, with a committed, pushed, installed solution.

Cast reads device credentials from setupwraith's CastInfoProvider. Keep setupwraith and launcherx enabled. Android selects launcherx before third-party Home activities on this firmware. Selecting the HOME role alone does not change that priority.

Use the existing HomeKeyService to redirect only stock HOME activity windows. Enable this through stock_home_redirect in the existing theme configuration. Require the custom launcher's HOME role so that restoring the stock role ends redirection. Start the custom launcher as an explicit ordinary activity, with CATEGORY_HOME for scroll reset but without ACTION_MAIN. This preserves launcherx's ability to start remote-button apps.

At boot, use upstream's invisible ProbeActivity once to obtain a current window event. Never open Home merely because the service connected. Leave Cast, playback apps, Settings, and other stock activities alone. Keep long Home presses with Android. Do not read screen content.

Alternatives: disabling setupwraith breaks Cast. Component disabling is blocked by this TV. Manifest priority and set-home-activity alone do not override the system HOME priorities. Periodically enabling setupwraith leaves credentials dependent on process lifetime. Keep the Google packages enabled instead.

A separate ADB installer builds and installs Home, preserves the existing theme keys and accessibility helpers, enables launcherx before setupwraith, and enables HomeKeyService after selecting the HOME role. Keep the existing generic installer for other TVs. Update the Chromecast example and troubleshooting guidance.

Evidence required: policy tests, installer failure/order tests, repository suite, release unit tests, lint, release build, real Home navigation, reboot and Cast credentials. Ask for Samsung playback with the new Home screen active. Preserve the installed APK and device configuration before installation.

References: upstream PR #698, merged PR #1121, and issue #1274. This is a scoped adaptation, not an upstream-wide update.
