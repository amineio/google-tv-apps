# AI-assisted missile assets

Research date: 2026-10-29. Scope: authoring strategy only. No app, asset, build, or device changes.

## Recommendation

Use Blender to render short effect sequences before release. Animate the missile and its path with native Kotlin code.
A sprite is an image that the game draws and moves. A sprite sheet stores several animation frames in one image.
This combination gives the fixed overhead board believable depth without a new game engine.
It also lets every shot land on its actual target instead of a destination baked into a video.
This is a suitability judgment, not a measured performance result.

The best first prototype uses one original missile, a launch flash, smoke sprites, and the existing hit and miss effects.
Use an overhead camera for every effect. Keep the boards fixed while the missile crosses the space between them.
Give the missile a small shadow, bright exhaust, and a smoke trail that widens and fades behind it.
Show height through size and shadow separation. Point the missile along its path as it descends.
Do not obscure the target coordinate or leave smoke over the next aiming position.
These details offer more value in this view than fluid simulation or rigid-body collision.

## What AI adds

AI can write a repeatable Blender Python script for the missile, camera, lighting, animation, and renders.
It can also propose visual references and help compare contact sheets.
An independent game UI/UX reviewer must inspect orientation, visibility, target accuracy, and consistency.

[MCP for Blender](https://github.com/ahujasid/mcp-for-blender) is a current, MIT-licensed community option for interactive AI authoring.
Its documented capabilities include object manipulation, materials, Python execution, renders, and animation-strip inspection.
It is not an official Blender Foundation tool.
Its optional model generators introduce separate services and costs. They are unnecessary for a small missile built from basic shapes.
For this task, direct scripts offer a smaller reproducible toolchain than installing a persistent MCP integration.
If that integration is used, disable telemetry completely and keep its socket local.

Do not treat a generated still image or a generated video as a production-ready animation.
Our recommendation is to use ImageGen for reference art, then control geometry, timing, and camera orientation in Blender.
Frame-to-frame consistency, transparent edges, exact landing coordinates, and restart behavior still require inspection.
This recommendation follows the integration needs of this app. It is not a claim that every AI animation tool fails these tasks.

Searches for recent X demos did not yield a verifiable missile workflow with reusable assets and Android performance evidence.
No X promotion supports the recommendation or its scores. The linked project documentation establishes the available authoring workflow.

## Authoring and packaging

1. Build an original low-detail missile from basic Blender shapes.
2. Lock an overhead camera and common lighting for all effects.
3. Render transparent image sequences for ignition, smoke, impact, and splash.
4. Pack each sequence into small sprite sheets with frame metadata.
5. Preserve the Blender version, script, seed, frame rate, dimensions, and source terms.
6. Inspect every frame over both light and dark water before compression.

Blender supports background rendering and frame ranges through its [documented command-line interface](https://docs.blender.org/manual/en/3.6/advanced/command_line/arguments.html).
Its [alpha documentation](https://docs.blender.org/manual/en/3.6/glossary/index.html) distinguishes straight alpha from premultiplied alpha.
That distinction matters for pale or dark fringes around smoke and fire.
The linked manual pages are version 3.6 references. Pin and test the selected installed Blender version before authoring.
Blender's [FAQ](https://www.blender.org/support/faq/) permits commercial use of original artwork and separates output ownership from Blender's GPL license.
Imported models and textures retain their own terms. Keep tool licensing separate from asset licensing.

Start with 24 to 32 frames at 256 by 256 pixels for one local effect.
A 32-frame RGBA sequence at that size requires 8 MiB of decoded pixel storage, before padding or renderer copies.
The same frame count at 512 by 512 requires 32 MiB.
These are arithmetic estimates. Compressed file size does not establish decoded memory use or playback speed.
Use a native smoke trail with a small shared texture instead of a full-screen sequence.
Measure the lowest-cost acceptable size before adding more frames or resolution.

## Tools and sound

| Candidate | Terms established by primary source | Fit score | Use in this app |
| --- | --- | --- | --- |
| Blender scripts and native sprites | Blender GPL, original artwork belongs to its creator | 9/10 | Best control of camera, consistency, and runtime cost |
| MCP for Blender | Repository MIT | 7/10 | Optional interactive authoring, no runtime role |
| [Kenney Particle Pack](https://kenney.nl/assets/particle-pack) | CC0, 80 textures at 512 by 512 | 7/10 | Fast smoke and spark prototypes, inspect style before use |
| [Tiny Naval Battle Sounds Set](https://opengameart.org/content/tiny-naval-battle-sounds-set) | CC0, author qubodup | 9/10 | Reuse the existing recorded launch and explosion masters |
| [jsfxr](https://github.com/chr15m/jsfxr) | Repository Unlicense | 4/10 | Useful for synthetic placeholders, weaker fit for realism |

Scores are research judgments for this fixed overhead game. They are not benchmark or listening scores.
The current provenance record already contains naval launch, explosion, splash, and bubble masters.
See `docs/battleship/sprite-polish/audio-assets.json` and `apps/battleship/tools/audio_sources/`.
Reuse these sources before searching for replacements. Do not add a new runtime synthesizer.

Synchronize ignition sound with visible launch, a short whoosh with flight, and the existing impact sound with contact.
Use restrained stereo movement between ship and target positions. Keep the result clear when the television mixes sound to mono.
Lower battle music during flight and impact. Retain the existing Sound control and interruption behavior.
For family use, avoid voices, injury sounds, excessive bass, or full-screen flashes.
Listening on speakers remains separate from successful file decoding or playback-state tests.

## Cost and evidence

Blender and the listed reusable packs require no asset purchase for this proposal.
Local rendering consumes authoring time, CPU/GPU time, and storage. It adds no inference service to the released app.
AI authoring consumes the selected assistant's allowance or API usage. This report does not assume that usage is free.
The main integration costs are animation state, interruption handling, sprite packaging, and design review.
Runtime cost comes from decoded textures, transparent overlap, and per-frame drawing.

Build one representative shot before producing a complete effect library.
Review launch, flight, miss, hit, pause, and background interruption on the emulator.
Measure frame timing, decoded memory, package growth, and target alignment at the actual board size.
Keep the current effects as a fallback until the prototype wins on visual quality and cost.
No render, performance benchmark, asset download, or audible-quality evaluation occurred during this research.
