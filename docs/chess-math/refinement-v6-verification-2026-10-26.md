# Version 0.6 delivery record

Date: 2026-10-26. Device: Chromecast, Android API 34, 1920 × 1080.

## Behavior

New sessions start with five foreground minutes of math. The existing three-answer minimum and current-exercise completion remain. Correct answers keep the 1.2-second feedback period. Math then starts a 60-second break. The centered countdown advances only while the app is active. Chess starts automatically when the countdown ends. Chess has no time cutoff. A completed chess game offers Start maths for the next cycle.

Existing schema 1 saves retain their board and exercise. The former chess cutoff returns to the unfinished board. A completed math handoff becomes a paused break. Schema 2 stores the new break phase and its elapsed time.

Settings opens with one press from Pause. Language offers Dutch and English, independently of progress. New chess game starts directly. Full progress reset retains its confirmation.

## Visuals and sound

Honey and sage cell backgrounds replace destination dots and capture circles. One square outline identifies remote focus. See the highlight concept record for exact colors and user decisions.

The v4 piece atlas uses thin dark contours and 30–37% taller source pieces within the same 2172 × 724 canvas. Lossless WebP preserves the source pixels. Bilinear filtering and a mipmap renderer hint support downsampling. The TV screenshots show the new artwork, but do not prove that every edge artifact is resolved. Two earlier ImageGen cleanup attempts remain rejected in art-v3.

Piece slides last 320, 380, or 420 ms. Quintic smootherstep gives zero velocity and acceleration at both endpoints. Pause, focus loss, and stale callback handling remain intact.

The four original sound cues use rounded wood resonances, a warm check notice, and a cheerful four-note math phrase. They do not copy Duolingo recordings. Device tests establish decoding, SoundPool playback, and audio focus behavior. Speaker quality and subjective loudness still need listening feedback.

## Evidence

The release unit suite passed 74 tests. Release lint, release assembly, and Android test assembly passed. Repository tests passed all 8 strict tests.

The final release passed 30 memory-only tests on the real TV in 61.092 seconds. Coverage includes sound playback, move animation, cancellation, artwork, geometry, math entry, direct settings, direct restart, language changes, and a fake-clock break countdown with pause and automatic chess start. The tests preserve actual saved family progress. The countdown test advances a fake clock rather than waiting one physical minute.

An earlier 29-test device run found one test synchronization failure: Continue was pressed before the old Activity save finished. The helper now waits for save completion and Activity destruction. The final run passed.

An independent Android review found no blocking issues. Language tests cover Activity relaunch plus separate storage reload unit tests. They do not establish a full device process-death test for Android language preferences.

Actual screenshots are in evidence/refinement-v6/evidence. The break image shows the final second under the fake clock. The English math image contains the retained exercise, input, and translated hint. The Clock overlay and thick text outlines come from the TV environment.

Release APK: 2486029 bytes, below the 2,621,440-byte limit.
SHA-256: `f9c22999756381d15fc9bcfde88e33ef1c7ca71d4ae6ec4859488df3413a143e`.
