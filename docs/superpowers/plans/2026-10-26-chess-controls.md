# Chess controls implementation plan

Goal: Remove chess messages, expose New game, and add Easy, Normal, and Hard.

Architecture: Keep controller persistence and rendering separate. Store difficulty independently from game progress. Use a bounded local search for Normal.

Spec: ../specs/2026-10-26-chess-controls-design.md

1. Remove the chess side column and routine save overlay. Update layout tests and test an actual delayed save.
2. Add New game beside Continue with the existing reset confirmation. Demonstrate the missing action with a failing TV test, then test cancel and confirmation.
3. Add preference storage and the Normal opponent with tactical unit tests. Wire the controller with stale-work protection.
4. Add a localized parent control and a remote interaction test. Preserve difficulty across reset and recreation.
5. Run unit tests, lint, release assembly, and instrumentation assembly. Run focused tests on the TV with the current app installation. Reset development progress as the user requests.
6. Request independent review. Resolve findings and report test results and remaining limits.
