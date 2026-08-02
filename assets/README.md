# assets/

Runtime assets loaded via `Gdx.files.internal("<name>")`. Both `:lwjgl3:run` and
`:fx-launcher:run` set their working directory here (see each module's
`build.gradle.kts`), so paths are relative to this folder.

## Files the game currently expects

`core/screens/GameScreen` loads these by name. A missing file throws on
`show()`, so all five must be present to launch a match:

| File              | What it is                     | Layout expected by GameScreen        |
|-------------------|--------------------------------|--------------------------------------|
| `map.png`         | Level background               | Drawn at origin, 48 px per tile      |
| `player.png`      | Player walk cycle              | Sprite sheet, **8 columns x 4 rows** |
| `player_idle.png` | Player idle animation          | Sprite sheet, **8 columns x 4 rows** |
| `zombie.png`      | Infected enemy animation       | Sprite sheet, 8 columns x 4 rows     |
| `inventory.png`   | Hotbar slot frame              | Single image                         |

Row order in the 4-row sheets is the facing direction. `GameScreen` indexes
rows directly, so if a sheet's rows are ordered differently the character will
face the wrong way — check that before assuming the animation code is broken.

## Collision is NOT read from these images at runtime

The walkability grid lives in `core/src/main/resources/maps/*.map` as plain text
and is parsed by `core/level/LevelLoader`. That keeps the host simulation
headless and unit-testable.

`level1.map` was traced from `map.png` once, offline. The art uses a
**41.2667 px** tile grid with its origin at image pixel **(32, 32)**, giving
45x33 tiles over the content box (x 32..1888, y 32..1394); `GameScreen` scales
the texture so the two grids coincide. Collision is stored three cells per tile
edge, so the file is 135x99.

Sprites are anchored by their **feet**, not their centre — an entity's position
is its ground point, which is what collision tests. `SPRITE_FEET_INSET_PX` in
`GameScreen` is the transparent padding below the feet in a frame; if you swap
in sheets with different padding, update it or characters will appear to stand
off the ground.

**If you replace `map.png`, the grid does not follow.** You must re-measure the
art's tile pitch and origin, update the constants in `GameScreen`, and re-trace
`level1.map` — otherwise players collide with walls that are not drawn, and walk
through walls that are. `LevelLoaderTest` will catch a ragged grid, a blocked
spawn, or an unreachable pocket, but it cannot tell you the grid is misaligned
with the art.

See `ASSETS_CREDITS.md` at the repo root for licensing — every third-party pack
must be credited there before the project is submitted.
