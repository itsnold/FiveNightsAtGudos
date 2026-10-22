# Five Nights at Gudo's

A Java 17 + Swing survival-style night shift game inspired by classic FNAF presentation: CRT/VHS effects, camera feeds, power management, and a six-hour shift loop.

---

## Table of contents

1. [What this project is](#what-this-project-is)
2. [Current gameplay](#current-gameplay)
3. [Controls](#controls)
4. [Requirements](#requirements)
5. [Run the game](#run-the-game)
6. [Build and test](#build-and-test)
7. [Save data and profiles](#save-data-and-profiles)
8. [Project architecture](#project-architecture)
9. [Assets and content pipeline](#assets-and-content-pipeline)
10. [Troubleshooting](#troubleshooting)
11. [License and attribution notes](#license-and-attribution-notes)

---

## What this project is

Five Nights at Gudo's is a desktop game where you survive each night by managing:

- **Time** (12:00 AM to 6:00 AM over a 6-minute real-time shift)
- **Power** (drains faster when monitor/light are active)
- **Awareness** (switch between 6 camera feeds)

### Important design constraints (current version)

- There are currently **no enemy AI entities, attacks, or jumpscares**.
- A failed run is currently a **power-out equipment failure**, not a death scene.
- Night progression is unlocked by completed runs and stored locally.

---

## Current gameplay

- Main menu with New Game, Continue, and Extras.
- Extras pages for:
  - Night select
  - Guide
  - Settings
  - Credits
  - Shift records
- Office panning with curved panorama projection.
- 6 camera feeds with autonomous sweep motion.
- Monitor/light mutual exclusion (opening monitor disables light).
- Pause/resume support.
- Borderless fullscreen toggle.
- Persistent local profiles, settings, and run history.

### Night and power rules

Core timing and drain constants are in `GameSession`:

- Shift length: **360 seconds**
- Base drain: **0.085/s**
- Monitor extra drain: **0.22/s**
- Light extra drain: **0.20/s**

So usage is effectively:

- Idle office: base drain
- Monitor open: base + monitor
- Light on: base + light
- Monitor + light together: not allowed

---

## Controls

| Control | Action |
| --- | --- |
| Mouse near left/right edge | Pan office view |
| A / D or Left / Right arrows | Pan office view |
| Space or Tab | Open/close monitor |
| 1..6 | Switch camera feed (when monitor is open) |
| Camera buttons on map | Switch camera feed |
| Hold **F** or hold Light button | Toggle office light |
| Esc | Pause / resume |
| F11 | Toggle borderless fullscreen |

---

## Requirements

- **JDK 17+**
- Internet access for first Maven dependency download
- Desktop environment (for running the UI)

Maven itself is included via the wrapper (`mvnw`, `mvnw.cmd`).

---

## Run the game

### Windows quick run

Double-click `run.cmd`, or run:

```powershell
.\run.cmd
```

`run.cmd` builds with Maven wrapper and launches:

```text
java --enable-native-access=ALL-UNNAMED -jar target\five-nights-at-gudos.jar
```

### macOS / Linux

```bash
sh ./mvnw clean verify
java --enable-native-access=ALL-UNNAMED -jar target/five-nights-at-gudos.jar
```

### Isolated save directory (manual testing)

```bash
java --enable-native-access=ALL-UNNAMED -Dgudo.dataDir=target/manual-test-data -jar target/five-nights-at-gudos.jar
```

---

## Build and test

### Full build + tests

Windows:

```powershell
.\mvnw.cmd clean verify
```

macOS/Linux:

```bash
sh ./mvnw clean verify
```

### Packaged distributable

The distributable output is:

- `target/five-nights-at-gudos.jar`

### Test coverage areas

Existing tests cover:

- Game timing/power/session behavior
- Camera motion and panorama projection
- SQLite persistence and idempotent writes
- Settings and shift-log text file IO
- Asset loading for images/audio/effects/font
- Headless UI rendering paths

Optional desktop smoke test class:

- `src/test/java/com/gudos/DesktopSmoke.java`

---

## Save data and profiles

Data is written **outside** the jar/application directory.

Default locations:

- Windows: `%LOCALAPPDATA%\FiveNightsAtGudos\`
- Other platforms: `~/.five-nights-at-gudos/`

Files:

- `settings.txt` — UTF-8 user settings
- `shift-log.txt` — append-only human-readable run report
- `gudo.db` — SQLite database with progress and run history

### SQLite tables

- `profiles`
- `progress`
- `night_runs`

Notes:

- Profile names are case-insensitive in SQLite.
- Run inserts are idempotent by run ID (`ON CONFLICT DO NOTHING`).
- Progress updates are transactional and tied to completed runs.

---

## Project architecture

### Startup flow

1. `Main` configures Swing look-and-feel and opens a loading panel.
2. Background boot loads:
   - App paths
   - Settings
   - SQLite progress
   - Assets
3. `Application` takes over navigation, audio, update loop, and async IO.

### Runtime responsibilities

- `com.gudos.game` — pure gameplay/session state
- `com.gudos.ui` — Swing/Java2D screens and HUD rendering
- `com.gudos.assets` — scene manifest/image loading/projection helpers
- `com.gudos.audio` — WAV playback service
- `com.gudos.persistence` — settings/log/database storage

### Repository layout

```text
src/main/java/com/gudos/
  Main.java
  Application.java
  assets/
  audio/
  game/
  persistence/
  ui/
src/main/resources/
  config/scenes.properties
  images/
  audio/
  effects/
  fonts/
  licenses/
src/test/java/
asset-sources/
docs/assets/
```

---

## Assets and content pipeline

### Scene manifest

`src/main/resources/config/scenes.properties` maps scene/state IDs to image paths.

Each scene must have a `.default` entry. Unknown state keys fall back to default.

This supports future state variants like:

```properties
cam03.occupied=/images/cameras/cam-03-kitchen-occupied.png
```

`GameSession#setSceneState(...)` provides the hook for selecting these states.

### Included source assets

- Runtime assets live in `src/main/resources/`.
- Original source packs are preserved under `asset-sources/` and docs under `docs/assets/`.

---

## Troubleshooting

### App fails on startup

- Confirm you are launching the built jar or `run.cmd` from the repository root.
- Confirm the save directory is writable.
- Confirm Java 17+ is installed.

### No sound

- Check Settings volume.
- Confirm your system audio device/mixer is available.

### Progress not where expected

- Use Settings → "COPY DATA FOLDER" to get the exact active save path.
- If using `-Dgudo.dataDir=...`, that override location is authoritative.

---

## License and attribution notes

- Attribution and bundled license texts are in `src/main/resources/licenses/`.
- Additional source-asset references are in `docs/assets/asset-manifest.md`.
- This is an unofficial fan project and not an official FNAF release.

Third-party usage rights vary by asset. Verify redistribution permissions before public release, especially for archived/source audio that may not include permissive redistribution terms.
