# CARINDERIA: Night Shift — Environment Render Pack v2

This archive contains **7 clean, ultrawide render assets** for a FNAF-inspired Filipino carinderia night-shift game: **1 master office panorama + 6 CCTV camera views**.

## Render index

| File (inside `renders/`) | In-game ID | Location | Purpose |
|---|---|---|---|
| `OFFICE_00_Master_Panorama.png` | OFFICE (player) | Office / player view | One stationary 21:9 panoramic image intended for horizontal panning in a 16:9 game; serving counter on left, dining/front entrance at center, side corridor at right. |
| `CAM_01_Dining_Area.png` | CAM 01 | CAM 01 — Dining area | Overhead view of guest tables and central aisle. Primary approach from the street/front entrance toward the player. |
| `CAM_02_Serving_Counter.png` | CAM 02 | CAM 02 — Serving counter | Overhead view of ulam display and service station on the office’s left; leads to the kitchen. |
| `CAM_03_Kitchen.png` | CAM 03 | CAM 03 — Kitchen | Back cooking room with stoves, pans, metal counters; adjacent to the serving counter and stockroom. |
| `CAM_04_Stockroom.png` | CAM 04 | CAM 04 — Stockroom | Cramped supply storage with rice sacks, crates, and boxes; behind the kitchen. |
| `CAM_05_Side_Corridor.png` | CAM 05 | CAM 05 — Side corridor | Right-hand utility corridor beside the dining room/office, approaching the rear service exit. |
| `CAM_06_Back_Alley.png` | CAM 06 | CAM 06 — Back alley | Exterior rear service entrance connected to the side corridor/service area. |

## Updated camera plan

The previous **second generated image** (the storefront / front-entrance camera) has been **excluded from this pack** because its layout did not match the rest of the environment. The remaining CCTV cameras have been renumbered in order, without a gap.

```text
                     FRONT / PUBLIC ENTRANCE
                               |
                     [CAM 01] Dining Area
                         /           \
         [CAM 02] Serving Counter   [CAM 05] Side Corridor
                        |                     |
               [CAM 03] Kitchen       [CAM 06] Back Alley
                        |
               [CAM 04] Stockroom

                  [OFFICE] Player station
        (left: serving / center: dining / right: corridor)
```

This is a **gameplay connection map**, not a measured architectural floor plan. The office overlooks the dining room, with a left-side service/kitchen threat route and a right-side corridor/alley threat route.

### Suggested enemy paths

- **Dining / center:** Street (off camera) -> CAM 01 -> office front.
- **Service / left:** CAM 04 -> CAM 03 -> CAM 02 -> office left.
- **Utility / right:** CAM 06 -> CAM 05 -> office right.

## Image specs and in-game use

- All 7 source renders are **1916 x 821 pixels**, approximately **21:9** (the generation system outputs this nearby pixel ratio).
- No enemies or CCTV HUDs are baked into the base renders. Add enemy overlays, static, scanlines, timestamps and camera buttons separately in your game engine.
- For a 16:9 gameplay window using the office panorama at full image height, show about **1460 x 821 pixels** of the panorama at once and pan over the remaining **456 horizontal pixels** (subject to engine scaling/viewport rounding).
- Use `OFFICE_00_Master_Panorama.png` as the horizontal-panning player view. The CAM files are still security-camera viewpoints.

## Included files

- `renders/`: 7 PNG game environment assets
- `docs/contact_sheet.jpg`: quick visual reference with their current filenames and in-game IDs
- `README.md`: this camera manifest, removal note, and updated route plan

## Continuity note

These are AI-generated concept/gameplay backgrounds, not camera views exported from a single 3D model. They share the visual design but may contain small architectural/prop differences. The deleted storefront render is not included in this archive.
