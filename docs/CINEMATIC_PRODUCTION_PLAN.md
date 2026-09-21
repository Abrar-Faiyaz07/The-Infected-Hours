# Ashgrove: The Last Cure — Cinematic Sequence

## Canon

- Elric is a senior field agent of Ashgrove Corporation.
- Jane is a missing Ashgrove agent from an earlier St. Mercy operation.
- The outbreak begins at St. Mercy Hospital and leads to Ashgrove's hidden
  forest research laboratory.
- The final enemy is the lead scientist transformed into the Final Mutation.
- Elric and Jane recover the antidote data and destroy the remaining samples.
- Elena and the former moral-choice/Jane-duel ending are not part of the game.

## Presentation

- The generated images use a 16:9 canvas.
- Cinematics show a dark lower subtitle panel, speaker label, title, and page
  count.
- `E`, Space, Enter, or left click reveals/advances a subtitle.
- `S` skips the current cinematic sequence.
- Optional narration uses one OGG file per image under `assets/audio/voice/`.
- Missing narration never blocks or crashes a cinematic.

## Canonical order

| Order | Image | Placement |
|---:|---|---|
| 0 | `assets/bg_menu.png` | Main menu |
| 1 | `assets/cinematics/intro_elric.png` | Before Map 1 |
| 2 | `assets/cinematics/intro_jane.png` | Before Map 1 |
| 3 | `assets/cinematics/map1_starting.png` | Before Map 1 |
| 4 | `assets/cinematics/map1_part2_starting.png` | After Map 1 |
| 5 | `assets/cinematics/map1_part2_escaping.png` | After Map 1 Part 2 |
| 6 | `assets/cinematics/map2_part1_starting.png` | Before Map 2 Part 1 |
| 7 | `assets/cinematics/map2_part1_ending.png` | After Map 2 Part 1 |
| 8 | `assets/cinematics/map2_part2_starting.png` | Before Map 2 Part 2 |
| 9 | `assets/cinematics/map2_part2_ending.png` | After Map 2 Part 2 |
| 10 | `assets/cinematics/map3_starting.png` | Before Map 3 |
| 11 | `assets/cinematics/map3_ending.png` | After Map 3 |
| 12 | `assets/cinematics/map3_final_starting.png` | Before the final level |
| 13 | `assets/cinematics/map3_final_ending1.png` | After final boss victory |
| 14 | `assets/cinematics/map3_final_ending2.png` | Campaign ending |

`assets/cinematics/ending.png` is not part of the currently approved walkthrough.

## Code

- `DialogueCatalog.java` owns each image, subtitle, speaker, title, and OGG path.
- `StoryPanelScreen.java` renders and synchronizes the cinematics.
- `GameScreen.java` routes Maps 1–5 through the appropriate transitions.
- `BossScreen.java` routes final victory through the ending sequence.
- Normal LAN co-op synchronizes panel advancement and skipping over the host.
- Developer split-screen co-op opens the matching cinematic sequence before
  whichever map is selected in the debug launcher.
