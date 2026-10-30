# Treasure or Retreat implementation plan

Build one independent offline Kotlin TV game with three expeditions and fair shuffled decks.
Use native Compose text, controls, focus, cards, and animations over original generated temple artwork.
Use separate chest base and lid images for the reveal. Keep the chest central in a 16:9 composition.
Use SharedPreferences for records and sound. Keep gameplay rules independent of animation completion.

1. Add deterministic rules tests for deck counts, guaranteed opening, banking, traps, guards, and three expeditions.
2. Implement rules and one playable Explore interaction. Ask the coordinator to compile and test it.
3. Generate and inspect original scene, objects, trap variants, and launcher artwork. Record prompts and rights.
4. Complete menus, pause and lifecycle behavior, celebrations, quiet SoundPool effects, and saved records.
5. Add remote navigation and full-game instrumentation tests. Ask the coordinator to run the app suite, lint, and packages.
6. Capture running gameplay and compare the complete 16:9 screen with the supplied concept during independent final review.

No physical-TV results or listening results are assumed. The coordinator owns builds, devices, and Git delivery.
