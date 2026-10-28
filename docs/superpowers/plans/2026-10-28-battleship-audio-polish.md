# Battleship audio and feedback polish

The user requests stronger hit sounds, enthusiastic military victory audio, a defeat cue, and less gameplay narration.
The existing approved offline game remains the design authority. This pass changes audio and compact feedback only.

## Design

Use original locally synthesized sound assets. A layered impact marks a successful attack.
A distinct lower hull impact marks damage to the friendly fleet.
A short brass fanfare with percussion marks victory. A descending bugle phrase marks defeat.
All cues remain bundled, short, and controlled by the existing sound preference.
Keep the current SoundPool implementation. Adjust gains per cue instead of increasing every sound.

Keep the right-side target coordinate, friendly board, and fleet status.
Replace narration and repeated turn instructions with a compact Hit, Miss, or named sunk-ship result.
Keep turn status in the header. Keep repeated-shot and storage-error feedback visible.
The gameplay layout and artwork remain unchanged apart from the removed text panel.

## Execution

- [ ] Add regression tests for concise feedback, friendly damage, completion cues, and mute behavior.
- [ ] Generate and inspect reproducible local sound assets. Record score, synthesis parameters, duration, levels, and rights.
- [ ] Apply compact feedback and sound routing. Increment the app to version 1.1, code 2.
- [ ] Operate debug/release unit tests, lint, builds, repository tests, and focused emulator tests.
- [ ] Obtain independent review. Inspect the native game capture and sound previews.
- [ ] Install the tested update when the TV is available. Preserve the saved match and family controls.
- [ ] Commit the installed app changes and merge the tracking PR into `origin/main`.

## Evidence boundaries

Sound levels and event routing are testable. Subjective sound quality requires listening.
Physical TV audio acceptance remains separate from emulator and file analysis.
The coordinator owns all builds, Git operations, integration, and device input.
