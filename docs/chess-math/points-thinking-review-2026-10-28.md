# Whole-branch independent review

Verdict: PASS. No material findings or required code changes.

Reviewed base `240f7680a8c4f5a6dc833dac49e65f56901b56d1` through head `32ce3304d9c06a5275bcc69ab7363489aad672aa` on `amine/chess-points-thinking`. The absolute checkout, branch, HEAD, status, and worktree list matched the review brief. The evidence directory was untracked. I read the supplied diff package, approved design, implementation plan, progress ledger, app guide, Task 1 review, and relevant source and tests. The diff output was truncated by the tool; focused source reads covered the omitted implementation and test paths.

Material points come from board contents and presentation snapshots. The mapping is 1/3/3/5/9, with zero for kings. Captures reduce only the captured side. En passant and each promotion use the resulting board. Capture promotion accounts for both the removed piece and the pawn replacement. The tests use literal expected totals. Points do not alter game rules or results.

During a live presentation, the score groups show the event's previous totals. The landing callback updates the board and totals, then emits signed feedback. Presentation identity consumption and callback identity checks prevent duplicate or stale feedback. Motion Off and Android animation Off use static signed feedback. Pause, focus loss, screen replacement, view detachment, and recreation clear the effects. Restore does not reconstruct presentation events.

The controller saves the full delay and its original scale with each nonterminal Black move. It settles elapsed active time before saves and pauses. Its monotonic anchor excludes presentation, storage work, pause, hidden time, errors, and non-Chess phases. Matching completion identities reject stale callbacks, and synchronous completion preserves its newly started anchor. Old saves default to zero delay. Off suppresses stale saved time, clears it through the persistence path, and normalizes a pending save after acknowledgement. Nonzero preference changes affect the next reply. Selection, promotion, and intended-move actions enforce the delay in the controller. Draw claims remain available; results, White moves, and new boards clear the delay.

The Settings control uses the approved six choices and applies the controller preference only after successful preference persistence. Its focus tag supports restoration after selection. Native dialog dismissal restores the existing row after Back. The new remote test exercises these paths with directional input and checks scrolling to the final Settings action. The StartScreenTest adjustment waits for native window focus transitions and does not force focus or change product behavior.

I inspected `thinking-english.jpg`, `thinking-large-text.jpg`, `promotion-points-motion.jpg`, and `thinking-choices.jpg`. These captures contain generic app content. The board remains centered. Scores, signed changes, the countdown, and enlarged Dutch text fit outside it. The upper-right area remains clear in the inspected board captures. The images do not establish physical Clock-overlay compatibility or physical-TV acceptance.

The change stays within Chess and its documentation. It adds no dependency, permission, network access, audio, or changes to other apps. The implementation remains Java with native Android controls.

Validation supplied by the coordinator: 129 unit tests passed; lint had zero errors and 23 existing warnings; debug, release, and instrumentation APK builds passed. Six new remote tests passed on the dedicated Chess emulator. A focused StartScreenTest regression passed after the dialog wait adjustment. The final full 56-test run was still running when this review was requested. I did not rerun builds or tests, operate devices, change product files, or change Git state. This report is the only file I wrote.

No minor suggestions require follow-up for this scope. The final full-suite result, release installation, verification record, and merge remain coordinator delivery steps. Physical-TV, playback, wake, and Cast evidence remain separate.
