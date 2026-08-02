# The Infected Hour — Application Flow

**Version:** 2.0 (6-day sprint: 3 levels, boss = Level 3)

---

## 1. Top-Level Flow

```
[Launch app (JavaFX)]
        │
        ▼
[Splash/Login] ──(no account)──▶ [Register] ──▶ back to Login
        │ auth OK (JWT stored)          │
        │◀── offline mode (solo only) ──┘
        ▼
[Main Menu] ──▶ [Profile] / [Leaderboards] / [Settings] / [Quit]
        │
        ├──▶ [Host Lobby]  (this laptop = server + Player 1 Elric)
        │        │  start solo OR when P2 ready
        │        ▼
        └──▶ [Join Lobby]  (this laptop = client, Player 2 Jane)
                 │  select discovered host / enter IP → Ready
                 ▼
        [JavaFX hides → libGDX window opens]
                 ▼
        [GAME SESSION]  (flow in §2)
                 ▼
        [libGDX closes → JavaFX Results screen]
                 │
                 ├──▶ Play Again → back to lobby (roles kept)
                 └──▶ Main Menu
```

## 2. Game Session Flow (inside libGDX)

```
[Level Briefing]  ← both players ready-gate
      ▼
[Intro Story Panels]        (level 1 only)
      ▼
┌────────────────────────────────────────────────┐
│              GAMEPLAY LOOP (per level)         │
│                                                │
│  explore → objectives → manage contamination   │
│      │                                         │
│      ├── all objectives done ──▶ LEVEL CLEAR   │
│      ├── global contamination 100% ─▶ FAIL     │
│      ├── both players downed ───────▶ FAIL     │
│      └── Esc → Pause overlay                   │
└────────────────────────────────────────────────┘
      │ CLEAR                        │ FAIL
      ▼                              ▼
[Level Complete overlay]      [Level Failed overlay + lesson text]
      ▼                              │
[Story Panel sequence]               ├─ Retry → restart same level
      ▼                              └─ Quit  → Results (defeat)
next level? ──yes──▶ [Level Briefing] (next)
      │no (L3 = boss cleared)
      ▼
[Ending panels] → [Credits] → Results (victory)
```

### Boss level (L3) sub-flow
```
[Boss Title Card]
  → Phase 1: Shield (Elric plants cure samples ×3, Jane defends)
  → Phase 2: Exposure window (both attack core, 15 s) 
       └─ core survives → shield returns (weaker) → back to Phase 1
  → core HP < 25% → Phase 3: Core Destruction (simultaneous strike QTE)
  → core destroyed → Victory
  (any time: arena contamination 100% → Fail)
```

## 3. Multiplayer Session States (host machine)

```
LOBBY_OPEN → CLIENT_JOINED → LOADING (both load level, ack)
   → BRIEFING_SYNC → PLAYING ⇄ PAUSED(host only)
   → LEVEL_TRANSITION (results push to backend, next-level ack)
   → MATCH_END (final results push) → CLOSED
Client drop during PLAYING → WAITING_RECONNECT (60 s) → solo-continue or abort
Host drop → client shows error → JavaFX lobby
```

## 4. Data Flow per Match

```
Login:        FX ──POST /auth/login──▶ Backend ──JWT──▶ FX (both laptops, same backend on host IP)
Lobby join:   Client ──UDP discover──▶ Host ──JoinAccept{backendUrl}──▶ Client
Gameplay:     Client input ──UDP 30Hz──▶ Host sim ──snapshots UDP 20Hz──▶ Client render
Level clear:  Host ──POST /matches/{id}/level-result──▶ Backend
Match end:    Host ──POST /matches/{id}/complete (both players' stats + saves)──▶ Backend
Save sync:    Each laptop ──GET /players/me/save──▶ Backend → local cache updated
Leaderboard:  FX Results ──GET /leaderboards?...──▶ Backend
```

## 5. Screen ↔ State Ownership

| Screen | Module | Network role |
|---|---|---|
| Login/Register/Menu/Profile/Settings/Leaderboards/Results | fx-launcher | REST only |
| Host/Join Lobby | fx-launcher | UDP discovery + TCP session setup |
| Briefing, Gameplay, Pause, Story, Overlays, Boss, Credits | core (libGDX) | KryoNet session |

## 6. Edge Flows

- **Offline mode:** Login skipped → solo only; saves written to local JSON; on next successful login, local save uploaded if newer.
- **Backend down mid-match:** results queued to `~/.infectedhour/pending/` and retried on next launcher start; match continues normally.
- **Version mismatch host/client:** JoinAccept carries protocol version; mismatch → clear error dialog "Update required on <machine>".
- **Second join attempt while full:** host rejects with `LOBBY_FULL`; client sees toast.
- **Alt-F4 during game:** treated as disconnect (see §3); local pending results saved.
