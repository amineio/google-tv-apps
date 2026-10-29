# Battleship v1.5 native design review — round 2

Reviewed native 1920×1080 captures in /tmp/battleship-v15-round2-captures. Samples: enemy miss E5 at 160/500 ms, edge miss A1 at 500 ms; enemy hit E5 at 200/600/1200 ms; enemy sink E5 at 300/1000/2000/2499 ms and completed; friendly sink E5 at 1000 ms; pause-resume. Read-only visual review; no code changes, builds, or device input.

Verdict: the two round-1 visual findings are resolved at the sampled phases. No further implementation change is required from these still images. All five categories meet the 8/10 target within the evidence limits below.

| Category | Score | Evidence |
|---|---:|---|
| Composition | 8.5/10 | Equal square grids retain aligned edges, generous board area, clear status hierarchy, and the five-item fleet strip. |
| TV readability | 8.3/10 | Coordinates, cyan focus, permanent markers, and names remain readable over the static ocean. Long roster names remain somewhat close but do not overlap. |
| Ship craft | 8.3/10 | Five distinct overhead classes, clean transparency, coherent materials, and controlled cell widths. No further asset generation needed. |
| Effect clarity | 8.1/10 for sampled appearance | Hit has bright warm lobes at 600 ms and cools to the correct marker at 1200 ms. Enemy and friendly sinking both show two clear localized fires at 1000 ms. At 2000 ms the hull has dimmed, fires have cooled, and light foam remains. The end state retains wreck information and hit markers. Splash has irregular broken arcs while preserving circular overhead geometry. |
| Interaction stability | 8/10 provisional | Board and status bounds remain consistent across reviewed states. Coordinator reports 50 debug and 50 release unit tests, 16 device tests, release lint and builds passing. Those test results were not independently rerun here. Real-time entry and interaction remain pending a clip. |

## Resolution of prior findings

- Fire visibility: resolved in the sample images. The flame remains above the smoke, and the sinking effect visibly continues past the initial explosion. The hottest regions remain local to the hit cells; they do not obscure unrelated cells.
- Splash appearance: resolved to the 8/10 threshold. Broken rings and foam reduce the previous radar-symbol appearance. Equal X/Y geometry and radial spread preserve the true overhead view. The effect remains restrained enough to preserve target readability.

The effects remain stylized Canvas work rather than photographic explosions. The current design provides clear impact feedback and matches the clean game interface. A higher score would require more organic motion and particle variation, which cannot be judged from these samples and is not required to close the current still-image findings.

## Remaining acceptance evidence

Review an uninterrupted normal-session clip before final motion acceptance. It must show fresh-game entry with no later board squeeze, a complete miss, a complete hit, the full 2–3-second sunk presentation, and pause/resume. Confirm that the next attack or result overlay does not cut off sinking; flame brightness changes smoothly; ripple arcs expand rather than appear as a fixed reticle; and targeting remains stable.

Do not turn the sample timestamps into a claim about elapsed wall-clock duration. The staged LAST SHOT Ready and staged fleet totals are fixture data, not independently observed runtime bugs. Physical TV viewing distance, hardware responsiveness, and audio acceptance remain separate from emulator visual evidence.

## Actual-session sequence review

Reviewed /tmp/battleship-v15-live-match.mp4 through extracted full-size frames and ordered quarter-second contact sheets. The recording is 1280×720 and silent. This is a sequence/timeline review, not direct continuous playback or an audio review.

Evidence examined:
- Full frames at 20, 30, 40, 45, 60, and 70 seconds, plus the supplied overall contact sheet.
- Quarter-second sequence from 13.0–16.75 seconds: menu/confirmation, gameplay entry, pause, resume, and aiming.
- Quarter-second sequence from 28.5–33.25 seconds: first enemy carrier sinking, incoming miss, and resumed aiming.
- Quarter-second sequence from 71.0–76.75 seconds: final sinking, victory, rematch, pause, and resume.

Findings:
1. Board dimensions and positions remain fixed across the sampled gameplay entry, effects, long feedback, rematch, and pause/resume states. No later board squeeze appears in those samples.
2. Real last-shot feedback is populated correctly in the reviewed frames: YOUR SHOT Hit, YOUR SHOT Carrier sunk, YOUR SHOT Battleship sunk, and INCOMING Miss. The full Battleship sunk text at 45 seconds fits with clear separation from the target and turn labels. The staged Ready issue is not present here.
3. The first carrier sink begins around 29.0 seconds. Ignition spreads to three hull locations, persists as visible fire through subsequent samples, then dims to the wreck and white hit markers. The next incoming attack appears around 31.5 seconds. This supports a complete approximately 2.5-second presentation without replacement by the next attack.
4. The final destroyer sink begins around 71.5 seconds. The board remains visible during the full burn/fade sequence. The victory result screen appears around 74.25 seconds, after the effect. The result screen does not cut off the final sinking.
5. Miss samples show radial expansion and fade, rather than a permanently drawn target reticle. Fire and wreck appearance change across the ordered frames. No discontinuous board movement or misplaced effect appears in the examined samples.
6. The later sequence shows a new fleet after Play Again, then pause and resume with the same board dimensions and focus location. The coordinator's reported passing actual-session D-pad test supplies separate evidence for directional input and completion of the match.

Final category scores for this emulator evidence: composition 8.5/10; readability 8.3/10; ship craft 8.3/10; effect clarity 8.1/10; interaction stability 8/10. No material visual finding remains open in the examined native captures and timeline samples.

Limits: quarter-second sampling cannot establish per-frame smoothness, frame rate, short input latency, or absence of every transient artifact. Report the observed sequences and approximate timing, not a continuous-playback judgment. The silent recording does not establish audio quality or volume. Physical TV readability and remote hardware remain separate from this emulator acceptance. The recording includes emulator Google TV setup screens outside the reviewed app sequences; these do not alter the app visual findings.
