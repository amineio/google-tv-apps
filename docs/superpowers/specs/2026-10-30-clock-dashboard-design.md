# Clock dashboard design

The user approved the generated dashboard concept and authorized implementation. Clock hands must have flat ends, never rounded ends.

## Approved presentation

Rename TV Clock to Clock in all user-visible text. Keep the package `com.amine.tvclock` and the independent Java app.

Use a near-black navy background, subtly bordered charcoal cards, rounded card corners, thin white digits, and a soft blue accent. The left card contains a large native analog clock, the current digital time, and the date. The right column contains a session timer card and a bedtime card. The timer offers duration, Set, Start, and Cancel controls as appropriate to its state. Bedtime offers the time, enable control, Edit, and Skip next. Bottom actions remain Clock appearance and At the limit. Permission actions remain available when needed.

Restyle Clock appearance and dialogs with the same palette, spacing, typography, and clear focus treatment. D-pad navigation must reach every action. Dialog dismissal and activity return must restore useful focus. Preserve existing view IDs and the live-clock tag.

## Design alternatives

The existing vertical settings list could retain its current structure with new colors. The approved alternative groups the clock, session timer, and bedtime into cards. The user selected the grouped cards through the generated visual reference.

## Behavior and boundaries

Preserve timer calculations, saved preferences, daily bedtime, permission handling, alarms, overlay behavior, and the voluntary limit model. Keep native Java views. Do not add photographs, weather, network access, dependencies, or a Kotlin migration.

Refresh the native square icon and 16:9 banner. Add a separate text-free 16:9 launcher background and `com.amine.familyhome.HERO_IMAGE` resource metadata if practical within this existing app. Keep clock hands flat in artwork too. Keep launcher presentation in the launcher.

## Design reference and assets

Reference: `/Users/amine/.codex/generated_images/01a18e5c-86e1-7920-bf78-be8cb8dce16a/exec-49f83c1e-89ba-439c-9e97-7dae256ee144.png` (1672 × 941).

This image is an approved built-in ImageGen design reference, not a runtime capture. Prompt summary: quiet TV clock dashboard; dark navy background; large analog and digital clock on the left; timer and bedtime cards on the right; soft blue focus; bottom settings actions. Implement controls, clock geometry, and artwork with native code or vectors. Do not bundle this raster reference. No external raster or font asset is introduced; native project assets retain the repository MIT license.

## Acceptance evidence

Use emulator `emulator-5582` for captures and actual directional navigation. Check initial focus, timer editing, bedtime editing, dialogs, Clock appearance, and focus after return. Run existing Clock regressions, lint, and debug/release packaging. Inspect instrumented test results before reporting coverage. Emulator evidence does not establish physical-TV audio, playback, Cast, wake, or accessibility behavior. Physical-TV installation is outside this task.
