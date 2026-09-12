# How to Run — The Infected Hour

Quick guide for running **The Infected Hour** project.

---

## 🚀 Quick Run Commands

### Windows (PowerShell / Command Prompt)

```powershell
# 1. Start Spring Boot Backend (Run FIRST on Host / Single Player)
.\gradlew.bat :backend:bootRun

# 2. Start Full Game Launcher (JavaFX UI - Run on all players' laptops)
.\gradlew.bat :fx-launcher:run

# 3. Dev Shortcut (Skips JavaFX launcher, launches directly into libGDX solo host)
.\gradlew.bat :lwjgl3:run

# 4. Run All Tests
.\gradlew.bat test
```

### macOS / Linux (Terminal)

```bash
./gradlew :backend:bootRun     # Backend API
./gradlew :fx-launcher:run     # Full Game Launcher
./gradlew :lwjgl3:run          # Dev Shortcut
./gradlew test                 # Run tests
```

---

## 🎮 How to Play

### Option A: Solo / Quick Development Setup
1. Open terminal 1 and run the backend:
   ```powershell
   .\gradlew.bat :backend:bootRun
   ```
2. Open terminal 2 and run the game directly:
   ```powershell
   .\gradlew.bat :lwjgl3:run
   ```

### Option B: Full Game with JavaFX Launcher & Local/LAN Multiplayer

#### Host Laptop:
1. Start Backend API:
   ```powershell
   .\gradlew.bat :backend:bootRun
   ```
2. Start Game Launcher:
   ```powershell
   .\gradlew.bat :fx-launcher:run
   ```
3. In the launcher: Register/Login, Create Lobby/Host Game.

#### Client / Teammate Laptop:
1. Ensure connected to the same Wi-Fi / Local Area Network as Host.
2. Start Game Launcher:
   ```powershell
   .\gradlew.bat :fx-launcher:run
   ```
3. In launcher: Login and Join Host's LAN lobby.

---

## 🛠 Prerequisites & Ports
- **Java 21** installed and configured in `JAVA_HOME`.
- **LAN Ports used by KryoNet**:
  - TCP: `54555`
  - UDP: `54777`
  - Discovery: `54778`
