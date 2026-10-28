# Family Home launcher polish

The user requests this implementation and autonomous design review on 2026-10-28.
Keep the existing Kotlin and Compose for TV build. Preserve the profile chooser, session rules, allowed apps, PIN, helper, and Cast configuration.

## Design

Center the child duration screen. Show the current portrait and name above a symmetric stepper.
Start at 20 minutes. Keep five-minute steps and the 5–60 minute range.
Remove “Choose up to one hour.” Start session receives initial focus.
Use neutral buttons until focused. Apply the gold focus treatment only to the focused control.

Keep the profile portrait and name at the upper right of the launcher.
Reserve fixed bounds for the countdown. Use equal-width digits and a stable suffix.
Remove Open app and promotional sentences. The focused tile opens its app with the remote center key.
Retain the Chess hero. Other apps use a subdued gradient derived from their icon color.
Fit landscape banners to landscape tiles. Fit square icons to square tiles without extra empty space.
Use familiar rounded TV buttons for the footer. Keep them clear of the separate Clock overlay.

## Approach

The coordinator selects a small change to the existing native components.
A broad redesign adds unnecessary work. Generated screen images remain design references only.
No new dependencies, audio, network access, or bundled images are required.
Record the ImageGen prompt and dimensions. Keep family screenshots and device data outside Git.

## Acceptance

Use directional remote input to test initial focus, duration limits, tile launch, footer navigation, scrolling, empty lists, and focus after return.
Make sure that countdown changes do not move the profile, countdown border, or suffix.
Inspect emulator captures. Resolve actionable developer and UI/UX review findings before physical installation.
Build and test locally. Install on the physical TV after the emulator work is complete.
After installation, push the feature branch, merge the PR into origin/main, and make sure that the PR closes.
Report installation, merge, and deferred hardware tests separately.
