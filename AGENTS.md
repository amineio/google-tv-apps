# Repository instructions

## Before work

Read the root README and the affected app guide.
For new Kotlin apps, also read [the Kotlin development guide](docs/kotlin-tv-development.md).
Make sure that the absolute checkout, branch, HEAD, worktrees, and status match the task.
Preserve unrelated edits, existing worktrees, and ignored device backups.
Use `--repo amineio/google-tv-apps` with `gh`.
The inherited upstream can otherwise become its target.

## App and build boundaries

Use Kotlin and Compose for TV for new apps.
Keep Home, Clock, and Chess in Java until a separate migration is requested.
Use `apps/family-home` as the reference for an independent modern Kotlin build.
Keep each app's package, version, permissions, resources, tests, and installer separate.
Do not change the root Java build to accommodate a new Kotlin app without a clear need.
Read the current Gradle files before copying tool versions or dependencies.

## Design and assets

For a substantial visual change, use ImageGen to explore screen concepts before implementation.
Show the concepts in chat so that the user can review them from a phone.
Use generated images as design references. Build the interface with native Compose for TV controls.
Use ImageGen for suitable raster assets such as portraits, backgrounds, and illustrations.
Use vector or code assets for simple icons, labels, and focus outlines.
Record asset prompts, source references, dimensions, and license information.
Compress bundled assets and preserve font licenses.
ImageGen creates images. Use an audio tool or local synthesis for music and sound effects.
Keep optional audio short, quiet, locally bundled, and easy to mute.
Do not add music or sounds unless the app design calls for them.

## Subagents

For substantial app work, use focused design, implementation, and independent review tasks.
Give each subagent the exact scope, owned files, constraints, and validation commands.
Parallelize independent research, asset work, and read-only reviews.
Keep shared-file changes, integration, Git operations, and device input under one coordinator.
Do not run multiple builds or instrumented test sessions against the same app or device at once.
Use one fresh reviewer for the completed change. Fix material findings before delivery.
Handle small documentation or configuration changes directly unless a separate review adds value.

## Local iteration and evidence

Build and test locally. GitHub APK builds are paused, and workflow definitions accept manual runs only.
Do not enable cloud builds, upload APKs, or create public releases without an explicit request.
Use the Google TV emulator for layout, contrast, and D-pad iteration.
Set an explicit ADB serial for every install, test, screenshot, and remote command.
Run instrumented tests on the emulator by default; the test runner can uninstall the tested app.
Test initial focus, directional navigation, scrolling, dialogs, empty lists, and focus after app return.
Use actual directional input in navigation tests instead of forcing focus onto the destination.
Keep presentation tests separate from timer, accessibility helper, playback, wake, and Cast evidence.
For variable fonts, make sure that the configured weights render correctly in device captures.
Keep launcher controls clear of the separate Clock overlay.
Use the physical TV for hardware behavior and final visual review when it is available.
If someone is watching, stop device input and finish work that does not need the TV.
Report deferred physical tests without claiming that emulator tests prove them.
Preserve Google TV Home and `com.google.android.tungsten.setupwraith` for Cast credentials.

## Privacy

The user deferred repository privatization. Do not detach the fork or change visibility without a new explicit request.
While the repository remains public, keep new family screenshots and personal data out of Git.
Use generic names in shared concepts, test data, and screenshots.
Keep account screens, device identifiers, credentials, and backups out of Git.
Do not use public raw GitHub URLs or public image hosting to share family screenshots.
Attach review images directly in the conversation through the available file or image tools.

## App installation and delivery

After each successful app installation, commit the installed app changes and push them to `origin/main`.
If the work is on a branch, push the branch and merge it into `main`.
Use the shortest safe delivery path. A pull request is optional.
If `main` is clean and direct pushes are allowed, commit and push to `main` directly.
Complete these actions in the same task without asking for further approval, unless the user explicitly requests otherwise.
Run the relevant tests and build checks before merging.
Include only changes for the installed app and related repository instructions. Preserve unrelated local work.
Make sure that the merge reaches `origin/main`. Report the installation and merge results separately.
If a test, push, or merge fails, resolve the failure when possible. Otherwise, report the blocker and preserve the work.
