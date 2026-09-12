# assets/

Everything in this folder is placed on `core`'s runtime classpath by
`core/build.gradle.kts`, so `Gdx.files.internal("map.png")` finds it in both
`:lwjgl3:run` and `:fx-launcher:run`.

## Files the game currently asks for

`core/screens/GameScreen` loads these five. **None of them are in the repo yet** —
they were referenced by commit `a7b7047`/`f7cfea4` but the image files were never
committed, so the game fell back to placeholders.

| File | Expected shape | Used for |
|---|---|---|
| `map.png` | single image | Level background |
| `player.png` | sprite sheet, **8 columns x 4 rows** | Player walk cycle |
| `player_idle.png` | sprite sheet, **8 columns x 4 rows** | Player idle animation |
| `zombie.png` | sprite sheet, **8 columns x 4 rows** | Enemy animation |
| `inventory.png` | single image | Inventory panel background |

The 8x4 grid matters: `GameScreen` slices these with
`TextureRegion.split(texture, width / 8, height / 4)`. A sheet with a different
row/column count will slice into the wrong frames rather than fail loudly.

## What happens when a file is missing

`core/assets/GameAssets` substitutes a magenta/black checkerboard of the correct
dimensions instead of throwing. The game keeps running and the missing names are
listed by `GameAssets.getMissingAssets()`.

This is deliberate: textures load inside `Screen.show()`, so an exception there
escapes the render loop and terminates the entire application. One uncommitted
PNG would otherwise take down networking, the menu and everything else.

Drop the real files in here and they are picked up on the next build — no code
change needed.

## Licensing

Every third-party asset must be CC0 or CC-BY and recorded in
`ASSETS_CREDITS.md` at the repo root before it is committed.
