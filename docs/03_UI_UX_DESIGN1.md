# The Infected Hour — UI/UX Design Document

**Version:** 2.0 (6-day sprint) — items marked **[STRETCH]** are skipped unless time remains

---

## 1. Visual Identity

Derived from the proposal deck's look: dark, moody pixel-art village under an epidemic.

### Palette
| Token | Hex | Use |
|---|---|---|
| `bg-night` | `#0E1420` | Backgrounds, JavaFX launcher base |
| `panel-dark` | `#1C2536` | Cards, HUD panels (semi-transparent 90%) |
| `accent-gold` | `#E8B02A` | Titles, highlights, objective markers |
| `accent-red` | `#E85A4F` | Danger, contamination, boss UI, failure |
| `safe-green` | `#4CAF6D` | Safe zones, success, health fill |
| `toxic-purple` | `#7B4FA6` | Contamination clouds, infection VFX |
| `text-primary` | `#F2F0E9` | Body text |
| `text-muted` | `#8A93A6` | Secondary text |

### Typography
- **Pixel display font** (e.g., "Press Start 2P" or "VCR OSD Mono") — titles, HUD numbers
- **Clean sans** (e.g., "Jost" / system) — story panel text, JavaFX forms, long text
- Minimum in-game text size: 14 px at 1280×720 base resolution

### Resolution
- Base virtual resolution **1280×720**, `FitViewport`, scales to window/fullscreen
- Pixel art at 16×16 tile size rendered at 2× (32 px on screen)

## 2. Screen Inventory

### JavaFX Launcher (pre-game)
1. **Splash / Login** — logo, username + password, Login / Register buttons, offline mode link
2. **Register** — username, display name, password ×2
3. **Main Menu (Lobby Hub)** — Play (Host / Join), Profile & Saves, Leaderboards, Settings, Credits, Quit
4. **Host Lobby** — shows host IP, level select (unlocked only), waiting slot for Player 2, character lock (Elric fixed to host), Start button (enabled solo or when P2 joins)
5. **Join Lobby** — auto-discovered hosts list (refresh), manual IP field, character preview (Jane), Ready toggle
6. **Profile** — stats (levels cleared, best times, contamination best), save slot info, logout
7. **Leaderboards** — tabs: Fastest Clear (per level), Lowest Contamination, Boss Time; top 20 rows
8. **Settings** — music/SFX volume sliders, fullscreen toggle, keybind display, backend URL (advanced)
9. **Post-Match Results** (returns here from game) — victory/defeat banner, per-player stats table, "New leaderboard entry!" badge, Play Again / Menu

### libGDX Screens (in-game)
10. **Level Briefing** — mission list with icons, controls reminder, "Both players ready" sync gate
11. **Gameplay (HUD)** — described below
12. **Pause Overlay** — Resume, Settings (volume only), Abandon Match (host) / Leave (client); multiplayer: pausing shows "Host paused" on client
13. **Story Panel Screen** — full-screen illustrated panel, typewriter text, both-players-ready advance
14. **Level Complete / Failed Overlay** — objective checklist recap, stats, Continue / Retry
15. **Boss Title Card** — static "The Virus Heart" card with roar SFX before Level 3 (full reveal-pan cinematic is **[STRETCH]**)
16. **Ending / Credits** — final panels → credits scroll with asset attributions

## 3. Gameplay HUD Layout (1280×720)

```
┌──────────────────────────────────────────────────────────────┐
│ [P1 portrait][HP bar][contam bar]      GLOBAL CONTAMINATION  │  top-left: local player
│ [P2 portrait][HP bar][contam bar]      [███████░░░░ 62%]     │  top-right: doom meter
│                                                              │
│                                                    ┌──────┐  │
│                    GAME WORLD                      │ MINI │  │  bottom-right minimap
│                                                    │ MAP  │  │  (Jane sees larger radius)
│                                                    └──────┘  │
│ OBJECTIVES                                                   │
│ ◆ Rescue villagers 2/4                                       │
│ ◆ Place barricades 1/3                 [1][2][3][4] hotbar   │  bottom-left objectives,
└──────────────────────────────────────────────────────────────┘  bottom-center inventory
```

- **Health bar:** green fill, shakes + red vignette flash on damage
- **Personal contamination bar:** purple fill under HP; pulses at ≥ 75% with heartbeat SFX
- **Global contamination meter:** top-center-right wide bar, gold frame; turns red ≥ 80% with screen-edge purple creep
- **Objective list:** auto-updates with checkmarks + brief gold flash on progress
- **Inventory hotbar:** 4 slots, keys 1–4 select, `E` interact/use, `Q` drop
- **World markers:** off-screen objectives get edge arrows in `accent-gold`; partner off-screen gets a small portrait arrow
- **Interaction prompts:** floating `[E] Rescue`, `[Hold E] Sanitize (2.5s)` with radial progress ring
- **Revive:** downed partner shows countdown ring (30s) over body; `[Hold E] Revive (3s)`

## 4. Controls

| Action | Key (both laptops use own keyboard) |
|---|---|
| Move | WASD |
| Interact / hold-interact | E |
| Attack | Space or left mouse |
| Ability (Elric heal / Jane dash) | Left Shift |
| Inventory slots | 1–4 |
| Drop item | Q |
| Minimap zoom | M |
| Pause | Esc |

## 5. UX Rules

1. **Never block gameplay on network UI** — sync waits (briefings, story panels) show a clear "Waiting for partner…" state with animated dots.
2. **Readable danger** — contamination tiles always have animated purple overlay + edge shimmer; expansion telegraphed 1.5 s ahead with faint outline.
3. **One-glance status** — a player must be able to read own HP, own contamination, global meter, and current objective within one second.
4. **Failure teaches** — fail screen states *why* (e.g., "The contamination reached 100% — barricades slow the spread") tying back to public-health learning.
5. **Story skippable together** — panels advance only when both press E (prevents one player skipping the other's story), with a 20 s auto-ready fallback.
6. **Consistent iconography** — shield = isolate, vial = sample, cross = medicine, running figure = rescue, spray = sanitation, cloud = contamination (mirrors the proposal deck icons).
7. **Colorblind safety [STRETCH]** — hatched overlay pattern on contamination in addition to purple; in v1 rely on position + icon differentiation.

## 6. Story Panels

- Full-screen 16:9 illustrated stills (pixel-painted), dark vignette, text box in `panel-dark` at bottom
- Typewriter text at 40 chars/sec, E to complete instantly, E again to advance
- 2–3 panels per sequence; 4 sequences: Intro, After L1 (Elric's family flashback), After L2 (Jane's streets + the earlier outbreak truth), Ending (redemption)
- Panel art: repurposed free key-art / tinted screenshots with vignette — custom illustrated panels are **[STRETCH]**

## 7. JavaFX Launcher Styling

- Single dark theme stylesheet (`launcher.css`) using the palette tokens above
- Background: blurred village key art with `bg-night` overlay at 85%
- Gold underline motif on headings (mirrors the proposal slides)
- Buttons: `panel-dark` fill, gold border on hover, 8 px radius
- Forms validate inline (red border + message under field)
- Lobby shows live connection status chip: gray "Searching…", green "Connected", red "Lost"

## 8. Audio–UX Mapping

| Event | Cue |
|---|---|
| Objective progress | short gold chime |
| Contamination ≥ 75% (personal) | heartbeat loop |
| Global meter ≥ 80% | low drone layered into music |
| Villager rescued | relieved vocal + chime |
| Boss phase change | roar + music stem switch |
| Victory / defeat | full stinger, music stops first for 0.5 s |
