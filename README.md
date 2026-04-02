# StageSet

StageSet is a tablet-first Android app for musicians who need to build song libraries and playable setlists for live use.

## What it does

- Keeps a local pool of songs sorted alphabetically by title.
- Stores song metadata: name, artist, and key.
- Lets you write and edit lyrics with chord charts in a monospace editor.
- Imports public Ultimate Guitar pages by URL with a best-effort parser that converts common chord markup into a stage-friendly chart.
- Builds setlists from the saved song pool and lets you reorder songs before a performance.
- Uses an offline Room database so the core experience still works without a connection.

## Project structure

- `app/src/main/java/com/codex/stageset/data`: storage, repositories, and import logic.
- `app/src/main/java/com/codex/stageset/ui`: Compose screens and responsive layouts.
- `app/src/test`: parser-focused unit tests can be added here without needing a device.

## Notes

- The Ultimate Guitar import path is intentionally defensive. Public page markup can change, so the parser includes several fallbacks and surfaces a friendly error if the source format changes.
- This workspace did not include Java or Gradle on `PATH`, so I scaffolded the Android project files directly rather than generating them from Android Studio.
