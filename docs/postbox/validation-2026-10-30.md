# Postbox validation: 2026-10-30

This record separates design approval, shell-tool checks, app verification, installation, and runtime delivery. The coordinator verified the runtime on `origin/main` before the final documentation-only commit.

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

## Accepted core and integration checkpoint

Core review accepted `bda5c90f` after the directory synchronization, malformed JPEG, TLS deadline, and stored UTF-8 fixes. Preserved XML under private `evidence/core-fix2` contains 29 JVM tests and 14 native TLS tests, with zero failures, errors, or skips. The storage tests cover synchronization of newly created parent directories and failure/retry. They do not simulate physical power loss.

The native TLS configuration uses `DIGEST_NONE`, `ENCRYPTION_PADDING_NONE`, and the stable `postbox.receiver.v2` alias. Key purposes remain signing and verification. Native tests exercise actual AndroidKeyStore keys, TLS handshakes, certificate pin checks, pairing, capture transfer, and reload.

The integration checkpoint reviewed at `d42c1acf` had these results:

| Check | Observed result |
| --- | --- |
| `testDebugUnitTest` | 29 passed |
| `testReleaseUnitTest` | 29 passed |
| `lintRelease` | Zero errors, 12 Warning entries, and 2 Hint entries |
| `assembleDebug`, `assembleRelease`, `assembleDebugAndroidTest` | Passed |
| Strict repository Python suite | Coordinator reported 29 passed, including the six installer tests |
| `com.amine.postbox.core.PinnedTransferTest` | 14 passed, zero failures/errors/skips |
| `com.amine.postbox.PhoneCaptureTest` | 15 passed, zero failures/errors/skips |
| `com.amine.postbox.PostboxNavigationTest` | 8 passed before the final review regression was added |
| `com.amine.postbox.LauncherArtworkTest` | 3 passed, zero failures/errors/skips |

The documentation worker inspected the debug/release unit XML, lint XML, and preserved native XML. Instrumented results were retained separately under private `evidence/core-fix2`, `phone-final`, `navigation-final`, and `artwork-final`. Repeated runs of the same tests are not additional coverage.

## Final review fixes

Whole-branch review at `d42c1acf` found two material UI integration defects: returning focus to a distant library item, and stale receiver work closing a newer receive window. The coordinator reproduced both defects before the fixes.

The independent reviewer approved the scoped fix package after `d42c1acf`. The reviewed diff SHA-256 was `a9270e8abf2dbce3da8dc1d1dbf83a02c14318f9e9bfef807e7c90eb2778d021`. This approval covers source and test design. It does not replace runtime checks or native visual review.

| Affected class | Green result after fixes | Preserved private XML |
| --- | --- | --- |
| `com.amine.postbox.PostboxNavigationTest` | 9 passed, zero failures/errors/skips | `evidence/navigation-fixed` |
| `com.amine.postbox.ReceiverLifecycleTest` | 2 passed, zero failures/errors/skips | `evidence/receiver-fixed` |

The new navigation test moves through 20 items with the D-pad, opens item 15, returns to its visible focused card, and continues to item 16. The receiver tests control stale start and expiry job order, then verify the surviving listener through real pinned TLS pairing. The documentation worker inspected both green XML reports.

The repeated phone suite passed all 15 tests after the shared view model changes. Preserved private XML under `evidence/phone-fixed` reports zero failures, errors, or skips.

The runtime fixes were committed as `23f2e4a6`. The final combined debug/release unit, lint, and packaging command passed. The documentation worker inspected `evidence/integration-fixed`: 29 debug and 29 release JVM tests passed with zero failures, errors, or skips. Lint again reported zero errors, 12 Warning entries, and 2 Hint entries. Debug, release, and Android-test APK assembly passed. The unchanged native TLS and artwork classes retain their prior 14-test and 3-test green results.

## Phone contrast correction and installed release

The portrait phone capture exposed black inherited text on the dark phone background. The UI owner corrected the phone Surface/content color at `45819574`. Scoped code review approved the fix. TV code and logic remain unchanged from the preceding `23f2e4a6` checkpoint.

After this correction, 29 debug and 29 release JVM tests passed with zero failures, errors, or skips. Release lint reported zero errors, 12 Warning entries, and 2 Hint entries. Debug, release, and Android-test packaging passed. `PhoneCaptureTest` passed all 15 tests with zero failures, errors, or skips; its report is preserved under private `evidence/phone-contrast-fixed`.

The coordinator ran `tools/install-postbox.sh` with the explicit emulator serial. Release `0.1`, version code `1`, installed and opened on the API 34 TV emulator. No physical TV installation was performed.

| Artifact field | Observed value |
| --- | --- |
| Runtime source revision | `45819574` |
| Version | `0.1`, code `1` |
| Release APK size | 2,561,843 bytes, about 2.44 MiB |
| APK budget | 12,582,912 bytes; passed |
| Release APK SHA-256 | `d5e74f609e34f45617448a7d5cba00fb16ceb12e55bfb715323d72c3e96559b0` |
| Installed APK hash | Coordinator observed an exact match through `pm path` and `sha256sum` |

The documentation worker inspected private `evidence/release-installed.json` and independently measured the local release file's size and SHA-256. The installed-hash result comes from the coordinator's recorded device check.

TV captures at `23f2e4a6` cover the empty library, Saved, long text scrolling, fit-width image scrolling, link-only details, Cancel-focused deletion, archive/restore, and receiver focus. They remain applicable because the later change affects phone content colors only. Corrected portrait phone captures cover the editor, saved capture, and Connect TV. The captures use explicitly labeled generic fixtures and remain in ignored private evidence.

The independent UX reviewer approved the supplied native TV, corrected phone, and launcher states with no material visual findings. The private `native-ux-review.md` records each fresh capture filename and its reviewed state. The native phone Ready to send and Received states were not supplied for visual review. Their actual save/pair/send/receipt behavior is covered by the separate 15-test phone suite.

The installed Family Home artwork also passed independent review. The focused banner has a clear amber outline. The native title has strong contrast against the separate quiet-left, dark-lower-area background. The coordinator confirmed launch into Postbox and a return state with Postbox focused and the correct artwork. The [asset record](assets.md#native-launcher-review) states this review's exact scope.

## Commands and final results

The coordinator uses these commands with the configured JDK 17 and Android SDK paths:

```sh
apps/postbox/gradlew -p apps/postbox testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease assembleDebugAndroidTest --console=plain
CLEANYOURTV_STRICT=1 python3 -m unittest discover -s tests -v
```

Each instrumented class runs separately with this form:

```sh
ANDROID_SERIAL=emulator-5554 apps/postbox/gradlew -p apps/postbox connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.amine.postbox.PostboxNavigationTest' --console=plain
```

Replace the class with `com.amine.postbox.PhoneCaptureTest`, `com.amine.postbox.LauncherArtworkTest`, `com.amine.postbox.ReceiverLifecycleTest`, or `com.amine.postbox.core.PinnedTransferTest`. Inspect each XML class name and count before the next invocation replaces the live report.

| Final check | State |
| --- | --- |
| Phone suite after the final shared-view-model fixes | 15 passed, zero failures/errors/skips |
| Final debug/release unit suites | 29 passed per variant, zero failures/errors/skips |
| Final release lint | Zero errors, 12 Warning entries, and 2 Hint entries |
| Final debug/release/Android-test APK assembly | Passed at runtime revision `45819574` |
| Phone contrast correction and affected verification | Scoped code review approved; 15 phone tests passed after the correction |
| Final release APK size and SHA-256 | Recorded above; within the 12 MiB budget |
| Native TV captures | Captured at `23f2e4a6`; TV code unchanged by `45819574` |
| Corrected phone portrait captures | Editor, saved capture, and Connect TV captured |
| Launcher captures and artwork review | Independent UX approval; actual banner, separate background, title contrast, focus, and launch observed |
| Independent review of current native captures | Approved supplied TV, corrected phone, and launcher states; visual limits stated above |
| Final emulator release installation and installed version | `0.1`, code `1`, at `45819574`; installed hash matches |
| Final tested runtime and repository delivery | `45819574` delivered to `origin/main` and `origin/amine/post-review` |

The coordinator repeated the strict repository suite after the runtime work: 29 tests passed in 8.518 seconds. The changed-Markdown file-target scan found 50 local links and zero missing files. That scan does not establish anchor or network-link validity. `git diff --check` was clean.

Keep native captures and raw reports in ignored private evidence. Generic fixtures label their content as locally created examples. Screenshots of active pairing codes, personal content, device addresses, and tokens are excluded from Git.

## Runtime delivery

The coordinator fetched `origin` before integration. `origin/main` still pointed to `6dd11d73`, so the validated runtime required no merge-conflict repair. An atomic fast-forward push updated both `origin/main` and `origin/amine/post-review`.

A second fetch confirmed that the task HEAD and both remote refs equal `458195748515255db26eab72c5da20ba56b7a440`. The shared main checkout was preserved. These are coordinator-observed Git results. The final documentation-only commit follows this verified runtime checkpoint and does not change the installed app.

## Nonblocking review limits

The receiver uses its preferred port without an automatic alternate port. The shipped phone generates lowercase UUIDs; the protocol also accepts uppercase spellings. A retained sent phone copy requires Edit capture and a new save before another send. The independent reviewer recorded these limits as nonblocking for this release.

## Evidence limits

No physical TV installation or test was requested. Emulator evidence is sufficient for this delivery. It cannot establish a physical phone's camera app behavior, home Wi-Fi connectivity, or physical-TV appearance. API 34 emulator results do not exercise Android 17's local-network permission prompt.

Cloud APK builds and public releases remain disabled unless separately requested.
