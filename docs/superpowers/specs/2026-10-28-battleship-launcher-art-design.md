# Battleship launcher artwork design

The user requests a matching ImageGen icon, TV banner, and selected-app background in the custom launcher.
Battleship owns all three images. Family Home owns display, contrast, focus, and fallback behavior.

A launcher-owned Battleship special case duplicates app branding. A banner enlarged into a background repeats its title and loses detail.
The selected approach uses one optional application metadata resource, com.amine.familyhome.HERO_IMAGE.
Battleship supplies a separate text-free hero. Family Home reads it generically for the selected app.

The opaque square icon uses a recognizable steel ship on navy ocean. The 16:9 banner includes the readable BATTLESHIP title.
The 16:9 hero keeps its left half quiet for native title text. All three use the game's navy, steel, cyan, and ivory palette.
Generated artwork stays in Battleship drawable-nodpi with compressed WebP files. Existing icon/banner names remain stable bitmap wrappers.

Family Home loads only the selected background off the main thread.
It limits decoded images to 1920 by 1080 pixels without upscaling.
Missing metadata, invalid resources, corrupt images, package removal, and decode failure preserve the app tile and gradient.
An app change clears old art before loading. Resume/package updates refresh it even when resource IDs stay unchanged.
Keep the existing Chess hero and all profile, session, focus, helper, permission, and Cast behavior.
No icon editor, online image service, library dependency, or unrelated app migration is included.

Test both apps locally. Use explicit emulator-5580 for visual, package, and directional integration tests.
Make sure that the banner fills its bounds at 320 by 180, 384 by 216, and 640 by 360 pixels.
Inspect the icon at small size and the background under real launcher controls.
Install tested updates without clearing data. Preserve Home routing and helpers, then commit, push, and merge into origin/main.
The existing AGENTS.md installation rule already matches the user's requested permanent delivery policy.
