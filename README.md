# The Infected Hour

2D top-down co-op action game — contain an epidemic, destroy the Virus Heart.
CSE 4402 Visual Programming Lab, Islamic University of Technology.
6-day sprint (v2.0) — see `docs/` for the full specs this skeleton was built from.

Boss/combat implementation research and the adaptation plan for the Level 3
Virus Heart fight are documented in
[`docs/09_PROMISE_BENEATH_THE_STORM_REFERENCE.md`](docs/09_PROMISE_BENEATH_THE_STORM_REFERENCE.md).

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

**Not done:** everything that draws or simulates the actual level — Tiled map
loading, collision, enemy AI, cloud BFS expansion, sprites, audio, and the Scene2D
HUD. In-game entities are currently coloured quads. These are marked with
`TEAMMATE TASK` blocks.

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

1. `core/level/LevelLoader` — real Tiled `.tmx` loading + collision extraction.
   Everything below needs the tile grid this produces.
2. `core/systems/MovementSystem` — normalise diagonal input and check the collision
   grid before moving (diagonal movement is currently ~41% faster than orthogonal).
3. `core/systems/ContaminationSystem.computeFrontierExpansion` — BFS cloud growth.
   Returns an empty set today, so clouds never expand; the snapshot delta pipeline
   around it is finished and tested.
4. `core/systems/AISystem` — enemy chase and attack.
5. `core/screens/GameScreen.drawWorld` — sprites + a camera following
   `client.findLocalPlayer(snapshot)`, replacing the placeholder quads.
6. `core/ui/Hud` — actual Scene2D widgets per UI/UX doc §3 layout.
7. `backend.service.MatchService.upsertLeaderboardEntries` — currently a TODO stub.
