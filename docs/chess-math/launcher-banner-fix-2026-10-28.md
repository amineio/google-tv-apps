# Chess 0.12 launcher banner

The rectangular launcher banner now fills the bounds that the launcher supplies.
The existing 320 × 180 artwork, title, colors, and source image remain unchanged.

The previous layer wrapper fixed its bitmap child at 160 × 90 dp.
At a larger size, the child did not fill the drawable bounds and left transparent space.
The replacement bitmap drawable uses `android:gravity="fill"` without a fixed child size.
Its intrinsic shape remains 16:9. It also works with Family Home's source-coordinate scaling.

The regression test loads the banner through PackageManager, as a launcher does.
It tests corner and edge opacity at 320 × 180, 384 × 216, and 640 × 360 pixels.
The old resource failed at 384 × 216 with a transparent bottom corner. The corrected resource passes.

Validation for version 0.12, code 12:

- 129 Chess unit tests pass.
- All eight artwork tests pass on the Google TV emulator.
- Android lint reports zero errors and 23 existing warnings.
- Release, debug, and instrumented test APK builds pass.
- A fresh independent review found no material issues.

The release installed successfully on the physical Chromecast, replacing version 0.9 without clearing app data.
The TV reports version 0.12 and code 12. The installed APK hash matches the tested local release.
SHA-256: `63f4f01c2f67774921dab6051d5645a381a2ed9169887f2754e23a8149555b58`.

Physical screenshots and routing state remain in the ignored backup directory because the launcher contains family profile data.
Only the Chess app was installed for this update. Family Home has its own version and separate delivery.
