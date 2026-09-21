# The Infected Hour
## 🎥 Project Presentation

<p align="center">
  <a href="https://youtu.be/X8p334KBfes">
    <img src="https://img.shields.io/badge/▶%20WATCH%20PROJECT%20PRESENTATION-FF0000?style=for-the-badge&logo=youtube&logoColor=white" alt="Watch Project Presentation">
  </a>
</p>
A 2D top-down co-op action game built with **libGDX + JavaFX + Spring Boot** where players contain an escalating biological epidemic, rescue stranded survivors, complete containment objectives, and destroy the final Virus Heart.

Developed as a final project for **CSE 4402: Visual Programming Lab**, Islamic University of Technology (IUT).

---

## 1. System Overview

This build features a playable vertical slice of the multi-level campaign, complete with combat, NPC escort AI, networking, and meta-systems:

| System / Feature | Status | Key File(s) |
| :--- | :---: | :--- |
| **JavaFX Pre-Game Launcher** (Login, Lobby, Dossiers, Settings) | Complete | `fx-launcher/.../views/`, `MainMenuController.java` |
| **libGDX Desktop Handoff & Single-Window Runner** | Complete | `core/bridge/GameBridge.java`, `Lwjgl3Launcher.java` |
| **In-Game Main Menu & Operative Dossiers** | Complete | `core/screens/MainMenuScreen.java` |
| **Character Selection** (Elric / Jane with unique stats & weapons) | Complete | `core/screens/MainMenuScreen.java`, `Player.java` |
| **Level Briefing & Squad Ready Gate** | Complete | `core/screens/LevelBriefingScreen.java` |
| **Level 1 — Hospital Containment** (Crafting, Revive, Swarm) | Playable | `core/screens/GameScreen.java`, `maps/level1.map` |
| **Senseless Jane Revive Sequence** (Herb Crafting) | Complete | `core/screens/GameScreen.java` |
| **Villager Rescue & Escort AI** (Follow, Bite reaction, First Aid) | Complete | `core/entities/Villager.java`, `GameScreen.java` |
| **Campaign Levels 2–5 Progression** (Upper Wing, Road, Perimeter) | Playable | `core/level/LevelDefinition.java`, `CampaignLevelPlan.java` |
| **Level 6 Final Boss Fight** (Multi-phase, Laser, Shockwave, Aura) | Playable | `core/screens/BossScreen.java`, `BossPhaseSystem.java` |
| **Story Sequences & Cinematics** (Typewriter dialogue, Voice-overs) | Complete | `core/screens/StoryPanelScreen.java` |
| **HUD Overlay** (Health, Contamination Meter, Minimap, Coins, Objective Checklist `[O]`) | Complete | `core/screens/GameScreen.java`, `MiniMap.java` |
| **Host-Authoritative LAN Co-Op** (KryoNet TCP/UDP, 60Hz Sim, Interpolation) | Complete | `core/net/GameServer.java`, `GameClient.java` |
| **LAN Lobby Auto-Discovery** (UDP Broadcast on 54778) | Complete | `shared/net/LanDiscovery.java` |
| **Spring Boot Backend** (Auth, Saves, Match History, H2 Database) | Complete | `backend/.../InfectedHourBackendApplication.java` |
| **Save / Checkpoint System** (Ctrl+S save screen, 22 Checkpoints) | Complete | `backend/service/PlayerService.java`, `GameScreen.java` |

---

## 2. Running the Application

Prerequisites: **JDK 21** and Gradle (wrapper included).

### Option 1: Direct Game Execution (Single-Window / Testing)
```bash
# 1. Start Spring Boot Backend (Terminal 1)
./gradlew :backend:bootRun

# 2. Run Game directly (Terminal 2)
./gradlew :lwjgl3:run
```
*(Windows: use `.\gradlew.bat`)*

### Option 2: Full JavaFX Launcher & LAN Co-Op
```bash
# Host Machine:
./gradlew :backend:bootRun
./gradlew :fx-launcher:run

# Client Machine (Same LAN / Wi-Fi):
./gradlew :fx-launcher:run
```

### Option 3: Run Automated Test Suite
```bash
./gradlew test
```

---

## 3. Controls Reference

### Operative Controls
| Action | Key / Input |
| :--- | :--- |
| Move | `W`, `A`, `S`, `D` |
| Sprint / Evade | `SHIFT` |
| Melee Attack | `SPACE` or `Left Click` |
| Interact / Loot | `E` |
| Heal | `H` |
| Order Survivor to Stay / Follow | `X` |
| Show / Hide Minimap | `M` |
| Show / Hide Objectives | `O` |
| Open / Close Inventory | `I` |
| Pause / Back | `ESC` |
| Open Controls from Pause Menu | `K` |
| Open Checkpoint Save Slots | `CTRL + S` |
| Adjust Audio Volume | `+` / `-` |

### System & Debug Controls
| Action | Key / Input |
| :--- | :--- |
| Toggle Fullscreen | `F11` |
| Take Screenshot | `F12` |
| Show Coordinate Diagnostics | `J` |
| Show Collision Overlay | `C` or `F1` |
| Toggle Split-Screen Debug View | `F3` |
| Exit to Main Menu (while paused) | `Q` |
| Player 2 Movement (Debug Co-op) | `Arrow Keys` / `Numpad` |
| Player 2 Attack (Debug Co-op) | `NUMPAD 0` |
| Player 2 Interact (Debug Co-op) | `NUMPAD 3` |
| Player 2 Stay / Follow (Debug Co-op) | `NUMPAD 7` |
| Player 2 Heal (Debug Co-op) | `NUMPAD 9` |

---

## 4. Project File Structure

```
infected-hour/
├── build.gradle.kts                   <- Multi-project build configuration (Java 21)
├── README.md
├── assets/                            <- Textures, spritesheets, audio, maps, fonts
│   ├── female/                        <- Jane operative animations & portraits
│   ├── Villager/                      <- NPC escort sprite sheets
│   └── music/                         <- Soundtrack catalog & sound effects
├── shared/                            <- Zero-dependency shared data contracts
│   └── src/main/java/com/infectedhour/shared/
│       ├── constants/                 <- GameConstants, tile dimensions, ports
│       ├── dto/                       <- Auth, match, and save DTOs
│       ├── net/                       <- LanDiscovery (UDP 54778)
│       └── network/                   <- InputCommand, WorldSnapshot, network packets
├── core/                              <- Core libGDX engine & gameplay logic
│   └── src/main/java/com/infectedhour/core/
│       ├── InfectedHourGame.java      <- Main libGDX Game lifecycle coordinator
│       ├── bridge/GameBridge.java     <- Thread-safe JavaFX <-> libGDX bridge
│       ├── entities/                  <- Player, Enemy, Villager, ContaminationZone
│       ├── level/                     <- LevelDefinition, TileMap, CampaignLevelPlan
│       ├── net/                       <- GameServer (60Hz), GameClient, SnapshotInterpolator
│       ├── screens/                   <- MainMenuScreen, LevelBriefingScreen, GameScreen, BossScreen, StoryPanelScreen
│       ├── systems/                   <- CollisionSystem, AISystem, ContaminationSystem, ObjectiveSystem
│       └── ui/                        <- Hud, MiniMap, InventoryHotbar
├── lwjgl3/                            <- LWJGL3 desktop backend entrypoint
│   └── src/main/java/com/infectedhour/lwjgl3/
│       └── Lwjgl3Launcher.java        <- Desktop bootstrapper
├── fx-launcher/                       <- Pre-game JavaFX desktop application
│   └── src/main/java/com/infectedhour/fxlauncher/
│       ├── LauncherApplication.java   <- JavaFX entry point
│       ├── bridge/                    <- GameLauncherBridge
│       ├── net/BackendClient.java     <- HTTP client for Spring Boot REST API
│       └── views/                     <- Controllers & views (Login, Lobby, Dossiers, Settings)
└── backend/                           <- Spring Boot REST backend
    └── src/main/java/com/infectedhour/backend/
        ├── InfectedHourBackendApplication.java
        ├── entity/                    <- User, Player, Match, SaveState, LevelResult
        ├── repository/                <- Spring Data JPA repositories
        ├── security/                  <- JwtAuthFilter, JwtService, SecurityConfig
        └── service/                   <- AuthService, MatchService, PlayerService
```

---

## 5. Team Contributions

### Abrar Faiyaz
- **Campaign & Gameplay:** Built the 6-level campaign progression, level transitions, story/cinematic sequences, and rescue/escort gameplay.
- **UI & Gameplay Systems:** Developed the main menu, HUD, minimap, objective system, pause menus, healing, crafting, and coin economy.
- **Networking & Integration:** Configured LAN co-op and Radmin VPN integration, dynamic backend routing, and master branch merges.
- **Database Implementation:** Implemented the complete database system, including its architecture, entities, repositories, and persistence.

### Sadnan Kibria
- **Player Movement & Combat:** Implemented the main character movement and combat mechanics, including attacks and related gameplay interactions.
- **Boss System:** Developed the multi-phase final boss fight, including boss AI, attack patterns, laser beams, shockwaves, bio-aura, and stun mechanics.
- **Sprites, Assets & Inventory:** Created and integrated Aseprite sprite sheets, animations, related game assets, and implemented the inventory system with its database integration.
- **Audio & Sound Effects:** Integrated game sound effects, including machete, laser, bomb, and stage soundtrack assets.

### AKM Azimul Ashique Khan
- **Collision Grid Authoring:** Generated and refined walkability grids for Levels 1–5 from painted map masks using the custom grid tool.
- **Map & Collision Fixes:** Resolved doorway alignment, pass-through wall bugs, stair transitions, and map reachability.
- **Checkpoints & Persistence:** Implemented local save slots, checkpoint mechanisms, and database architecture documentation.
- **Core Architecture:** Contributed to the initial multi-module skeleton, entity model designs, and baseline networking.
