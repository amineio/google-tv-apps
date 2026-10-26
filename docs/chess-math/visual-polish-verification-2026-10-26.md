# Visual polish evidence

The parent requested fresh TV screenshots, an independent design comparison, and artwork that matches the approved forest and ivory design.

The designer compared the original ImageGen concepts with fresh TV captures. The review identified flat pieces, repeated foliage, and a moving math prompt. The review is in [visual-review-2026-10-26.md](visual-review-2026-10-26.md).

ImageGen produced an original twelve-piece atlas and a quiet forest backdrop. The production assets use lossless WebP encoding. The source PNG files and prompts remain in [art-v2](art-v2/README.md). Native Android views retain all text, buttons, board cells, and semantic markers.

The renderer decodes each asset once. The combined decoded artwork uses approximately 12 MiB. The board preserves its size, remote controls, and piece descriptions. The original piece paths and forest gradient remain as fallbacks.

The math heading and equation retain their positions when hints appear. A separate outlined answer field improves the hierarchy. Reserved hint and feedback regions reduce layout movement.

## Initial integration evidence

Release unit tests, release lint, release assembly, and device-test assembly passed. Strict repository tests passed all eight cases. The first integrated release APK measured 1,882,366 bytes, below the existing 2 MiB limit.

Seven memory-only tests passed on the physical Chromecast with the release APK. They covered all twelve artwork tiles, equal board cells, stable math geometry, expression widths, remote navigation, parent controls, and promotions. The runner reported `OK (7 tests)` in 23.45 seconds. The fixtures preserve the actual saved game.

Independent code review identified one issue: a long reading pane had no remote scroll path. The final revision adds and tests this path. Final evidence follows after that revision.

The TV retains its existing high-contrast text setting and Clock overlay. Neither is part of the app artwork. No emulator was used.

## Final revision

The final release build, all 40 release unit tests, lint, and device-test compilation passed. Eight memory-only device tests passed on the physical Chromecast against the release APK in 24.773 seconds. The additional test uses an app-local font scale of 1.6 and a long subtraction hint. It proves remote entry, native scrolling, and return focus. The test restores the app configuration afterward. The TV configuration stays unchanged.

The independent code reviewer closed the scrolling finding after reviewing the fix. The final APK measures 1,883,646 bytes. Its SHA-256 is `a6cfb49da343ad05b32e39a497f31d24b185925a7b8894db8e86e45d828d24e6`. The existing 2 MiB APK limit stays unchanged.

The final four fixture screenshots are in [ux-review-v2](evidence/ux-review-v2/math-hint.png). They show the release APK with temporary memory sessions. The Start capture shows the actual saved-game entry screen. The app update preserved saved progress. The temporary test package was removed after the run.

The independent designer inspected the final TV frames and found no material visual blocker. The sculpted pieces, natural foliage, rounded focus, and answer field improve fidelity to the concepts. A smaller gap between short equations and the answer field remains an optional refinement.
