# Chess and maths version 0.9 release plan

Use the existing Java views and forest theme. Preserve remote controls, game progress, and all accepted gameplay changes.

## Settings and About

- Group preferences under Play and learning, App preferences, Current session, and Information.
- Use wide list rows with a focus highlight. Keep native switches and existing focus tags.
- Keep the language value as supporting text. Retain confirmation dialogs for phase skips and reset.
- Put the app name, version, build number, and help in About. Read version values from BuildConfig.
- Update the app to version 0.9 and version code 9.

## Delivery

- Test About metadata, both languages, large text, remote navigation, maths modes, and saved-game behavior.
- Run repository tests and the Android CI commands. Inspect screenshots on the TV.
- Obtain independent code review. Address findings before integration.
- Commit the accepted app changes and release records on amine/chess-math-v0.9.
- Push the branch, create a pull request, and wait for CI.
- Merge into main after all required checks pass. Bring the local main branch to the merged commit.
- Install the final release on the TV and make sure that its reported version is 0.9.

The user explicitly authorized commit, push, and merge. No additional approval gate applies.
