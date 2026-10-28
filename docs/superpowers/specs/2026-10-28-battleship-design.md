# Battleship for Google TV

The user specification on 2026-10-28 authorizes a complete offline game and autonomous implementation.
The app uses Kotlin, Compose for TV, ViewModel, StateFlow, coroutines, and an independent Gradle build.
The package is `com.amine.battleship`. The app requires Android 8 or later, subject to dependency compatibility.

## Scope and approach

The game uses two 10 by 10 boards and ships of lengths 5, 4, 3, 3, and 2.
Ships can touch. Automatic placement uses injectable randomness. The human starts. Hits do not grant extra turns.
Invalid attacks preserve the turn. Victory ends all attacks.
The Normal computer uses hunt and target decisions from prior public shot results only.
The engine exposes immutable state and a redacted opponent view. It contains no Android or Compose types.

An independent build follows Family Home. This preserves the root Java builds.
A single focusable Canvas board controls the targeting coordinate. This avoids 100 competing focus targets.
TV Material buttons control menus and dialogs. The Back button opens pause and restores board focus on resume.
These choices follow the supplied specification. No network, accounts, database, multiplayer, or tablet work is included.

## Presentation

The generated naval concept is `docs/battleship/concepts/naval-concept.webp`.
It is a visual reference. All boards, text, ships, markers, and controls render from native state.
The enemy board is square and larger than the friendly board. Five-percent margins protect essential content.
Dark navy water, steel ships, cyan focus, red crosses, and pale miss rings define the palette.
Selected coordinate, last attack, shot feedback, fleet damage, and turn status remain visible.
The menu uses a separate compressed naval background. Gameplay uses a dark overlay for contrast.
Brief native effects show firing, splash, impact, sinking, and completion.
Short local synthesized sounds are optional and have a mute control. No music is planned.

## Session and persistence

Phases are MAIN_MENU, INITIALIZING, PLAYER_TURN, COMPUTER_TURN, and GAME_OVER.
Pause is separate from the underlying turn. Backgrounding pauses all delayed work.
The engine commits each shot atomically. Animation never owns a partially applied move.
An app-private serialized file stores the full match, selected coordinate, sound preference, and hint state.
AI knowledge derives from saved shots and sunk ships. Invalid or damaged saves produce a usable fresh menu.
Continue appears only for a valid unfinished match. A restored computer turn performs exactly one pending shot.
The pause menu has Resume, Restart Game, Settings, and Return to Main Menu.
Restart and abandonment require confirmation. Returning home retains the saved match for Continue.

## Evidence and delivery

Unit tests cover rules, placement, visibility, persistence, turn control, AI behavior, and 1,000 seeded complete games.
Emulator tests use directional input for all cells, press gating, pause, dialogs, rematch, and focus restoration.
Layout captures cover 1080p and 4K. Emulator evidence does not establish physical TV readability or sound quality.
The coordinator controls all builds, Git delivery, and device input. A fresh reviewer examines the completed change.
The feature starts from clean `main` at `240f7680` in a managed worktree and uses a tracking pull request.
After installation, the repository instructions require tested changes to reach `origin/main` in the same task.
Cloud builds, APK uploads, and public releases remain disabled.
