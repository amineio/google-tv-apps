# Contributing

This fork maintains independent Google TV apps in one Gradle build.
Read [README.md](README.md) for the layout and commands.
The original CleanYourTV project remains credited in [UPSTREAM.md](UPSTREAM.md).

## Apps

Keep changes inside the app that owns the behavior.
Preserve package names and saved preferences unless a migration is part of the change.
Use native TV controls with visible remote focus.
Keep runtime dependencies, memory use, and permissions small.
Do not add tracking, ads, or device access without a clear product requirement.

Use `amine/` as the branch prefix.
Run the affected unit tests and Android lint before publishing a change.
Build both releases when changing the shared build or repository layout.
For visible changes, include actual TV screenshots and describe device validation separately from unit tests.

## Device tools and lists

Run `python3 -m unittest discover -s tests -v` from the repository root.
Device tests and installation commands operate a real TV. Use them only as part of an authorized device task.
Keep backups, device identifiers, local SDK files, build outputs, and credentials out of Git.

Existing device lists remain in `examples/`. Keep their package syntax and index tables consistent.
The inherited project guide and TV table remain in `UPSTREAM.md`.
Themes remain in `themes/`.

## License

Contributions use the repository MIT license.
Preserve upstream notices and the separate Outfit font license.
