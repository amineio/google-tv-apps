# Google TV UX capture

Captured on 2026-10-26, approximately 18:51–18:54 Europe/Brussels.

Device: physical Chromecast, ADB serial `192.168.1.121:39469`, 1920×1080.
Installed app: `com.amine.chessmath`, versionName `0.1`, versionCode `1`, release flags (not debuggable).

The TV's existing high-contrast text setting and external clock overlay remain unchanged. The PNGs include both effects.

## Actual saved session

- [01-promotion-current.png](01-promotion-current.png): initial screen found on the TV; Bishop focused in promotion choices.
- [02-pause.png](02-pause.png): pause screen after Back from promotion.
- [03-chess-before-relaunch.png](03-chess-before-relaunch.png): current saved board after Back resumed the session.
- [04-start.png](04-start.png): Start screen with Continue focused after closing and reopening the app.
- [05-chess.png](05-chess.png): saved board after a process restart and Continue. The game position matches the earlier board.
- [06-selected.png](06-selected.png): f2 pawn selected; legal destinations shown. No move was made.
- [07-final-start.png](07-final-start.png): final TV state, Continue focused.

The first relaunch showed Start but did not respond to OK or Enter. A force-stop and launch restored interaction. No app data was cleared. Navigation changed pause and selection state only. Saved moves and configuration were not reset.

## Memory fixtures

Installed the existing test APK only, then ran `ParentAndPromotionRemoteTest`. Result: `OK (3 tests)` in 14.886 seconds. This class uses memory persistence, restores the previous controller, and closes each fixture.

- [fixture-settings-default.png](fixture-settings-default.png): settings with Addition focused. The bottom buttons are partly below the visible viewport.
- [fixture-promotion.png](fixture-promotion.png): promotion with Queen focused.
- [fixture-chess-selection.png](fixture-chess-selection.png): initial board with e2 pawn selected.
- [fixture-math-hint.png](fixture-math-hint.png): Math with `24 + 5 = 29`, hint visible, keypad 5 focused.

The test package `com.amine.chessmath.test` was removed after capture. The main app remained installed. No product source, global configuration, other app, emulator, or downloaded dependency was changed. All PNGs were inspected visually before handoff.
