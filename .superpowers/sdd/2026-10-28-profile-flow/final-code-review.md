# Final code review

Reviewed the complete tracked and untracked change against base `436bd83c657557585801dffca01ab1ecde661e49` in `/Users/amine/Developer/worktrees/family-home-profile-flow/google-tv-apps`.

Result: **zero actionable findings**.

The review covered direct switching for parent, child, and Time is up; retained parent PIN and End session confirmation; resume-badge conditions and semantics; schema 1–3 migration into schema 4; per-profile durable order storage and failure handling; allowed-app filtering and fallback order; move-mode boundaries, scrolling, cancellation, held-key release, and save lifecycle. Existing timer, usage, and app-artwork paths were checked for unintended changes.

This was a read-only source review. No builds, tests, installations, device input, or Git mutations were performed by the reviewer. Runtime results and capture acceptance remain separate coordinator checks.

## Focus correction follow-up

One actionable finding in the subsequent window-focus change:

**P2 — Limit the parent focus request to initial focus.** `ProfileScreen.kt:83` requests parent focus on every window regain. With enough profiles, directional navigation can scroll the parent item out of the LazyRow composition. If system UI or another activity then takes and returns window focus, `first` has no attached focus target and `requestFocus()` can throw `IllegalStateException`. When the parent remains attached, this also resets the user's current profile focus. Remember that initial focus has completed, and do not repeat it for later window-focus changes.

The new waits and directional assertions in `ProfileChooserPresentationTest` are appropriate. No builds or device commands were run for this follow-up.

### Resolution

Reviewed the subsequent `initialFocusApplied` guard in `ProfileScreen`. The first focused window requests parent focus once. Later window-focus changes no longer request an off-screen parent or reset the selected profile focus. The P2 finding is resolved in source. **No remaining actionable findings.** The planned dialog loss/regain regression test still requires separate runtime validation by the coordinator.

Reviewed `dialogReturnKeepsTheLastScrolledProfileWithoutRequestingTheDisposedParent`. It uses directional navigation, confirms the parent is disposed, opens a real dialog, observes window loss and regain, dismisses with Back, and checks that the last profile retains focus. The test covers the reported failure path. No actionable findings from this final source check; runtime results remain with the coordinator.

### Placement-aware initial focus

Reviewed the final `firstPlaced` change. `onGloballyPositioned` gates the initial request until the parent Surface is placed; the effect also waits for window focus. `initialFocusApplied` is set only when `requestFocus(FocusDirection.Enter)` succeeds. After success, later window-focus changes cannot request the disposed parent. No actionable source findings. Targeted and full instrumentation results remain separate validation.
