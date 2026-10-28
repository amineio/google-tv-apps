# Family Home settings and profiles

The user requests familiar Android or Google TV settings, less clutter, and optional profile creation.
Do not create a fourth real profile. Preserve the three current profiles, portraits, balances, app lists, and parent PIN.
The coordinator selects child-only creation unless the user requests adult profiles through the pending clarification.

## Interface

Use a restrained Google TV layout with a title on the left and a continuous settings list on the right.
The root has three rows: Profiles, Parent PIN, and Device setup.
Profiles and Device setup are navigation rows. Parent PIN uses a saved-state switch and the existing approval flow.
Profiles shows the protected parent as information, child navigation rows, and Add child profile.
A child row opens Allowed apps directly. Each installed app has one switch row.
Place unavailable default apps after installed apps as non-focusable status rows.
Device setup shows the Home helper and Default Home app status. Open Android configuration through navigation actions, not false switches.
Do not add tabs, toolbars of action buttons, nested cards, or an on-screen Back button.

Each row is one remote focus target. Up and Down move through rows. OK opens or toggles the focused row.
A trailing switch is not a second focus stop. Show its On or Off state independently of the focus treatment.
Restore the originating row and scroll position after Back, dialogs, and return from Android configuration.
Preserve settings navigation across activity recreation. Keep lower-right controls clear of the Clock overlay.

Add child profile uses a name field, a native initials portrait, and one Save action.
Use the Android TV keyboard. Keyboard dismissal retains the form and text.
Only explicit Save creates a profile. Remote Back creates nothing.
Keep the current one-action duration and parent dialogs.
Trim the name. Accept 1 through 24 characters. Reject blank names, control characters, and case-insensitive duplicates.
A new child gets the current default six apps and no saved session.
Keep the 20-minute default, five-minute steps, and 5–60 minute session range.
Do not add rename, deletion, avatar selection, or extra adult roles in this change.
Additional profiles remain reachable by horizontal scrolling on the chooser. Keep the default three-card composition.

## Saved state

Replace the Viewer enum with a serializable value class with stable IDs.
Retain the exact wire IDs AMINE, ELIAS, and SOFIA, including JSON map keys.
Viewer.AMINE remains the only parent. New children use generated IDs independent of their names.
Store ordered FamilyProfile metadata with viewer, name, and Portrait.
Portrait values are AMINE, ELIAS, SOFIA, and CHILD. CHILD uses native initials, not a new image asset.
FamilyState has schema 2 and an explicit profile list.

The pure FamilyStateCodec migrates schema 1 by adding only schema 2 and the three metadata records.
Preserve selected IDs, session values, allowed maps, PIN fields, revision, and recovered values during migration.
Do not repopulate omitted child entries in an existing allowlist map.
Keep the DataStore key state_v1. Keep its 65,536-character limit.
Require an explicit profile list in schema 2. Reject unknown schemas and invalid or duplicate profile IDs.
Require the parent and valid membership for selected, session, and allowlist IDs.
Keep pause-on-load recovery separate from migration.

FamilyController.addChild(name) returns Viewer? and requires the authenticated selected parent.
Reject invalid user input or unknown IDs before commit. Report a notice without setting storageFailed or writing data.
A failed durable write must not add the profile in memory.
Guard select, start, end, and setAllowed against unknown IDs.
SessionEngine also requires membership. AccessPolicy denies an unknown selected profile.
Keep existing timer, PIN, helper, Cast, package, permissions, and network behavior.

## Delivery and evidence

Use a worktree from origin/main and a feature PR. One coordinator owns builds, Git, and devices.
Use separate model and UI implementation tasks, followed by independent code and UI reviews.
Use the isolated Google TV emulator and an explicit serial for every command.
Use actual D-pad input in navigation tests. Test creation with isolated generic fixtures, not a real fourth family profile.
Prove legacy state preservation, current-state round trips, authorization, validation, independent child balances, and focus restoration.
Inspect generated concepts and actual native captures. Record prompts, dimensions, and source links without new personal images in Git.
Use the physical TV only when available. Never clear its data or create a new profile during delivery.
After successful installation, commit and push the installed changes and merge the PR into origin/main.
Keep cloud APK builds paused. Do not upload APKs or create public releases.

## Sources

Android settings: https://source.android.com/docs/core/settings/settings-guidelines
TV navigation: https://developer.android.com/design/ui/tv/guides/foundations/navigation-on-tv
TV list component: https://android.googlesource.com/platform/frameworks/support/+/93812c5bf21c3d8cf974c67ebe1d29188e7a915e/tv/tv-material/src/main/java/androidx/tv/material3/ListItem.kt

The coordinator chooses the three-row hierarchy over a single long list because profile creation increases the item count.
The hierarchy keeps device setup separate from daily family controls. It follows the user request without copying Android system behavior that the app cannot control.
