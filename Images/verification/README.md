# Verification screenshots — 2026-07-31

Evidence for the changes described in `docs/07_CHANGE_LOG_LAN_AND_MENU.md`.
Captured on a 1920x1080 display at 125% Windows scaling.

| File | What it shows |
|---|---|
| `01_menu_before.png` | **Before.** "HOUR" and both primary buttons unstyled (comma-vs-space `styleClass` bug); About/Quit/footer clipped off the bottom. |
| `02_menu_after.png` | **After.** Gold "HOUR", all six buttons styled, footer with connection chip and version visible, background scaling with the window. |
| `03_coop_briefing.png` | Two instances connected. Left = host (`ELRIC`), right = client (`JANE`), **both** reporting `mode: COOP`, each waiting for the other's READY. |
| `04_coop_ingame.png` | Both in `GameScreen`. Identical `tick 11292` and `global contamination 28.2%` on both — one authoritative simulation, two views. Each renders both players. |
| `05_coop_movement_sync.png` | `D` held on the **client window only**. Jane (gold) moved right on **both** screens by the same amount; Elric (green) did not move. Client input → UDP → host sim → snapshot → both renderers. |
| `06_host_lobby_discovery.png` | Host Lobby with the discovery responder live on UDP 54778, showing the **routable** LAN address. Before the fix this read `192.168.19.1` (a virtual adapter) while the responder actually answered from `192.168.0.206` — see change log §3.9.2. |

## Reproducing 03–05 on one PC

```bash
./gradlew :lwjgl3:run
```

```bash
./gradlew :lwjgl3:run --args="join"
```

Press `E` in both windows to clear the ready gate and the story panels, then move
with `WASD` in either window and watch both.
