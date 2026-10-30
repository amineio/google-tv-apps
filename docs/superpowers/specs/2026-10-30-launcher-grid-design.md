# Family Home app grid

## Intent

Replace the horizontal app row with a grid that makes more apps visible at once.
Make the Home profile picture larger and circular.
The user approved the concept on October 30, 2026, and authorized implementation.

## Approaches

A five-column grid keeps landscape banners readable and shows two complete rows.
A denser grid makes more apps visible but reduces tile and label size.
The selected design uses five equal landscape cells with vertical scrolling.

## Layout

Keep the selected app background and gold focus outline.
Use a compact selected app heading above the grid.
Show app labels below equal-size tiles.
Fit square icons inside landscape cells without stretching them.
Keep the existing right-side profile control and timer.
Use a 64 dp Home header and a 48 dp profile picture with equal width and height.
Apply a circular crop and leave sufficient vertical space for the picture and padding.
Keep controls clear of the separate Clock overlay.

## Remote behavior

Left and Right move within a row and stop at its ends.
Up and Down move to the same column in the adjacent row.
A short final row uses the nearest available column.
Up from the first row reaches the profile control.
Down from the profile control returns to the selected app.
Scroll before requesting focus when the destination is outside the visible area.
Restore the selected app after returning from an app.

Hold OK to enter move mode.
All four arrow keys move the selected app through the grid using the same geometry.
OK saves the order once.
Back cancels the draft and retains focus on the selected app in its original position.

## Constraints

Keep the saved flat package order and existing child app filtering.
Preserve profiles, timers, allowed apps, PIN, voice preference, and usage records.
Keep empty lists, profile dialogs, app return, and catalog removal usable.
Keep other screens at their existing header size.
Use native Compose for TV controls.
Do not change dependencies, permissions, Home routing, or accessibility behavior.

The ImageGen concept is a preview reference only.
It uses generic labels and a generic profile.
No generated screen image or new raster asset is bundled with the app.
Existing artwork and font licenses remain applicable.

## Evidence

Use actual directional input for navigation tests.
Cover row boundaries, scrolling, short rows, reorder save and cancel, app return, dialogs, empty lists, and square portrait bounds.
Inspect fresh emulator captures for readability, circular crop, focus outlines, and safe margins.
The coordinator owns builds, device input, integration, and Git delivery.
Physical TV testing is outside this task.
