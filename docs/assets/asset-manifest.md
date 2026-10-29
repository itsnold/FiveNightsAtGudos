# Asset organization and provenance

All **61 supplied files** were retained: 7 PNG backgrounds, 52 WAVs, the render-pack README, and its contact sheet. No image or sound was re-encoded. Only locations and the names of runtime assets changed.

## Backgrounds

Original source directory: `resources/Carinderia_FNAF_Render_Pack_v2/renders/`.

| Original | New location under `src/main/resources/` |
| --- | --- |
| `OFFICE_00_Master_Panorama.png` | `images/office/office-panorama.png` |
| `CAM_01_Dining_Area.png` | `images/cameras/cam-01-dining.png` |
| `CAM_02_Serving_Counter.png` | `images/cameras/cam-02-counter.png` |
| `CAM_03_Kitchen.png` | `images/cameras/cam-03-kitchen.png` |
| `CAM_04_Stockroom.png` | `images/cameras/cam-04-stockroom.png` |
| `CAM_05_Side_Corridor.png` | `images/cameras/cam-05-corridor.png` |
| `CAM_06_Back_Alley.png` | `images/cameras/cam-06-alley.png` |

Original `README.md` is now `docs/assets/render-pack-readme.md`. Original `docs/contact_sheet.jpg` is now `docs/assets/contact-sheet.jpg`. These describe AI-generated backgrounds, not a measured architectural model. The map follows the supplied gameplay route plan.

## Active audio

Original source directory: `resources/PC _ Computer - Five Nights at Freddy's - Miscellaneous - Sound Effects/FNAF1 sound/`.

| Original | New location under `src/main/resources/audio/` |
| --- | --- |
| `BallastHumMedium2.wav` | `ambience/electrical-hum.wav` |
| `Buzz_Fan_Florescent2.wav` | `ambience/office-fan.wav` |
| `blip3.wav` | `effects/camera-switch.wav` |
| `CAMERA_VIDEO_LOA_60105303.wav` | `effects/monitor-open.wav` |
| `put down.wav` | `effects/monitor-close.wav` |
| `chimes 2.wav` | `effects/shift-complete.wav` |
| `powerdown.wav` | `effects/power-down.wav` |
| `error.wav` | `effects/error.wav` |

## Archived audio

Menu audio update: `darkness music.wav` and `static2.wav` are now also copied into the runtime bundle as `audio/music/menu-darkness.wav` and `audio/effects/menu-static.wav`. Both loop together on menu load and stop when entering gameplay. Their original archive copies are retained. `ambience2.wav` and `ColdPresc B.wav` remain unused.

The other **44 WAV files** retain their original filenames under `asset-sources/audio/`, organized into `ambience/`, `character/`, `effects/`, `music/`, and `voiceovers/`. They are excluded from Maven resources and are not played by the game. This preserves future references without bundling unused voice dialogue, scares or large music files.

## Rights notice

The files were supplied with the project; no audio license, author attribution beyond the pack name, or redistribution grant accompanied them. Attribution here is provenance, **not permission**. The FNAF audio belongs to its respective rights holders. Verify rights or replace it before publishing. Preserve any additional source/license information you obtain here.

## Sir Gudo encounter assets (29 October 2026)

Ten new images were generated using the built-in GPT Image tool, editing the existing rooms with the supplied menu portraits as Sir Gudo's character references. The seven original backgrounds and menu images remain unchanged.

- Six occupied camera renders: `src/main/resources/images/cameras/cam-01-dining-occupied.png`, `cam-02-counter-occupied.png`, `cam-03-kitchen-occupied.png`, `cam-04-stockroom-occupied.png`, `cam-05-corridor-occupied.png`, `cam-06-alley-occupied.png`.
- Three office renders: `src/main/resources/images/office/office-left-occupied.png`, `office-front-occupied.png`, `office-right-occupied.png`.
- Jumpscare still: `src/main/resources/images/entities/sir-gudo-jumpscare.png`.

The complete prompt set is saved in `docs/assets/sir-gudo-prompts.md`. The flashlight fade and jumpscare lunge/shake/blur are runtime Java2D effects. All state assets are bundled and registered in `config/scenes.properties`.

Four original procedural mono PCM sounds were also added under `src/main/resources/audio/effects/`: `gudo-step.wav`, `gudo-warning.wav`, `gudo-retreat.wav`, and `gudo-jumpscare.wav`. Their reproducible source is `tools/generate-encounter-audio.ps1`. The archived third-party character/scare WAVs remain unused.

## Downloaded effects and font (22 October 2026)

These are additional files, not replacements for any of the original 61 supplied assets.

| Asset | Source | License | Bundled location |
| --- | --- | --- | --- |
| Television static.gif — GravelTravel22 | https://commons.wikimedia.org/wiki/File:Television_static.gif | CC BY-SA 4.0 | `effects/television-static.gif` |
| VT323 Regular — The VT323 Project Authors | https://github.com/google/fonts/tree/main/ofl/vt323 | SIL OFL 1.1 | `fonts/VT323-Regular.ttf` |

Paths above are under `src/main/resources/`. The GIF and font are unmodified downloads. For overlay rendering, GIF frames are decoded, alpha-masked and scaled at runtime, with additional tracking bands and scanlines. Adapted noise visuals are available under CC BY-SA 4.0. Full source links, change notices and both full license texts are in `src/main/resources/licenses/`, also bundled inside the game JAR. Downloaded noise replaces the previous generated random-noise frames; no network access occurs during gameplay.

The supplied FNAF reference screenshots guide layout only. They are not bundled or used as backgrounds/character sprites.
