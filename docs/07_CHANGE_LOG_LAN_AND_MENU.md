# Change log — LAN, launcher, menu, and presentation fixes

**Created:** 2026-07-31

**Last updated:** 2026-09-01
**Scope:** (1) make two machines actually connect and play, (2) fix the main-menu
rendering defects, (3) unbreak `./gradlew build`, and (4) make the transition
from the launcher into the native game window seamless and fullscreen.

Everything below is traceable: each entry names the file, what was wrong, what it
is now, and why. Read top-to-bottom to reconstruct the session.

---

## 0. How to verify any of this yourself

```bash
./gradlew build
```

```bash
./gradlew :shared:test :core:test
```

Two windows on one PC (fastest full-path check — no second laptop needed):

```bash
./gradlew :lwjgl3:run
```

```bash
./gradlew :lwjgl3:run --args="join"
```

The full launcher:

```bash
./gradlew :fx-launcher:run
```

---

## 1. Bugs found and fixed in the main menu

The menu was built but rendered wrong. Diagnosis was done by running the app and
screenshotting it, not by reading the FXML.

### 1.1 Multi-class `styleClass` silently matched nothing — `views/MainMenu.fxml`

**Symptom:** the gold "HOUR" title and both primary buttons rendered as tiny
unstyled default controls, while single-class buttons (Load Game, Settings,
About) rendered correctly.

**Cause:** `FXMLLoader` splits list-valued attributes on **commas**, not spaces.

```xml
<!-- WRONG: one class literally named "menu-btn menu-btn-primary" -->
<Button styleClass="menu-btn menu-btn-primary"/>

<!-- RIGHT -->
<Button styleClass="menu-btn, menu-btn-primary"/>
```

Nothing matched, so no rule applied. Fixed on all four affected nodes.

### 1.2 Root pinned to 1920x1080 — `MainMenu.fxml`, `LauncherApplication.java`

**Symptom:** About, Quit and the player footer were clipped off the bottom.

**Cause:** the root `StackPane` declared `prefWidth="1920" prefHeight="1080"` and
the `Scene` was constructed at 1920x1080. On a 1920x1080 display at **125%
scaling** the usable scene is only ~1536x793 *logical* units, so the layout pass
ran against a size the window could never show.

**Fix:** root carries no pref size; the scene starts at 1280x720 and maximises.

### 1.3 Background did not rescale — `MainMenu.fxml`, `launcher.css`

Replaced the fixed `ImageView fitWidth="1920" fitHeight="1080"` with a CSS
background on `.menu-root`:

```css
-fx-background-image: url("assets/bg_home.png");
-fx-background-size: cover;
```

`cover` re-fits on every resize; a fixed-size `ImageView` cannot.

### 1.4 Type scale too large for the column — `launcher.css`

`.brand-title` 58px → **44px** (at 58px "THE INFECTED" measured ~490px and
overflowed the 460px column), `.menu-btn` 19px/15-24 padding → **17px/11-22**,
subtitle 18px → 16px. Column narrowed 460 → 440.

### 1.5 Overflow safety net + pinned footer — `MainMenu.fxml`

Two structural changes so this cannot regress on a different screen:

- the brand/button column lives in a `ScrollPane` with `fitToHeight="true"` —
  stretched (so the `vgrow` spacers centre it) when it fits, scrolled when it
  does not, never clipped;
- the player footer (connection chip, name, version) is anchored to the
  **AnchorPane**, outside the scroll region, so connection state is readable at
  every window size.

### 1.6 `Alert.INFORMATION` → `Alert.AlertType.INFORMATION` — `MainMenuController.java`

Compile error introduced while wiring the About dialog; fixed.

**Verification:** screenshotted before/after at 1920x1080 @125%. All six buttons
styled and visible, footer visible, background scales.

---

## 2. The port-collision bug in the docs

`GameConstants.DISCOVERY_UDP_PORT` and `KRYONET_UDP_PORT` were both `54777`,
copied faithfully from **TRD §5, which has the same error**.

KryoNet's `Server` binds 54777 for snapshots. A discovery responder binding the
same port *on the same machine* throws
`BindException: Address already in use` the moment the Host Lobby opens.

**Fix:** discovery moved to **54778**. KryoNet keeps TCP 54555 / UDP 54777.
`LanDiscoveryTest.discoveryPortDoesNotCollideWithKryonet` locks this in.

> **Doc conflict — needs a decision.** `docs/02_TRD1.md` §5 still says 54777 for
> both. The code now disagrees with the doc deliberately. Per the project brain's
> rule ("when this Skill and a doc disagree, the doc wins"), someone should
> update TRD §5 to say 54778 for discovery rather than reverting the code —
> the code is correct and the doc is not.

---

## 3. The socket layer

### 3.1 Discovery — `shared/net/LanDiscovery.java` *(new)*

```
Join Lobby                                  Host Lobby
----------                                  ---------
broadcast "IH_DISCOVER"    --UDP:54778-->    Responder (daemon thread)
collect "IH_HOST:<name>:<v>" <--UDP:54778--  replies to the sender
   |
   +--> user picks a row -> KryoNet TCP 54555 / UDP 54777
```

Plain `DatagramSocket`, deliberately in `shared` (no libGDX, no Spring, no
KryoNet) so the JavaFX lobby can run discovery before a game session exists.

Design notes:

- **Broadcasts go to 255.255.255.255 *and* every interface-specific broadcast
  address.** The global address alone is dropped by some Windows adapter/driver
  combinations; the per-interface addresses are what make this work in practice.
- **`resolveLanIpv4()` enumerates interfaces instead of calling
  `InetAddress.getLocalHost()`.** The latter resolves to `127.0.0.1` on many
  machines via `/etc/hosts`, which is useless to the other laptop.
- **Manual IP entry is permanent, not a debug leftover.** Campus Wi-Fi with
  client isolation drops broadcast entirely (TRD risk table); typing the address
  is the only way in on those networks.
- Bind failure is reported to the lobby as text, never thrown.

### 3.2 Kryo registration — `shared/network/NetworkMessages.java`

**Latent runtime bug fixed.** Kryo serializes the *runtime* class of a field.
`WorldSnapshot.players` is declared `List<PlayerState>` but travels as an
`ArrayList`, and `CloudFrontierDelta.addedTileIndices` is an `int[]`. Neither was
registered, so the first snapshot broadcast would have died with
`IllegalArgumentException: Class is not registered: java.util.ArrayList`.

Both are now registered first in the fixed order. Consequence for callers,
documented on the class: build snapshot lists with `new ArrayList<>()` —
`List.of(...)` returns `ImmutableCollections$ListN`, a different class, and fails
identically.

### 3.3 Host — `core/net/GameServer.java`

Was a skeleton with four TODO blocks. Now complete:

| Area | What it does now |
|---|---|
| Handshake | version check → capacity check → seat assignment → `JoinAccept` |
| Seating | first joiner = ELRIC, second = JANE, max 2 |
| Input | per-connection latest-input map (previously **every player was driven by the same input** — `.values().stream().findFirst()`) |
| Snapshot | players, enemies, cloud **deltas**, objectives, global meter |
| Disconnect | broadcast → freeze → 60s countdown → convert to solo (App Flow §3) |
| Reconnect | rejoin inside the window resumes instead of going solo |
| Events | relays client TCP events to the other player |

**Both players enter through the same door.** The host runs a `GameClient`
against its own loopback, so the host's own player is created by exactly the
same `JoinRequest` handshake as the remote player. There is no "local player"
special case anywhere in the class.

**Buffer sizing.** `new Server()` allocates a 2048-byte object buffer. A snapshot
with two players, enemies and a cloud delta exceeds that and dies with "Buffer
overflow". Both ends now use 64KB write / 16KB object, sized against the TRD §8
budget (≤8KB per snapshot) with headroom.

**Threading**, documented in the class javadoc — KryoNet's thread and the render
thread meet in exactly three places:
1. joins/disconnects mutate the roster → guarded by `rosterLock` so "is there
   room?" and "take the slot" cannot interleave;
2. the roster is a `ConcurrentHashMap` so the sim thread can iterate during a join;
3. each connection's latest input is one volatile reference — a torn read is
   impossible and a dropped input is last-writer-wins, which is correct for a
   30Hz stream.

**Cloud deltas** diff against a per-cloud "already sent" set. A newly joined
client has no history, so joining clears the set and the next snapshot resends
every tile.

### 3.4 Client — `core/net/GameClient.java`

- sends `JoinRequest` over **TCP** after connecting (a UDP handshake can be
  silently dropped);
- handles `JoinAccept` / `JoinReject` / `LevelTransition` / `MatchResultMessage` /
  `EventMessage`;
- `findLocalPlayer(snapshot)` — the client needs `JoinAccept.assignedPlayerId`
  (new field) to pick its own entry out of the snapshot for camera and HUD;
- **nothing throws on a network problem.** Refused connection, full lobby,
  version mismatch and a vanished host all reach
  `GameBridge.notifyJoinFailed(reason)`, which the launcher turns into a dialog.
  Previously `connect()` threw `RuntimeException` and killed the window.

### 3.5 Interpolation — `core/net/SnapshotInterpolator.java`

Was "return the newest snapshot older than target", which stutters visibly at
20Hz inside a 60 FPS loop. Now true interpolation: find the two snapshots
straddling `renderTime - 100ms`, compute `alpha`, and **lerp positions only**.

HP, downed, revive seconds and objective progress snap to the newer snapshot —
interpolating them would render states the host never simulated (73.4 HP, a
half-completed pickup).

Also added: out-of-order UDP rejection by `serverTick`, and locking, because
`pushSnapshot` runs on KryoNet's thread while `getInterpolated` runs on the
render thread.

### 3.6 Session plumbing

- `core/net/SessionConfig.java` *(new)* — one immutable value carrying
  host/join, address, player identity and backend URL across the FX→libGDX
  boundary, instead of positional booleans. `advertisedBackendUrl()` substitutes
  the host's real LAN IP for `localhost` so the joining laptop writes saves to
  the same backend (TRD §6).
- `core/bridge/GameBridge.java` — added `notifyJoinFailed` and
  `notifyPartnerEvent`; all callbacks are now `volatile` (set on the FX thread,
  invoked from the render and KryoNet threads).
- `InfectedHourGame.stepSimulation(delta)` — the fixed-timestep accumulator,
  clamped at 0.25s so one long frame cannot trigger a catch-up spiral. No-op on
  a joining machine, which owns no world state.

### 3.7 Lobby screens

- `HostLobbyView` — starts the discovery responder, shows the real LAN IP,
  populates level select from the backend save, stops the responder before the
  match starts (so nothing races over UDP on that machine).
- `JoinLobbyView` — discovery runs on a background thread (`probe()` blocks for
  its full 2s window; on the FX thread that is a 2-second UI freeze), results
  published via `Platform.runLater`. A typed IP always beats the selected row.
- `CoopModeView` *(new)* — Host/Join fork.
- `MainMenuController` — buttons wired to real destinations instead of TODOs.

### 3.8 Screens made functional

`LevelBriefingScreen`, `GameScreen` and `StoryPanelScreen` were black screens
with no input path, so a "runnable app" ended at the menu. They now render real
state and drive the network:

- **LevelBriefingScreen** — READY gate over TCP, "waiting for partner…" with
  animated dots, auto-ready in solo, live connection/port debug line.
- **GameScreen** — steps the sim (host), sends input at 30Hz, draws every entity
  from the interpolated snapshot as coloured quads, shows partner
  disconnect/reconnect banners. Art is placeholder; swapping in sprites changes
  only `drawWorld`.
- **StoryPanelScreen** — typewriter text, both-players-advance sync with the 20s
  fallback so nobody is held hostage.

Placeholder art is intentional and marked; the networking underneath is real.

---

## 3.9 Two bugs that only the live two-instance run exposed

Both passed the unit tests and both would have wasted an afternoon on demo day.
Each now has a regression test.

### 3.9.1 The host believed it was solo forever

**Symptom:** two instances connected and each reported the correct character,
but the **host** showed `mode: SOLO` while the client showed `mode: COOP`.

**Cause:** `JoinAccept.matchMode` is a snapshot of the roster at the instant that
machine joined. The host joins its own loopback *first*, when it genuinely is
alone — so it received `SOLO` and never heard otherwise.

**Consequence:** the host's ready gate auto-opens in solo, so the host would
start the level alone and strand the partner on the briefing screen forever.

**Fix:** `GameClient` now updates `matchMode` from the `PARTNER_JOINED` /
`PARTNER_RECONNECTED` / `CONVERTED_TO_SOLO` events the host already broadcasts.
`LevelBriefingScreen` also stopped *latching* "solo means the partner is ready"
and recomputes the gate every frame, so the gate closes again when the mode
flips to COOP.

Regression test: `LoopbackSessionTest.hostMatchModeFlipsToCoopWhenPartnerArrives`.

### 3.9.2 The Host Lobby displayed an unreachable IP address

**Symptom:** the lobby showed `Your LAN address: 192.168.19.1`, but a discovery
broadcast against that same running lobby was answered from `192.168.0.206`.

**Cause:** `resolveLanIpv4()` returned the first non-loopback IPv4 from
`NetworkInterface.getNetworkInterfaces()`. On any machine with VirtualBox,
VMware, Hyper-V, WSL or Docker installed that is usually a *virtual* adapter.
`NetworkInterface.isVirtual()` does not catch these — it flags sub-interfaces
like `eth0:1`, not virtual NICs.

**Consequence:** the number the host reads aloud to their partner is one the
partner's laptop cannot route to. This is exactly the failure that gets
misdiagnosed as "the networking is broken" during a demo.

**Fix:** ask the routing table. "Connect" a UDP socket to an off-link address and
read back the local address the OS chose:

```java
try (DatagramSocket socket = new DatagramSocket()) {
    socket.connect(InetAddress.getByName("8.8.8.8"), 53);
    return socket.getLocalAddress().getHostAddress();
}
```

UDP `connect` only fixes the peer for later sends — **no packet is transmitted**
and nothing has to be reachable, so this works on an air-gapped LAN. Interface
enumeration remains the fallback for a machine with no default route, and now
deprioritises adapters whose names look virtual.

Regression test: `LanDiscoveryTest.resolveLanIpv4ReturnsSomethingUsable`
cross-checks against the routing table.

---

## 4. Build fixes

### 4.1 `Failed to load JUnit Platform` — all modules

Gradle 8+/9 no longer puts `junit-platform-launcher` on the test runtime
classpath implicitly. Added `testRuntimeOnly("org.junit.platform:junit-platform-launcher")`
to `shared`, `core` and `backend`.

Declared **per module, not** in a root `subprojects { dependencies { } }` block:
doing it at the root mutates `testRuntimeOnly` (and therefore `runtimeOnly`)
after Spring Boot's plugin has resolved `:backend:runtimeClasspath`, which fails
`bootJar`. There is a comment in the root build file saying so.

### 4.2 `./gradlew build` was already broken before this session

`:backend:bootJar` failed with:

```
Cannot mutate the dependency attributes of configuration ':backend:runtimeOnly'
after the configuration's child configuration ':backend:runtimeClasspath' was resolved.
```

Confirmed pre-existing by stashing the backend build file and re-running.
The wrapper is Gradle **9.6.1**; the Spring Boot **3.3.0** plugin does not
support Gradle 9.

**Fix:** Spring Boot plugin 3.3.0 → **3.5.4**, dependency-management 1.1.5 →
**1.1.7**. Both backend tests still pass (`AuthControllerTest`,
`MatchCompleteIntegrationTest`).

---

## 5. Tests added

`core/src/test/java/.../net/LoopbackSessionTest.java` — the "loopback host+client
in one JVM" check TRD §10 asks for on every day networking is touched. Real
KryoNet over real sockets, no mocks, because the failures being guarded against
(unregistered Kryo classes, buffer overflow, a handshake that never completes)
only appear on the wire.

| Test | Asserts |
|---|---|
| `twoPlayersJoinAndGetDistinctCharacters` | first joiner ELRIC, second JANE, shared backend URL, mode COOP |
| `thirdPlayerIsRejectedAsLobbyFull` | third connection gets `LOBBY_FULL`, roster stays at 2 |
| `snapshotsAreBroadcastAndDeserialize` | snapshot arrives and deserializes with every joined player |
| `clientInputDrivesTheAuthoritativeSimulation` | input sent by the client moves that client's player on the host |

`shared/src/test/java/.../net/LanDiscoveryTest.java` — responder answers with
name+version, ignores foreign packets, ports do not collide, `resolveLanIpv4`
always returns something usable.

Discovery is tested with a **directed** datagram to 127.0.0.1, not a broadcast:
broadcast delivery to your own machine depends on the OS, the adapter and
whether a VPN or virtual switch is installed, so a broadcast-based test fails for
reasons unrelated to this code. Broadcast fan-out is what the two-laptop
playtest covers.

**Requires the game ports to be free** — stop any running copy before
`./gradlew test`, or `bind()` fails with "Address already in use".

---

## 6. Two-laptop run book

1. **Same network.** Both laptops on one LAN or one phone hotspot. Campus Wi-Fi
   with client isolation will not work — that is what the manual IP field is for.
2. **Firewall.** Allow inbound TCP 54555 and UDP 54777/54778 for Java on the
   host. This is the single most common cause of "no host answered".
3. **Backend** (host laptop only, optional — the game runs offline):
   ```bash
   ./gradlew :backend:bootRun
   ```
4. **Host laptop:** `./gradlew :fx-launcher:run` → 2 Player Co-op → Host a match.
   Note the LAN address shown.
5. **Client laptop:** `./gradlew :fx-launcher:run` → 2 Player Co-op → Join →
   *Search the LAN*, or type the host's address.
6. Both press **E** on the briefing screen; the match starts when both are ready.

Expected: host is ELRIC, client is JANE, both see `mode: COOP`, and both see two
squares moving when either player presses WASD.

---

## 7. Still open

Not touched — these are gameplay, not networking, and are still marked with
`TEAMMATE TASK` blocks in the code:

- `LevelLoader` — real Tiled `.tmx` loading and collision extraction
- `MovementSystem` — input normalisation and tile collision (diagonal movement is
  still ~41% faster)
- `ContaminationSystem.computeFrontierExpansion` — BFS growth returns an empty
  set, so clouds never actually expand; the snapshot delta path around it is
  finished and tested
- `AISystem` — enemies neither chase nor attack
- `Hud` / `MiniMap` / `InventoryHotbar` — real Scene2D widgets
- `BossScreen` — the three phase bodies
- Sprites and audio; everything on screen is a coloured quad
- `backend.MatchService.upsertLeaderboardEntries` — still a stub

Find them with:

```bash
grep -rn "TEAMMATE TASK" --include="*.java" .
```

---

## 8. Launcher-to-game presentation fixes (2026-09-01)

### 8.1 Launcher no longer disappears while the game window starts

**Symptom:** pressing single-player, starting a hosted match, or joining a match
hid the complete JavaFX application for several seconds before the libGDX
window appeared. The desktop was exposed during startup, which looked like the
application had crashed.

**Cause:** `GameLauncherBridge.startMatch()` called `primaryStage.hide()` before
the native LWJGL window had been created or rendered.

**Fix:** the launcher now places a modal loading overlay over the current view
and keeps the stage visible. `InfectedHourGame` reports readiness through
`GameBridge` after its first frame is rendered; only then is the launcher stage
hidden. Startup and connection failures remove the overlay, restore the
launcher, and display an error instead of leaving a blocked or invisible UI.

Files changed:

- `fx-launcher/src/main/java/com/infectedhour/fxlauncher/bridge/GameLauncherBridge.java`
- `fx-launcher/src/main/resources/launcher.css`
- `core/src/main/java/com/infectedhour/core/bridge/GameBridge.java`
- `core/src/main/java/com/infectedhour/core/InfectedHourGame.java`
- `core/src/test/java/com/infectedhour/core/bridge/GameBridgeTest.java`

### 8.2 Native game now opens fullscreen by default

**Symptom:** the maximized JavaFX launcher was replaced by a centered 1280x720
game window, leaving the launcher visible around it.

**Cause:** `Lwjgl3Launcher` used `setWindowedMode(1280, 720)`. The 1280x720
value in the UI/UX document is the game's **base virtual resolution** for
consistent layout and scaling; it was incorrectly being used as the physical
desktop window size.

**Fix:** the LWJGL application now uses the current monitor's display mode and
starts fullscreen. The existing Settings checkbox is connected to
`SessionState`, defaults to enabled, and can select the 1280x720 windowed mode
for the next launch. The direct `:lwjgl3:run` development shortcut also defaults
to fullscreen.

Files changed:

- `lwjgl3/src/main/java/com/infectedhour/lwjgl3/Lwjgl3Launcher.java`
- `fx-launcher/src/main/java/com/infectedhour/fxlauncher/bridge/GameLauncherBridge.java`
- `fx-launcher/src/main/java/com/infectedhour/fxlauncher/state/SessionState.java`
- `fx-launcher/src/main/java/com/infectedhour/fxlauncher/views/SettingsView.java`

### 8.3 Loading overlay is removed when the game opens or closes

**Regression:** after the loading overlay was introduced, closing the native
game window could return to a launcher that remained dimmed and permanently
displayed “Preparing your story…”.

**Cause:** the original menu root was still a child of the temporary
`StackPane` wrapper when `Scene.setRoot(originalRoot)` tried to restore it.
JavaFX rejected a node being attached in two places and threw
`IllegalArgumentException: ... is already inside a scene-graph`. Because the
exception occurred inside the ready callback, the launcher never hid normally;
the close callback then hit the same failure and could not clear the overlay.

**Fix:** `removeLoadingScreen()` now clears its transition state first, detaches
the original menu from the wrapper, and only then restores it as the scene
root. The operation is idempotent, so ready, connection-failure, startup-error,
and game-close callbacks can safely race without leaving a stuck overlay.

File changed:

- `fx-launcher/src/main/java/com/infectedhour/fxlauncher/bridge/GameLauncherBridge.java`

### 8.4 Returning with Q no longer exposes the desktop

**Symptom:** choosing “Exit to Main Menu” with **Q** closed the fullscreen
libGDX window immediately, then left the desktop visible while JavaFX restored
and painted the main menu.

**Cause:** `GameScreen` called `onGameWindowClosed()` and `Gdx.app.exit()` in
the same frame. `InfectedHourGame.dispose()` then emitted the close event a
second time. There was no acknowledgement that the launcher was ready before
the native fullscreen window disappeared.

**Fix:** `GameBridge` now supports an orderly return handshake. JavaFX restores
the menu, brings its stage forward, and waits 150 ms for a render pulse before
releasing the libGDX window. An atomic guard makes the later `dispose()` close
notification a harmless fallback instead of a duplicate navigation. The pause
overlay displays “RETURNING TO MAIN MENU…” while the short handoff completes.

Files changed:

- `core/src/main/java/com/infectedhour/core/bridge/GameBridge.java`
- `core/src/main/java/com/infectedhour/core/screens/GameScreen.java`
- `core/src/test/java/com/infectedhour/core/bridge/GameBridgeTest.java`
- `fx-launcher/src/main/java/com/infectedhour/fxlauncher/bridge/GameLauncherBridge.java`

### 8.5 Play remains locked until the previous server releases its ports

**Symptom:** after returning to the menu, immediately pressing Play could show
“Ports 54555/54777 are already in use” even though no separate game was visible.

**Cause:** the reverse handoff made the JavaFX menu interactive before
`InfectedHourGame.dispose()` had disconnected the client and stopped the
`GameServer`. A fast click could therefore start another libGDX application in
the same JVM while the previous application still owned the sockets.

**Fix:** the launcher now displays a blocking “Closing current session…” overlay
during the short shutdown interval. Only the actual `onGameWindowClosed`
callback—which is emitted after `client.disconnect()` and `server.stop()`—can
remove that overlay and re-enable Play. This makes rapid relaunches safe and
prevents multiple libGDX instances from competing for global state and ports.

File changed:

- `fx-launcher/src/main/java/com/infectedhour/fxlauncher/bridge/GameLauncherBridge.java`

### 8.6 F3 debug view no longer crashes the game

**Symptom:** pressing **F3** displayed an error and returned to the launcher;
subsequent launches could temporarily report that the game ports were busy.

**Cause:** the split-screen branch ended `SpriteBatch` after rendering its two
camera views, but the shared zombie and inventory code continued calling
`batch.draw()` and then `batch.end()`. libGDX requires every draw to be inside
exactly one matching `begin()` / `end()` pair, so the first F3 frame threw and
disposed the game.

**Fix:** zombie simulation now updates once per frame and its current sprite is
drawn inside each camera pass. Both the normal and dual-view passes own complete
and balanced batch lifecycles, while the inventory uses a separate HUD batch.
F3 now only toggles the debug split-screen view and cannot launch, close, or
restart a session.

File changed:

- `core/src/main/java/com/infectedhour/core/screens/GameScreen.java`

### 8.7 Home menu redesigned as a cinematic console dashboard

**Request:** replace the form-like vertical menu with a home screen inspired by
modern console game libraries while retaining *The Infected Hour* visual
identity and existing navigation behavior.

**Change:** the village scene now uses a lighter cinematic overlay, a compact
top navigation bar, and a horizontally scrollable activity rail for Story,
Co-op, Load Game, Settings, About, and Quit. A featured Story panel and compact
operation summary fill the lower portion of the screen. Gold, crimson, and
night-blue styling keeps the dashboard consistent with the survival-horror
theme. All cards remain connected to their original controller actions, and
the loading overlay is unchanged.

Files changed:

- `fx-launcher/src/main/resources/assets/bg_home_console.png`
- `fx-launcher/src/main/resources/views/MainMenu.fxml`
- `fx-launcher/src/main/resources/launcher.css`

### 8.8 Intro and results screens receive cinematic presentation

**Problem:** the story intro rendered gold and white placeholder copy on a
plain black frame, while the defeat screen used a nearly empty JavaFX layout.
Neither screen matched the visual quality of the redesigned home menu.

**Change:** the intro now presents a rewritten three-part Ashgrove briefing
over illustrated arrival artwork, with a dark readability gradient, stronger
heading hierarchy, typewriter copy, progress counter, and the existing co-op
advance synchronization preserved. The results screen now uses outcome-aware
cinematic copy, real reached-level data, an operation-status card, themed
buttons, and separate victory/defeat background treatment.

Files changed:

- `assets/story_intro.png`
- `core/src/main/java/com/infectedhour/core/screens/StoryPanelScreen.java`
- `fx-launcher/src/main/java/com/infectedhour/fxlauncher/views/ResultsView.java`
- `fx-launcher/src/main/resources/assets/bg_defeat.png`
- `fx-launcher/src/main/resources/launcher.css`

### 8.9 Upstairs exit now advances the campaign

**Problem:** reaching the upstairs exit sign in Level 1 provided no prompt and
could not advance the run, even though a `LevelTransition` network message
already existed.

**Change:** the exit is now an explicit tile-based interaction zone. When the
local player approaches it, the HUD displays **Press [E] to proceed to LEVEL
2** (or explains that unfinished objectives still lock the exit). The E press
travels to the host as a reliable TCP event; the host validates the player's
authoritative position and objective state, broadcasts one level transition,
and every client enters the post-level story followed by the next briefing.
Level 2 has the same mechanism at its service-tunnel exit, while the final boss
level intentionally has no next-level zone.

Files changed:

- `core/src/main/java/com/infectedhour/core/level/LevelExit.java`
- `core/src/main/java/com/infectedhour/core/net/GameServer.java`
- `core/src/main/java/com/infectedhour/core/screens/GameScreen.java`
- `core/src/test/java/com/infectedhour/core/level/LevelExitTest.java`
- `shared/src/main/java/com/infectedhour/shared/constants/GameConstants.java`

### 8.10 Level 2 and Level 3 campaign skeleton

**Request:** continue from the existing hospital level into an outdoor roadside
village, then return through a hidden route to a laboratory for the final boss.

**Level 2 — Roadside Village:** the level now has its own generated outdoor
placeholder map instead of reusing the hospital artwork. The mission contains a
zombie-road-patrol encounter, two stranded villagers, two power-relay puzzle
points, a visible objective list, interaction prompts, and the existing service
tunnel exit. Objective actions travel reliably to the host, are validated
against the player's position, and can only be counted once across both players.
The tunnel remains locked until every objective is complete.

**Level 3 — Hidden Laboratory:** the previous TODO-only boss screen is now a
playable three-phase skeleton. Players inject three cure samples, attack the
exposed Virus Heart during its damage window, then hold the final overload
action to destroy the core. The arena has a temporary laboratory visualization,
phase instructions, cure indicators, a boss-health bar, and a working transition
to the campaign ending.

**Campaign presentation:** level definitions, briefing mission text, and story
panels now consistently describe Level 1 as Ashgrove Hospital, Level 2 as the
Roadside Village, and Level 3 as the Hidden Laboratory.

Files changed:

- `core/src/main/java/com/infectedhour/core/level/CampaignLevelPlan.java`
- `core/src/main/java/com/infectedhour/core/level/LevelDefinition.java`
- `core/src/main/java/com/infectedhour/core/net/GameServer.java`
- `core/src/main/java/com/infectedhour/core/screens/BossScreen.java`
- `core/src/main/java/com/infectedhour/core/screens/GameScreen.java`
- `core/src/main/java/com/infectedhour/core/screens/LevelBriefingScreen.java`
- `core/src/main/java/com/infectedhour/core/screens/StoryPanelScreen.java`
- `core/src/main/java/com/infectedhour/core/systems/ObjectiveSystem.java`
- `core/src/test/java/com/infectedhour/core/level/CampaignLevelPlanTest.java`
- `core/src/test/java/com/infectedhour/core/systems/ObjectiveSystemTest.java`
- `shared/src/main/java/com/infectedhour/shared/constants/GameConstants.java`
- `shared/src/main/java/com/infectedhour/shared/level/CheckpointRegistry.java`
- `shared/src/test/java/com/infectedhour/shared/level/CheckpointRegistryTest.java`
