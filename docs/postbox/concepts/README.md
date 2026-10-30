# Postbox concept review

The user authorized development and delegated visual approval to an independent UI/UX reviewer. The coordinator reported acceptance of the corrected concepts on 2026-10-30. This record preserves design references; it does not claim that the native app has passed review.

## Review rounds

1. **Direction selection:** Direction A established warm ink, sage, cream, and apricot. Controls, labels, QR codes, and focus remain native.
2. **Library and phone corrections:** The library distinguished link-only captures from captured text/images. The phone separated **Save on phone** from **Send to TV**. The corrected library image is a flow sketch with the wrong aspect for a literal TV layout.
3. **Reader and archive review:** The reviewer required a usable reader, archive/restore, confirmation before deletion, and predictable Back behavior. A generated reader sketch shows double focus; that defect is not approved for native implementation.
4. **Final corrections and approval:** Pairing and image-reader references were corrected. The upper empty-state composition was approved. The lower composition is not approved. The coordinator reported independent acceptance after these corrections.

## Accepted references and limits

| Reference | Use and limit |
| --- | --- |
| [Direction A](direction-a.webp) | Selected visual language. Do not copy generated controls or labels as raster UI. |
| [Library and link-only flow](library-link-flow.webp) | Corrected information flow. Wrong generated aspect; use a native 16:9 TV composition. |
| [Phone capture](phone-capture.webp) | Approved capture direction. Keep Save on phone separate from Send to TV. |
| [Reader and archive](reader-archive.webp) | Flow reference. Do not reproduce its double-focus defect. |
| [Final pairing](pairing-final.webp) | Accepted pairing direction. Generate the real QR in native code from the current receiver session. |
| [Final image reader](image-reader-final.webp) | Accepted image direction. Keep tall screenshots readable through fit-width scrolling. |
| [Empty state, upper composition only](empty-upper-reference.webp) | Use only the upper composition. The reference retains the full generated sheet to preserve provenance. |

## Native acceptance criteria

- Keep essential content inside six-percent margins on every TV edge.
- Show exactly one actual focus indicator. Selection and focus must remain visually distinct.
- Use **Saved**, **Archive**, and **Add posts** on TV.
- In the reader, Left enters the body or image. Up and Down scroll. Right returns to actions without resetting scroll.
- Fit tall enlarged screenshots to the available width. Do not shrink the whole screenshot into a tiny centered image.
- Hide **Next image** when only one image exists.
- Keep capture type and **Link only** honest. Do not invent fetched text or thumbnails.
- Keep empty-library examples labeled and outside the real library.
- Keep controls clear of the separate Clock overlay.

The coordinator supplied the original generated PNGs. These generic references contain no family photographs or personal account captures. They were compressed to WebP at quality 78, with the longest edge limited to 1,280 pixels. The seven copies total 394,136 bytes.

[references.json](references.json) records original filenames, dimensions, SHA-256 hashes, derivative hashes, compression, review limits, and the matching verbatim prompts. [image-prompts.json](image-prompts.json) is a byte-identical copy of the coordinator's full prompt manifest. It includes historical exploration prompts as well as the accepted corrections. Historical instructions are not current native requirements.

## Launcher size previews

The documentation worker inspected the prepared icon at [64 pixels](launcher-previews/icon-64x64.webp) and [128 pixels](launcher-previews/icon-128x128.webp). The bookmark silhouette remains clear. The prepared banner title remains readable at [320 by 180](launcher-previews/banner-320x180.webp) and [384 by 216](launcher-previews/banner-384x216.webp).

These are direct resource previews. They do not establish installed launcher crop, focus, background rendering, or package-refresh behavior.
