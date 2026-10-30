# Postbox validation: 2026-10-30

This record separates design approval, shell-tool checks, app verification, installation, and Git delivery. Pending work is not a passing result.

## Design and documentation

The coordinator reported independent acceptance of the corrected concepts on 2026-10-30. The [concept record](concepts/README.md) identifies approved references and generated sketch defects. This approval does not establish native rendering or runtime behavior.

The design and plan use the agreed synchronous `com.amine.postbox.core` interfaces. Public terms are `Saved`, `Archive`, `Add posts`, `Save on phone`, and `Send to TV`.

## Completed shell-tool checks

The documentation/tooling worker created installer tests before the installer. The first run returned six missing-file errors because `tools/install-postbox.sh` did not yet exist. After implementing the installer, the same command passed all six tests:

```sh
python3 -m unittest discover -s tests -p test_postbox_install.py -v
```

These tests substitute fake Gradle and ADB commands. They verify explicit serial selection, repository paths with spaces, successful Postbox-only delivery commands, and stop behavior after device, build, installation, or launch failure. They do not build an APK, access a device, or establish installation success.

## Completed asset preparation

The three generated launcher resources were resized with preserved proportions, compressed to WebP, and measured at 512 by 512, 640 by 360, and 1920 by 1080 pixels. They total 73,902 bytes. The [asset record](assets.md) and [machine-readable manifest](launcher-assets.json) contain source and final hashes, exact prompts, preparation settings, and rights limits.

The documentation worker inspected the icon at 64 and 128 pixels, and the banner at 320 by 180 and 384 by 216. The bookmark silhouette and Postbox title remain readable. These checks inspect resource files; they do not establish installed launcher behavior.

## Pending app and delivery evidence

The coordinator reported a core checkpoint at commits `8d53f49a` and `0121f30d`, with 27 JVM tests and 14 native emulator tests passing. This checkpoint includes the AndroidKeyStore TLS configuration with `DIGEST_NONE`, `ENCRYPTION_PADDING_NONE`, and the stable `postbox.receiver.v2` alias. Key purposes remain signing and verification.

The core owner is addressing a further parent-directory synchronization case for the first save in a new directory. The checkpoint does not establish that this later change passes. It also does not establish full-app integration, native UI behavior, release packaging, or final delivery. Final verification remains pending below.

| Check | State |
| --- | --- |
| Tested app revision and working-tree state | Pending coordinator record |
| Debug and release unit suites | Pending |
| Release lint and APK packaging | Pending |
| Actual release APK within the 12 MiB budget | Pending |
| Actual AndroidKeyStore/pinned TLS transfer | Pending |
| Wrong-pin, wrong-secret, duplicate, oversize, expiry, and stopped-socket cases | Pending |
| Phone share activity and saved capture persistence | Pending |
| Real directional navigation, reader/image scroll, dialog focus, and app return | Pending |
| Current native screen and launcher captures | Pending |
| Final independent implementation and native-capture review | Pending |
| Emulator release installation, installed version, and APK hash | Pending |
| Repository integration and delivery to `origin/main` | Pending |

The coordinator must record exact commands, XML class names and test counts, fresh capture filenames, the tested revision, and relevant artifact hashes when these checks run. Keep personal captures, addresses, and pairing secrets out of this public record.

## Evidence limits

No physical TV installation or test was requested. Emulator evidence is sufficient for this delivery. It cannot establish a physical phone's camera app behavior, home Wi-Fi connectivity, or physical-TV appearance. API 34 emulator results do not exercise Android 17's local-network permission prompt.

Cloud APK builds and public releases remain disabled unless separately requested.
