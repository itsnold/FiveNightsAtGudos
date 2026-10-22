# Five Nights at Gudo's

A **Java + Swing** carinderia night-shift game with a classic FNAF-style interface: a bare white pixel-font title screen, animated television interference, an outlined CCTV floor plan, green usage bars and a chevron monitor tab. Java2D draws the rooms and HUD; real Swing controls provide the menus, camera buttons, preferences and records table. Java Sound handles WAV playback, and SQLite/JDBC stores local progress.

## Run on Windows

Install a **JDK 17 or newer**, then double-click **`run.cmd`** (or run `.\run.cmd` in PowerShell).

The included Maven wrapper downloads Maven and dependencies on the first build, so the first run requires internet. The built game runs completely offline:

```powershell
.\mvnw.cmd clean verify
java --enable-native-access=ALL-UNNAMED -jar target/five-nights-at-gudos.jar
```

On macOS/Linux, use `sh ./mvnw clean verify`, then the same `java` command. Java 17-compatible bytecode is produced even if a newer JDK is installed.

## What's playable now

- Static-covered title screen with New Game, Continue and Extras. Extras contains night selection, handbook, settings, credits and shift records.
- Mouse-controlled **cylindrical perspective** office panning, with curved/nonlinear image projection instead of a sliding flat crop.
- Six selectable CCTV feeds using the same perspective projection, each **sweeping left/right automatically** on a camera-specific phase and period. Camera movement is independent of the mouse and continues on the shift clock even when another feed is selected.
- Downloaded animated television static, rolling tracking bands, scanlines and a bundled VT323 pixel font. Assets work offline; their licenses and credits are included.
- Desk light, live power usage, a six-minute 12 AM–6 AM shift, and pause/resume.
- Power-failure and completed-shift results. Five unlockable nights with identical rules for now.
- Automatic pause on focus loss and borderless fullscreen.
- Persistent local profiles, progress, attempt history, text preferences and shift reports.

**There are intentionally no enemies, attacks, fake character silhouettes or jumpscares.** Running out of power is an equipment failure, not a death. Separate room-state renders and enemy logic can be added later.

### Controls

| Control | Action |
| --- | --- |
| Mouse near left/right edges, A/D or arrow keys | Pan across the office |
| Space or Tab | Raise/lower the monitor |
| 1–6, or camera map buttons | Switch camera while the monitor is open |
| Hold F, or hold the light button | Desk light in the office |
| Esc | Pause/resume |
| F11 | Borderless fullscreen |

Finish a shift to unlock the next. New Game starts Night 1 without deleting existing records; Continue starts the next unlocked night. Replaying never resets progress. The monitor and light consume additional power, and cannot be used together. Clock, power and automatic CCTV movement stop while paused. Leaving or normally closing during a shift records an abandoned attempt. Force-killing the process cannot save an unfinished attempt. **Continue does not restore a mid-shift checkpoint.**

## Text files and database

Writable data is kept **outside the application/JAR**:

- Windows: `%LOCALAPPDATA%\FiveNightsAtGudos\`
- Other platforms: `~/.five-nights-at-gudos/`

| File | Actual use |
| --- | --- |
| `settings.txt` | UTF-8 key/value preferences, created on first launch, read at startup and atomically rewritten on Apply or F11. |
| `shift-log.txt` | A readable UTF-8 report appended after a completed, power-failed or abandoned attempt. Save retries do not duplicate entries. |
| `gudo.db` | SQLite tables `profiles`, `progress`, and `night_runs`; queried for Continue, night unlocking and the records screen. |

Change your player name in Settings to select/create a separate local profile. Names are case-insensitive; this is not an online account or authentication system. SQLite is authoritative for progress; the text report is not used as a substitute database. Storage operations run off Swing's event thread, SQL uses prepared statements, and attempt/progress writes are transactional.

For an isolated test save location:

```powershell
java --enable-native-access=ALL-UNNAMED "-Dgudo.dataDir=target/manual-test-data" -jar target/five-nights-at-gudos.jar
```

## Project layout

```text
src/main/java/com/gudos/
  Application.java         Screen navigation and asynchronous storage
  Main.java                Swing startup
  assets/                  Scene loading, render-state fallback, cylindrical projection
  audio/                   Background Java Sound service
  game/                    UI-independent shift, power, scene state, automatic camera sweep
  persistence/             SQLite, settings.txt, shift-log.txt
  ui/                      Custom Java2D scenes and Swing components
src/main/resources/
  images/office/           Original panorama, renamed consistently
  images/cameras/          Six original camera images
  audio/                   Eight active ambience/UI effects
  effects/                 Downloaded animated television-static GIF
  fonts/                   Bundled VT323 pixel font
  licenses/                Downloaded asset attribution and full licenses
  config/scenes.properties Scene-to-render manifest
src/test/java/             Game, JDBC, text-file, asset and Swing render tests
asset-sources/audio/       44 unused source WAVs, excluded from the JAR
docs/assets/              Preserved pack README, contact sheet and mapping
```

### Adding your separate state renders later

1. Place the new image under `src/main/resources/images/office/` or `images/cameras/`.
2. Add a named entry in `src/main/resources/config/scenes.properties`, for example:

   ```properties
   cam03.occupied=/images/cameras/cam-03-kitchen-occupied.png
   ```

3. Future game logic can call `session.setSceneState(Scene.CAM_03, "occupied")`. The existing renderer picks that full image up automatically. Use `"default"` to return to the clean room.

Unmapped states fall back to the default image. Missing files explicitly listed in the manifest fail startup with an error rather than silently hiding a broken asset. No enemy behavior is implemented by adding an image alone.

## Tests

`mvnw.cmd clean verify` exercises shift timing, pause, power failure, camera/light exclusion, autonomous camera motion, nonlinear panorama projection, SQLite transactions/idempotency/profile isolation, actual text-file reads/writes, bundled images/WAVs/GIF/font, and headless Swing rendering. Visual previews are generated under **`target/ui-preview/`**. Audio decoding is tested without requiring a speaker/mixer.

An optional real-window smoke check (uses the mouse and keyboard, so run it only when the desktop is free):

```powershell
java --enable-native-access=ALL-UNNAMED "-Dgudo.dataDir=target/desktop-smoke-data" -cp "target/test-classes;target/five-nights-at-gudos.jar" com.gudos.DesktopSmoke
```

This uses isolated saves and captures only the game window under `target/desktop-preview/`.

The distributable is **`target/five-nights-at-gudos.jar`**; dependencies and runtime assets are bundled. The smaller version-numbered JAR is only an intermediate build artifact, not the distributable.

## Asset rights

All supplied assets were preserved; see `docs/assets/asset-manifest.md`. The supplied FNAF audio has **no accompanying redistribution license**. The original game's phone calls, character sounds and scares are archived, not used. Confirm all third-party permissions or replace the sounds before public distribution. This is an unofficial project, not an official FNAF release.

New downloaded assets have documented licenses: **Television static.gif**, GravelTravel22, [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/), from [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Television_static.gif); and **VT323**, The VT323 Project Authors, [SIL OFL 1.1](https://github.com/google/fonts/tree/main/ofl/vt323). The original files are retained unmodified. Noise is alpha-masked/resized at runtime; the adapted noise visuals are available under CC BY-SA 4.0. Full attribution and licenses are bundled in `src/main/resources/licenses/` and the JAR. Reference screenshots were not imported as game assets, and no FNAF character art was added.
