# Repository instructions

## Before work

Read the root README and the affected app guide.
For new Kotlin apps, also read [the Kotlin development guide](docs/kotlin-tv-development.md).
For Chromecast-specific work, read the [Chromecast device profile](docs/devices/google-chromecast-gtv-4k.md).
Make sure that the absolute checkout, branch, HEAD, worktrees, and status match the task.

At each new task or thread, fetch `origin`, not the inherited `upstream` remote.
Compare local `main` and the current task branch with freshly fetched `origin/main`.
If the task branch has a tracking branch, compare it too.
Report whether each comparison is equal, ahead, behind, or diverged.
Do not claim freshness for tracking references that were not fetched.
If fetching fails, report that remote freshness is unknown.

Before code, documentation, or configuration edits, create a new isolated worktree and task branch from freshly fetched `origin/main`.
If the user explicitly requests an existing branch or another base ref, use that request instead.
If the task already has its own isolated worktree, continue there.
Do not create nested worktrees or use the shared `main` checkout for edits.
Apply this default without asking for permission.

Preserve unrelated edits, local commits, existing worktrees, and ignored device backups.
Do not reset or rebase another task's work.
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
For substantial game changes, ask an independent game UI/UX reviewer to inspect the concept and final native captures.
Keep board geometry, diagrams, labels, and focus controls in native code.
Use ImageGen for suitable raster assets such as portraits, backgrounds, and illustrations.
Use vector or code assets for simple icons, labels, and focus outlines.
Record asset prompts, source references, dimensions, and license information.
Compress bundled assets and preserve font licenses.
For every new app or game, plan matching launcher artwork with the app design.
Deliver a square app icon, a 16:9 TV banner, and a separate text-free 16:9 launcher background before release.
Keep all three resources inside the app. The launcher owns presentation, contrast, focus, and fallback.
Declare the background through application metadata named `com.amine.familyhome.HERO_IMAGE`, with an `android:resource` drawable reference.
Do not enlarge the icon or banner into a background or add a launcher special case for each new app.
Follow the [launcher artwork requirements](docs/kotlin-tv-development.md#launcher-artwork-for-new-apps) for ImageGen, composition, resource declarations, and rendered review.
ImageGen creates images. Use an audio tool or local synthesis for music and sound effects.
Keep optional audio short, quiet, locally bundled, and easy to mute.
Do not add music or sounds unless the app design calls for them.

## Subagents

Every conversation's main thread remains the coordinator and delegates development and independent review to subagents.
This applies to new and follow-up threads.
For substantial app work, use focused design, implementation, and independent review tasks.
Give each subagent the exact scope, owned files, constraints, and validation commands.
Parallelize independent research, asset work, and read-only reviews.
One coordinator owns planning, integration commands, builds, device input, and Git delivery.
Delegate all implementation file edits to subagents, including small code changes.
Give shared files to one implementation owner at a time.
Do not run multiple builds or instrumented test sessions against the same app or device at once.
Use one fresh reviewer for the completed change. Fix material findings before delivery.
Delegate documentation and configuration edits too. Keep their review proportional to the risk.

## Local iteration and evidence

Build and test locally. GitHub APK builds are paused, and workflow definitions accept manual runs only.
Do not enable cloud builds, upload APKs, or create public releases without an explicit request.
Use the Google TV emulator for layout, contrast, and D-pad iteration.
During iteration, run focused tests for the changed behavior.
At an integration checkpoint, run the relevant app suite when logic, persistence, or shared behavior changes.
After a minor resource correction, repeat affected tests and captures without repeating unaffected suites.
Record the tested revision and commands. Keep lint and release packaging in the final delivery checks.
Follow the [risk-based test workflow](docs/kotlin-tv-development.md#choose-tests-by-risk).
Set an explicit ADB serial for every install, test, screenshot, and remote command.
Run instrumented tests on the emulator. Do not run them on the physical TV because the runner can uninstall the tested app.
For focused tests, select one class per invocation or use a verified package filter.
Inspect XML test counts and class names before claiming coverage. Build success alone does not establish test selection.
Test initial focus, directional navigation, scrolling, dialogs, empty lists, and focus after app return.
Use actual directional input in navigation tests instead of forcing focus onto the destination.
Keep presentation tests separate from timer, accessibility helper, playback, wake, and Cast evidence.
For variable fonts, make sure that the configured weights render correctly in device captures.
Keep launcher controls clear of the separate Clock overlay.
Use emulator coverage for development and instrumented tests.
Always install the changed app's tested release APK on the physical TV before merging app changes or pushing them to `origin/main`.
This standing instruction authorizes the installation without renewed permission.
Make sure that the installed version and APK SHA-256 match the tested release artifact.
Preserve app data, unrelated apps, Google TV Home, and `com.google.android.tungsten.setupwraith`.
If the TV is unavailable or installation fails, block the merge, preserve the task branch, and report the blocker.
Physical installation does not require unrelated Cast, wake, playback, or audio tests.
State untested hardware behavior separately from installation evidence.
Emulator results do not establish audio focus, playback, Cast, wake, accessibility, or audible quality on the physical TV.
Use fresh capture filenames and make sure that each image belongs to the current build.
Do not infer audible quality from MediaPlayer state or successful decoding.
Requested development, testing, and installation take priority over TV viewing.
Do not delay authorized TV work because playback is active or paused.
Stop device input when the user explicitly asks you to stop.
Report any requested physical tests that remain unperformed. Do not infer their results from emulator tests.
Preserve Google TV Home and `com.google.android.tungsten.setupwraith` for Cast credentials.

## Privacy

The user deferred repository privatization. Do not detach the fork or change visibility without a new explicit request.
While the repository remains public, keep new family screenshots and personal data out of Git.
Use generic names in shared concepts, test data, and screenshots.
Keep account screens, device identifiers, credentials, and backups out of Git.
Do not use public raw GitHub URLs or public image hosting to share family screenshots.
Attach review images directly in the conversation through the available file or image tools.

## App installation and delivery

Always complete physical TV installation before merging app changes or pushing them to `origin/main`.
An emulator installation alone does not satisfy this requirement.
Documentation-only changes do not require an APK installation unless they accompany app delivery.
After successful physical installation, commit the installed app changes and push them to `origin/main`.
Fetch `origin` again before integration or push.
If remote changes exist, integrate them into the task branch and repeat the affected checks.
Do not force-push over concurrent changes.
If the work is on a branch, push the branch and merge it into `main`.
Use the shortest safe delivery path. A pull request is optional.
If direct pushes are allowed, fast-forward `origin/main` from the validated task branch.
Complete these actions in the same task without asking for further approval, unless the user explicitly requests otherwise.
Run the relevant tests and build checks before merging.
Include only changes for the installed app and related repository instructions. Preserve unrelated local work.
Make sure that the merge reaches `origin/main`. Report the installation and merge results separately.
If a test, push, or merge fails, resolve the failure when possible. Otherwise, report the blocker and preserve the work.
