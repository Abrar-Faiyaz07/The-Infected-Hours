# How to Run — The Infected Hour

Quick guide for running **The Infected Hour** project.

---

## 🚀 Quick Run Commands

### Windows (PowerShell / Command Prompt)

```powershell
# 1. Start Spring Boot Backend (Run FIRST for database & saves)
.\gradlew.bat :backend:bootRun

# 2. Unified Game (Single-Window: Main Menu, Character Select, Discord/OBS Streaming & Screenshot friendly)
.\gradlew.bat :lwjgl3:run

# 3. Multiplayer Lobby Launcher (JavaFX UI - for LAN co-op rooms)
.\gradlew.bat :fx-launcher:run

# 4. Run All Tests
.\gradlew.bat test
```

### macOS / Linux (Terminal)

```bash
./gradlew :backend:bootRun     # Backend API
./gradlew :lwjgl3:run          # Unified Game (Single-Window)
./gradlew :fx-launcher:run     # LAN Multiplayer Launcher
./gradlew test                 # Run tests
```

---

## 🎮 How to Play

### Option A: Unified Story Mode (Single Window — Recommended for Streaming & Screenshots)
1. Open terminal 1 and run the backend:
   ```powershell
   .\gradlew.bat :backend:bootRun
   ```
2. Open terminal 2 and run the game directly:
   ```powershell
   .\gradlew.bat :lwjgl3:run
   ```
   - Opens in **1 single window** with native Main Menu, Character Selection (Elric or Jane), Save Slot loading, and Controls.
   - 100% compatible with **Discord stream, OBS Window Capture, and Windows Snipping Tool (`Win+Shift+S`)**.

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
