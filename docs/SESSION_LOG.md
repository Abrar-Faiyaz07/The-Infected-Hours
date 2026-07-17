# Session Log — The Infected Hour

Chronological record of what each AI-assisted session did. Newest entry last.
Future sessions: append, don't rewrite.

---

## 2026-07-17 — Gradle multi-module skeleton (Week 1 milestone)

**Input specs:** `CLAUDE.md`, `docs/Architecture.md` §3–§8, all four
`docs/uml/*.puml` diagrams.

**Decision escalated to team:** Architecture.md contradiction — §5 signatures
use libGDX types (`SpriteBatch`, `Vector2`) but §3 forbade libGDX in
game-core. Team chose: **allow libGDX in game-core**, keep §5 signatures
literal. `network` remains libGDX-free (uses `shared`'s own `Vector2`).

**Actions:**
1. Verified environment: JDK 21.0.10, Gradle 9.6.1 (forced Spring Boot 3.5.6 —
   older Boot plugins break on Gradle 9).
2. Created root `settings.gradle` (5 modules), `build.gradle` (Java 21
   toolchain, JUnit 5, shared conventions), `gradle/libs.versions.toml`
   (libGDX 1.13.1, Boot 3.5.6, JUnit 5.11.4, MySQL connector 9.1.0),
   `.gitignore`.
3. Created per-module `build.gradle` × 5 with the exact §3 dependency edges.
4. Scaffolded 133 empty types (70 game-core, 17 game-desktop, 17 network,
   7 shared, 24 backend — includes 2 package-info files in the 135 file
   total): correct packages, extends/implements per the UML, §5 method
   signatures with `// TODO: implement` bodies, one-line Javadoc each.
5. Pattern scaffolding wired: State (`GameStateManager`+`GameState`×4,
   `BossPhase`×3), Strategy (`EnemyAI` + Swarm/Guard), Factory
   (`ItemFactory`, `EnemyFactory`), Observer (`EventBus`+`GameEvent`+
   `GameEventListener`), Command (`GameCommand`+Move/Interact),
   Singleton (`GameManager`, `EventBus` — private ctor + synchronized
   `getInstance()`).
6. Ran `gradle build` — **BUILD SUCCESSFUL**, all 5 modules compile.
7. Generated Gradle wrapper (`gradlew`, `gradlew.bat`).
8. Updated `CLAUDE.md` (milestone complete, layout, 10 recorded
   deviations/judgment calls), created `claude.cmd` resume file, created
   this log.

**Not done (by design):** gameplay logic, rendering, socket I/O, DB config,
tests, `application.properties` for backend (it compiles but is not runnable
until a datasource is configured).

---

## 2026-07-17 (later) — libGDX feature scaffolding + RE-style section loading

Team decision: lean on libGDX built-ins wherever possible; maps authored in
the Tiled editor; Resident-Evil-style progression (finish a mission → next
part of the map unlocks) plus a minimap that reveals sections as they open.

**Actions:**
1. New: `world/MapSection` (named region + unlock state + gating mission),
   `events/MissionCompletedEvent`, `events/SectionUnlockedEvent`.
2. `Level` now holds a `TiledMap` + sections + spawn points; `MapLoader`
   scaffolds `TmxMapLoader` with expected Tiled object layers ("spawns",
   "sections", "gates", "contamination_zones"); `LevelManager` subscribes to
   MissionCompletedEvent and unlocks/publishes sections (Observer, no
   mission↔world coupling).
3. `CameraManager`: OrthographicCamera in a FitViewport(1280×720), clamped
   to the active section's bounds (locked areas never shown); `resize()` hook.
4. `GraphicsManager`: shared SpriteBatch + OrthogonalTiledMapRenderer
   (note: package is `maps.tiled.renderers.*`) + ParticleEffect loading.
5. `AudioManager`: `playSpatial(sound, sourcePos, listenerPos)` pan/volume
   scaffolding; music track handling.
6. `MiniMap`: revealed-section set, `revealSection()`; `MainMenu`: Scene2D
   Stage/Table/Skin scaffolding.
7. `gradle build` — **BUILD SUCCESSFUL** (one fix: OrthogonalTiledMapRenderer
   import path).

**Deliberately NOT added:** gdx-box2d. It would replace the custom
PhysicsManager/CollisionManager design in Architecture.md — recorded as an
open decision; adopt only if the custom collision system proves too painful.
