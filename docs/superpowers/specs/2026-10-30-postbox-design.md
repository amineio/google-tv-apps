# Postbox design

Date: 2026-10-30

Status: The user authorized development and delegated visual approval to an independent UI/UX reviewer. The reviewer accepted the corrected concepts on 2026-10-30. Implementation is authorized. No further user approval question is required. See the [concept review record](../../postbox/concepts/README.md) for accepted references and sketch limits.

## Purpose

Postbox is a personal post library for Google TV. A phone captures selected text, links, and screenshots. The TV stores those captures locally for comfortable reading with the remote.

The first release does not import X bookmarks or Instagram Saved collections automatically. A shared link does not establish that the app received the post text or images. The app must show that distinction clearly.

## App boundary

- App title: `Postbox`.
- Independent build: `apps/postbox`; package: `com.amine.postbox`.
- Version: `0.1`; version code: `1`; minimum SDK: `34`; compile and target SDK: `37`.
- One Kotlin APK contains a TV `MainActivity` and a phone `PhoneActivity`.
- The TV activity uses `LEANBACK_LAUNCHER`. The phone activity uses the ordinary launcher and Android share actions.
- Declare leanback, touchscreen, and camera hardware as optional. Keep all permissions, resources, tests, and installation tools app-specific.
- Use native Compose for TV controls on TV and touch controls on the phone. Do not change the root Java build or other apps.

No account login, paid API, cloud storage, scraping, AI, OCR, automatic content extraction, or video capture belongs in this release. No music or sound is needed.

## Phone capture

The phone opens its local drafts. It can create a capture from manual entry, `ACTION_SEND` text or one image, or `ACTION_SEND_MULTIPLE` images. Read shared text and images together when both exist. The user can select more images with the Android photo picker.

Each capture has a title, multiline plain text, an optional source URL, up to four images, a capture time, and a UUID. Preserve line breaks and the submitted plain text. Do not interpret shared HTML as executable content.

Allow only HTTPS source URLs on X, Twitter, or Instagram hosts. Reject credentials and non-default ports. Normalize host aliases, tracking query parameters, and fragments. Do not fetch the URL. Text without a source URL and screenshots without text remain valid captures.

If the shared content contains only a URL, show `Link only` and `Add text or image`. On TV, show `Post details` without invented text or thumbnails. A link-only capture remains a valid explicit save.

`Save on phone` writes the capture locally. `Send to TV` is a separate explicit action. An unavailable TV leaves the draft saved and offers retry. Do not send automatically when the activity opens, a share arrives, or the app returns from the background.

Show a sent state only after the TV confirms durable storage. Editing a saved capture creates a new UUID and clears its sent state. Replace the old phone draft only after saving the new capture succeeds. This prevents a changed draft from overwriting an existing TV capture or losing the old draft on failure. Keep sent drafts until the user deletes them.

## Data limits and persistence

| Limit | Value |
| --- | --- |
| Title | 160 characters |
| Plain text | 20,000 characters |
| Images per capture | 4 |
| Image storage format | Compressed JPEG |
| Image longest edge | 2,048 pixels maximum |
| Encoded capture | 4 MiB maximum, including JSON and base64 images |
| Captures in each local library | 50 maximum, including archived and sent captures |
| Total local capture files | 64 MiB maximum |

Store each capture as a private JSON file with inline base64 JPEG images. Keep local archive and sent flags outside the immutable capture payload. Use deterministic canonical capture encoding for its SHA-256 digest. The digest excludes archive and sent flags.

All file access, image inspection, normalization, decoding, and network work runs off the main thread. Inspect image dimensions before allocating the complete bitmap. Copy shared URI content while access is available. Do not rely on a shared URI remaining readable later.

Validate the complete encoded capture before writing. Write a temporary file, flush and synchronize it, then atomically replace the destination. A failed write must preserve the previous library and remove incomplete temporary files. A receiver acknowledges success only after storage commits.

The UUID is the idempotency key. The same UUID and canonical digest returns success without adding another item. The same UUID with another digest returns a conflict and preserves the stored item. At capacity, preserve existing captures and explain that the user must delete items. Do not evict captures silently.

Malformed stored files must not crash the library. Ignore the damaged entry, preserve its file, and expose a generic local storage message without logging its contents. Disable Android backup for this private content.

## TV screens and remote behavior

Use the approved warm ink background, sage surfaces, cream text, and apricot focus accents. Keep labels, icons, focus shapes, QR codes, and layout in native code. Keep essential content inside six-percent margins on all TV screen edges.

The library has `Saved`, `Archive`, and `Add posts`. Initial focus lands on the first saved capture, or `Add posts` when Saved is empty. Cards identify the source, title, and capture type honestly. Label dates as capture dates. Show exactly one actual focus indicator; a selected tab or card must not appear focused when another control owns focus.

An empty library explains the phone capture flow. Offer `Add posts` and one isolated `Example`. The example has a visible example label and never enters the real library or changes its counts.

Opening a capture shows its title, source, text, and images. Left moves from the action column into the body or image. Up and Down scroll that content. Right returns to the actions without resetting the reading position. Images preserve their proportions. A tall image enlargement fits the available width and scrolls vertically, rather than shrinking the entire screenshot into a tiny centered image. Hide `Next image` when the capture has only one image. Back closes enlargement and restores focus to that image.

`Archive` moves an item out of Saved. `Restore` returns it from Archive. `Delete` requires confirmation with `Cancel` initially focused. Back cancels deletion. Returning from details, dialogs, Android settings, or the launcher restores a meaningful previous focus target and scroll position.

The original source action shows a QR code for the saved source URL. This is distinct from the pairing QR. A capture without a source URL does not show a working original-link action. Keep controls clear of the separate TV Clock overlay.

## Local pairing and transfer

The receiver runs only on the TV `Add posts` screen while its activity is resumed. Each opening starts a five-minute window, measured with a monotonic clock. Back, activity stop, expiry, or listener failure closes the listener and every accepted socket. No background service keeps it alive.

The TV creates an RSA key and valid self-signed certificate through `AndroidKeyStore`. Use the stable alias `postbox.receiver.v2` across receive windows. Expose TLS through `SSLServerSocket`; do not add a cleartext fallback. Keep key purposes `PURPOSE_SIGN | PURPOSE_VERIFY`. Authorize SHA-256/384/512, `DIGEST_NONE`, RSA PKCS#1/PSS signature padding, and `ENCRYPTION_PADDING_NONE`. The native Android TLS provider needs `DIGEST_NONE` and `ENCRYPTION_PADDING_NONE` for prehashed RSA signing. The core emulator checkpoint exercised this configuration; full-app verification remains separate.

The QR contains `postbox://pair?host=<local-ip>&port=<port>&pin=<sha256-cert-der>&token=<temporary-token>`. Use full SHA-256 certificate DER pinning. Validate scheme, authority, parameter uniqueness, local IP, port, pin encoding, and token length before connecting. Do not follow redirects or accept arbitrary internet destinations from a pairing link.

The phone must finish the TLS handshake and verify the pin before sending any token or capture. Scope its trust manager to this client. Do not install a global trust override or use a permissive fallback. A changed certificate requires pairing again.

`POST /pair` exchanges a random 256-bit temporary token for a random 256-bit persistent phone token. The temporary token expires with the receive window and is consumed by a successful exchange. Only one phone is paired. A new valid QR pairing replaces the previous phone token; `Forget phone` revokes it. Existing paired phones can send during later receive windows without scanning again.

`POST /captures` requires the persistent phone token and a validated capture. The response distinguishes added, already present, conflict, capacity, invalid input, and unavailable receiver outcomes. Never acknowledge an interrupted or uncommitted capture as sent.

Use a bounded HTTP subset over TLS: exact supported paths and methods, a request line of at most 2,048 bytes, headers of at most 8,192 bytes, one valid `Content-Length`, and no chunked request bodies. Use five-second connection, handshake, and idle-read timeouts plus a 30-second total request deadline. Admit at most two active connections and two queued connections. Session expiry takes precedence and closes them all. Reject duplicate authorization or length headers and unsupported transfer encodings before reading a body. Pair request bodies cannot exceed 4,096 bytes; capture request bodies cannot exceed 4 MiB. Token and capture values must not enter logs or error text.

On Android 17 and later, check and request `ACCESS_LOCAL_NETWORK` before opening or contacting the LAN receiver. Handle denial and revocation. On older supported versions, use the normal `INTERNET` permission without requesting an unavailable permission.

## Pairing input on the phone

Offer `Photograph TV code`. Delegate full-resolution capture through `ActivityResultContracts.TakePicture` into a temporary private `FileProvider` file. Decode the photograph using bundled ZXing core. Do not use thumbnail capture or request the app's own camera permission for delegated capture.

Cancel leaves existing drafts unchanged. A failed decode offers retake. Delete the temporary QR photograph after success, failure, or cancellation cleanup. It never becomes a captured post image.

Also accept the pairing deep link through `ACTION_VIEW` and offer `Paste pairing link`. System camera QR handling is a convenience and must not be the only supported route. Do not add an unpinned host-plus-short-code pairing path or a live camera scanner in this release.

## Artwork and repository privacy

Generate three matching raster assets: a 512 by 512 icon, a 640 by 360 banner with `Postbox`, and a separate text-free 1920 by 1080 launcher background. Use a simple native bookmark symbol for controls. Compress bundled raster assets to WebP and preserve any font license.

The release APK budget is 12 MiB. Measure the built release APK before delivery. Artwork sizes alone do not establish the APK size.

Declare `android:icon`, `android:banner`, and application metadata `com.amine.familyhome.HERO_IMAGE` with a local drawable resource. Do not change Family Home for this app. Record prompts, source references, dimensions, compression, hashes, and rights in `docs/postbox/assets.md`.

Use generic examples and synthetic posts in committed tests and concepts. Keep personal captures, pairing links, tokens, device addresses, and backups out of Git. Attach review images directly in chat.

## Acceptance and delivery

The coordinator owns builds, emulator input, integration, and Git delivery. Development and final review remain delegated. Do not run competing builds or device sessions.

Verify unit contracts, release lint and packaging, real directional navigation, phone share handling, persistence, and an actual pinned TLS handshake and transfer in the emulator. Include wrong-pin and bad-secret rejection, duplicate retry, oversize input, storage failure, expiry, and closing accepted sockets.

Use one test class per focused instrumented invocation. Inspect XML class names and counts. Capture the current installed build with fresh filenames. A fresh independent reviewer inspects the complete change and native captures before delivery.

Emulator evidence is sufficient for this release. No physical TV installation is requested. State that phone camera behavior, Wi-Fi isolation, and physical-device appearance were not established by emulator tests.

After successful emulator installation and required checks, fetch `origin`, integrate concurrent changes, repeat affected checks, commit only this app and related documentation, and deliver the validated change to `origin/main`. Keep cloud APK builds paused.

## Source constraints checked on 2026-10-30

- [X bookmarks](https://docs.x.com/x-api/posts/bookmarks/introduction) require an authenticated user and developer app; [API reads are metered](https://docs.x.com/x-api/getting-started/pricing).
- The [Instagram professional-account API reference](https://developers.facebook.com/documentation/instagram-platform/instagram-graph-api/reference/ig-user) has no documented Saved collection edge. This absence is an API-surface inference, not a claim about undocumented endpoints.
- [Instagram oEmbed](https://developers.facebook.com/documentation/instagram-platform/oembed) is for front-end embedding and prohibits extracting or persisting returned content for other purposes. Do not base offline capture or future AI on it.
- [Android shared data](https://developer.android.com/develop/ui/compose/sharing/receive) supports text and images, but the sender determines the payload.
- [Android local network protections](https://developer.android.com/privacy-and-security/local-network-permission) require runtime permission for target SDK 37 LAN communication on Android 17.
