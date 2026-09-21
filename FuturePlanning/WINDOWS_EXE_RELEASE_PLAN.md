# Windows Standalone EXE Release Plan

## 1. Final goal

Create a Windows release of **ASHGROVE: THE LAST CURE** that players can
download, install, and run without separately installing Java or MySQL.

The release should provide:

- `Ashgrove-The-Last-Cure-Setup.exe` for normal installation.
- An optional portable ZIP for testing and demonstrations.
- A private Java 21 runtime used only by the game.
- All JavaFX, libGDX, LWJGL native libraries, maps, cinematics, audio, fonts,
  and other game assets.
- Automatic backend and local database management.
- Solo, local debug co-op during development, and two-player remote co-op over
  Radmin VPN.
- No command prompt or Gradle commands for players.

Packaging should be done after the remaining gameplay, cinematics, dialogue,
and audio work is stable. The packaging work must not rewrite or replace the
teammate-owned `level_final` implementation.

## 2. Recommended release architecture

Use one installed game folder containing three managed parts:

1. **Windows launcher EXE**
   - Starts the JavaFX launcher and the libGDX/LWJGL game.
   - Displays clear Host and Join options.
   - Starts the backend only when this computer is the host.
   - Stops the backend cleanly when the host exits the game.

2. **Bundled backend**
   - Package the Spring Boot backend JAR inside the application image.
   - Start it as a hidden child process using the same bundled Java runtime.
   - Wait for a health check before enabling login/hosting.
   - Record useful logs under the player's application-data directory.

3. **Embedded persistent database**
   - Replace the release build's local MySQL requirement with H2 file mode or
     SQLite. H2 is the easiest first option because the project already uses
     H2 in backend tests.
   - Store data outside the installation directory, for example:
     `%LOCALAPPDATA%\AshgroveTheLastCure\data\`
   - Keep a MySQL profile available for development if the team still wants it.
   - Never store saves or database files inside `Program Files`, because that
     directory is normally read-only for standard users.

## 3. Remote two-player flow over Radmin VPN

Both players install the same game build and join the same Radmin VPN network.

### Host player

1. Opens the game EXE.
2. Selects **Host Game**.
3. The launcher starts the bundled backend and embedded database.
4. The launcher finds and displays the host's Radmin IPv4 address.
5. The game server listens for the second player.
6. The host shares the Radmin IP or lobby code with Player 2.

### Joining player

1. Opens the same game EXE.
2. Selects **Join Game**.
3. Enters the host's Radmin IP if automatic discovery does not work.
4. Connects to the host's game server.
5. Uses the backend URL supplied by the host so both players use the same
   accounts, saves, statistics, and match history.

### Required network ports

The current project uses:

| Purpose | Protocol | Port |
| --- | --- | ---: |
| Game connection | TCP | 54555 |
| Gameplay snapshots | UDP | 54777 |
| Lobby discovery | UDP | 54778 |
| Spring backend | TCP | 8080 |

Manual Radmin IP entry must remain available because UDP broadcast discovery
may not work on every VPN or Windows network configuration.

Before release, consider moving the backend from common port `8080` to a
project-specific configurable port. MiniTool ShadowMaker already occupied
`8080` on the development computer, demonstrating why the launcher needs a
port preflight check and a helpful conflict message.

Only the host should start the packaged backend. The guest must connect to the
host backend rather than creating a separate local save database.

## 4. Launcher flow changes required before packaging

The current launcher defaults to `http://localhost:8080`. A distributable
build needs a deliberate connection flow before authentication:

1. Add a startup choice: **Solo/Host** or **Join Remote Host**.
2. In Solo/Host mode, start the local backend and wait until it is healthy.
3. In Join mode, request the host's Radmin IP first and configure the backend
   URL before login or registration.
4. When the lobby connection succeeds, verify the backend URL sent in
   `JoinAccept` and use it for the entire session.
5. Show user-friendly errors for unavailable hosts, incompatible versions,
   blocked ports, and backend startup failures.
6. Prevent two backend instances from starting on the same computer.
7. Shut down only the backend process created by this launcher. Never stop an
   unrelated process automatically.

## 5. Backend and database preparation

### Phase A: create release profiles

- Keep the current MySQL configuration as the `dev-mysql` profile.
- Add a `release` profile using an embedded file database.
- Make schema creation and migrations deterministic.
- Add a lightweight health endpoint for launcher startup checks.
- Generate secrets on first run or read them from the local application-data
  directory instead of shipping production secrets in source code.
- Write logs to `%LOCALAPPDATA%\AshgroveTheLastCure\logs\`.

### Phase B: data behavior

- Confirm that registration, login, save slots, leaderboards, statistics, and
  match history work with the embedded database.
- Define whether guest progress is permanently stored on the host computer.
- Add database backup/export support before changing schemas in future builds.
- Test paths containing spaces and non-English Windows usernames.

### Phase C: managed backend process

- Build the backend with `:backend:bootJar`.
- Start that JAR through the bundled runtime in Host mode.
- Pass the selected port, database location, and release profile as arguments.
- Poll the health endpoint with a short timeout.
- Display a readable error if startup fails.
- On normal exit, request graceful backend shutdown and wait briefly before
  terminating the exact child process as a fallback.

## 6. Prepare game resources for packaging

Inventory and include all runtime files:

- `assets/`
- `audio/`
- Cinematic images and dialogue metadata
- Map files
- JavaFX FXML and CSS files
- Fonts, icons, shaders, and native libraries
- Any configuration templates required on first launch

The installed application directory should be treated as read-only. Runtime
code must resolve packaged asset paths reliably and write only to the player's
application-data directory.

Before packaging, run a clean build on a computer without the developer's IDE
or Gradle caches to detect missing resources and accidental absolute paths.

## 7. Create the self-contained runtime

Use Java 21 `jlink`/`jpackage`, preferably through a Gradle packaging plugin
such as the Beryx Runtime/JLink plugin.

The build process should:

1. Produce the JavaFX launcher/game distribution.
2. Produce the Spring Boot backend JAR.
3. Collect runtime dependencies and LWJGL Windows natives.
4. Create a trimmed Java runtime containing all required Java and JavaFX
   modules.
5. Copy game assets and the backend JAR into the application image.
6. Set `com.infectedhour.fxlauncher.LauncherApplication` as the entry point.
7. Assign the final game name, publisher, version, and `.ico` icon.

The packaged runtime means the player does not need a system Java installation.

## 8. Build the Windows installer

Use `jpackage --type exe` on Windows. The developer/build computer will need:

- JDK 21 with `jpackage`.
- The Windows WiX Toolset version supported by the selected JDK.
- The project's Gradle wrapper.
- The final multi-resolution Windows `.ico` file.

Recommended installer behavior:

- Install per user where practical.
- Allow choosing the installation directory.
- Add Start Menu and optional desktop shortcuts.
- Include an uninstaller.
- Show the game version in Windows Apps settings.
- Do not require Java or MySQL.
- Do not silently add broad firewall exceptions.

Create two release outputs:

```text
release/
  Ashgrove-The-Last-Cure-Setup.exe
  Ashgrove-The-Last-Cure-Portable-vX.Y.Z.zip
  SHA256SUMS.txt
  RELEASE_NOTES.txt
```

## 9. Windows Firewall handling

On the first Host action, check whether the required ports can be bound. If
Windows Firewall blocks the game, show a clear explanation and offer an
administrator-approved setup action.

Firewall rules should:

- Target only this game's executable or the exact required ports.
- Cover TCP 54555, UDP 54777, UDP 54778, and the configured backend TCP port.
- Prefer the Radmin/private network profile instead of exposing services on
  every public network.
- Be removable by the uninstaller if the installer created them.

The joining player generally needs outbound access only. The host needs the
inbound rules.

## 10. Version compatibility

Remote players must use compatible builds.

- Add a release version and network protocol version to the lobby handshake.
- Reject mismatched builds with a useful message.
- Include the version in logs and the main menu.
- Avoid silently connecting clients with different maps, dialogue, audio, or
  gameplay definitions.

## 11. Security and signing

- Bind remote services only where necessary.
- Do not expose the backend directly to the public internet; Radmin VPN is the
  expected transport for this release.
- Never package development passwords such as the local MySQL root password.
- Validate backend URLs received from another player.
- Avoid logging passwords, JWTs, or personal information.
- Generate SHA-256 checksums for published downloads.
- Code-sign the installer and game executable when a signing certificate is
  available. Unsigned test builds may trigger Windows SmartScreen warnings.

## 12. Release testing matrix

Test the installer and portable build on clean Windows machines or virtual
machines that do not have Java, MySQL, Gradle, or the project source installed.

### Installation tests

- Fresh installation
- Launch from Start Menu and desktop shortcut
- Installation path containing spaces
- Standard non-administrator Windows account
- Upgrade over an older version
- Uninstall without deleting saves unless the user explicitly chooses that

### Game tests

- Solo game from start to finish
- Cinematics, subtitles, and every voice-over file
- Keyboard/controller input and fullscreen behavior
- Saves survive restart and application upgrade
- No asset path points to the developer's computer

### Two-player tests

- Two clean computers on the same Radmin VPN network
- Host and join using manual Radmin IP
- Lobby discovery where supported
- Firewall initially blocked, then approved
- Host disconnect and guest disconnect handling
- Reconnection after a failed attempt
- Both accounts and save updates appear in the host database
- Different Windows usernames and installation locations
- Version mismatch produces a readable error

### Conflict tests

- Backend port already occupied
- Game TCP or UDP port already occupied
- Radmin disconnected
- Backend crashes during play
- Database file is unavailable or read-only

## 13. Suggested implementation order

1. Finish and verify gameplay, cinematics, subtitles, and audio.
2. Add the embedded release database profile.
3. Add startup mode selection before authentication.
4. Implement managed host-backend startup and shutdown.
5. Add health checks, logs, and port-conflict reporting.
6. Verify Radmin co-op using normal Gradle development runs.
7. Normalize all asset and writable-data paths.
8. Add the Gradle runtime and `jpackage` configuration.
9. Produce and test a portable application image.
10. Produce the Windows installer EXE.
11. Test on two clean Windows computers over Radmin VPN.
12. Add signing, checksums, release notes, and publish the final build.

## 14. Definition of done

The Windows release is complete when:

- A player can install and launch the game without Java, MySQL, Gradle, or an
  IDE.
- Solo mode works with a persistent local database.
- Two installed copies can host and join over Radmin VPN.
- The guest uses the host's backend and both players' save/stat updates work.
- The game handles occupied ports and firewall problems without crashing.
- All images, maps, cinematics, subtitles, and audio are present.
- Save data remains after closing, updating, or uninstalling the program unless
  the player explicitly requests its removal.
- No development password, absolute developer path, debug-only screen, or
  unnecessary tool is included in the public build.

