# Direct profile switching and resume badges

Switch profile opens the chooser directly for parents and children. All Switch profile actions, including Time is up, use the same durable pause operation. Remove the switch confirmation dialog. Keep session balances, usage accounting, parent access PIN, and End session confirmation unchanged.

Remove Who is watching?, Choose your profile to begin, and the generic unfinished-session message from the chooser. Center the portrait row vertically. Keep profile names, initial parent focus, horizontal scrolling, setup status, and storage notices readable.

Show a compact clock icon and remaining time at the top of each child portrait with a positive resumable balance. Reserve matching space for every portrait. The badge has no separate action or focus target. Exclude parents, expired sessions, and absent sessions. Expose the profile's resume state through accessibility semantics. Build the icon with code and keep it clear of focus rings and the Clock overlay.

Use native Compose for TV. The chooser changes need no new assets, dependencies, permissions, state migration, or real profile. Test immediate switching with and without a PIN, preserved child balances, badge conditions, initial focus, horizontal navigation, and helper or error notices. Review emulator captures before TV installation. Commit, push, merge the PR, and synchronize main after installation.

## App order

Hold OK on an app to enter move mode without opening it. Left and Right move that app by one position. OK saves the order. Back cancels the draft. Keep the moved app focused and visible. At row boundaries, movement stops. A short OK press outside move mode still opens the app. Show a compact movement hint only during editing. Profile navigation does not steal focus during a move. Cancel the draft on profile or catalog change.

Store a separate package order for each profile. Use schema 4 with `appOrder: Map<Viewer, List<String>>`, default empty. Migrate schemas 1 through 3 without changing prior fields. Omit empty appOrder on the wire to preserve the existing size limit. Validate known profile keys, unique packages, and valid package names. The controller saves only the selected profile's order through the existing durable queue. Reordering does not change allowed apps or timers.

Saved packages sort before the existing fallback order. New apps append. Missing or excluded apps do not appear. Keep existing two-argument catalog and UI APIs compatible through default parameters. A failed save must not publish a new durable order. Do not add another confirmation or PIN for this cosmetic action.
