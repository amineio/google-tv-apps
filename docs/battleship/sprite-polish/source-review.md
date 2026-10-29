# Independent source review — Battleship 1.5

Reviewed 2026-10-29 against base `bb99134a21c03cae20a791be318f2e12c4df39c2` and the current working tree. Scope: approved sprite design, task brief/report, implementation, tests, and audio preparation. No application edits, builds, device input, or commits were performed.

## Specification verdict

**Source behavior meets the reviewed requirements. One provenance correction is required before delivery.** Rendered quality and final device acceptance remain pending separate evidence.

- Shared outcome durations are MISS 1100ms, HIT 1500ms, SUNK 2500ms. Outgoing feedback holds the computer attack. Incoming feedback holds player fire and results. Both final-shot paths issue SUNK before the result cue.
- Pause/background cancel pending turns and clear transient impacts. Pause, Home, and mute send STOP. SoundPool stops tracked streams instead of pausing and later replaying them. Saved state excludes transient impacts.
- Both boards receive the same square size and coordinate gutter. Status, board titles, fleet entries, and warning slots have fixed heights. Source geometry preserves equal actual grids.
- The engine supplies only sunk enemy ships to the renderer. Ship length, class width, rotation, layer order, dim wreck opacity, and exact sunk-hull targeting match the design.
- Ocean rendering uses a static bitmap. Frame requests are limited to active finite impacts. The shader implementation is removed.
- The new audio manifest records current recordings, creators, CC0 source URLs, canonical hashes, preparation, durations, levels, and current output hashes. See finding R1 for the conflicting older generated manifest.

## Code quality verdict

**Runtime implementation: no material defect found. Audio tooling/provenance: changes required for R1.**

Timing and cancellation are compact and use the existing session generation guard. A stale completion can only clear its matching impact ID. Existing game rules, persistence, and redaction remain in their original layers. Tests cover outcome holds, final victory ordering, cancellation, stale completions, geometry, focus, and finite rendering. This review does not treat tests as proof of visual craft or listening quality.

## Finding R1 — P2: Make the generated audio records agree about current assets

Location: `apps/battleship/tools/generate_audio.py:362-366`, with the retained manifest content defined at lines 289-297.

The new generator replaces the impact, damage, and sunk bytes after generating the older `docs/battleship/audio-polish-assets.json`. It retains that older manifest with current resource paths, obsolete hashes, and the statement that no third-party audio license applies. It also says fire and splash remain unchanged. The current files now contain CC0 recordings. A direct SHA-256 comparison reports mismatches for impact, damage, and sunk in that manifest, even while `generate_audio.py --check` reports success. The new sprite-polish manifest is accurate, but the older generated record does not identify those entries as superseded historical data.

Required correction: either make the old record explicitly historical for replaced cues and link their current manifest, or retain only its still-current victory/defeat records. Update the generator as well, so regeneration preserves the correction. Keep rights and hashes unambiguous for every current resource path. Validate both deterministic regeneration and the hashes of entries claimed to describe current assets.

## Evidence and limits

- Independently ran `python3 apps/battleship/tools/generate_audio.py --check`: passed.
- Independently compared file hashes against both audio manifests: the new manifest matches all five battle cues; the older manifest mismatches impact, damage, and sunk, and matches victory/defeat.
- Read the current source pages: [Tiny Naval Battle Sounds Set](https://opengameart.org/content/tiny-naval-battle-sounds-set), [Ocean splash](https://opengameart.org/content/ocean-splash), and [40 CC0 water / splash / slime SFX](https://opengameart.org/content/40-cc0-water-splash-slime-sfx). Each lists CC0. The naval page documents the replacement explosion. This confirms published source terms, not an independent chain-of-title audit.
- Coordinator reports 50 debug unit tests passing and successful debug APK/test APK builds. I did not rerun them.
- The observed emulator log had reached connected tests but had no completion result at review time. No emulator pass is claimed here.
- Specialist capture review, a real-time clip, release checks, physical TV installation, and physical audio/readability evidence remain coordinator work.
- Delivery documentation also needs the current v1.5 behavior: the app README still describes moving water/shaders and five generated WAV files. This is a delivery follow-up, not a separate runtime defect.

## R1 follow-up — resolved

Independently reviewed the correction in `generate_audio.py`, both audio records, and the app README. The score record now contains only victory/defeat in its current `assets` section. Replaced battle recipes have a separate historical section, `historicalPath`, an explicit superseded status, and a link to the current battle manifest. Rights statements distinguish original synthesis from current CC0 recordings. The README describes version 1.5 and the correct regeneration scope.

Independent checks passed: deterministic `generate_audio.py --check`; all seven current asset hashes across both records; historical entries have no current `path`; the current-manifest link resolves. **R1 is resolved. Specification and code-quality source verdicts are approved for the reviewed scope, with no remaining material findings.**

Coordinator now reports all 16 round-one emulator tests passing. Rendered effect quality remains a separate ongoing design review; this follow-up makes no new visual or physical-device claim. Later effect changes require their own focused review.

## Final scoped source review — round two approved

Reviewed the current `ImpactEffects.kt`, the added warm-pixel regression assertion in `NativeWaterPresentationTest.kt`, removal of `command_battleship.webp`, and the trailing blank-line cleanup in `GameplayScreen.kt`. R1 remains resolved. No application edits, builds, or device commands were performed.

**Specification verdict: PASS for this source scope.** HIT heat holds through 650ms and fades by 1050ms. SUNK heat uses the shared global event time for the primary and delayed hull fires, holds through 1400ms, and fades by 1900ms. The scene retains the 1500ms/2500ms effect deadlines. Each fire draws smoke before its warm lobes. Cooling smoke continues toward the end of the sinking sequence. Splash fragments retain equal X/Y radii and radial motion. No gravity offset, hull translation, continuous scheduler, grid geometry change, or session timing change was introduced.

**Code-quality verdict: PASS. No material findings.** The effect changes remain local and finite. Global heat time prevents delayed fires from extending the sinking heat beyond its intended deadline. Color opacity and progression remain bounded for the production call paths. The image assertion checks HIT at 600ms and SUNK at 1000ms on both boards against each board's own baseline. It is a useful regression check for lost warm color; it does not establish overall visual quality. Searches found no remaining production or test reference to the deleted command-ship drawable. Whitespace cleanup has no behavioral effect.

Coordinator reports the warm-pixel test failed on the old HIT rendering before the correction. Coordinator also reports 16 final emulator tests, 50 debug unit tests, 50 release unit tests, lint/build checks, and an additional live full-match check passing. These results were not rerun by this reviewer. Specialist visual scores and physical-TV/audio acceptance remain separate evidence.
