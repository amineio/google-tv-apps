# Postbox asset record

Date: 2026-10-30

## Source and rights

The coordinator generated the concept references and launcher artwork with OpenAI ImageGen for this project. The coordinator supplied no external image references. Use generic subjects and labels. Keep personal screenshots and family photographs out of Git.

These are project-generated raster assets. No separate third-party artwork license was supplied. Repository code remains under its existing MIT license. The app uses the platform Sans font and bundles no font files.

The [full generation prompt manifest](concepts/image-prompts.json) was copied byte for byte from the coordinator's manifest. Source filenames and measured hashes establish which generated outputs were supplied. These records provide provenance without a blanket guarantee about artwork rights.

## Concept references

The [review record](concepts/README.md) records direction selection, corrections, approval, and generated sketch defects. The [reference manifest](concepts/references.json) contains dimensions, source and compressed hashes, and exact compression settings.

Concepts are design references only. They are not bundled interface screens. Keep layout, text, icons, focus, and QR rendering in native code.

## Launcher artwork

| Resource | Final size | Bytes | Composition |
| --- | --- | --- | --- |
| `postbox_icon.webp` | 512 by 512 | 5,804 | Central bookmark motif, inspected at 64 and 128 pixels |
| `postbox_banner.webp` | 640 by 360 | 20,640 | Readable Postbox title, inspected at 320 by 180 and 384 by 216 |
| `postbox_hero.webp` | 1920 by 1080 | 47,458 | Separate text-free art, quiet left half and dark lower quarter |

All three resources are in `apps/postbox/src/main/res/drawable-nodpi`. They total 73,902 bytes. Their complete SHA-256 values are:

```text
postbox_icon.webp    f4b7d0ef1df76ce8b0eb3ffb31f90c544efb7c2e9f5475ee4c9461d2e80835ec
postbox_banner.webp  ac08638e7808c18f22845234728bd098dde02d580273c336c3d904a70889c50a
postbox_hero.webp    cbc011294f5378a91206d93b24b356454f834e5dbe6ee3dc1249e1c21ef6e917
```

The [launcher manifest](launcher-assets.json) records each source filename, original dimensions and hash, final dimensions and hash, compression settings, preview files, and verbatim prompt. Sources came from the coordinator's generated-image directory `01a18a1e-524d-7810-9078-d97960db0b78`.

Asset preparation used ImageMagick to resize with preserved proportions and a centered crop for the small difference from the exact target ratio. It used `cwebp -m 6`, with quality 86 for the icon/banner and 82 for the background. No native control, title outside the banner, or focus outline was rasterized into the app interface.

The release APK budget is 12 MiB, or 12,582,912 bytes. The installed release at runtime revision `45819574` measures 2,561,843 bytes, about 2.44 MiB. The [validation record](validation-2026-10-30.md) records its SHA-256 and the matching installed APK hash.

The manifest must declare `android:icon`, `android:banner`, and `com.amine.familyhome.HERO_IMAGE` with an `android:resource` drawable reference. Family Home owns presentation, contrast, focus, and fallback. No launcher-specific Postbox code is needed.

## Native launcher review

The documentation worker inspected the [small icon and banner previews](concepts/README.md#launcher-size-previews). Their silhouette and title remain readable at the required sizes.

The independent UX reviewer approved the installed Family Home selection. The actual Postbox banner has a clear amber focus outline. The native Postbox title remains readable against the separate background. The background keeps its left half quiet and its lower area dark.

The coordinator pressed OK from the launcher and confirmed that `com.amine.postbox/.MainActivity` resumed. A fresh return capture kept Postbox focused with the correct banner and background. These results establish the reviewed selection, launch, and return. They do not claim an additional package-refresh test. All native screenshots remain in ignored private evidence.
