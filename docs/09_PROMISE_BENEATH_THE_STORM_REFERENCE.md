# Promise Beneath the Storm — Engineering Reference for The Infected Hour

**Studied repository:** <https://github.com/shahamatirtisham/Promise-Beneath-the-Storm>  
**Revision inspected:** `801256be9a069694d0c37a0cdb4244db4744e71a` (`main`, 10 September 2026)  
**Purpose:** capture reusable gameplay lessons—especially input, keys/puzzles, combat, and boss design—before implementing the Level 3 Virus Heart fight.

This is an analysis and adaptation guide, not a source-code copy. The reference
repository did not contain a licence file at the inspected revision, so its code
and assets should not be copied into this project unless the owner gives explicit
permission and a compatible licence is added. The architecture and gameplay
patterns described here can be independently implemented for our own theme.

## 1. What was verified

- The repository was cloned to a temporary directory and all remote branches were
  inspected. `main`, `abtahi-new`, and `arkam-new` currently point to the same
  revision. The sprite branch is older.
- The core module contains 121 Java files and about 9,300 lines of Java.
- `:core:compileJava` succeeds. The only output is three warnings that Java 8
  source/target settings are obsolete on the installed JDK.
- There are no automated test files in the inspected repository.
- The README is behind the code: it calls several features “planned,” but the
  current source already contains the complete four-stage boss, telegraphs,
  combat, keys, chests, levers, checkpoints, game-over, and victory flow.

## 2. Repository architecture

The game uses LibGDX, Ashley ECS, and Box2D.

| Area | Responsibility | Approximate files |
|---|---|---:|
| `components` | Data and state such as health, boss phase, attack timers, status effects, teams, keys, and defence | 42 |
| `systems` | Input, AI, combat, boss behaviour, projectiles, death, collection, puzzle interactions, and status updates | 34 |
| `entities` | Factories that assemble players, enemies, the boss, projectiles, keys, chests, and rewards | 11 |
| `dungeon` | Level themes, room layouts, encounter recipes, doors, hazards, and generation | 12 |
| `screens` | Menu, gameplay, pause, settings, game over, upgrades, credits, and victory | 10 |
| `ui` | HUD and reusable menu/control builders | 6 |
| `state` | Input preferences and checkpoint/run state | 2 |

The important separation is:

1. A component stores state but does not decide what happens.
2. A system changes that state each simulation tick.
3. A factory creates a complete entity with all required components.
4. `GameScreen` owns the level/room lifecycle and connects systems to the HUD.

System ordering is part of the gameplay contract. Input and defence update before
attacks; AI chooses actions before combat resolution; invulnerability and damage
resolution happen before death/reward processing. Changing that order can create
one-frame bugs.

### Main reference files

All paths below are relative to the friend repository:

| Topic | Files to consult |
|---|---|
| Boss data and state | `core/src/main/java/com/github/shahamatirtisham/promise_beneath_the_storm/components/BossComponent.java` |
| Boss phase thresholds | `.../systems/BossPhaseSystem.java` |
| Boss attack execution | `.../systems/BossCombatSystem.java` |
| Boss creation and base stats | `.../entities/EnemyFactory.java` |
| Combat input and timing | `.../systems/InputSystem.java`, `AttackSystem.java`, `DamageSystem.java` |
| Normal enemy behaviour | `.../systems/EnemyAISystem.java`, `EnemyAttackSystem.java`, `DeathSystem.java` |
| Blocking, parrying, and dash | `.../systems/DefenseSystem.java`, `DashSystem.java` |
| Ranged attacks | `.../systems/ProjectileSystem.java`, `PlayerRangedSystem.java`, `RangedAttackSystem.java` |
| Keys and puzzles | `.../systems/KeyCollectionSystem.java`, `LeverSystem.java`, `ChestSystem.java` |
| Encounter and boss lifecycle | `.../screens/GameScreen.java` |
| Boss/player HUD | `.../ui/GameHud.java` |

## 3. Input (“key”) logic

The project routes gameplay actions through `GamePreferences.Action` instead of
scattering raw key codes through ordinary movement/combat systems. This supports
rebinding and gives menus one source of truth for displaying control names.

Useful rules:

- Use edge-triggered input (`just pressed`) for one-shot actions such as attacks,
  dash, interact, and opening a chest.
- Use held input for continuous actions such as movement and blocking.
- Clear movement velocity at the start of each update, then reconstruct it from
  current input. This prevents a released key from leaving the character moving.
- Normalize diagonal movement so it is not faster than horizontal movement.
- Reject gameplay input while the player is dead, stunned, control-locked, or a
  UI element owns the pointer/input.
- When an overlay closes, consume one gameplay frame so the same key/click does
  not also trigger an attack behind it.
- During a dash, the dash system overrides ordinary movement and grants
  invulnerability for exactly the active dash duration.
- Blocking begins with a short perfect-parry window. Continued holding becomes
  ordinary damage reduction.

### Adaptation for The Infected Hour

Our game already has `GameBridge`, a launcher, and network messages, so keyboard
state must remain client-side but gameplay results must be host-authoritative:

1. Client maps a physical key to a semantic action.
2. Client sends an action request with a sequence number/tick.
3. Host validates player state, cooldown, range, and target.
4. Host mutates the world once.
5. Clients render the result from events/snapshots.

Do not let both clients independently decide that a boss was hit, an injection
station was completed, or the final cooperative action succeeded.

## 4. Collectible key, lever, and chest logic

The friend project also contains literal dungeon-key logic. Its strongest idea is
the separation of a temporary world entity from durable room progress.

### Flow

1. One elite enemy receives a `KeyCarrierComponent`.
2. When it dies, the room is marked `keyDropped` and a key entity is spawned at
   the enemy position.
3. `KeyCollectionSystem` automatically collects the key when the player is close.
4. The room is marked `keyCollected`, and the temporary key entity is removed.
5. The elite chest checks the durable `keyCollected` state before opening.
6. Chest opening is one-shot, creates one reward, and marks the room `opened`.

There is a recovery path: if room state says the key was dropped but no live key
entity exists, the game respawns it at a known loot position. This prevents a
lost key from blocking a run after room re-entry or incomplete cleanup.

### Puzzle interaction rules

- Lever and chest actions are proximity-checked.
- Interaction uses a one-shot press, not a held key.
- Each object has an activated/opened latch, preventing duplicate rewards.
- The unlock requirement varies by room type: clear the room, activate a lever,
  or collect the elite key.

### Adaptation for our Level 2 village

Our existing `CampaignLevelPlan.Feature.actionId` plus the server’s
`completedObjectiveActions` set already implements the same durable/idempotent
idea for survivors and power relays. Keep that model and extend it rather than
introducing client-owned key entities.

Recommended rule for any future physical key:

- stable key ID: `l2_clinic_service_key`;
- host-owned state: `LOCKED -> DROPPED -> COLLECTED -> CONSUMED`;
- spawn position and carrier ID in the authoritative state;
- duplicate collect/use requests are harmless;
- if the visual entity disappears, the state can recreate it;
- save/checkpoint stores the state, never only the sprite/entity reference.

## 5. Normal combat logic

### Player attack lifecycle

Each melee attack has explicit active time, cooldown time, combo step, and a
monotonically increasing attack ID.

The three-hit combo increases damage, reach, width, active duration, recovery,
and final-hit knockback. If the combo reset timer expires, the next attack starts
at step one again.

The damage system records the last attack ID that damaged each enemy. Therefore,
an attack that remains active across several frames still damages a target only
once. This is much safer than using animation-frame overlap alone.

The melee hit area is a directional wedge:

- a forward dot product checks that the enemy is in front;
- forward distance is limited by reach;
- sideways distance is limited by attack width;
- larger enemies and the boss add their collision radius.

### Enemy attack lifecycle

Ordinary melee enemies use a small finite state machine:

`IDLE -> CHASE -> ATTACK/WINDUP -> RECOVER -> CHASE`

`STUNNED` and `DEAD` are interrupt states. An attack is only resolved once, and
range is checked again after the windup, so the player can dodge a telegraphed
strike instead of being hit because they were close when the animation began.

### Defence and status effects

- Invulnerability frames stop repeated contact/projectile damage.
- Blocking reduces damage only when facing the attacker/projectile.
- A correctly timed parry cancels a hit and stuns or disrupts the attacker.
- Projectiles include a team/faction, lifetime, radius, velocity, and damage.
- Fire, poison, slow, and stun are timed status components rather than special
  cases embedded in every enemy.
- Death disables the enemy body/state before reward processing.

These are the foundations needed before a spatial boss fight will feel fair.

## 6. Boss fight: complete state model

The boss has 400 maximum health and four health bands:

| Health | Phase | Main lesson |
|---:|---|---|
| 100–75% | Iron Fist | Learn a locked-position ground slam |
| 75–50% | Burning Gauntlets | Learn a locked-direction charge |
| 50–25% | Devil’s Crown | Learn a radial projectile pattern with a safe gap |
| 25–0% | Irhos Revealed | Faster cycle combining all three learned attacks |

The boss component holds phase, attack sub-state, timers, locked targets,
telegraph values, movement tuning, damage, and one-hit latches. The phase system
owns health thresholds and transformation rules. The combat system owns attack
selection and execution. Keeping those responsibilities separate is one of the
best ideas in the project.

### Phase transition safety

On every threshold crossing, the phase system:

1. clamps health to the exact threshold so a large hit cannot skip a phase;
2. starts a 1.5-second transformation;
3. grants matching invulnerability;
4. stuns/stops the boss;
5. clears the previous attack cycle; and
6. applies the next phase’s speed, windup, recovery, and damage tuning.

This creates a clean transition and prevents attacks from one phase leaking into
the next phase.

### Attack A — ground slam

1. Boss pursues until the player is within the trigger range and cooldown is ready.
2. It stores the player’s current position as the slam target.
3. A ground circle grows/intensifies during windup.
4. At resolution, damage is tested against the stored circle—not the boss and not
   the player’s old position.
5. The player is safe if they moved outside the circle before resolution.
6. Boss waits through recovery before pursuing again.

This is a fair area attack because the danger is readable and spatially stable.

### Attack B — burning charge

1. Boss stores a normalized direction toward the player once.
2. A line of warning markers shows the future route.
3. After windup, the boss dashes along that locked direction.
4. A one-hit latch prevents repeated damage while bodies overlap.
5. A successful hit can add burning; blocking and invulnerability still apply.
6. The boss stops during recovery, creating a punish window.

Locking the direction is important. A charge that continuously homes during its
active frames would invalidate the telegraph.

### Attack C — projectile crown

1. Boss displays the radial pattern during windup.
2. Twelve projectile directions are considered.
3. Two adjacent directions are omitted, forming a visible escape corridor.
4. After each volley, the pattern rotates and the safe corridor moves.
5. Projectiles have fixed speed, damage, radius, and lifetime.

The explicit safe gap makes the pattern challenging without becoming unavoidable.
Moving the gap prevents the player from camping in one position indefinitely.

### Final phase

The final phase does not introduce an unrelated fourth mechanic. It cycles through
the three attacks the player has already learned, with shorter windups, shorter
recovery, larger/faster attacks, and higher damage.

This follows a strong boss-design principle: teach separately, then test mastery by
combining. Its attack order is deterministic, which also makes debugging and
network synchronization easier.

### Encounter lifecycle

Before the fight, the game removes previous room enemies, projectiles, loot, keys,
merchant/chest/lever state, moves the player to the boss spawn, resets transient
combat state, heals according to restart/first-entry rules, and grants short spawn
invulnerability. It then creates one boss through the ordinary enemy factory.

When the boss enters `DEAD`, player controls are locked, remaining projectiles are
removed, and the victory overlay becomes the only active flow. A boss checkpoint
allows a clean retry without replaying the whole run.

The HUD shows the phase name, transformation status, and one continuous health
bar. The arena renderer shows attack telegraphs in the world, which is more useful
than relying on HUD text alone.

## 7. What to adopt and what to avoid

### Adopt

- Explicit finite states for phase and attack sub-state.
- Telegraph, active, and recovery timing for every boss attack.
- Locked target/direction for dodgeable attacks.
- One-hit IDs/latches and invulnerability frames.
- Health clamps and temporary invulnerability at phase boundaries.
- A safe lane in projectile patterns.
- Final phase recombination of already learned mechanics.
- Cleanup/reset methods for encounter start, retry, and victory.
- Stable progress IDs and idempotent interactions.
- Debug shortcuts to jump to the boss and force individual phases.
- Small pure-logic tests for state machines and timing boundaries.

### Avoid or improve

- Do not copy the 2,500+ line `GameScreen`; separate encounter simulation,
  rendering, UI, and campaign transitions.
- Do not read raw keyboard input inside authoritative boss logic.
- Do not hard-code every tuning value as a public component field. Put balance
  values in a boss definition/config object.
- Do not target a single stored player. A co-op boss needs an explicit target
  policy and must handle disconnected/downed players.
- Do not make the renderer decide damage or phase completion.
- Do not rely only on manual play testing; the reference repository currently has
  no automated tests.
- Do not reuse its characters, phase names, text, sprites, or assets without
  permission/licensing.

## 8. Current gap in The Infected Hour

Our project already has a testable `BossPhaseSystem` and a playable
`BossScreen`, but the screen is presently a presentation prototype:

- pressing E increments all three injections regardless of player position;
- pressing Space directly subtracts boss health;
- the final E hold passes `true, true`, so it simulates both players locally;
- the boss has no position, movement, attacks, hitboxes, projectiles, or target AI;
- boss state is not yet owned and broadcast by `GameServer`;
- player HP/downed/revive and contamination do not yet affect the encounter;
- victory is decided locally before moving to the ending story panel.

The useful existing foundation is:

- `BossPhaseSystem` already describes our thematic three-phase fight;
- it is graphics-independent and has unit tests;
- `GameServer` already validates Level 2 feature proximity and makes objective
  progress idempotent;
- reliable events and snapshots already exist;
- Level 3 briefing, transition, hidden-lab checkpoint, and ending flow exist.

## 9. Proposed Virus Heart fight

This keeps our PRD identity while applying the strongest lessons from the friend
project.

### Phase 1 — Containment Shield

Goal: place cure samples at three distinct injection stations.

- The Virus Heart uses a telegraphed contamination slam.
- The boss periodically selects a living player and locks a ground target.
- Elric carries/charges samples; Jane protects and creates safe interaction time.
- Each station requires proximity and a short interruptible hold.
- The host validates station ID, role, distance, alive state, and one-shot status.
- Completing all three stations starts a transformation, clears active hazards,
  and opens the damage phase.

Solo fallback: the same player can complete all stations. Co-op version can reduce
hold time or require the other player to clear nearby infected growths, but must
never become impossible if one player is downed.

### Phase 2 — Exposed Core

Goal: damage the exposed core before the 15-second window ends.

- Normal combat attacks—not repeated UI key presses—produce damage requests.
- Boss alternates a locked-direction tendril charge and a radial spore volley.
- The volley always exposes a readable safe corridor.
- If HP reaches 25%, transition to final phase.
- If time expires, the shield returns. The next shield cycle should be shorter:
  for example, only two stations reactivate or prior stations require a brief
  cleanse rather than three full interactions.
- Health never regenerates when the shield returns.

### Phase 3 — Core Destruction

Goal: complete a cooperative finisher while surviving the combined attack cycle.

- The boss cycles slam, charge, and spore volley more quickly.
- Elric must hold the cure charge at one console.
- Jane must strike the exposed conduit within a synchronization window.
- Host records two readiness timestamps and succeeds when both are valid within
  the configured window (recommended starting value: 1.25 seconds).
- A partial failure produces clear red feedback and preserves progress; rising
  contamination is enough pressure, so an arbitrary damage punishment is not
  necessary.
- In solo mode, use a two-step sequence with a generous travel window rather than
  pretending that two players were ready.

### Suggested boss attack state

Keep this data in core/shared logic, with rendering reading but never owning it:

| State | Important data |
|---|---|
| Phase | shield, exposed, core destruction, defeated |
| Attack | pursuit, slam windup/active/recovery, charge windup/active/recovery, volley windup/recovery |
| Timers | phase transition, exposure, attack windup, active, recovery, cooldown |
| Target | target player ID, locked X/Y, locked direction X/Y |
| Combat | HP/max HP, attack sequence, hit players for current attack, invulnerable-until tick |
| Puzzle | station completion set, Elric ready tick, Jane ready tick |
| Network | encounter ID, revision/tick, last accepted action sequence per player |

## 10. LAN-authoritative design

The friend project is effectively single-player. Our boss cannot be integrated by
copying its direct entity/player references.

### Host owns

- boss phase, HP, position, velocity, target selection, and attack state;
- all timers and deterministic attack sequence;
- injection station and cooperative-finisher validation;
- projectile/hazard spawning and collisions;
- player damage, downed state, revive, contamination, and victory;
- cleanup and transition to the ending.

### Client sends

- movement/input sequence;
- attack start with local sequence number;
- interact start/cancel/complete request with stable target ID;
- final-action readiness request.

### Continuous snapshot data (UDP)

- encounter revision and host tick;
- phase and phase timer;
- boss transform, HP, and current attack state;
- locked telegraph target/direction and timing;
- active hazard/projectile snapshots;
- station states and player combat states.

### Reliable events (TCP)

- encounter started/restarted;
- phase changed;
- station completed;
- final action partial failure/success;
- boss defeated and ending transition.

Telegraphs should be derived from host state plus timestamps. Do not broadcast a
new cosmetic event every frame. If packets arrive late, the client can calculate
current telegraph progress from the host tick.

### Co-op target policy

Use a deterministic policy, for example:

1. keep current target while they are alive/in range;
2. otherwise choose the nearest living player;
3. alternate after a completed attack when both are similarly close;
4. never target a disconnected or downed player;
5. include the chosen player ID in snapshots.

This avoids target flicker and makes both clients render the same attack.

## 11. Implementation plan

Use small, reviewable steps. The friend project’s history also implemented one
boss attack/phase per commit, which made the evolution easier to inspect.

1. **Authoritative data contract**
   - Add boss state/action DTOs and network registration.
   - Add unit tests for serialization and stale/duplicate actions.
2. **Server encounter lifecycle**
   - Start, reset, checkpoint, cleanup, defeat, and ending transition.
   - Move `BossPhaseSystem` ownership to the host simulation.
3. **Spatial player combat**
   - Attack IDs, active/recovery timers, directional hit test, invulnerability.
4. **Phase 1 and slam**
   - Three host-validated stations and a locked-position telegraphed slam.
5. **Phase 2 and charge**
   - Exposure timer/damage gate and locked-direction charge with one-hit latch.
6. **Projectile volley**
   - Host-owned radial spores with rotating safe corridor.
7. **Phase 3 cooperative finisher**
   - Role/timestamp validation, solo fallback, partial-fail feedback.
8. **Rendering and HUD**
   - Boss sprite/animation, world telegraphs, phase label, HP, exposure timer,
     station indicators, and partner-readiness state.
9. **Failure/retry and polish**
   - Player death, revive, contamination fail, boss checkpoint, audio, screenshake,
     particles, accessibility colours, and cleanup.

### Likely files in our repository

| Existing/new area | Work |
|---|---|
| `core/.../systems/BossPhaseSystem.java` | Expand pure phase rules and expose immutable state/results |
| `core/.../screens/BossScreen.java` | Render snapshots/telegraphs; remove local authority |
| `core/.../net/GameServer.java` | Own encounter tick, validation, damage, actions, and victory |
| `core/.../net/GameClient.java` | Send semantic boss actions and consume authoritative state |
| `shared/.../network/WorldSnapshot.java` | Add continuous boss snapshot or nested boss state |
| `shared/.../network/EventMessage.java` | Use stable event types/payloads for phase and finisher events |
| `core/.../entities/Enemy.java` | Reuse common movement/HP fields; avoid a separate duplicated combat model |
| `core/src/test/.../BossPhaseSystemTest.java` | Add thresholds, clamps, timeouts, invalid calls, and reset tests |
| New `BossCombatSystem` | Pure/tick-driven boss attack state machine |
| New boss action tests | Timing, locked targets, hit latches, safe gaps, duplicate packets, co-op readiness |

## 12. Minimum test matrix

### Phase tests

- initial state is shield with full HP;
- shield cannot weaken twice from duplicate station messages;
- exposure lasts exactly the configured duration;
- damage cannot skip required phase transitions;
- expired exposure returns to shield without healing;
- HP threshold enters core destruction;
- defeated state cannot transition backward;
- restart clears every attack/puzzle/transient timer.

### Attack tests

- slam uses locked coordinates and misses a player who escaped;
- charge direction does not home after windup;
- charge hits each player at most once;
- projectile volley always has the configured safe lanes;
- safe lane rotates deterministically;
- phase transition cancels old attacks/projectiles;
- block/parry/dash invulnerability works at boundary timestamps.

### Network/co-op tests

- only host mutates boss HP and phase;
- duplicate/stale attack and interaction sequences are ignored;
- out-of-range injection is rejected;
- disconnected/downed target is replaced;
- readiness outside the final window fails safely;
- readiness within the window succeeds once;
- solo fallback is completable;
- late join receives a complete current boss state;
- restart produces a new encounter ID so old packets cannot affect it.

### Manual play checks

- every damaging attack is readable before it becomes active;
- a valid escape route always exists;
- phase changes visibly clear old hazards;
- both players see the same target, timer, HP, and station states;
- UI remains visible during transitions and the application window never vanishes;
- game over, retry, main menu, and victory leave no stale server/port state.

## 13. Recommended first implementation slice

Before adding sprites or all three attacks, make one complete vertical slice:

1. host starts the boss encounter;
2. clients receive boss phase/HP/position;
3. one injection station validates by distance and completes once;
4. one locked-position slam telegraphs and damages through host authority;
5. defeat/retry fully resets the station, attack, player, and boss state;
6. tests cover the same flow without graphics.

Once this slice is stable in both solo and two-player LAN, duplicate the pattern for
the other stations and attacks. This reduces the chance of building a visually
complete boss on top of an unreliable network state model.

