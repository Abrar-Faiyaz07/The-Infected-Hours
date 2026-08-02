# The Infected Hour

2D top-down co-op action game — contain an epidemic, destroy the Virus Heart.
CSE 4402 Visual Programming Lab, Islamic University of Technology.
6-day sprint (v2.0) — see `docs/` for the full specs this skeleton was built from.

## Module layout (TRD §3)

```
infected-hour/
├── shared/       DTOs + KryoNet message classes + constants — no libGDX/Spring deps
├── core/         libGDX game logic: entities, systems, KryoNet networking, HUD, levels
├── lwjgl3/       LWJGL3 desktop backend — booted by fx-launcher, or run directly as a dev shortcut
├── fx-launcher/  JavaFX pre-game app: login, lobby, settings, profile, leaderboards, results
├── backend/      Spring Boot: player profiles, saves, matches, leaderboards (H2 + JWT)
├── docs/         Source specs (PRD, TRD, UI/UX, App Flow, Backend Schema) — authoritative
└── assets/       Sprites, tilesets, audio, fonts (see ASSETS_CREDITS.md)
```

## Architecture at a glance

- **JavaFX ↔ libGDX handoff (TRD §2):** app starts as JavaFX (`fx-launcher`). On match
  start, `GameLauncherBridge` hides the FX stage and boots a libGDX `Lwjgl3Application`
  on its own dedicated thread. Never touch Gdx from the FX thread or vice versa —
  everything crosses through `core.bridge.GameBridge`.
- **Networking (TRD §5) — implemented and tested:** host-authoritative. Laptop A (Elric)
  runs `core.net.GameServer` (KryoNet, TCP 54555 + UDP 54777, 60Hz sim, 20Hz snapshot
  broadcast). Laptop B (Jane) runs `core.net.GameClient` — sends input at 30Hz, renders
  via snapshot interpolation (`SnapshotInterpolator`, 100ms buffer). **No client-side
  prediction** in this build. Lobby discovery is a UDP broadcast on **54778**
  (`shared.net.LanDiscovery`) — *not* 54777, which KryoNet already owns; see
  `docs/07_CHANGE_LOG_LAN_AND_MENU.md` §2.
  The host also runs a client against its own loopback, so both players are created by
  the same join handshake and there is no "local player" special case.
- **Backend (Backend Schema doc):** single Spring Boot instance, run on the host laptop,
  both laptops call the same `http://<host-ip>:8080`. H2 file DB only. JWT auth (HS256,
  12h). `POST /matches/{id}/complete` is the single-transaction source of truth for
  saves + leaderboards (business rules in Backend Schema §7, implemented in `MatchService`).

## Running

```bash
./gradlew :backend:bootRun       # Spring Boot backend (host laptop only)
./gradlew :fx-launcher:run       # full game, each laptop
./gradlew :lwjgl3:run            # dev shortcut: skips JavaFX, boots host-solo directly
./gradlew test                   # all unit + MockMvc/integration tests
```

### Testing co-op without a second laptop

Two windows on one machine exercise the entire networking path — discovery aside:

```bash
./gradlew :lwjgl3:run
```

```bash
./gradlew :lwjgl3:run --args="join"
```

Press `E` in both to clear the ready gate, then move with `WASD` in either window;
both windows render both players from the host's snapshots. `--args="join <ip>"`
targets a real host on the LAN.

> The network tests bind the real ports. Close any running copy of the game before
> `./gradlew test`, or `bind()` fails with "Address already in use".

### Two-laptop checklist

Allow inbound **TCP 54555** and **UDP 54777/54778** for Java on the host — a
firewall prompt nobody clicked is the usual reason "no host answered". Full run
book in `docs/07_CHANGE_LOG_LAN_AND_MENU.md` §6.

## Status

**Done and verified:** the backend, the critical game logic (contamination math,
objective state machine, boss phase machine), the JavaFX main menu, and the whole
LAN networking layer — discovery, join handshake, authoritative 60Hz sim, 20Hz
snapshot broadcast with cloud deltas, snapshot interpolation, disconnect/reconnect,
and the READY sync gates. Two instances connect and share one simulation; see
`Images/verification/` for screenshots and `docs/07_CHANGE_LOG_LAN_AND_MENU.md`
for what changed and why.

**Collision (TRD §4) — implemented and tested.** `core.systems.CollisionSystem`
does circle-vs-tile blocking with axis-separated wall sliding, substepping so
fast movers cannot tunnel, and circle-vs-circle separation between entities. It
is shared by `MovementSystem` and `AISystem` so players and enemies obey one
rule, and it runs host-side only — clients still just render interpolated
snapshots, so **the wire protocol is unchanged**. Diagonal movement is now
normalised (it used to be ~41% faster), and input is clamped host-side so a
client cannot move further by lying about its input vector.

The walkability grid is plain text in `core/src/main/resources/maps/*.map`
(`#` = blocked), parsed by `LevelLoader` into an immutable `level.TileMap`.
**This is not Tiled.** No `.tmx` exists in the repo and `gdx-maps` is not on the
core classpath; the text format keeps level loading free of libGDX so the host
sim stays headless-testable. Swapping to Tiled later means changing
`LevelLoader.loadTileMap` only — nothing downstream sees anything but a `TileMap`.

**Level 1 is traced from `assets/map.png`** (the hospital interior). The art's
grid is **41.2667 px per tile**, origin at image pixel **(32, 32)**, giving
**45 x 33 tiles** over the art's content box; `GameScreen` scales the map
texture to match. Replace `map.png` and you must re-measure those constants and
re-trace the grid.

Collision is stored at **3 cells per tile edge** (a 135 x 99 grid). The art is
hand-drawn and its walls do not sit on the tile grid, so at tile resolution the
collision either ate walkable corridor or let players clip into walls. Gameplay
still works in whole tiles — `ContaminationZone` and the snapshot tile indices
are unchanged.

Doorways are opened to a full tile where the drawn door is not floor-coloured
(18 rooms, 131 cells). This is not cosmetic: a gap the player only just fits
through is one they must line up with exactly, which reads in play as movement
sticking — and a gap narrower still is connected on paper but impossible to walk
through, which is how every room in the hospital ended up unenterable.
`LevelLoaderTest` flood-fills **eroded** space (where the player's disc actually
fits) and asks `CollisionSystem` itself to decide, so a map cannot pass its tests
and then behave differently in play.

Collider radii (`GameConstants`) are a **foot-print, not the sprite outline** —
0.25 tiles against a sprite roughly a tile wide. A collider as wide as the sprite
cannot pass a one-tile doorway with any margin, and forces the level geometry to
be carved open to compensate. Players and enemies share a radius so anywhere a
player can go, an enemy can follow.

`CollisionSystem` also applies **corner assist**: clipping a door frame nudges
the entity up to 0.22 tiles sideways to round it, instead of stopping dead.

### Editing collision by painting the map

The fastest way to correct collision is to draw it. Automatic tracing cannot
tell a door leaf from a wall face from a bed frame — they are the same beige —
so a human marking the map directly overrides everything else.

```bash
java tools/ExportMask.java assets/map.png assets/collision_mask.png
```

Open `assets/collision_mask.png` in any paint program. It is the level art with
the collision grid drawn on it (fine lines per cell, bright yellow per tile).

- **pure red** `(255,0,0)` → force BLOCKED
- **pure green** `(0,255,0)` → force WALKABLE
- leave everything else untouched — it keeps whatever was traced automatically

Paint roughly; a cell goes to whichever colour covers most of it, and antialiased
edges are ignored. **Do not resize or crop the image** — cells are found by pixel
position (the tool refuses a mismatched size). Then:

```bash
# PATCH mode — only what you painted changes; the rest keeps the auto-trace
java tools/GenMap.java assets/map.png core/src/main/resources/maps/level1.map assets/collision_mask.png

# GREEN-ONLY mode — the green you painted IS the walkable world,
# everything not green blocks. Use when you paint the whole map.
java tools/GenMap.java assets/map.png core/src/main/resources/maps/level1.map assets/collision_mask.png green-only
```

That rewrites the grid in place, preserving the file's comment header. The mask
is applied last, so it beats both the auto-trace and `FORCED_DOORS`. It reports
how much green it found — if that reads `0.0% green` you saved the wrong file.

Run `./gradlew build` afterwards: if a painted region seals a room off, the
reachability test fails rather than the playtest.

Regenerate the grid with `tools/GenMap.java` after any change to the map art —
its `RADIUS_CELLS` must stay in step with `GameConstants.PLAYER_COLLISION_RADIUS`.

**Press `F1` in game to overlay the collision grid.** Every blocked cell fills
red over the map. Any future "I got stuck on nothing" report should start here —
it distinguishes a misaligned grid from a misclassified tile in seconds.

Levels 2 and 3 are still placeholder layouts at tile resolution.

**Not done:** everything else that draws or simulates the actual level — enemy
AI behaviour, cloud BFS expansion, sprites, audio, and the Scene2D HUD. In-game
entities are currently coloured quads. These are marked with `TEAMMATE TASK`
blocks.

### Course / PRD Definition of Done checklist (PRD §13)

- [x] Two laptops host/join on LAN (**networking done**; completing all 3 levels needs the level content below)
- [ ] All 6 mission types implemented (`core.systems.ObjectiveSystem` types)
- [ ] 3-phase boss fight functional (`core.systems.BossPhaseSystem` — logic done, rendering TODO)
- [ ] HUD: health, contamination, global meter, inventory, minimap (`core.ui` — structure done, rendering TODO)
- [ ] 4 story panel sequences (`core.screens.StoryPanelScreen` — sync + typewriter done, art/copy TODO)
- [x] Backend live: login, save sync, leaderboard, match history (`backend` module)
- [ ] Stable 60 FPS, no crashes across a full playthrough

### Finding your tasks — the TEAMMATE TASK convention

Every unfinished piece of code is marked with a detailed placeholder block that
explains WHAT to build, HOW to build it step by step, and WHICH doc section it
comes from. To list all of them:

```bash
grep -rn "TEAMMATE TASK" --include="*.java" .
```

Or filter by area (each TODO is tagged):

```bash
grep -rn "TODO(net)" --include="*.java" .        # networking tasks
grep -rn "TODO(screens)" --include="*.java" .    # game screens / flow
grep -rn "TODO(ui)" --include="*.java" .         # HUD / minimap / hotbar
grep -rn "TODO(boss)" --include="*.java" .       # boss fight
grep -rn "TODO(fx)" --include="*.java" .         # JavaFX launcher
grep -rn "TODO(backend)" --include="*.java" .    # backend (very little left)
grep -rn "TODO(level)" --include="*.java" .      # Tiled map loading
grep -rn "TODO(story)" --include="*.java" .      # story panels
```

Tags also make ownership easy: assign each teammate one or two tags and they can
grep exactly their own work.

### Suggested build order

Steps 3 and 5 of the original list (KryoNet payloads + interpolation, and lobby
UDP discovery) are **done**. What is left, in dependency order:

1. ~~`core/level/LevelLoader` — collision grid loading~~ **done** (text `.map`
   grids, not Tiled — see the Status section for why).
2. ~~`core/systems/MovementSystem` — diagonal normalisation + collision~~ **done**
   (`core/systems/CollisionSystem`).
3. `core/systems/ContaminationSystem.computeFrontierExpansion` — BFS cloud growth.
   The walkability check it needs now exists: use `TileMap.isWalkable(tileIndex)`,
   and note tile indices are row-major `y * width + x` on both sides.
   Returns an empty set today, so clouds never expand; the snapshot delta pipeline
   around it is finished and tested.
4. `core/systems/AISystem` — enemy chase and attack. Collision is already wired
   in: step enemies with `collisionSystem.moveWithCollision(enemy, dx, dy)` and
   never `enemy.move(...)` directly. Nothing spawns enemies yet either — no code
   calls `GameServer.addEnemy`.
5. `core/screens/GameScreen.drawWorld` — sprites + a camera following
   `client.findLocalPlayer(snapshot)`, replacing the placeholder quads.
6. `core/ui/Hud` — actual Scene2D widgets per UI/UX doc §3 layout.
7. `backend.service.MatchService.upsertLeaderboardEntries` — currently a TODO stub.
