# Postbox

Capture selected post text, links, and screenshots on an Android phone. Send them locally to Google TV and read them with the remote.

Postbox uses one independent Kotlin APK for phone and TV. Version `0.1` uses package `com.amine.postbox` and requires Android 14 or later. It needs no social account login, cloud storage, or paid API.

Release `0.1` passed local tests and is installed on the Android 14 TV emulator. The validated runtime is delivered to `origin/main`. The [validation record](../../docs/postbox/validation-2026-10-30.md) records the tested revision, native review, APK hash, and evidence limits.

## Capture on the phone

Open a saved post in X or Instagram and share it to Postbox. You can also open Postbox and enter text or a source link manually. Add screenshots through sharing or the photo picker.

The sending app chooses what it shares. A shared URL can contain no post text or images. Postbox labels that capture **Link only**. Enter the post text or select **Add image** to supply readable content. The app does not fetch missing post content.

Check the title, multiline text, source link, and images. Select **Save on phone** to keep the capture locally. **Send to TV** is a separate action. A failed transfer leaves the capture saved for retry. Sent captures stay on the phone until you delete them.

Editing a saved capture gives it a new identity. This prevents edited content from overwriting a previous TV capture. The old phone draft is replaced only after the new save succeeds.

To send a previously sent phone copy again, choose **Edit capture** and save it first. This creates a new capture identity.

## Pair and send

1. Connect the phone and TV to the same local network.
2. On TV, open **Add posts**.
3. On the phone, open **Connect TV**.
4. Select **Photograph TV code** and photograph the complete QR code.
5. Confirm the photograph in the camera app.
6. Select a saved phone capture and choose **Send to TV**.

The phone can also open a pairing link from its system QR camera or use **Paste pairing link**. The photograph route does not depend on the system camera recognizing the custom link.

Postbox verifies the TV certificate before sending a pairing token or capture. Pairing remains saved for later sessions. Pairing another phone replaces the previous phone. **Forget phone** revokes the saved phone token.

The TV receives captures only while **Add posts** stays visible and resumed. The window closes after five minutes. Back or leaving the app also closes it. Open **Add posts** again before a later send.

On Android 17 or later, allow local network access when requested. If the TV is unavailable, keep the saved capture and retry. Some guest Wi-Fi networks prevent devices from reaching each other.

## Read on TV

**Saved** contains active captures. **Archive** contains archived captures. An empty library offers **Add posts** and one clearly labeled example. The example does not enter the real library.

Use the D-pad to select a capture. In the reader, press Left to enter the text or image. Up and Down scroll. Right returns to the actions and keeps your reading position.

Enlarge an image to read a portrait screenshot. Tall images fit the available width and scroll vertically. **Next image** appears only when another image exists. Back closes enlargement and restores focus to the image.

**Original post** shows a QR code for the saved source URL. A link-only capture has no invented preview. Dates describe when you captured the item.

**Archive** moves a capture out of Saved. **Restore** returns it. **Delete** asks for confirmation with **Cancel** focused. Back cancels the deletion.

## Local storage and limits

Each capture supports a 160-character title, 20,000 characters of plain text, and up to four JPEG images. Images are reduced to at most 2,048 pixels on their longest edge.

The encoded capture limit is 4 MiB. Each phone or TV library holds at most 50 captures and 64 MiB of capture files. Archived and sent captures count toward those limits. Delete items to make room; the app does not remove older captures automatically.

Captures and pairing information stay in private app storage. Android backup is disabled. Uninstalling the app or clearing its data removes that device's stored content and pairing.

Postbox does not import bookmark collections automatically. It does not scrape posts, extract text from screenshots, play captured videos, or create AI summaries.

## Build and emulator installation

Use JDK 17, SDK platform `android-37.0`, and build tools `36.0.0`. From the repository root:

```sh
apps/postbox/gradlew -p apps/postbox testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease assembleDebugAndroidTest --console=plain
```

The release APK is `apps/postbox/build/outputs/apk/release/postbox-release.apk`. Local release builds use debug signing. Store distribution requires separate signing work.

Install and open only Postbox on the emulator:

```sh
TV_SERIAL=emulator-5554 tools/install-postbox.sh
```

The installer checks the selected device, runs release unit tests and lint, builds the APK, installs it without clearing existing data, and opens Postbox. It prints the APK hash and installed package version.

Use the same release APK on the Android phone. Copy or send `postbox-release.apk` to the phone through a private file transfer. Open the APK in the phone's Files app and select **Install**. If Android requests permission, allow installation for the file app that opened this APK. Open Postbox from the phone's app list after installation.

With ADB, install it on an explicitly selected Android phone:

```sh
adb -s '<phone serial>' install -r apps/postbox/build/outputs/apk/release/postbox-release.apk
```

Physical TV installation requires an explicit request. The installer preserves Home routing, Cast packages, and other apps.

## Focused emulator tests

Run one instrumented class per invocation. For example:

```sh
ANDROID_SERIAL=emulator-5554 apps/postbox/gradlew -p apps/postbox connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.postbox.PostboxNavigationTest' --console=plain
```

The focused classes are `PostboxNavigationTest`, `PhoneCaptureTest`, `LauncherArtworkTest`, `ReceiverLifecycleTest`, and `com.amine.postbox.core.PinnedTransferTest`. Run each class separately. Inspect XML class names and counts before claiming coverage.

Emulator tests can establish native layout, remote navigation, phone share handling, persistence, and a real local pinned TLS handshake. They do not establish physical camera behavior, external phone connectivity, home Wi-Fi access, or final physical-TV appearance.

Read the [design](../../docs/superpowers/specs/2026-10-30-postbox-design.md), [accepted concepts](../../docs/postbox/concepts/README.md), and [asset record](../../docs/postbox/assets.md) for scope and visual references.
