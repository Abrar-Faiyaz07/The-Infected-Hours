# The Infected Hour — Architecture

CSE 4402: Visual Programming Lab
Sadnan Kibria (230041119) · Abrar Faiyaz (230041143) · Ashique Khan (230041153)

Status: **Design phase — no gameplay code written yet.**

---

## 1. Purpose of this document

This is the single source of truth for how The Infected Hour is built. Before any
gameplay code is written, every teammate should be able to answer, from this
document alone:

- Which module does my feature belong in?
- Which package does my class belong in?
- What pattern should I reach for, and why?
- How does a packet get from Player 2's controller to Player 1's screen?

If a design decision isn't written here, it isn't decided yet — raise it before
coding around it.

---

## 2. Technology stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Rendering / game loop | libGDX |
| Multiplayer transport | Java Socket Programming (TCP) |
| Backend API | Spring Boot |
| Persistence | MySQL via JPA/Hibernate |
| Build | Gradle (multi-module) |
| VCS | Git + GitHub |
| Testing | JUnit 5 |

---

## 3. Module breakdown (Gradle multi-module)

```
The-Infected-Hour/
├── game-core/       # pure game logic — no libGDX, no Spring. Unit-testable in isolation.
├── game-desktop/    # libGDX launcher: rendering, input, audio. Depends on game-core.
├── network/         # socket layer + packet definitions. Depends on shared.
├── shared/          # DTOs shared by client and backend (packets, save schema).
├── backend/         # Spring Boot REST API. Depends on shared.
├── docs/            # this file, PlantUML sources, ADRs.
└── tests/           # module-mirrored test suites.
```

**Why split this way:** `network` has zero dependency on libGDX or Spring Boot.
`game-core` depends on libGDX for math/rendering types only (`Vector2`,
`SpriteBatch`) — **per the project proposal**, not the original zero-dependency
plan. This is a deliberate deviation from strict Clean Architecture: unit
tests on `game-core` will need libGDX's headless backend
(`HeadlessApplication`) to run, and `game-core` is no longer reusable for a
future headless dedicated-server mode without that dependency. Accepted
tradeoff — revisit only if testing friction becomes a real problem.

Dependency direction (never reversed):

```
game-desktop ──▶ game-core ──▶ libGDX (math/render types only)
game-desktop ──▶ network ──▶ shared
backend      ──▶ shared
```

`game-core` never imports from `game-desktop`, `network`, or `backend`, and
never imports Spring Boot. Its only external dependency is libGDX itself
(for `Vector2`, `SpriteBatch`, etc.) — not `game-desktop`.

---

## 4. Package structure

```
com.theinfectedhour

├── core/                        // bootstrap & orchestration
│   ├── GameApplication.java
│   ├── GameManager.java         // Singleton — top-level orchestrator
│   └── GameStateManager.java    // State pattern: Menu / Playing / Paused / GameOver

├── entities/                    // domain model — no rendering
│   ├── Entity.java                      (abstract)
│   ├── Character.java                   (abstract, composes behavior, doesn't
│   │                                      inherit it — see §6)
│   ├── player/
│   │   ├── Player.java
│   │   ├── PlayerRole.java              // enum: FIELD_MEDIC, LOCAL_SCOUT
│   │   ├── FieldMedic.java
│   │   └── LocalScout.java
│   ├── enemy/
│   │   ├── Enemy.java                   (abstract)
│   │   ├── MutatedCell.java
│   │   └── ai/
│   │       ├── EnemyAI.java             (interface — Strategy)
│   │       ├── SwarmBehavior.java
│   │       └── GuardBehavior.java
│   └── boss/
│       ├── VirusHeart.java
│       └── phases/
│           ├── BossPhase.java           (interface — State)
│           ├── ShieldPhase.java
│           ├── ExposurePhase.java
│           └── CoreDestructionPhase.java

├── world/
│   ├── Level.java
│   ├── LevelManager.java        // manages exactly 2 levels — see §4a
│   ├── MapLoader.java
│   ├── infection/
│   │   ├── ContaminationZone.java
│   │   ├── ContaminationSystem.java     // spreads over time, ticks via Updatable
│   │   └── SanitationPoint.java
│   └── objects/
│       ├── Barricade.java
│       ├── SupplyCrate.java
│       └── VillagerNPC.java

├── missions/
│   ├── Mission.java
│   ├── MissionManager.java
│   ├── MissionObjective.java            (interface)
│   ├── IsolateZoneObjective.java
│   ├── CollectSampleObjective.java
│   ├── DeliverMedicineObjective.java
│   ├── RescueVillagerObjective.java
│   └── ActivateSanitationObjective.java

├── inventory/
│   ├── Inventory.java
│   ├── Item.java                        (abstract)
│   ├── CureSample.java
│   ├── Medicine.java
│   └── ItemFactory.java                 // Factory pattern

├── input/                               // Command pattern
│   ├── GameCommand.java                 (interface)
│   ├── MoveCommand.java
│   ├── InteractCommand.java
│   └── InputManager.java

├── physics/ collision/ camera/ animation/ audio/ graphics/
│   // engine-adjacent systems; each Manager implements Updatable

├── ui/
│   ├── hud/       (HealthBar, ContaminationMeter, MiniMap, InventoryBar)
│   ├── menu/      (MainMenu, PauseMenu, LoadingScreen)
│   └── story/     (StoryPanelScreen)     // narrative reveals between levels

├── multiplayer/                          // client-side network glue
│   ├── NetworkManager.java
│   ├── HostSession.java
│   └── ClientSession.java

├── save/
│   ├── SaveManager.java
│   └── Saveable.java                     (interface)

├── events/                               // Observer pattern bus
│   ├── EventBus.java                     // Singleton
│   ├── GameEvent.java
│   └── listeners/...

└── utilities/ exceptions/ constants/
```

```
// network module
com.theinfectedhour.network
├── SocketServer.java / SocketClient.java
├── ConnectionManager.java
├── PacketManager.java
├── PacketSerializer.java                 // wire format — TBD: Kryo vs manual
├── Heartbeat.java
└── packets/
    ├── Packet.java                       (abstract, implements SerializablePacket)
    ├── PlayerInputPacket.java
    ├── EntitySyncPacket.java
    ├── GameStatePacket.java
    ├── MissionUpdatePacket.java
    ├── ChatPacket.java
    └── DisconnectPacket.java
```

```
// backend module
com.theinfectedhour.backend
├── controller/   (AuthController, LeaderboardController, SaveController)
├── service/      (AuthService, LeaderboardService, SaveService)
├── repository/   (Spring Data JPA repositories)
├── entity/       (JPA @Entity — UserAccount, SaveGame, ScoreEntry, Achievement)
├── dto/
└── config/       (SecurityConfig, CorsConfig)
```

---

### 4a. Scope note — 2 levels, not 3+

**Confirmed:** the game ships with **2 maps/levels**, superseding the "3+
Progressive Levels" goal on the original proposal slide. This affects:

- `LevelManager` should be written generically (don't hardcode "2" as a magic
  number in logic — read level count from `MapLoader`/level config), but the
  actual content, testing, and roadmap target 2 levels only.
- `level_unlocks` table and mission design should assume Level 1 → Level 2 →
  boss fight, not a longer chain.
- Story panels: 2 narrative reveals (one per level), not 3+.

If this changes again, update this section — don't let the proposal slide's
"3+" silently disagree with what's actually being built.

---

## 5. Core interfaces and abstract classes

| Type | Name | Contract |
|---|---|---|
| interface | `Renderable` | `render(SpriteBatch batch)` |
| interface | `Updatable` | `update(float deltaTime)` |
| interface | `Damageable` | `takeDamage(int amount)`, `isDead()` |
| interface | `Interactable` | `interact(Player source)` |
| interface | `Movable` | `move(Vector2 direction)` |
| interface | `MissionObjective` | `isComplete()`, `getProgress()` |
| interface | `Saveable` | `toSaveData()`, `fromSaveData(SaveData)` |
| interface | `SerializablePacket` | `serialize()`, `deserialize(byte[])` |
| interface | `GameCommand` | `execute()`, `undo()` |
| abstract | `Entity` | id, position, implements `Renderable`, `Updatable` |
| abstract | `Character` | extends `Entity`; composes an `EnemyAI` or input source rather than subclassing behavior |
| abstract | `Item` | id, name, `use(Player p)` |
| abstract | `Packet` | header (type, tick, senderId) + payload |
| abstract | `UIScreen` | `show()`, `hide()`, `render()` |

An entity implements only what it needs — a `SupplyCrate` is `Interactable`,
not `Damageable`. This is the Interface Segregation half of SOLID and it's
non-negotiable: don't add empty method bodies to satisfy a fat interface.

---

## 6. Design patterns — where and why

| Pattern | Applied to | Reason |
|---|---|---|
| **State** | `GameStateManager`, `BossPhase` | Boss fight has 3 discrete phases with different rules and inputs; the alternative is a spreading mess of booleans. |
| **Strategy** | `EnemyAI` | Swarm vs guard behavior swapped at runtime without a subclass per enemy variant. |
| **Factory** | `ItemFactory`, `EnemyFactory` | Level data spawns items/enemies by id; the loader never depends on concrete classes. |
| **Observer** | `EventBus` | Contamination hitting 100%, an objective completing, a player going down — many independent systems (HUD, audio, missions) react without direct coupling. |
| **Command** | `GameCommand` hierarchy, `PlayerInputPacket` handling | Input becomes a first-class, loggable, replayable object — this is what makes host-side input validation and future debug-replay tooling possible. |
| **Singleton** | `GameManager`, `EventBus` | Exactly one instance is meaningful; used sparingly, not as a default. |

**Composition over inheritance**: `Character` does not grow a deep subclass
tree for every ability combination. `FieldMedic` and `LocalScout` compose a
role-specific `AbilitySet` rather than overriding a pile of virtual methods —
this is what the "prefer composition" rule concretely means here, and it's
what keeps adding a third playable role from touching existing classes.

---

## 7. Networking architecture — host-authoritative TCP

**Player 1 = Host** (runs `SocketServer`), **Player 2 = Client** (runs
`SocketClient`). This is fixed, not negotiated at runtime — simplifies the
state machine considerably.

Rules:

1. The client **never sends position** — only intent, via `PlayerInputPacket`
   (movement vector, action button, tick number).
2. The host simulates: movement, collisions, contamination spread, mission
   state. The host's world state is the only one that matters.
3. The host broadcasts `EntitySyncPacket` (positions/animations) at a fixed
   tick rate (target 20Hz), independent of render framerate.
4. The host broadcasts `GameStatePacket` (contamination %, mission progress,
   boss phase) on change, not every tick — this is low-frequency, high-value
   data.
5. `Heartbeat` runs every tick in both directions; missing N consecutive
   heartbeats triggers `DisconnectPacket` handling and pauses the session.
6. Networking code never imports from `entities/` or `world/` directly —
   it moves `Packet` objects. Translation between packets and game state
   happens in `multiplayer/NetworkManager`, keeping `network` engine-agnostic.

See `uml/sequence-networking.puml` for the full exchange diagram.

**Open decision, not yet made:** wire serialization format (Kryo vs hand-rolled
binary). Kryo is faster to build with; hand-rolled gives full control over
packet size. Revisit once `EntitySyncPacket` payload size is measured against
real level entity counts.

---

## 8. Database schema (MySQL via Spring Boot backend)

See `uml/erd-database.puml` for the full entity-relationship diagram.

- `users` — id, username, email, password_hash, created_at
- `save_games` — id, user_id (FK), level_reached, contamination_stats (JSON), updated_at
- `score_entries` — id, user_id (FK), level_id, time_seconds, deaths, completed_at — leaderboard source
- `level_unlocks` — user_id (FK), level_id, unlocked_at — composite PK; backs "finishing a map saves progress"
- `achievements` — id, code, description
- `user_achievements` — user_id (FK), achievement_id (FK), earned_at

---

## 9. Development roadmap

| Week | Focus | Deliverables |
|---|---|---|
| 1 | Requirements & design | This document, PlantUML diagrams, Gradle skeleton (no gameplay code) |
| 1–2 | UI & asset pipeline | `ui/` scaffolding, tileset integration from prototype |
| 2–3 | Core gameplay | `entities/`, `physics/`, `world/` — single-player loop working first |
| 3 | Networking | `network` module, host/client handshake, input → sync loop |
| 3–4 | Levels & story | `missions/`, `LevelManager`, story panels |
| 4 | Testing & bug fixing | Unit tests on `game-core` — this is where module isolation pays off |
| 5 | Boss battle & polish | `VirusHeart`, `BossPhase` states, HUD polish |

---

## 10. Team ownership

| Owner | Modules |
|---|---|
| Architecture / multiplayer / backend / AI lead | `core/`, `network/`, `backend/`, `entities/enemy/`, `entities/boss/` |
| UI / HUD / inventory / menus | `ui/`, `inventory/` |
| Levels / boss content / audio / story | `world/`, `missions/`, `ui/story/`, `audio/` |

Assign these explicitly among the three of you before Week 2 — the module
boundaries above are drawn so two people can work in `game-core` without
touching the same files.

---

## 11. Related documents

- `uml/class-diagram-entities.puml` — player, enemy, boss hierarchy
- `uml/class-diagram-network.puml` — packet and socket classes
- `uml/sequence-networking.puml` — full host/client packet exchange
- `uml/erd-database.puml` — MySQL schema
- `CLAUDE.md` — project checkpoint / resume point for future sessions
