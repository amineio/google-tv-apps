# Audio and piece motion proposal

Status: approved by the parent on 2026-10-26. This document retains the original proposal. See [implementation evidence](audio-motion-verification-2026-10-26.md) for the delivered behavior and test limits.

## Recommendation

Add quiet wooden placement sounds and short piece slides within the existing native board. Add one soft sound when an arithmetic answer becomes correct. Keep all information visible when sound or motion is off.

Use the same restrained treatment for White and Black. A capture sounds slightly fuller than a normal move. Check uses a gentle two-note cue instead of an alarm. No music, speech, applause, particles, screen shake, or failure buzzer is proposed.

The purpose is to make moves easier to follow and give a clear confirmation of completed actions. This is a design aim. It is not evidence of better learning or suitability for every child.

## Observed implementation

The observations below come from the current source at `36d42846` on `amine/chess-math`.

- `ChessBoardView.bind()` replaces the board reference and invalidates the view immediately. It has no transition state or sound.
- `onDraw()` paints the board, pieces, legal destinations, selection, check, and focus together. The view explicitly uses `LAYER_TYPE_SOFTWARE`.
- `PiecePainter` loads one shared bitmap atlas. It reuses drawing objects and also has vector fallback pieces. Moving existing artwork needs no new piece assets.
- `SessionController.commit()` saves a snapshot before adopting the new state. Its successful callback calls `changed()` and then `scheduleComputer()` immediately.
- Selection, timing checkpoints, answer edits, pause, and moves all use revisions. A revision increase alone does not identify a move.
- `MainActivity.render()` usually rebinds an existing board. Promotion, result, and phase handoff can replace the whole screen. A slide added only to `bindBoard()` can disappear on those paths.
- Activity pause and window focus loss already pause the controller. The controller survives Activity recreation within the process. It restores saved sessions paused after process restart.
- Correct arithmetic answers commit automatically. The existing 1,200 ms foreground success delay is separate from sound and must stay separate.

`SessionControllerTest` covers delayed saves, failed saves, stale computer results, promotion boundaries, paused restoration, and success timing. `ChessGameTest` covers castling, en passant, promotions, check, and draws. These tests do not establish audio timing or smooth animation.

The three supplied screenshots show a large centered board, quiet colors, compact math, and four promotion choices. They also show strong system text outlines. The proposal preserves that composition, focus contrast, and the external top-right clock. No device was operated for this design.

## Approaches and tradeoffs

| Approach | Benefit | Cost or limit |
| --- | --- | --- |
| A. Canvas slides with native SoundPool, preferred | Reuses the board and atlas. No runtime dependency. One presentation path covers both players. | Needs explicit move events and a short gate before the computer reply. Software drawing needs a physical TV measurement. |
| B. Canvas board with one or two separate moving piece views | Android can translate the moving views while the board stays static. | More layering, clipping, promotion, accessibility, and focus work. Useful if measurements show board repainting is too slow. |
| C. Sound first, slides in a second delivery | Smallest first change and immediate sound review. | Does not deliver both requested improvements together. The slide sequencing work still follows. |

A is the smallest coherent solution. B is a measured fallback, not a reason to replace the board now. A new game engine, Compose rewrite, Media3, or continuous render loop adds no clear value for these effects.

For sequencing, a bounded acknowledgment before the computer action is the lean choice. A queue of two committed move snapshots is another option. That queue lets the computer save its reply during the child's slide. It also requires the screen, input, result, and phase to follow an older position until both slides finish. The queue is bounded, but its presentation rules are more complex. Prefer the acknowledgment unless measurements show a need to overlap computer work.

## Sound vocabulary

All values below are initial tuning proposals. Durations include the tail. Gains are relative playback multipliers after matching the assets by ear. They do not specify speaker loudness or a safe physical volume.

| Confirmed event | Sound and timing | Duration | Gain | Priority |
| --- | --- | --- | --- | --- |
| Normal chess move, either player | Soft wooden tap at landing | 90–120 ms | 0.45 | 1 |
| Capture, including en passant | Fuller wooden knock at landing, replacing the tap | 130–170 ms | 0.55 | 2 |
| Move that gives check | Gentle two-note cue at landing, replacing tap or capture sound | 200–260 ms | 0.45 | 3 |
| Newly correct math answer | Warm two-note confirmation when committed `Goed!` first appears | 220–300 ms | 0.50 | 3 |
| Castling or promotion without check | One normal move tap, not one sound per piece | As normal move | 0.45 | 1 |
| Wrong math input, illegal chess destination | Silent in the MVP. Keep existing editable input and text. | None | 0 | None |
| Selection, arrows, digit entry, Hint, delete, screen open, pause, save | No custom cue | None | 0 | None |

Checkmate can use the check cue when the final move leaves the king checked. Other results use only the final move sound. There is no win fanfare or defeat sound in this scope.

Use one cue per event. Higher priority selects the cue for a combined event, such as capture plus check. Do not queue stale sounds or overlap move and check cues. Use one SoundPool stream and an application rule that drops a lower-priority cue while a higher-priority cue plays. A new priority-3 cue can replace a lower cue. Normal play separates chess landings enough to avoid this conflict.

Keep standard Android menu sounds where they already work. Do not add a custom click to every button. Suppress the standard click on promotion confirmation and the digit that completes an answer if it otherwise doubles the custom cue. Handle the latter before Android plays the button click, using a pure answer preview, not a second state mutation. Preserve standard click feedback on other digits. Make sure this behavior works with remote activation and accessibility activation.

Do not sound partial answers or wrong entries. An optional future invalid-move cue can be a 60 ms dull tap at gain 0.20, priority 0, limited to once per 600 ms. It is excluded until the family requests it.

## Motion vocabulary

Use 180 ms for one or two squares, 220 ms for longer moves, and 240 ms for castling. These values apply at normal system animation scale. Use `AccelerateDecelerateInterpolator` for a gentle start and stop. It is a platform API available since API 1. [Android interpolator reference](https://developer.android.com/reference/android/view/animation/AccelerateDecelerateInterpolator)

Slide pieces in a straight line between square centers. Knights also slide directly above intervening pieces. An L-shaped travel path can imply intermediate stops or squares. Keep the existing piece size and orientation. Add no bounce, tilt, lift, or extra shadow. The artwork already has depth, and these effects add visual and rendering cost.

Paint the static position first and moving pieces last. Hide each mover from its final square until landing. Use the before-position to retain captured pieces during travel. Remove the captured piece exactly at landing. This is presentation only. The saved board already contains the completed move.

| Move type | Presentation |
| --- | --- |
| Ordinary capture | Keep the target piece visible until landing. Replace it with the moving piece on the final frame. |
| En passant | Keep the captured pawn on its actual square until landing. Remove that pawn, not a piece on the destination. |
| Castling | Slide king and rook together, with one duration and completion. Draw the king last if paths overlap. Play one landing cue. |
| Promotion | Keep the chooser unchanged until confirmation commits. Return to the board, slide the pawn, then replace it with the chosen piece at landing. No second flourish. |
| Capture promotion | Combine capture timing and pawn replacement. All four chosen pieces use the same transition. |
| Check | Preserve the previous check marker during travel. Show the final checked-king marker and final status at landing. Keep these markers static. |
| Result or math handoff | Show the final move on the board first. Show the existing result or handoff screen after landing. |

During motion, clear selection and legal destinations. Retain the honey focus at the last chosen square. Arrows can move focus and update its description. OK does not select or move while a slide is active. Do not queue OK presses for later. Back pauses immediately. Back and Home must never wait for landing.

The board description and turn prompt must match the position currently presented. Do not announce the computer's final square while its piece is still at the origin. At landing, synchronize piece descriptions, check, last-move markers, and the final turn prompt. This requires a small presentation snapshot, not direct reads of the new game from every drawing path.

## Durable facts and transient events

Keep moves, answers, time, phase, and atomic save behavior in `SessionController`. Animation progress and emitted sounds are temporary presentation state. Do not save a half-moved piece or replay a sound from stored history.

Create a small immutable move event after a successful save. Include the session generation, originating commit revision, ply count, UCI move, before-position, after-position, and affected squares. A ply is one move by one player. Include captured piece and square, optional rook movement, promotion result, and check/result metadata. Derive these facts from the accepted move and both positions. Do not infer capture solely from an occupied destination because en passant has an empty destination.

Create a math success event only for a committed false-to-true `solved` transition for the same exercise ID. Its identity contains the session generation, exercise ID, and commit revision. Do not compare feedback strings to detect events.

Assign the session generation in memory. Increment it on reset or game replacement. Use the event identity for consumption and completion. Unrelated focus or timing revisions must neither replay nor cancel the event. The event is not a new persisted queue or a schema of animation history.

A listener must register its presentation gate before `scheduleComputer()` can run. The controller then waits for the matching completion token before scheduling the reply. Start the computer worker after landing and a short 100 ms foreground gap. This is simpler than computing during the slide and holding a stale result. Keep existing epoch, revision, legal-move, and worker guards.

The computer uses the same save, event, slide, and landing sequence. Release White input after the Black landing. Normal animation time remains foreground elapsed time under the existing controller rules. It does not create a new clock pause or alter the five-minute boundary.

The durable phase can already be `TO_MATH` after the final Black save. Defer only the screen switch until that live slide completes. The controller's existing boundary still decides elapsed time and phase. Do not keep the phase active merely to play an animation. The same rule covers a terminal result.

Completion must be safe to call twice. Cancellation can invoke both animator cancellation and end callbacks. A stale completion must not schedule a computer reply after pause or reset. If a view detaches, a listener disappears, or a transition cannot start, clear its gate and show the latest committed position. Use a two-second elapsed-time deadline for a missing completion. Its action snaps to the final board and clears the matching gate. Respect system duration scaling within that limit. At extreme slow developer scales, the deadline deliberately ends the slide early to keep remote control responsive.

The acknowledgment changes only when the next computer action starts and when the next screen appears. It cannot decide whether the saved move happened. The final board remains valid if every presentation callback fails. Clear the gate through a posted controller fallback when no presenter is attached. Never require an animation callback to complete a save.

## Interruption and recovery

Pause, window focus loss, background, standby, recreation, screen replacement, and reset cancel pending slide work and sound callbacks. Stop active sound streams. Drop unsounded events and snap to the latest committed board. Never resume an interrupted sound or slide. Existing session logic keeps restoration paused.

If a save finishes after focus loss, apply the existing pending pause behavior. Do not emit effects. A failed save emits neither landing sound nor success sound. A durable retry restores paused and silent. The next explicit Resume continues the game from its committed facts.

The first bind after Activity recreation or process restoration is a silent baseline. Do not animate the last history entry. A restored solved exercise can still show its existing success delay without playing the success cue again.

Cancel the foreground gap as well as the slide. Resume on a Black turn schedules one owed reply using existing controller logic. Headless tests and absent listeners complete presentation immediately. Presentation failure must not become a lost turn or a save failure.

## Controls and platform choices

Add `Geluidseffecten` and `Stukbewegingen` as independent On/Off parent choices. Proposed defaults are On and On. Add a small `Geluid aan/uit` action to Pause for immediate mute. It uses the same persistent sound preference. No volume slider, audio mixer, or child-facing settings panel is needed.

Store presentation preferences separately from the game snapshot. They survive restart and progress reset. A failed preference write keeps the previous setting and shows the existing style of error feedback. Do not attach these choices to `MathConfig`.

Motion Off snaps immediately, with landing sound still available. Sound Off disables custom effects. Android menu sounds remain governed by the TV's system setting. Preserve the clock overlay, high-contrast text, focus outlines, and existing visible check and success feedback. Do not replace accessibility announcements with tones.

Use `ValueAnimator` and Canvas with no extra runtime dependency. `ValueAnimator` is available from API 11. `areAnimatorsEnabled()` is available from API 26, the app's current minimum. If it returns false, skip motion and release sequencing immediately. Also respect the explicit motion preference. [Android animator reference](https://developer.android.com/reference/android/animation/ValueAnimator)

Use `SoundPool.Builder`, `setAudioAttributes()`, and `setMaxStreams(1)`. These builder APIs exist from API 21. Set `USAGE_GAME` and `CONTENT_TYPE_SONIFICATION`. Preload four short resources and wait for `OnLoadCompleteListener` before using each. Skip an unavailable cue rather than playing it late. Stop tracked streams on interruption and release native resources with the owning Activity. Do not use `autoResume()` for these one-shot effects. [SoundPool builder](https://developer.android.com/reference/android/media/SoundPool.Builder), [SoundPool reference](https://developer.android.com/reference/android/media/SoundPool)

Use `AudioFocusRequest`, available from API 26. Request transient focus with ducking immediately before a cue, using the same audio attributes. If focus is denied, stay silent. Decline delayed gain. On focus loss, stop and discard the cue. Abandon focus after its known tail, using a generation-protected callback, and immediately on lifecycle loss. Audio focus alone does not need to pause the game. Do not retain audio focus between moves. This policy needs testing with Assistant and TV playback. [Audio focus guidance](https://developer.android.com/media/optimize/audio-focus), [AudioFocusRequest reference](https://developer.android.com/reference/android/media/AudioFocusRequest)

Package original or clearly licensed mono 16-bit PCM WAV files at 44.1 kHz in `res/raw`. Four clips totaling under one second need under 90 KB of PCM samples, before headers. Set an initial total file budget of 120 KB. Trim leading silence, soften sharp attacks, and leave no clipping. Record provenance and license in the existing notices. PCM/WAVE is a documented platform format, but Android notes that support can vary across device types. Confirm decoding on the Chromecast. [Supported formats](https://developer.android.com/media/platform/supported-formats)

Keep `LAYER_TYPE_SOFTWARE` for the first measured version. Avoid bitmap decoding, legal-move generation, layout, and new allocations in each animation frame. Reuse the existing atlas and cached geometry. If repainting fails the TV check, compare the hardware-backed Canvas path before choosing separate moving views. Actual hardware testing is necessary for custom drawing changes. [Hardware acceleration guidance](https://developer.android.com/topic/performance/hardware-accel)

## Implementation boundaries and acceptance

Keep changes within the chess-math module, its tests, four audio resources, licenses, and this documentation. Expected areas are a small effect player, immutable presentation events, a board transition, controller completion guards, Activity lifecycle wiring, and two parent preferences. Do not change chess rules, computer strength, arithmetic generation, success delay, session requirements, Home, or Clock.

Before calling the work complete, run the current unit tests, release lint/build, and instrumentation build. Add focused tests for event classification and consumption, special moves, save failure/retry, stale acknowledgments, instant completion, reset, and no-listener operation. Test the controller with delayed executors and a fake presentation completion, not real animation sleep.

On the physical Chromecast, make sure that all of these hold:

- A live child move and reply each slide once, in order. Repeated render, focus, save checkpoints, and OK do not repeat effects.
- Captures, en passant, both castles for both colors, and all promotions leave no duplicate or missing piece. Check, mate, and final phase handoff show the landing first.
- Pause, Home, standby, Assistant, focus loss, and recreation during save, travel, landing, and the reply gap stop effects. Resume never replays history or credits hidden time.
- Every math success sounds once after its save. Wrong and partial entries remain silent and editable. A failed or retried save stays silent. The 1,200 ms foreground success display still works.
- Mute, motion Off, system animation Off, system menu sounds, accessibility activation, and high-contrast text keep a clear usable flow. The clock stays visible.
- TV speakers and the family's usual output produce a quiet, clear landing without harsh clicks or audible double cues. Check the first sound after idle and audio-output latency.
- Short and long slides show no sustained stutter at 1080p. Record frame timing on the TV and inspect a video, rather than claiming smoothness from unit tests. Idle rendering stops when the transition ends.

These checks remain unperformed. Software tests cannot establish sound balance, viewing comfort, or child preference.

## Open choices

The main taste choice is wooden sounds versus a more musical game style. This proposal recommends wooden moves and only two gentle tonal cues. The other taste choice is whether effects start On or Off. The proposed defaults are On, with immediate mute from Pause.

The fixed two-second motion limit is a proposed engineering choice. It prevents a lost callback or extreme developer setting from blocking play. No visual prototype or audio asset was created in this design stage.
