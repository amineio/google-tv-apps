# Design review

Date: 2026-10-26. Scope: proposed design and static screen concepts only.

An independent GPT-6.1 Sol reviewer with medium reasoning read the design and inspected rendered chess, promotion, math, and pause concepts. The reviewer did not author the design. No app code, APK, emulator session, or physical TV test forms part of this review.

## Findings to resolve before design approval

| Finding | Required change |
| --- | --- |
| A process restart can lose a pending selection or promotion choice. | Save selection and promotion changes explicitly. |
| The generator recovery contract omits shuffled exercise state. | Save bag state, cursor, and random state. |
| Capture instructions refer only to destination dots. | Include capture destinations in the instruction. |
| White pieces have low contrast on light squares. | Require contrasting outlines in product vectors. Placeholder glyphs remain concept art. |
| Parent controls have only a text preview. | Add a concrete editable parent screen and remote focus rules. |
| Draw claims have no remote entry path. | Define current-position and intended-move claim actions. |
| An asynchronous save cannot guarantee a maximum five-second time loss. | Describe loss since the last acknowledged checkpoint. Test pause during an unfinished save. |
| The repository MIT license does not replace dependency licenses. | Include Apache 2.0 licenses and applicable NOTICE files in the APK and source distribution. |

The coordinator also requested that crossing ten starts disabled, all ten multiplication tables remain selectable, and parents can set the addition/subtraction number limit. Only tables 1, 2, 5, and 10 start enabled.

The review found the proposed child keypad and promotion layout workable as concepts. Actual TV focus, legibility, standby, chess dependency compatibility, and playing strength remain implementation acceptance work. General dead-position detection remains a stated dependency decision. This review does not establish complete chess adjudication or child suitability.

## Final disposition

The designer added the snapshot fields, checkpoint events, claim controls, notice requirements, and corrected time-loss wording. Concept 7 now shows editable parent settings. The independent reviewer read those revisions and inspected the rendered parent screen. The reviewer found no remaining design approval blocker. Human approval remains pending.

The coordinator parsed and rendered all seven SVG files and made sure that every local Markdown link resolves. These checks concern design artifacts only. Product tests and Android builds remain pending implementation.
