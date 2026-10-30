# AI Chat Sign-in Diagnostic Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Identify the callback exception safely and apply the authorized AI Chat name.

**Architecture:** Retain the Android loopback server and all existing subscription boundaries. Add fixed-stage exception-class diagnostics without request data. Change display metadata and the code-rendered banner.

**Tech Stack:** Existing independent Kotlin/Compose build, Android Log, ImageMagick, and WebP.

**Spec:** [Diagnostic scope](../specs/2026-10-30-ai-chat-sign-in.md).

## Global Constraints

- Worktree: `ask-tv-sign-in/google-tv-apps`, branch `amine/ai-chat-sign-in`, base `4aa14ef36a269293fb9361b9fb5ffc50a0595496`.
- Package stays `com.amine.asktv`. Candidate version is `0.2`, code `2`.
- Diagnostics contain only a fixed stage and exception class. Keep the generic browser body unchanged.
- Preserve storage, existing issued registrations, endpoints, context, the icon, and the hero.
- Do not implement a QR flow or a speculative behavior fix.
- The coordinator owns builds, tests, devices, browser authorization, and Git delivery.

## Review Focus

- Android logging must not use a Throwable overload.
- Name changes must not replace a saved issued client ID or reset storage.
- The banner must retain its dimensions and existing composition.
- Historical validation evidence must keep its original version and results.
- A later bug fix needs a regression based on the observed exception.

### Task 1: Prepare the diagnostic and name candidate

**Files:** `LoopbackAuthServer.kt` owns safe stage diagnostics. `OAuthProtocol.kt`, `AskScreen.kt`, `AskViewModel.kt`, app values, build metadata, installer/helper copy, and the existing name assertion own the rename. `docs/ask-tv/render-banner.sh` owns banner rendering. Current app/root guides and the asset record describe the candidate.

- [ ] Add only exception-class and fixed-stage diagnostics to the generic callback catch.
- [ ] Change current display labels and new-registration hint to AI Chat.
- [ ] Bump version to `0.2`, code `2`.
- [ ] Render the matching banner and update its hash/source record.
- [ ] Update current documentation and the private implementation report.
- [ ] Ask the coordinator to build and inspect the exact callback diagnostic.
- [ ] After root cause is established, add a focused regression before any behavior fix.

The coordinator can run `apps/ask-tv/gradlew -p apps/ask-tv testDebugUnitTest testReleaseUnitTest lintRelease assembleDebug assembleRelease assembleDebugAndroidTest --console=plain`.
Low-impact rename and diagnostic changes need no new tests. The existing new-registration assertion must use the new display name.
