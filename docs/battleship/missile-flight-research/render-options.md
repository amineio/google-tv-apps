# Missile rendering options

Research date: 2026-10-29. Scope: evaluation only. No app implementation, build, installation, or device input.

Use a native Compose flight overlay with small, pre-rendered Blender effects. Keep the missile path, timing, target, and sound events in Kotlin. This offers the best expected visual return for this fixed overhead game. It requires no new game engine or web runtime.

## Evidence and limits

The checkout is `/Users/amine/Developer/google-tv-apps`, branch `main`, revision `b0298413ed3bda9f43b8f14a103c8a211eed1f55`. An unrelated edit to `apps/family-home/README.md` remains untouched. I inspected the root README, Battleship guide, Kotlin TV guide, and current rendering, session, and audio code.

The rankings below are engineering judgments for this app. They are not measured frame rates, asset sizes, development estimates, or device benchmarks. No candidate runs on the TV during this research. Browser search did not establish a reproducible, AI-authored missile demo with Android TV performance evidence. A social video establishes appearance at its recorded settings, not compatibility, runtime cost, or asset rights.

## Ranked options

Scores run from 1 to 5, where 5 is best. Integration cost means ease of integration, so a higher score means less work. Weights are visual impact 30%, integration 25%, TV performance 25%, asset iteration 10%, and license/offline fit 10%. The weighted score equals the sum of each score multiplied by its weight.

| Rank | Option | Visual | Integration | TV performance | Asset iteration | License/offline | Weighted / 5 |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | Compose path and particles, small Blender effect sprites | 4 | 5 | 5 | 4 | 5 | 4.60 |
| 2 | Compose path and fully procedural effects | 3 | 5 | 5 | 5 | 5 | 4.40 |
| 3 | Large pre-rendered Blender sequences | 4 | 3 | 4 | 2 | 5 | 3.65 |
| 4 | Native Filament 3D | 5 | 2 | 3 | 3 | 5 | 3.55 |
| 5 | Three.js in a WebView | 4 | 2 | 2 | 4 | 5 | 3.10 |
| 6 | Unity as a Library | 5 | 1 | 2 | 3 | 2 | 2.75 |

Visual scores refer to the present overhead board. Full 3D gains more value with camera movement, lighting changes, or a new scene. Performance scores reflect expected integration complexity and workload. A prototype must establish actual performance.

### 1. Native path with small rendered effects

Compose supports Canvas drawing, image drawing, coordinate transforms, and cached drawing objects. Its official examples fit a single overlay above the existing two boards. [Android graphics documentation](https://developer.android.com/develop/ui/compose/graphics/draw/overview).

Use Kotlin for missile movement, orientation, smoke positions, shadows, and event timing. Use a few transparent textures for plume, smoke, and impact detail. A sprite atlas stores several frames in one image. Sample its frames at runtime rather than decode an image every frame.

This is 2.5D: flat rendering with depth cues. It can look convincing from the fixed overhead camera. It cannot provide correct views from arbitrary cameras, volumetric lighting, or interactive smoke simulation. The score assumes that assets match the overhead camera and current art.

### 2. Fully procedural Compose fallback

Use the same path and timeline, with native gradients, circles, and a small fixed particle pool. This avoids an external authoring pipeline and keeps the first prototype cheap. Smoke tends to look cleaner and more graphic than rendered smoke. Retain this fallback if atlas detail provides little visible gain at board scale.

### 3. Larger Blender sequences

Blender can render animations to separate images. Its manual recommends lossless image sequences for recoverable rendering. Use transparent output and pack selected frames into an atlas. Rendering is an authoring step on the development computer, not work for the TV. [Blender animation output](https://docs.blender.org/manual/id/4.0/render/output/animation.html).

Pre-render only local effects. A full-screen movie cannot adapt its launch and landing points to arbitrary ship and target cells. Large sequences also cost decoded memory and make color or timing changes slower. Render with an overhead camera and consistent light direction. Inspect transparency edges over the actual ocean image.

Blender itself uses the GPL. Blender states that rendered artwork belongs to its creator. Third-party models, textures, and add-ons retain their own terms. The app can bundle rendered original artwork without bundling Blender. [Blender license and artwork terms](https://www.blender.org/support/faq/).

### 4. Filament native 3D

Filament provides native Android rendering, physically based materials, glTF loading, lighting, and shadows under Apache-2.0. Its repository includes Android samples and current Maven instructions. Material tools and runtime versions must match. These features make Filament the preferred escalation path if we later need real camera movement. [Filament repository](https://github.com/google/filament).

Filament renders scenes. It does not supply a complete missile game or solve the smoke simulation and gameplay timeline for us. We must manage the rendering surface, lifecycle, assets, camera alignment, transparency, and Compose layering. That cost is hard to justify for one short effect.

### 5. Three.js in a WebView

Three.js is a JavaScript 3D library under MIT. Its current repository provides browser rendering, examples, and migration guidance. The name is Three.js, rather than 3D.js. [Three.js repository](https://github.com/mrdoob/three.js).

An offline bundle is possible in principle. This app then needs a WebView, JavaScript communication, a second frame loop, and matching lifecycle and audio behavior. Native board coordinates must cross that boundary. Browser demonstrations do not establish TV WebView GPU compatibility or transparent overlay performance. Those are prototype questions. This route is useful for an existing web game, but offers poor integration value here.

### 6. Unity integration

Unity offers real 3D authoring and an Android library integration. Its documented standard library mode supports only full-screen rendering. Unloading also retains runtime memory. This conflicts with the requirement for a small effect above native Compose boards. [Unity Android library documentation](https://docs.unity.com/en-us/engine/6000.0/manual/platform-specific/android/developing/unityasa-library).

Unity uses product licensing rather than a permissive open-source engine license. Specific eligibility and commercial terms need a separate review before adoption. No pricing assumption enters this ranking. A complete game migration can justify Unity. A missile overlay does not.

## Concrete effect pipeline

The first prototype must preserve both square boards and use one scene overlay above them. `BoardGeometry.center()` supplies cell centers. Convert both board bounds into the overlay coordinate system once layout completes. The present impact layer clips to one board. A cross-board flight cannot remain inside that layer.

1. Choose a muzzle point on a surviving friendly ship. Use a stable ship-relative anchor rather than its board cell center. Hide enemy launch origins so that their trails cannot expose hidden ship positions. An incoming missile can enter from the opponent scene edge.
2. Show a brief ignition flash and expanding launch smoke at the muzzle. Play a short launch sound. Use a local glow rather than a full-screen flash.
3. Move the missile toward the target over a proposed 600 to 850 milliseconds. Use a path tangent to rotate the missile. Use a fixed height curve for scale, shadow separation, and trail width.
4. Emit smoke along distance traveled. Let each puff grow, drift slightly, and fade. Avoid a continuous opaque white line across the boards.
5. Keep a faint shadow on the projected water path. Reduce shadow separation near impact. A fixed overhead view uses scale and shadow to imply altitude.
6. Start the existing miss, hit, or sinking presentation only when the missile reaches the target. Trigger its matching sound at that same event. A miss needs an overhead splash and spreading rings. A hit needs a short flash, debris, smoke, then the existing fire.
7. Keep the existing full sinking duration after arrival. Begin the next turn only after flight plus impact finishes. Release the overlay and active effects when the app pauses or leaves the foreground.

Use 600 to 850 milliseconds for the minimum prototype. Compare an optional cinematic flight of 900 to 1,300 milliseconds separately. Neither range is measured, and no timing decision is accepted yet. Avoid slow flight on every shot. A 700-millisecond flight adds about 70 seconds across 100 shots.

The two boards represent separate tactical spaces. A visible path across their gap is a cinematic transition, not a physical map. Use a restrained curved path and altitude cues. A large screen-Y parabola can make an overhead missile look incorrectly tilted, repeating the earlier splash problem.

For physically plausible motion, use a time-based position and consistent velocity, acceleration, orientation, and trail aging. A powered guided missile does not follow a simple unpowered ballistic parabola. No collision solver is needed: the game already supplies the exact destination and result. True rigid-body or fluid simulation adds little value to this predetermined event.

## Integration work that the prototype must include

`GameSession.fire()` currently commits the attack, emits FIRE, creates `activeImpact`, and emits the impact sound immediately. The computer path creates its impact immediately too. Merely adding a rocket above that sequence makes the explosion happen before arrival.

Add an explicit flight and impact presentation timeline. Keep accepted game results deterministic and save them once. Defer visible hit markers, sunk hull revelation, feedback, and impact audio until arrival. A presentation snapshot must prevent already-committed game state from revealing the result during flight.

Use one shot identifier and one time source for movement, audio, impact, and turn completion. Retain the current cancellation generation rule. Pause, background, restart, and return home must cancel pending visual and sound events. Continue must show the committed saved state without replaying the old missile. Reject repeated fire while resolving.

Keep launch and impact sounds in the existing SoundPool. It supports short preloaded sounds, stream limits, and separate left/right volume. A conservative pan can follow launch and landing sides. Keep music reduction and Sound mute behavior. [Android SoundPool reference](https://developer.android.com/reference/android/media/SoundPool).

The current pool permits three streams. Budget launch, flight, and impact overlap deliberately. Stop the flight layer at arrival instead of accumulating loops. SoundPool stream state does not establish audible quality or timing on the TV.

## Asset and performance targets

Start with one small missile image, one smoke texture, and compact plume and impact sequences. Render the missile at the actual board scale before increasing resolution. A few convincing depth cues matter more than distant surface detail.

An example atlas with 24 frames of 128 by 128 pixels occupies 1.5 MiB as uncompressed RGBA pixels. Packing those frames in a 1024 by 512 image reserves 2 MiB. These are arithmetic examples, not measured Android allocations. WebP reduces bundled bytes, but does not imply equal decoded-memory savings.

Set candidate caps of one active missile and 24 to 40 smoke puffs. Preload textures, reuse particles, cache brushes and paths, and allocate no bitmaps during frames. Measure actual frame timing and memory before accepting those caps. Keep splash and fire in the same overhead plane.

AI can accelerate asset concepts, Blender scripts, and source code. It does not establish consecutive-frame consistency, correct geometry, alpha quality, or platform performance. Prefer a reproducible Blender scene and script for animation. Use ImageGen for references or individual textures, then inspect the final native capture independently.

## Reusable examples and next decision

Reuse the Compose graphics snippets linked from the Android documentation for native image transforms. Reuse Filament Android samples only for a separate 3D feasibility spike. Three.js official examples can help compare the visual vocabulary, but avoid importing a browser engine for reference alone. Check each example asset license before copying it.

The lowest-cost next step is a short native prototype with existing impact effects and placeholder smoke. Judge launch origin, trajectory, arrival timing, board readability, and interruption behavior first. Then compare native smoke against one Blender-rendered atlas in the same scene. Keep the atlas only if the independent game UI/UX reviewer finds a clear visual gain.

A later implementation needs focused timeline tests, interruption tests, emulator captures, frame timing, release lint, and packaging. It does not need a routine physical TV installation. Audio quality and actual TV frame pacing remain separate observations.
