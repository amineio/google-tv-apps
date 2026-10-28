# Family Home daily usage

Record selected-profile screen-on time locally for every profile, including the parent. Menus and paused videos count. Standby, profile exit, helper loss, and recovery stop measurement. Child measurement stops at the exact session deadline. Keep session limits unchanged.

Use elapsed time for duration. Split intervals at local midnight with the previous clock sample and time zone. Reanchor after clock changes without adding the clock jump. Keep the cursor in memory and publish it only after a successful durable write. Restart paused without counting process downtime. Use the existing ten-second checkpoint interval.

Store daily milliseconds in schema 3. Migrate schemas 1 and 2 without changing profiles, balances, app lists, or PIN data. Start with empty history. Keep up to 30 recorded dates. Prune oldest usage records if the existing state size limit requires it. Preserve core state.

Add Daily usage after Profiles in parent settings. Show today's total for every profile. Open a profile to show its daily history. Keep native TV navigation, Back restoration, readable focus, and Clock clearance. History rows accept directional focus for scrolling without click actions. Use generic test fixtures only.

Do not add quotas, playback-only measurement, per-app reports, titles, network access, reset controls, or permissions. Do not infer historical use from remaining balances.

Retire the old Java Home package from the TV after a private APK backup. Retain its data and repository source. Preserve stock Google Home, setupwraith, and TV Clock.
