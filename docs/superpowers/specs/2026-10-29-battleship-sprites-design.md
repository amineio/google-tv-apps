# Battleship sprite and effects design

The user authorizes autonomous selection, implementation, specialist review, and up to four rendered development rounds. Use the emulator during iteration. Install only the final version on the physical TV. This authorization replaces intermediate approval gates.

## Chosen approach

Use five transparent ImageGen ship masters with native Compose animation. Use a compressed still ocean image below both grids. Use CC0 recorded sound assets with original local sound layers, edited into short bundled cues. Retain SoundPool and the sound control. No game-engine rewrite, WebView, network permission, model runtime, or background music.

The alternative sprite-gen state-row pipeline offers precise manifests but adds frame curation. Blender offers geometric control but adds modeling work. Native animation of a consistent master best fits five static ships.

## Requirements

Both actual grids must be equal square sizes, at least 90% side ratio, with no bounds change during startup, shots, long feedback, pause, resume, or restart. Remove decorative gameplay header ship. Move five fleet health entries beneath the friendly board. Use one fixed-height compact status strip for target and last shot. Preserve selected target and last-result semantics, D-pad focus, repeated-shot rejection, persistence, redaction, and existing rules.

Use the same coordinate gutter on both boards. Keep labels at least 13sp and aim for grid pitch at least 30dp on the standard TV canvas. Grid below sprites, damage above sprites, effects above damage, cursor on top. Use a darkened still ocean drawable on each grid. Remove shader and continuous water work from active presentation.

Five distinct realistic overhead bow-up RGBA ship sprites must fit exact 5/4/3/3/2 cell footprints. Use a fixed length of cell span minus 0.15 cell. Normalize width by class to 0.80/0.68/0.55/0.44/0.42 cell for carrier/battleship/cruiser/submarine/destroyer. This is a deliberate game-art adaptation of source proportions, chosen to preserve every occupied cell and avoid overlap. Rotate by 90 degrees for horizontal ships. Never reveal enemy hulls before sinking. Preserve a dim sunk silhouette after the animation. Record prompts, sources, dimensions, preparation, hashes, and rights.

Use an overhead radial splash with equal X/Y ripple radii and no upward crown or gravity offsets. MISS lasts 1100ms, HIT lasts 1500ms, SUNK lasts 2500ms. SUNK shows ignition, distributed fire on the exact sunk hull, cooling and submersion, then dim wreck. No screen shake, no sideways hull movement. No later attack or results transition can truncate an active sequence. Pause, exit, mute and background stop extended audio. Continue restores marks without replay. Final-shot sinking audio precedes victory/defeat cues.

Retain local short sound files with quiet levels, no clipping, short fades, and a working mute control. Use one mixed sinking cue with blast, crackle and bubbles through the 2500ms timeline. The playback layer must stop old effects when pause/mute interrupts them. Public repository audio must allow redistribution of raw assets.

## Review and delivery

Use senior game UI/UX and art direction reviews of actual emulator captures, not only concepts. Review composition, TV readability, ship craft, effect clarity, and interaction stability out of ten. Aim for at least eight in every category. Use up to four development/review rounds, and fix material findings before final delivery. Scores are expert judgments, not user acceptance.

Test timing and cancellation with unit tests. Test stable layout bounds, D-pad actions, redaction, splash/hit/sunk frames, initial focus, pause/resume, dialogs, and saved-match return on the emulator. Capture staged effects plus a real-time clip. Run debug/release unit tests, lint, and builds. Use a fresh independent final reviewer. Install final APK on TV, then commit scoped app changes and push origin/main. Keep unrelated worktrees, Cast services, and personal data unchanged. Report physical review limits separately.
