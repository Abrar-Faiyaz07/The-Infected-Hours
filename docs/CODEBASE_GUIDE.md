# The Infected Hour — Codebase Guide (for humans, zero knowledge assumed)

This document explains **every folder and every file** in the project: what it
is, why it exists, and how it connects to everything else. Read it top to
bottom once, then use it as a lookup table.

> **Current state of the code:** this is a *skeleton*. Every class exists with
> the right name, the right package, the right relationships (extends /
> implements), and the right method signatures — but almost every method body
> is a `// TODO: implement`. Nothing "plays" yet. That is intentional: the
> structure was built first so the team can fill in logic without arguing
> about where things go.

---

## 1. The big picture

The game is a **2-player cooperative LAN game** built with **libGDX** (a Java
game framework). One player hosts, the other joins. There is also a small
**Spring Boot web server** for accounts, cloud saves, and leaderboards.

The code is split into **5 modules** (think: 5 mini-projects that build
separately and depend on each other in one direction only):

```
game-desktop  ──►  game-core          (the game window uses the game rules)
game-desktop  ──►  network ──► shared (the game window talks LAN via packets)
backend       ──►  shared             (the web server reuses the same DTOs)
```

| Module | One-line job | Allowed to use |
|---|---|---|
| `game-core` | The game's brain: entities, missions, world, rules | libGDX |
| `game-desktop` | The game's face: window, rendering, UI, sound, camera, multiplayer glue | libGDX + game-core + network |
| `network` | Raw LAN plumbing: sockets and packets | **nothing** (pure Java + shared) |
| `shared` | Tiny common types both game and backend understand | **nothing** (pure Java) |
| `backend` | Web server: login, saves, leaderboard (REST API + MySQL) | Spring Boot + shared |

**Why the strict separation?** So a change in one area can't silently break
another. The `network` module deliberately knows nothing about libGDX or the
game — it just moves bytes. If we ever swap the game engine, the networking
code doesn't change.

---

## 2. Files at the project root

| File | What it is |
|---|---|
| `settings.gradle` | Tells Gradle (the build tool) which 5 modules exist. |
| `build.gradle` | Rules shared by ALL modules: use Java 21, use JUnit 5 for tests. |
| `gradle/libs.versions.toml` | One central list of library versions (libGDX 1.13.1, Spring Boot 3.5.6, …) so no module pins its own conflicting version. |
| `gradlew` / `gradlew.bat` | The "Gradle wrapper" — lets anyone build with `./gradlew build` **without installing Gradle**. Use this, always. |
| `.gitignore` | Tells git to ignore generated stuff (`build/`, IDE files). |
| `CLAUDE.md` | Project checkpoint: where we are, decisions made, open questions. |
| `docs/Architecture.md` | The binding design document everything was built from. |
| `docs/uml/*.puml` | UML diagrams (class diagrams, networking sequence, database ERD). |
| `docs/SESSION_LOG.md` | Chronological log of what each work session did. |

**How to build:** open a terminal in the project root and run
`./gradlew build`. All 5 modules compile. (The backend compiles but won't
*run* until a MySQL datasource is configured — that's a later milestone.)

---

## 3. Module `game-core` — the game's brain

Path: `game-core/src/main/java/com/theinfectedhour/`

This is where all game *rules* live: what a player is, what an enemy does,
what a mission means. It renders nothing and opens no windows.

### 3.1 `core/` — the heartbeat

| File | What it does |
|---|---|
| `GameApplication.java` | The top-level "game program" object that owns the main loop lifecycle (create → update every frame → dispose). |
| `GameManager.java` | **Singleton** (a class guaranteed to have exactly one instance — see §8 patterns). The central coordinator that ticks all the managers each frame. You get it with `GameManager.getInstance()`. |
| `GameStateManager.java` | Keeps track of *which mode the game is in* (menu? playing? paused?) and forwards each frame's `update()` to the current mode. Switching modes = swapping one object. |

### 3.2 `core/state/` — the game modes (State pattern)

The game is always in exactly one of these "states". Each state is a class,
so the code never needs a giant `if (mode == PAUSED) ... else if ...` chain.

| File | What it does |
|---|---|
| `GameState.java` | The interface every mode implements: `onEnter()`, `update(delta)`, `onExit()`. |
| `MenuState.java` | Game is sitting on the main menu. |
| `PlayingState.java` | Actual gameplay is running. |
| `PausedState.java` | Gameplay frozen, pause menu shown. |
| `GameOverState.java` | The run ended (win or lose). |

### 3.3 `entities/` — everything that exists in the world

The small interfaces first — these are "capability badges" a class can wear:

| File | What it does |
|---|---|
| `Updatable.java` | "I have an `update(float deltaTime)` method — tick me every frame." (`deltaTime` = seconds since last frame.) |
| `Renderable.java` | "I can draw myself: `render(SpriteBatch)`." |
| `Movable.java` | "I can move." |
| `Damageable.java` | "I have health and can take damage." |
| `Interactable.java` | "The player can press E on me." |

Then the class family tree:

```
Entity  (abstract: has an id + position, is Updatable + Renderable)
 └── Character  (abstract: adds health/Damageable + an AbilitySet)
      ├── Player        ├── Enemy (abstract)      └── VirusHeart (the boss)
      │    ├── FieldMedic     └── MutatedCell
      │    └── LocalScout
```

| File | What it does |
|---|---|
| `Entity.java` | Base of everything placed in the world: an id and a `position`. |
| `Character.java` | An entity that is *alive*: health, damage, plus an `AbilitySet` it carries (composition — the ability is a separate object plugged in, not inherited). ⚠️ Name clash: always `import com.theinfectedhour.entities.Character;` explicitly, or Java thinks you mean `java.lang.Character`. |
| `AbilitySet.java` | Interface for a character's special ability (`useAbility(owner)`). Medic heals, Scout scouts — same slot, different plug. |

### 3.4 `entities/player/`

| File | What it does |
|---|---|
| `Player.java` | A human-controlled character; owns an `Inventory` and has a `PlayerRole`. |
| `PlayerRole.java` | Enum: which of the two roles this player is. |
| `FieldMedic.java` | Player subclass — the healer role. |
| `LocalScout.java` | Player subclass — the scout role. |

### 3.5 `entities/enemy/` and `entities/enemy/ai/` (Strategy pattern)

Enemies don't hardcode their behaviour. Each enemy *carries* an `EnemyAI`
object that decides its next action — swap the object, swap the behaviour.

| File | What it does |
|---|---|
| `Enemy.java` | Abstract enemy; composes one `EnemyAI` brain. |
| `MutatedCell.java` | The basic enemy type. |
| `EnemyFactory.java` | **Factory**: give it a string id ("mutated_cell"), get a fully-built enemy back. Spawning code never calls `new MutatedCell()` directly. |
| `ai/EnemyAI.java` | The brain interface: `Action decideAction(WorldContext)`. |
| `ai/SwarmBehavior.java` | Brain #1: rush the players in groups. |
| `ai/GuardBehavior.java` | Brain #2: hold a position, attack what comes close. |
| `ai/Action.java` | What a brain returns ("move there", "attack that"). |
| `ai/WorldContext.java` | What a brain is allowed to see (player positions etc.) — keeps AI from reaching into everything. |

### 3.6 `entities/boss/` and `entities/boss/phases/` (State pattern again)

| File | What it does |
|---|---|
| `VirusHeart.java` | The final boss. Note: extends `Character`, not `Enemy` — it doesn't use the normal enemy AI system; it uses phases instead. |
| `phases/BossPhase.java` | Interface for one phase of the fight. |
| `phases/ShieldPhase.java` | Phase 1: boss is invulnerable until players do something. |
| `phases/ExposurePhase.java` | Phase 2: the weak point is exposed. |
| `phases/CoreDestructionPhase.java` | Phase 3: final destruction sequence. |

### 3.7 `world/` — the map and level progression ⭐ (the Resident-Evil system)

| File | What it does |
|---|---|
| `Level.java` | One playable map: holds the `TiledMap` (loaded from a `.tmx` file made in the Tiled editor), its list of `MapSection`s, and spawn points. |
| `MapSection.java` | **One named region of the map** with a rectangle boundary, an unlock flag, and `requiredMissionId` — the mission that must finish before this section opens. First section has no requirement → starts unlocked. |
| `MapLoader.java` | Loads `.tmx` files with libGDX's `TmxMapLoader` and reads the object layers we author in Tiled: `spawns`, `sections`, `gates`, `contamination_zones`. |
| `LevelManager.java` | Owns the active level. **This is where the RE-style flow lives:** it listens on the EventBus for `MissionCompletedEvent`; when one arrives it unlocks every section gated on that mission and publishes `SectionUnlockedEvent` so the camera and minimap react. Missions never talk to the world directly. |

### 3.8 `world/infection/` — the contamination mechanic

| File | What it does |
|---|---|
| `ContaminationZone.java` | A polluted area of the map (authored in Tiled). Standing in it is bad. |
| `ContaminationSystem.java` | Tracks global contamination %, spreads it over time; hitting 100% = lose. |
| `SanitationPoint.java` | An interactable station players activate to push contamination back. |

### 3.9 `world/objects/` — placed props

| File | What it does |
|---|---|
| `SupplyCrate.java` | Interactable crate that drops items. |
| `Barricade.java` | Damageable obstacle — enemies can break it, blocks paths. |
| `VillagerNPC.java` | Interactable NPC to rescue (mission target). |

### 3.10 `missions/` — objectives

| File | What it does |
|---|---|
| `Mission.java` | One mission: a set of objectives + completion state. |
| `MissionObjective.java` | Interface for a single objective ("is it done yet?"). |
| `MissionManager.java` | Tracks active missions; when one completes it publishes `MissionCompletedEvent` → which unlocks map sections (see §3.7). |
| `CollectSampleObjective.java` | Objective: collect a cure sample. |
| `DeliverMedicineObjective.java` | Objective: bring medicine somewhere. |
| `RescueVillagerObjective.java` | Objective: save a villager. |
| `ActivateSanitationObjective.java` | Objective: turn on a sanitation point. |
| `IsolateZoneObjective.java` | Objective: seal off a contamination zone. |

### 3.11 `inventory/`

| File | What it does |
|---|---|
| `Item.java` | Base item type. |
| `CureSample.java` / `Medicine.java` | Concrete items. |
| `Inventory.java` | A player's item container. |
| `ItemFactory.java` | **Factory**: string id in → item object out. |

### 3.12 `input/` — keyboard → actions (Command pattern)

Keys don't call game code directly. A key press creates a small "command
object", which is executed. Why: commands can be queued, sent over the
network (Player 2 sends *inputs*, never positions!), and undone.

| File | What it does |
|---|---|
| `GameCommand.java` | Interface: `execute()` / `undo()`. |
| `MoveCommand.java` | "Move this player in this direction." |
| `InteractCommand.java` | "This player interacts with that object." |
| `InputManager.java` | Reads raw keys, produces command objects. |

### 3.13 `events/` — the EventBus (Observer pattern) ⭐

The project's decoupling backbone. Instead of system A calling system B
directly, A *publishes an event* and anyone interested *subscribes*.

| File | What it does |
|---|---|
| `EventBus.java` | **Singleton** message hub: `subscribe(type, listener)`, `publish(event)`. |
| `GameEvent.java` | Marker interface — anything publishable implements it. |
| `MissionCompletedEvent.java` | "Mission X is done" — published by MissionManager. |
| `SectionUnlockedEvent.java` | "Map section Y just opened" — published by LevelManager; camera + minimap listen. |
| `listeners/GameEventListener.java` | The listener shape: one method, `onEvent(event)`. |

### 3.14 The rest of game-core

| File | What it does |
|---|---|
| `physics/PhysicsManager.java` | Movement/velocity integration each frame. (Custom — we chose NOT to add Box2D for now.) |
| `collision/CollisionManager.java` | Detects overlaps (player↔wall, player↔zone) and resolves them. |
| `save/Saveable.java` | "I can be saved": `toSaveData()` / `fromSaveData()`. |
| `save/SaveData.java` | The bag of values that gets written to disk. |
| `save/SaveManager.java` | Writes/reads save files locally. |
| `constants/GameConstants.java` | All magic numbers in one place. |
| `exceptions/GameException.java` | Our own exception type. |
| `utilities/package-info.java` | Placeholder for future helper functions. |

---

## 4. Module `game-desktop` — the game's face

Path: `game-desktop/src/main/java/com/theinfectedhour/`

Everything that needs a real window, a GPU, or a sound card lives here.

| File | What it does |
|---|---|
| `desktop/DesktopLauncher.java` | **`main()` — the program starts here.** Will create the libGDX Lwjgl3 window and hand control to GameApplication. |
| `graphics/GraphicsManager.java` | Owns the shared `SpriteBatch` (libGDX's drawing tool), the `OrthogonalTiledMapRenderer` that draws the Tiled map, and particle effects (contamination mist etc.). Controls draw order: map → entities → HUD. |
| `camera/CameraManager.java` | An `OrthographicCamera` inside a `FitViewport(1280×720)` — window resizing letterboxes instead of stretching. **Clamps the view to the active MapSection's bounds** so locked areas are never on screen (the RE feel). |
| `audio/AudioManager.java` | Music + sound effects. `playSpatial(sound, sourcePos, listenerPos)` pans left/right and fades with distance. |
| `animation/AnimationManager.java` | Sprite-sheet animation playback (walk cycles etc.). |
| `ui/UIScreen.java` | Base class for all full screens: `show()` / `hide()` / `render()`. |
| `ui/menu/MainMenu.java` | Title screen — built with libGDX **Scene2D** (`Stage` + `Table` + `Skin`) so buttons/layout/click handling come free. |
| `ui/menu/PauseMenu.java` | The in-game pause overlay. |
| `ui/menu/LoadingScreen.java` | Shown while a level loads. |
| `ui/story/StoryPanelScreen.java` | Comic-style story panels between levels. |
| `ui/hud/HealthBar.java` | On-screen health display. |
| `ui/hud/ContaminationMeter.java` | Global contamination % display. |
| `ui/hud/InventoryBar.java` | Item hotbar display. |
| `ui/hud/MiniMap.java` | The minimap. Keeps a set of revealed section ids; **only unlocked sections are drawn** — `revealSection()` is called when a `SectionUnlockedEvent` arrives. |
| `multiplayer/NetworkManager.java` | The translator between the `network` module and the game: turns game state into packets and packets back into game state. |
| `multiplayer/HostSession.java` | Player 1's session: runs the authoritative simulation + a `SocketServer`. |
| `multiplayer/ClientSession.java` | Player 2's session: sends inputs, applies the host's sync packets. |

---

## 5. Module `network` — LAN plumbing

Path: `network/src/main/java/com/theinfectedhour/network/`

Pure Java sockets. **Zero libGDX, zero Spring, zero game classes** — by rule.
The model: **Player 1 (host) is the single source of truth.** Player 2 only
ever sends "here's what keys I pressed"; the host simulates and sends back
"here's where everything is". This prevents the two machines from disagreeing.

| File | What it does |
|---|---|
| `SocketServer.java` | The host's listener: accepts Player 2's TCP connection. |
| `SocketClient.java` | Player 2's side: connects to the host, sends packets. |
| `ClientHandle.java` | The server's handle to one connected client. |
| `ConnectionManager.java` | Tracks open connections, can broadcast a packet to all. |
| `PacketManager.java` | Registry: "when a packet of type X arrives, call handler Y." |
| `PacketHandler.java` | The shape of one such handler. |
| `PacketSerializer.java` | Turns packet objects ↔ bytes. (Wire format still an open decision: Kryo vs hand-rolled.) |
| `Heartbeat.java` | Periodic "still alive?" ping; detects a dead connection. |

### `network/packets/` — the messages themselves

| File | What it does |
|---|---|
| `SerializablePacket.java` | Interface: `serialize()` to bytes, `deserialize()` from bytes. |
| `Packet.java` | Abstract base: every packet has a type, a tick number, a sender id. |
| `PacketType.java` | Enum listing every packet kind. |
| `PlayerInputPacket.java` | Client → Host: "these are my inputs this tick." Uses `shared`'s own Vector2, NOT libGDX's. |
| `EntitySyncPacket.java` | Host → Client: positions/states of all entities (proposed 20×/second). |
| `GameStatePacket.java` | Host → Client: contamination %, mission progress, boss phase. |
| `MissionUpdatePacket.java` | Host → Client: a mission changed. |
| `ChatPacket.java` | Text chat between the two players. |
| `DisconnectPacket.java` | Graceful "I'm leaving." |

---

## 6. Module `shared` — the common language

Path: `shared/src/main/java/com/theinfectedhour/shared/`

Tiny, dependency-free types that BOTH the game and the backend understand.

| File | What it does |
|---|---|
| `math/Vector2.java` | Our own (x, y) pair. Exists so `network` can describe positions **without** importing libGDX. |
| `dto/EntityState.java` | One entity's snapshot (id, position, animation) — the payload inside EntitySyncPacket. |
| `dto/AuthRequest.java` / `AuthResponse.java` | Login request/response bodies for the backend REST API. |
| `dto/SaveGameDto.java` | A save game as sent over HTTP (game-desktop converts SaveData ↔ this). |
| `dto/ScoreEntryDto.java` | One leaderboard row over HTTP. |
| `dto/AchievementDto.java` | One achievement over HTTP. |

(DTO = "Data Transfer Object": a dumb bag of fields whose only job is to be
sent over a wire.)

---

## 7. Module `backend` — the web server

Path: `backend/src/main/java/com/theinfectedhour/backend/`

A Spring Boot REST API with a MySQL database. Layered like every Spring app:
**Controller** (receives HTTP) → **Service** (business logic) → **Repository**
(database access) → **Entity** (one DB table row as a Java object).

| File | What it does |
|---|---|
| `BackendApplication.java` | `main()` for the server — boots Spring. |
| `controller/AuthController.java` | HTTP endpoints under `/api/auth` — register/login. |
| `controller/SaveController.java` | `/api/saves` — upload/download cloud saves. |
| `controller/LeaderboardController.java` | `/api/leaderboard` — submit/fetch scores. |
| `service/AuthService.java` | Login/registration logic (empty until auth design is decided). |
| `service/SaveService.java` | Save/load logic. |
| `service/LeaderboardService.java` | Score ranking logic. |
| `repository/*.java` (6 files) | Spring Data JPA interfaces — you declare them, Spring writes the SQL. One per table. |
| `entity/UserAccount.java` | `users` table: id, username, password hash. |
| `entity/SaveGame.java` | `save_games` table: a user's save blob. |
| `entity/ScoreEntry.java` | `score_entries` table: leaderboard rows. |
| `entity/Achievement.java` | `achievements` table: the catalogue of achievements. |
| `entity/LevelUnlock.java` + `LevelUnlockId.java` | `level_unlocks` table: which user unlocked which level. Two-column primary key (user + level), hence the extra `...Id` class — JPA requires it. |
| `entity/UserAchievement.java` + `UserAchievementId.java` | `user_achievements` table: which user earned which achievement. Same composite-key deal. |
| `config/SecurityConfig.java` | Placeholder for auth configuration (open decision). |
| `config/CorsConfig.java` | Placeholder for CORS rules (lets the game client call the API). |
| `dto/package-info.java` | Placeholder — backend-internal DTOs go here later; client-facing ones live in `shared`. |

---

## 8. The design patterns cheat-sheet

Your course will ask about these. Where each one lives:

| Pattern | Idea in one line | Where |
|---|---|---|
| **Singleton** | Exactly one instance, globally reachable. | `GameManager`, `EventBus` (private constructor + `getInstance()`) |
| **State** | Behaviour changes by swapping a state object, not with if-chains. | `GameStateManager` + `core/state/*`; boss: `VirusHeart` + `phases/*` |
| **Strategy** | Plug interchangeable algorithms into a slot. | `Enemy` + `EnemyAI` (Swarm/Guard) |
| **Factory** | Centralize object creation behind an id. | `EnemyFactory`, `ItemFactory` |
| **Observer** | Publish events; interested parties subscribe. No direct coupling. | `EventBus` + all `*Event` classes (drives the map-section unlocking!) |
| **Command** | Wrap an action as an object (queue it, send it, undo it). | `GameCommand` + Move/Interact + `InputManager` |
| **Composition over inheritance** | Give objects parts instead of deep class trees. | `Character` *has an* `AbilitySet`; `Enemy` *has an* `EnemyAI`; `VirusHeart` *has a* `BossPhase` |

---

## 9. How the Resident-Evil map flow works, end to end

1. You draw a level in the **Tiled editor** and save `level1.tmx`. On its
   `sections` object layer you draw rectangles; each rectangle gets a custom
   property `requiredMission` (empty on the starting section).
2. `MapLoader` loads the file and builds `MapSection` objects.
3. Players play; `MissionManager` notices an objective finished and publishes
   **`MissionCompletedEvent("clear_the_square")`** on the EventBus.
4. `LevelManager` (a subscriber) finds every locked section whose
   `requiredMissionId` is `"clear_the_square"`, calls `unlock()`, and
   publishes **`SectionUnlockedEvent`**.
5. `CameraManager` reacts: its clamp rectangle grows to the new section, so
   the camera can now travel there.
6. `MiniMap` reacts: `revealSection()` — the new area appears on the minimap.

No step knows about the others except through events. You can add a new
reaction (sound sting, screen flash) by subscribing — touching zero existing
code.

## 10. Known gotchas

- `entities.Character` shadows `java.lang.Character` — always import it
  explicitly outside its own package.
- The backend compiles but won't start without a MySQL config
  (`application.properties`) — deliberate, comes later.
- `OrthogonalTiledMapRenderer` lives in `com.badlogic.gdx.maps.tiled.renderers`
  (note the `.renderers`) — easy import mistake.
- Never put a libGDX or Spring import in `network` or `shared` — the build
  reviews will reject it.
