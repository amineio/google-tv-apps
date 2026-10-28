# TV Clock settings design

The user requests flat settings that follow Android and Google TV style, useful icons, TV screenshots, and a new launcher logo.
The user also requests an Imagine reference through a design sub-agent.
The Chess settings edits reached origin/main through PR #3 before this work started.

## Design

Use the generated reference at `docs/clock/concepts/2026-10-28-settings-impression.png` for composition, spacing, focus, and brand direction.
Use one solid charcoal background, a left brand area, and a continuous settings list on the right.
Rows have no resting container. The focused row uses a pale surface and dark text.
Use distinct line icons for appearance, timer, bedtime, reminder action, position, text size, and reset.
Keep the existing two destinations: Time limits and Clock appearance.
Use full rows for settings. Move exclusive timer and reminder choices into native choice dialogs.
Position and text size each have one focus target. Left and Right adjust the value immediately.
Up and Down move between rows. Back closes a dialog or returns to the previous screen.
Use a clock ring with clear hands for the app icon and a matching TV Clock wordmark for its launcher banner.

## Approaches

Restyling the current button grid keeps its crowded navigation and uneven control layout.
A continuous native list follows the requested style and keeps the app small. This is the selected implementation approach.
A new UI framework adds dependencies without a required capability.
The implementation choices above follow the user's request and the generated reference. They do not imply separate design approval.

## Constraints and acceptance

Keep time limits, permissions, saved preferences, background alarms, and the outlined overlay behavior.
Appearance changes must not enable a hidden clock.
Keep Home, Chess, and TV system configuration unchanged.
Use native Java Android controls and vector assets without new runtime dependencies.
Capture and inspect actual TV screens after installation, including the launcher tile and focused rows.
Run Clock unit tests, release lint, release build, Android test compilation, and TV interaction tests.
Request an independent code review. After installation, commit, push, and merge into origin/main under AGENTS.md.

Google's list guidance supports continuous rows, meaningful icons, clear selection controls, and a single focus target per row.
Reference: https://developer.android.com/design/ui/tv/guides/components/lists
