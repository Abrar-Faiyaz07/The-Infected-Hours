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
- **Networking (TRD §5):** host-authoritative. Laptop A (Elric) runs `core.net.GameServer`
  (KryoNet, TCP 54555 + UDP 54777, 60Hz sim, 20Hz snapshot broadcast). Laptop B (Jane)
  runs `core.net.GameClient` — sends input at 30Hz, renders via snapshot interpolation
  (`SnapshotInterpolator`, 100ms buffer). **No client-side prediction** in this build.
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

## Status — this is a skeleton

Every module compiles-shaped and matches the module/package layout in the TRD exactly,
so file paths line up with what the docs describe. Business-critical logic is real
(ImmunityBar-equivalent contamination math, objective state machine, boss phase state
machine, JWT auth, match-complete transaction) and unit/integration tested. Rendering,
input wiring, actual KryoNet payload population, Tiled map loading, and most Scene2D/JavaFX
widget construction are marked `// TODO` — these are the next things to build.

### Course / PRD Definition of Done checklist (PRD §13)

- [ ] Two laptops host/join and complete all 3 levels co-op on LAN
- [ ] All 6 mission types implemented (`core.systems.ObjectiveSystem` types)
- [ ] 3-phase boss fight functional (`core.systems.BossPhaseSystem` — logic done, rendering TODO)
- [ ] HUD: health, contamination, global meter, inventory, minimap (`core.ui` — structure done, rendering TODO)
- [ ] 4 story panel sequences (`core.screens.StoryPanelScreen` — structure done, content TODO)
- [ ] Backend live: login, save sync, leaderboard, match history (**done** — `backend` module)
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

### Suggested build order for Claude Code

1. `core/level/LevelLoader` — real Tiled `.tmx` loading + collision extraction
2. `core/screens/GameScreen` — wire real input → `GameServer.fixedTimestepUpdate` → rendering from snapshot
3. `core/net/GameServer` / `GameClient` — populate `WorldSnapshot` fully, real `SnapshotInterpolator` lerp
4. `core/ui/Hud` — actual Scene2D widgets per UI/UX doc §3 layout
5. `fx-launcher` lobby discovery (UDP broadcast) to make Host/Join Lobby actually find each other
6. `backend.service.MatchService.upsertLeaderboardEntries` — currently a TODO stub
