# Direct profile switching and resume badges

Switch profile opens the chooser directly for parents and children. All Switch profile actions, including Time is up, use the same durable pause operation. Remove the switch confirmation dialog. Keep session balances, usage accounting, parent access PIN, and End session confirmation unchanged.

Remove Who is watching?, Choose your profile to begin, and the generic unfinished-session message from the chooser. Center the portrait row vertically. Keep profile names, initial parent focus, horizontal scrolling, setup status, and storage notices readable.

Show a compact clock icon and remaining time at the top of each child portrait with a positive resumable balance. Reserve matching space for every portrait. The badge has no separate action or focus target. Exclude parents, expired sessions, and absent sessions. Expose the profile's resume state through accessibility semantics. Build the icon with code and keep it clear of focus rings and the Clock overlay.

Use native Compose for TV. No new assets, dependencies, permissions, state migration, or new real profile. Test immediate switching with and without a PIN, preserved child balances, badge conditions, initial focus, horizontal navigation, and helper or error notices. Review emulator captures before TV installation. Commit, push, merge the PR, and synchronize main after installation.
