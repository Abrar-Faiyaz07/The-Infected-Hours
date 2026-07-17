# The Infected Hour — Project Checkpoint

Last updated: 2026-07-17 — **Gradle skeleton milestone COMPLETE** (Week 1 deliverable)
+ libGDX feature scaffolding: Tiled (.tmx) map pipeline, **RE-style section
unlocking** (MapSection + MissionCompletedEvent/SectionUnlockedEvent via
EventBus), FitViewport camera clamped to active section, minimap section
reveal, spatial audio, Scene2D main menu. Maps are authored in the Tiled
editor with object layers: "spawns", "sections", "gates",
"contamination_zones". Box2D deliberately NOT added (would replace the custom
PhysicsManager/CollisionManager) — open decision.

## Where we are
- Full architecture documented in `docs/Architecture.md`; UML in `docs/uml/`.
- **Gradle multi-module skeleton generated and building**: `gradle build` passes
  on all five modules (JDK 21, Gradle 9.6.1, wrapper committed as `gradlew`).
- 133 empty classes/interfaces/enums scaffolded with correct packages,
  inheritance, §5 method signatures, and one-line Javadocs. **No gameplay
  logic, rendering, socket I/O, or DB connections yet.**
- Session-by-session action log: `docs/SESSION_LOG.md`. Resume file: `claude.cmd`.

## Actual module/package layout produced
```
the-infected-hour/
├── settings.gradle, build.gradle, gradle/libs.versions.toml, gradlew
├── game-core/      com.theinfectedhour.{core, core.state, entities, entities.player,
│                   entities.enemy, entities.enemy.ai, entities.boss, entities.boss.phases,
│                   world, world.infection, world.objects, missions, inventory, input,
│                   physics, collision, events, events.listeners, save, constants,
│                   exceptions, utilities}                       (70 files)
├── game-desktop/   com.theinfectedhour.{desktop, ui, ui.hud, ui.menu, ui.story,
│                   multiplayer, graphics, audio, camera, animation}  (17 files)
├── network/        com.theinfectedhour.network{, .packets}          (17 files)
├── shared/         com.theinfectedhour.shared.{math, dto}           (7 files)
└── backend/        com.theinfectedhour.backend.{controller, service, repository,
                    entity, dto, config}                             (24 files)
```
Dependency edges (verified in build files): game-desktop → game-core;
game-desktop → network → shared; backend → shared.

## Standing rules for any future session
- Follow `the-infected-hour-project-brain` skill: SOLID, Clean Architecture,
  composition over inheritance, patterns only when justified (State, Strategy,
  Factory, Observer, Command, Singleton).
- Player 1 = Host (authoritative), Player 2 = Client. Client sends input only,
  never position.
- `network` must never depend on libGDX or Spring Boot (verified: packets use
  `shared`'s own `Vector2`/`EntityState`, not libGDX types).
- Explain architecture before writing implementation code — don't skip ahead.
- Critically review code/decisions; don't rubber-stamp.

## Resolved decisions
- **Level count: 2 maps**, not the "3+" from the original proposal slide.
  `LevelManager` should stay generic (no hardcoded level count in logic) but
  content/testing/roadmap target 2 levels + boss fight only.
- **game-core depends on libGDX** (math/render types: `Vector2`,
  `SpriteBatch`) — per project proposal, not the original zero-dependency
  plan. `Renderable.render(SpriteBatch)` as literally documented in
  Architecture.md §5. Known cost: `game-core` unit tests need libGDX's
  headless backend; no future headless-server reuse without libGDX on the
  classpath. `network` still has zero libGDX/Spring dependency.
  (Reconfirmed by team during scaffolding, 2026-07-17.)

## Deviations / judgment calls made during scaffolding (vs Architecture.md)
1. **libGDX in game-core** — see resolved decisions above (this was the one
   genuine contradiction: §3 zero-dependency rule vs §5 literal signatures).
2. **Stack versions**: Spring Boot 3.5.6 (Gradle 9 requires Boot plugin ≥ 3.5),
   libGDX 1.13.1, JUnit 5.11.4 — pinned in `gradle/libs.versions.toml`.
3. **§4 tree split across modules** (docs don't say which package goes where):
   `ui/`, `graphics/`, `audio/`, `camera/`, `animation/`, `multiplayer/` →
   game-desktop (need libGDX runtime or both game-core+network); everything
   else → game-core. `input/` (Command pattern) kept in game-core.
4. **Classes added that the docs imply but don't list**: `GameState` + 4 state
   impls (makes the State pattern visible); `AbilitySet` (entities/, from UML);
   `Action`, `WorldContext` (entities/enemy/ai/, from `EnemyAI` signature);
   `EnemyFactory` (entities/enemy/, from §6); `PacketType` enum (§5 says header
   has "type"); `PacketHandler`, `ClientHandle` (from network UML signatures);
   `SaveData` (save/); `GameEventListener` (events/listeners/);
   `DesktopLauncher`; `BackendApplication`; `LevelUnlock`/`UserAchievement`
   JPA entities + composite-key id classes (from the ERD — §4 lists only 4
   entities but the ERD has 6 tables); shared DTOs (`EntityState`, `Vector2`,
   `AuthRequest/Response`, `SaveGameDto`, `ScoreEntryDto`, `AchievementDto`).
5. **`SaveData` lives in game-core**, not shared — §3's dependency diagram has
   no game-core → shared edge; game-desktop translates SaveData ↔ SaveGameDto.
6. **Packets live in `network`** (per §4 and the task brief), though §3's
   comment on `shared` mentions "packets" — shared holds only the DTO types
   packets carry.
7. **Root `tests/` dir replaced by standard per-module `src/test/java`** —
   JUnit 5 wired in the root build; no test files yet (Week 4 roadmap).
8. **Manager class names invented** for the §4 one-line packages:
   `PhysicsManager`, `CollisionManager` (game-core), `CameraManager`,
   `AnimationManager`, `AudioManager`, `GraphicsManager` (game-desktop) — each
   implements `Updatable` as §4 requires.
9. **Interface choices for world objects**: `SupplyCrate` = Interactable only
   (explicit in §5); `Barricade` = Damageable; `VillagerNPC`, `SanitationPoint`
   = Interactable. `VirusHeart` extends `Character` (not `Enemy`) per the UML.
10. **Naming hazard**: `com.theinfectedhour.entities.Character` shadows
    `java.lang.Character` — always import it explicitly outside its package.

## Open decisions (not yet made — flag before assuming an answer)
- Packet wire format: Kryo vs hand-rolled binary serialization.
- Exact tick rate for `EntitySyncPacket` (currently proposed 20Hz, unverified).
- Whether `game-core` targets a headless dedicated-server mode later
  (now harder — game-core carries libGDX), or stays strictly two-player LAN.
- Backend auth design (SecurityConfig is an empty stub; no security starter
  dependency added yet).

## Team ownership (assign explicitly before Week 2)
- Architecture / multiplayer / backend / AI
- UI / HUD / inventory / menus
- Levels / boss content / audio / story

## Next milestone (Week 1–2 per roadmap §9)
UI & asset pipeline: flesh out `ui/` screens, tileset integration from the
prototype in `(VeryBadCode)TheInfectedHour/`. Then core gameplay loop
(entities/physics/world, single-player first).
