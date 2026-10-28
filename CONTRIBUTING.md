# Contributing

This repository maintains independent Google TV apps with two Gradle builds.
The Java apps share the root build. Family Home uses a separate Kotlin build.
Read [README.md](README.md) for the layout and commands.
The original CleanYourTV project remains credited in [UPSTREAM.md](UPSTREAM.md).

## Apps

Keep changes inside the app that owns the behavior.
Preserve package names and saved preferences unless a migration is part of the change.
Use native TV controls with visible remote focus.
Keep runtime dependencies, memory use, and permissions small.
Do not add tracking, ads, or device access without a clear product requirement.

Use `amine/` as the branch prefix.
Run the affected unit tests, Android lint, and release build locally before delivery.
If shared build files change, build all apps that use them.
For Kotlin work, read the [development guide](docs/kotlin-tv-development.md) and [agent instructions](AGENTS.md).
For visible changes, include emulator captures and describe physical TV results separately from local tests.
Use generic names in shared captures. Keep personal account screens and device data out of Git.
Keep GitHub APK builds paused unless the user explicitly requests a change.
Do not detach the fork or change repository visibility without a new explicit request.

## Device tools and lists

Run `python3 -m unittest discover -s tests -v` from the repository root.
Device tests and installation commands operate the selected emulator or TV. Use an explicit ADB serial.
Use physical TV commands only as part of an authorized device task.
Keep backups, device identifiers, local SDK files, build outputs, and credentials out of Git.

Existing device lists remain in `examples/`. Keep their package syntax and index tables consistent.
The inherited project guide and TV table remain in `UPSTREAM.md`.
Themes remain in `themes/`.

## License

Contributions use the repository MIT license.
Preserve upstream notices and the separate Outfit font license.
