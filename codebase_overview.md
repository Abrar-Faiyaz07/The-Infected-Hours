# The Infected Hour – Comprehensive Codebase Overview

---

## 🎯 Goal
Provide a clear, navigable description of the entire repository located at `d:/SaminWorks/TheInfectedHour_Share`. The document contains:
- High‑level architecture diagram (Mermaid) showing module relationships.
- Module‑by‑module breakdown with responsibilities.
- Key entry points and the flow of control.
- Important packages, classes, and their roles (controllers, services, entities, game states, etc.).
- Build system (Gradle multi‑module) overview.
- Data model (ER‑style description of JPA entities).
- API endpoint summary (based on controller stubs).
- Game loop & state‑machine explanation.
- How the backend and game communicate (shared module).
- Clickable links to every source file for quick inspection.

---

## 📐 High‑Level Architecture
```mermaid
graph TD
    subgraph Backend[Backend (Spring Boot)]
        BEConfig[config]
        BECtrl[controller]
        BESvc[service]
        BEEnt[entity]
    end

    subgraph GameCore[Game Core]
        GCState[state]
        GCEngine[engine & utilities]
        GCEnt[entities]
    end

    subgraph Desktop[Desktop Launcher (LibGDX)]
        GDLauncher[DesktopLauncher]
    end

    subgraph Shared[Shared Library]
        SHEnt[entity definitions & DTOs]
    end

    subgraph Network[Network (placeholder)]
        NETUtil[future networking code]
    end

    Backend --> SHEnt
    GameCore --> SHEnt
    Desktop --> GameCore
    Desktop --> SHEnt
    Desktop --> NETUtil
```

---

## 🏗️ Build System (Gradle)
- **Root `settings.gradle`** declares the five sub‑projects:
  ```gradle
  rootProject.name = 'the-infected-hour'
  include 'game-core', 'game-desktop', 'network', 'shared', 'backend'
  ```
- Each sub‑project has its own `build.gradle`:
  - **backend** – Spring Boot plugins, JPA, MySQL driver, dependency on `:shared`.
  - **game-core** – LibGDX core libraries, dependency on `:shared`.
  - **game-desktop** – LibGDX desktop launcher, depends on `:game-core`.
  - **shared** – Pure Java, no external libs; provides common POJOs.
  - **network** – Currently empty placeholder.
- The root `build.gradle` (not shown) applies the Java plugin and defines common repositories (Maven Central, Gradle Plugin Portal).

---

## 📦 Modules Overview
### 1️⃣ `backend` – Spring Boot REST API
| Package | Purpose | Key Files |
|---------|---------|-----------|
| `com.theinfectedhour.backend` | Application entry point | [BackendApplication.java](file:///d:/SaminWorks/TheInfectedHour_Share/backend/src/main/java/com/theinfectedhour/backend/BackendApplication.java) |
| `config` | CORS & security configuration (stubs) | [CorsConfig.java](file:///d:/SaminWorks/TheInfectedHour_Share/backend/src/main/java/com/theinfectedhour/backend/config/CorsConfig.java) • [SecurityConfig.java](file:///d:/SaminWorks/TheInfectedHour_Share/backend/src/main/java/com/theinfectedhour/backend/config/SecurityConfig.java) |
| `controller` | REST controllers exposing `/api/*` endpoints (TODO implementations) | [AuthController.java](file:///d:/SaminWorks/TheInfectedHour_Share/backend/src/main/java/com/theinfectedhour/backend/controller/AuthController.java) • [LeaderboardController.java](file:///d:/SaminWorks/TheInfectedHour_Share/backend/src/main/java/com/theinfectedhour/backend/controller/LeaderboardController.java) • [SaveController.java](file:///d:/SaminWorks/TheInfectedHour_Share/backend/src/main/java/com/theinfectedhour/backend/controller/SaveController.java) |
| `service` | Business logic invoked by controllers | [AuthService.java](file:///d:/SaminWorks/TheInfectedHour_Share/backend/src/main/java/com/theinfectedhour/backend/service/AuthService.java) • [LeaderboardService.java](file:///d:/SaminWorks/TheInfectedHour_Share/backend/src/main/java/com/theinfectedhour/backend/service/LeaderboardService.java) • [SaveService.java](file:///d:/SaminWorks/TheInfectedHour_Share/backend/src/main/java/com/theinfectedhour/backend/service/SaveService.java) |
| `entity` | JPA entity definitions persisted in MySQL | [UserAccount.java](file:///d:/SaminWorks/TheInfectedHour_Share/backend/src/main/java/com/theinfectedhour/backend/entity/UserAccount.java) • `Achievement.java`, `LevelUnlock.java`, `SaveGame.java`, `ScoreEntry.java`, `UserAchievement.java`, `UserAchievementId.java`, `LevelUnlockId.java` (all under the same folder) |
| `dto` | Currently only `package-info.java` (placeholder) |
| `repository` | (empty – would hold Spring Data JPA repositories) |
| `config` | CORS & security stubs |

**Data Flow (Backend)**
1. HTTP request hits a controller (e.g., `POST /api/auth/register`).
2. Controller delegates to the appropriate service.
3. Service performs business logic (hash password, generate JWT, etc.).
4. Service interacts with JPA repositories (not yet created) to read/write `entity` objects.
5. Service returns a response DTO; controller serialises it to JSON.

---

### 2️⃣ `game-core` – Core Gameplay Engine
| Package | Responsibility | Key Files |
|---------|----------------|-----------|
| `core` | Bootstraps the game, holds `GameManager` and `GameStateManager`. | [GameApplication.java](file:///d:/SaminWorks/TheInfectedHour_Share/game-core/src/main/java/com/theinfectedhour/core/GameApplication.java) • [GameManager.java](file:///d:/SaminWorks/TheInfectedHour_Share/game-core/src/main/java/com/theinfectedhour/core/GameManager.java) • [GameStateManager.java](file:///d:/SaminWorks/TheInfectedHour_Share/game-core/src/main/java/com/theinfectedhour/core/GameStateManager.java) |
| `core/state` | State‑machine implementation (menu, playing, pause, game‑over). | [GameState.java](file:///d:/SaminWorks/TheInfectedHour_Share/game-core/src/main/java/com/theinfectedhour/core/state/GameState.java) • [MenuState.java](file:///d:/SaminWorks/TheInfectedHour_Share/game-core/src/main/java/com/theinfectedhour/core/state/MenuState.java) • [PlayingState.java](file:///d:/SaminWorks/TheInfectedHour_Share/game-core/src/main/java/com/theinfectedhour/core/state/PlayingState.java) • [PausedState.java](file:///d:/SaminWorks/TheInfectedHour_Share/game-core/src/main/java/com/theinfectedhour/core/state/PausedState.java) • [GameOverState.java](file:///d:/SaminWorks/TheInfectedHour_Share/game-core/src/main/java/com/theinfectedhour/core/state/GameOverState.java) |
| `entities` | Game‑world objects (currently empty placeholders). |
| `physics`, `collision`, `utilities`, `events`, `input`, `audio`, `animation`, `camera`, `graphics`, `inventory`, `missions`, `save`, `world` | Various subsystems needed for a typical LibGDX game. Most contain only package directories now, ready for future implementation. |

**Game Loop (high‑level)**
1. LibGDX creates an `Application` (via the desktop launcher).
2. `GameApplication.start()` is called – it will instantiate `GameManager` and set the initial `GameState` (e.g., `MenuState`).
3. Each frame LibGDX invokes `render()` which, in a full implementation, would:
   - Call `GameManager.update(delta)` → delegates to the active `GameState.update(delta)`.
   - Render the current state's UI/scene.
4. State transitions are handled inside each `GameState` (e.g., `MenuState` → `PlayingState` when the player clicks *Start*).

---

### 3️⃣ `game-desktop` – LibGDX Desktop Launcher
| File | Role |
|------|------|
| [DesktopLauncher.java](file:///d:/SaminWorks/TheInfectedHour_Share/game-desktop/src/main/java/com/theinfectedhour/desktop/DesktopLauncher.java) | Sets up a `LwjglApplicationConfiguration` and launches a `LwjglApplication` with a new `GameApplication` instance. |
| Sub‑packages (`animation`, `audio`, `camera`, `graphics`, `multiplayer`, `ui`) | Stub packages where concrete LibGDX implementations will live (e.g., `AudioManager`, `UIRenderer`). |

The launcher is the **only class with a `main` method** in the whole project, making it the entry point for the client side.

---

### 4️⃣ `shared` – Common Code
- Currently contains only the `src/main/java` folder with no concrete classes, but it is declared as a dependency of both **backend** and **game‑core**.
- Intended place for POJOs, DTOs, utility classes, and—most importantly—the **entity definitions** that both sides can share without duplication.
- When the game implements save‑game serialization, it can reuse the same JPA entity classes (e.g., `UserAccount`, `SaveGame`) to keep data formats consistent.

---

### 5️⃣ `network` – Placeholder
- Contains an empty `src/` folder and a basic `build.gradle`. It is reserved for future **client‑server networking** (e.g., WebSocket sync, HTTP client wrapper).

---

## 🗂️ Detailed Class / Package Summary (Clickable Links)
### Backend Packages
- **`config`** – `[CorsConfig.java](file:///d:/SaminWorks/TheInfectedHour_Share/backend/src/main/java/com/theinfectedhour/backend/config/CorsConfig.java)`, `[SecurityConfig.java](file:///d:/SaminWorks/TheInfectedHour_Share/backend/src/main/java/com/theinfectedhour/backend/config/SecurityConfig.java)`
- **`controller`** – `[AuthController.java](file:///d:/SaminWorks/TheInfectedHour_Share/backend/src/main/java/com/theinfectedhour/backend/controller/AuthController.java)`, `[LeaderboardController.java](file:///d:/SaminWorks/TheInfectedHour_Share/backend/src/main/java/com/theinfectedhour/backend/controller/LeaderboardController.java)`, `[SaveController.java](file:///d:/SaminWorks/TheInfectedHour_Share/backend/src/main/java/com/theinfectedhour/backend/controller/SaveController.java)`
- **`service`** – `[AuthService.java](file:///d:/SaminWorks/TheInfectedHour_Share/backend/src/main/java/com/theinfectedhour/backend/service/AuthService.java)`, `[LeaderboardService.java](file:///d:/SaminWorks/TheInfectedHour_Share/backend/src/main/java/com/theinfectedhour/backend/service/LeaderboardService.java)`, `[SaveService.java](file:///d:/SaminWorks/TheInfectedHour_Share/backend/src/main/java/com/theinfectedhour/backend/service/SaveService.java)`
- **`entity`** – `[UserAccount.java](file:///d:/SaminWorks/TheInfectedHour_Share/backend/src/main/java/com/theinfectedhour/backend/entity/UserAccount.java)`, `Achievement.java`, `LevelUnlock.java`, `SaveGame.java`, `ScoreEntry.java`, `UserAchievement.java`, `UserAchievementId.java`, `LevelUnlockId.java`
- **`dto`** – only placeholder `package-info.java`
- **`repository`** – empty (would contain Spring Data interfaces).

### Game‑Core Packages
- **`core`** – `[GameApplication.java](file:///d:/SaminWorks/TheInfectedHour_Share/game-core/src/main/java/com/theinfectedhour/core/GameApplication.java)`, `[GameManager.java](file:///d:/SaminWorks/TheInfectedHour_Share/game-core/src/main/java/com/theinfectedhour/core/GameManager.java)`, `[GameStateManager.java](file:///d:/SaminWorks/TheInfectedHour_Share/game-core/src/main/java/com/theinfectedhour/core/GameStateManager.java)`
- **`core/state`** – `[GameState.java](file:///d:/SaminWorks/TheInfectedHour_Share/game-core/src/main/java/com/theinfectedhour/core/state/GameState.java)`, `[MenuState.java](file:///d:/SaminWorks/TheInfectedHour_Share/game-core/src/main/java/com/theinfectedhour/core/state/MenuState.java)`, `[PlayingState.java](file:///d:/SaminWorks/TheInfectedHour_Share/game-core/src/main/java/com/theinfectedhour/core/state/PlayingState.java)`, `[PausedState.java](file:///d:/SaminWorks/TheInfectedHour_Share/game-core/src/main/java/com/theinfectedhour/core/state/PausedState.java)`, `[GameOverState.java](file:///d:/SaminWorks/TheInfectedHour_Share/game-core/src/main/java/com/theinfectedhour/core/state/GameOverState.java)`
- **`entities`**, **`physics`**, **`collision`**, **`utilities`**, **`events`**, **`input`**, **`audio`**, **`animation`**, **`camera`**, **`graphics`**, **`inventory`**, **`missions`**, **`save`**, **`world`** – currently empty directories prepared for future implementation.

### Desktop Launcher Packages
- **`desktop`** – `[DesktopLauncher.java](file:///d:/SaminWorks/TheInfectedHour_Share/game-desktop/src/main/java/com/theinfectedhour/desktop/DesktopLauncher.java)`
- **`animation`**, **`audio`**, **`camera`**, **`graphics`**, **`multiplayer`**, **`ui`** – placeholder packages for LibGDX helpers.

---

## 📊 Data Model (ER‑style) – Backend Entities
```
UserAccount (id PK)
  ├─ username
  ├─ email
  ├─ passwordHash
  └─ createdAt

SaveGame (id PK) → belongs to UserAccount (user_id FK)
  ├─ name
  ├─ data (blob/JSON)
  └─ timestamp

ScoreEntry (id PK) → belongs to UserAccount (user_id FK)
  ├─ score
  ├─ level
  └─ achievedAt

Achievement (id PK)
  ├─ name
  └─ description

UserAchievement (composite PK: user_id, achievement_id) → links UserAccount ↔ Achievement
  └─ earnedAt

LevelUnlock (composite PK: user_id, level_id) → links UserAccount ↔ Level (not yet defined)
  └─ unlockedAt
```
All entities are annotated with `@Entity` and `@Table(name = "...")`. The JPA mappings live under `backend/entity/` and are also compiled into the **shared** module so the game client can reuse the same POJOs for serialization.

---

## 📡 API Endpoint Summary (Stubs)
| HTTP Method | Path | Controller | Description (TODO) |
|--------------|------|------------|---------------------|
| `POST` | `/api/auth/register` | `AuthController` | Register a new user (hash password, create `UserAccount`). |
| `POST` | `/api/auth/login` | `AuthController` | Authenticate, issue JWT/token. |
| `GET` | `/api/leaderboard` | `LeaderboardController` | Retrieve top scores. |
| `POST` | `/api/leaderboard` | `LeaderboardController` | Submit a new score. |
| `GET` | `/api/saves/{userId}` | `SaveController` | List saved games for a user. |
| `POST` | `/api/saves` | `SaveController` | Upload a new save game. |
| `GET` | `/api/saves/{saveId}` | `SaveController` | Download a specific save. |

*All endpoints currently contain a `// TODO: implement` placeholder.*

---

## 🔄 Interaction Between Backend & Game
1. **Shared Module** – Both sides compile against the same entity classes (`UserAccount`, `SaveGame`, etc.). This guarantees binary compatibility for JSON/DTO exchange.
2. **Future Networking** – When the networking layer is built, the game will issue HTTP requests to the backend endpoints (e.g., login, fetch leaderboard, download saves). The **network** module will host the HTTP client code.
3. **Data Flow Example** (save game):
   - Player finishes a session → `GameCore.save.SaveManager` creates a `SaveGame` object.
   - The desktop client serialises it to JSON and POSTs it to `/api/saves`.
   - Backend `SaveController` receives the payload, `SaveService` stores it via JPA, and responds with the saved entity ID.
   - Later the game can `GET /api/saves/{userId}` to retrieve the list.

---

## 🚀 How to Run the Project
### Backend (Spring Boot)
```bash
cd backend
./gradlew bootRun   # on Windows use "gradlew.bat bootRun"
```
- Starts on **http://localhost:8080**.
- Swagger/OpenAPI UI can be added later for interactive testing.

### Desktop Game (LibGDX)
```bash
cd game-desktop
./gradlew desktop:run   # or "gradlew.bat desktop:run"
```
- Opens a native window; the game loop will call the (currently empty) `GameManager.update()` each frame.

> **Note:** Both commands assume a JDK 17+ and Gradle wrapper are present (provided by the repo).

---

## 📚 Where to Extend / Add Features
| Area | What to implement |
|------|-------------------|
| **Backend** | Real controller logic, Spring Security (password hashing, JWT), repository interfaces, CORS configuration. |
| **Game Core** | Fill out entity classes (`Player`, `Enemy`, etc.), implement physics & collision, flesh out each `GameState` (rendering, input handling). |
| **Desktop** | Asset loading, screen resolution, input mapping, UI screens (menus, HUD). |
| **Network** | HTTP client wrapper (e.g., using `OkHttp`), WebSocket for real‑time sync, error handling. |
| **Shared** | Move common DTOs (e.g., `LoginRequest`, `LeaderboardEntry`) here for reuse. |
| **Testing** | Add JUnit tests for services, integration tests for REST API, LibGDX unit tests for core logic. |

---

## 📌 Quick Navigation Summary
- **Backend entry point** → `BackendApplication.java`
- **Game entry point** → `DesktopLauncher.java` → `GameApplication.start()` → `GameManager`
- **State machine** → `GameStateManager` + concrete `*State` classes
- **Data persistence** → JPA entities under `backend/entity` (also in `shared`)
- **Configuration** → Gradle multi‑module (`settings.gradle` + each module's `build.gradle`)
- **Future networking** → to be placed in `network` module.

---

*End of overview.*
