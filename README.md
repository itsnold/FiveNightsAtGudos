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
- Directional flashlight, live power usage, a six-minute 12 AM–6 AM shift, and pause/resume.
- Sir Gudo follows three connected routes, appears in six occupied CCTV renders, and arrives at the left, center or right of the office.
- Hold the flashlight while facing his approach to make him fade away. Breaking contact lets him recover. He returns by a different route after retreating.
- Generated close-up jumpscare with runtime lunge, shake and directional blur, plus original synthesized footsteps, warning, retreat and scare cues. Reduced visual effects keeps the scare still.
- Completed, power-failure and caught results. Five unlockable nights with increasingly fast movement and shorter defense windows.
- Automatic pause on focus loss and borderless fullscreen.
- Persistent local profiles, progress, attempt history, text preferences and shift reports.

**Survive Sir Gudo:** watch his progress on CCTV, lower the monitor when he reaches the office, face his doorway with A/D or the mouse edges, and hold F until he completely disappears. Release the flashlight between encounters to conserve power. Footsteps and a direction hint announce an office arrival even while the monitor is open. Ignoring him causes a jumpscare and a recorded loss.

| Approach | Camera route | Office direction |
| --- | --- | --- |
| Service | CAM 04 stockroom → CAM 03 kitchen → CAM 02 counter | Left |
| Utility | CAM 06 alley → CAM 05 corridor | Right |
| Dining | CAM 01 dining room | Center |

The routes cycle in this order after successful defenses. Night 1 gives nine seconds at the office and needs 1.6 seconds of light; Night 5 gives 5.8 seconds and needs 2.4 seconds. A failed defense does not unlock the next night. Power failure remains its own outcome.

### Controls

| Control | Action |
| --- | --- |
| Mouse near left/right edges, A/D or arrow keys | Pan across the office |
| Space or Tab | Raise/lower the monitor |
| 1–6, or camera map buttons | Switch camera while the monitor is open |
| Hold F, or hold the light button | Flashlight in the office; face his doorway until he fades |
| Esc | Pause/resume |
| F11 | Borderless fullscreen |

Finish a shift to unlock the next. New Game starts Night 1 without deleting existing records; Continue starts the next unlocked night. Replaying never resets progress. The monitor and light consume additional power, and cannot be used together. Clock, power, Sir Gudo and automatic CCTV movement stop while paused. Leaving or normally closing during a shift records an abandoned attempt. Force-killing the process cannot save an unfinished attempt. **Continue does not restore a mid-shift checkpoint.**

## Text files and database

Writable data is kept **outside the application/JAR**:

- Windows: `%LOCALAPPDATA%\FiveNightsAtGudos\`
- Other platforms: `~/.five-nights-at-gudos/`

| File | Actual use |
| --- | --- |
| `settings.txt` | UTF-8 key/value preferences, created on first launch, read at startup and atomically rewritten on Apply or F11. |
| `shift-log.txt` | A readable UTF-8 report appended after a completed, caught, power-failed or abandoned attempt. Save retries do not duplicate entries. |
| `gudo.db` | SQLite tables `profiles`, `progress`, and `night_runs`; queried for Continue, night unlocking and the records screen. |

Change your player name in Settings to select/create a separate local profile. Names are case-insensitive; this is not an online account or authentication system. SQLite is authoritative for progress; the text report is not used as a substitute database. Storage operations run off Swing's event thread, SQL uses prepared statements, and attempt/progress writes are transactional.

Existing version-1 databases automatically migrate to version 2 in a transaction so `CAUGHT` can be stored while preserving profiles, progress and old runs. A caught attempt begins saving while its 2.1-second jumpscare plays.

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
  game/                    UI-independent shift, power, Sir Gudo routes, camera sweep
  persistence/             SQLite, settings.txt, shift-log.txt
  ui/                      Custom Java2D scenes and Swing components
src/main/resources/
  images/office/           Original panorama, renamed consistently
  images/cameras/          Six clean and six occupied camera images
  images/entities/         Generated Sir Gudo jumpscare
  audio/                   Ambience, UI effects and four original encounter cues
  effects/                 Downloaded animated television-static GIF
  fonts/                   Bundled VT323 pixel font
  licenses/                Downloaded asset attribution and full licenses
  config/scenes.properties Scene-to-render manifest
src/test/java/             Game, JDBC, text-file, asset and Swing render tests
asset-sources/audio/       44 unused source WAVs, excluded from the JAR
docs/assets/              Preserved pack README, contact sheet and mapping
```

### Room states and generated art

The original seven room images are preserved. Ten new images were created using the built-in GPT Image tool with the supplied menu portraits as Sir Gudo's identity reference. Prompts and saved asset paths are recorded in `docs/assets/sir-gudo-prompts.md`. The three office states are blended back toward the clean panorama during flashlight exposure; intermediate fading frames are rendered at runtime. The jumpscare uses a separate 16:9 still, animated in Java2D.

To add another state:

1. Place the new image under `src/main/resources/images/office/` or `images/cameras/`.
2. Add a named entry in `src/main/resources/config/scenes.properties`, for example:

   ```properties
   cam03.occupied=/images/cameras/cam-03-kitchen-occupied.png
   ```

3. Game logic can call `session.setSceneState(Scene.CAM_03, "custom-state")`. The renderer picks that full image up automatically. Sir Gudo's actual location takes priority over custom states. Use `"default"` to return to the clean room.

Unmapped states fall back to the default image. Missing files explicitly listed in the manifest fail startup with an error rather than silently hiding a broken asset. Enemy timing and route state live in `GudoEncounter`; `GameSession` splits updates at encounter, power and morning deadlines so behavior is independent of frame size.

The four new PCM audio cues are synthesized specifically for this game. Reproduce them from the repository root with `powershell -File tools/generate-encounter-audio.ps1`; no extra software or downloaded character audio is needed.

## Tests

`mvnw.cmd clean verify` exercises shift timing, all three enemy routes, gradual retreat and recovery, directional flashlight defense, pause, attack/morning precedence, full survival on all five nights, power failure, camera/light exclusion, camera motion, panorama projection, SQLite migration/transactions/profile isolation, text files, bundled assets and headless Swing rendering. Visual previews of occupied cameras, all three office approaches, partial fade, retreat, animated/reduced jumpscare and caught results are generated under **`target/ui-preview/`**. Audio decoding is tested without requiring a speaker/mixer.

An optional real-window smoke check (uses the mouse and keyboard, so run it only when the desktop is free):

```powershell
java --enable-native-access=ALL-UNNAMED "-Dgudo.dataDir=target/desktop-smoke-data" -cp "target/test-classes;target/five-nights-at-gudos.jar" com.gudos.DesktopSmoke
```

This uses isolated saves and captures only the game window under `target/desktop-preview/`.

The distributable is **`target/five-nights-at-gudos.jar`**; dependencies and runtime assets are bundled. The smaller version-numbered JAR is only an intermediate build artifact, not the distributable.

## Asset rights

All supplied assets were preserved; see `docs/assets/asset-manifest.md`. The supplied FNAF audio has **no accompanying redistribution license**. The original game's phone calls, character sounds and scares are archived, not used. Confirm all third-party permissions or replace the sounds before public distribution. This is an unofficial project, not an official FNAF release.

New downloaded assets have documented licenses: **Television static.gif**, GravelTravel22, [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/), from [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Television_static.gif); and **VT323**, The VT323 Project Authors, [SIL OFL 1.1](https://github.com/google/fonts/tree/main/ofl/vt323). The original files are retained unmodified. Noise is alpha-masked/resized at runtime; the adapted noise visuals are available under CC BY-SA 4.0. Full attribution and licenses are bundled in `src/main/resources/licenses/` and the JAR. Reference screenshots were not imported as game assets, and no FNAF character art was added.
