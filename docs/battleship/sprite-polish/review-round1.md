# Battleship v1.5 native design review — round 1

Reviewed native 1920×1080 captures from /tmp/battleship-v15-round1-captures: pause-resume; enemy miss E5 at 160/500 ms; enemy hit E5 at 200/600/1200 ms; enemy sink E5 at 300/1000/2000/2499 ms and completed; friendly sink E5 at 1000 ms. Also inspected current ImpactEffects.kt and the blue-background ship alpha composite.

Verdict: layout, readability, and ship presentation meet the 8/10 threshold. Effects require another pass. Motion and real-time pacing remain pending a clip.

| Category | Score | Evidence and limit |
|---|---:|---|
| Composition | 8.5/10 | Equal square grids, approximately 632 px each; aligned edges; stable status hierarchy; five-item fleet strip. Large friendly board is a clear improvement. |
| TV readability | 8.3/10 | Clear coordinates, strong cyan focus, readable marks over restrained static water, about 63 px cells. Fleet names fit but have little breathing room between long adjacent labels. Physical TV viewing distance is untested. |
| Ship craft | 8.3/10 | All five ship classes are distinct at native scale. Carrier deck, battleship turrets, cruiser detail, dark submarine, and small destroyer read correctly. No gray matte or visible rectangular glow appears in the native images. The earlier glow concern is resolved by actual alpha compositing evidence. |
| Effect clarity | 6.5/10 | Splash is truly circular overhead. Hit is recognizable at 200 ms, but nearly all visible heat is gone by 600 ms. Sinking has ignition at 300 ms and little convincing fire at 1000 ms on either board. Hull fade is visible across samples, but the requested burn is weak. |
| Interaction stability | 8/10 provisional | Board bounds match across reviewed states and pause/resume capture. Coordinator reports 16 passing device tests, 50 unit tests, and equal-grid bounds checks. I did not rerun or independently audit those tests. Startup motion, event sequencing, and real-time smoothness require a clip. |

## Required changes

1. Strengthen visible fire and separate hit/sink envelopes. Draw smoke behind fire. The current drawFire function draws smoke after warm lobes, which dulls ignition into brown/gray. Its heat also reaches zero at 940 ms regardless of duration. Keep regular-hit fire visibly warm through about 600 ms, then fade to smoke by 1000–1200 ms. For sunk ships, keep at least two localized fires clearly visible from roughly 350–1400 ms, then fade their heat by about 1900 ms while the hull dims through 2500 ms. Keep warm lobes on top of smoke and white-hot centers small. Preserve exact hull bounds and persistent hit markers. Acceptance captures: hit at 600 ms still reads as hot impact; sink at 1000 ms clearly reads as burning ship on both boards; sink at 2000 ms reads as cooling/submerging, not a vanished effect.

2. Make the circular splash look less like a radar symbol. The 500 ms capture shows three extremely clean concentric rings and almost no visible irregular foam. Keep equal X/Y radii, but break at least one ring into uneven arcs, vary ring opacity and stroke, and retain several bright foam fragments or droplets through 350–550 ms. Do not add vertical lift or elliptical perspective. Acceptance: a brief glance distinguishes this from a target reticle while the permanent miss ring remains identifiable after completion.

No additional layout or ship asset generation is needed for this round. Small roster-spacing improvements are optional and must not reduce grid size.

## Evidence caveats

The staged captures show LAST SHOT Ready while demonstrating injected effects. Some friendly fixtures also show fleet totals that do not represent the drawn staged fleet. Treat these as fixture states unless normal gameplay reproduces them; do not infer runtime feedback bugs from these images alone.

Still images establish phase appearance, not duration or animation quality. Review one uninterrupted clip containing fresh-game entry, a miss, hit, sunk sequence, and pause/resume before granting final interaction/effects acceptance. Physical TV readability and audio remain separate.
