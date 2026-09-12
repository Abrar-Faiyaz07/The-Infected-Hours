# The Infected Hour — Backend Schema & API (Spring Boot)

**Version:** 2.1 (6-day sprint) | Spring Boot 3.x · Spring Data JPA · Spring Security (JWT) · MySQL

---

## 1. Overview

Single Spring Boot service, run on the host laptop for demos (`http://<host-ip>:8080`). Stores **player profiles & saves, leaderboards/stats, and match history**. Both laptops call the same instance so saves land on both machines via GET-after-match sync.

Database: **MySQL** (teacher requirement). Connection via `jdbc:mysql://localhost:3306/infected_hour`. Schema managed by `ddl-auto=update` — no manual migrations needed. Integration tests use H2 in-memory (MySQL mode) so tests don't require a running MySQL instance.

## 2. Entity-Relationship Diagram

```
Player 1───* MatchParticipant *───1 Match
  │ 1                                 │ 1
  │                                   └───* LevelResult
  ├───1 SaveState
  └───* LeaderboardEntry
```

## 3. Tables / JPA Entities

### player
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| username | VARCHAR(24) UNIQUE NOT NULL | login name |
| display_name | VARCHAR(32) NOT NULL | shown in lobby/leaderboards |
| password_hash | VARCHAR(100) NOT NULL | BCrypt |
| created_at | TIMESTAMP | |
| last_login_at | TIMESTAMP | |

### save_state  (one per player)
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| player_id | UUID FK → player UNIQUE | |
| highest_level_unlocked | INT DEFAULT 1 | 1–3 |
| story_progress | INT DEFAULT 0 | last panel sequence seen (0–5) |
| total_playtime_sec | BIGINT DEFAULT 0 | |
| settings_json | TEXT | volumes, fullscreen (optional cloud settings) |
| updated_at | TIMESTAMP | conflict resolution: latest wins |

### save_slot  (nine per player — manual saves, Resident Evil style)
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| player_id | UUID FK → player | |
| slot_number | INT | 1–9; UNIQUE together with `player_id` |
| level_number | INT | which level the save is in |
| level_name | VARCHAR(64) | shown on the slot card |
| checkpoint_id | VARCHAR(64) | where in the level to resume |
| checkpoint_name | VARCHAR(96) | human label for the card |
| story_progress | INT | |
| playtime_sec | BIGINT | shown as "1h 04m" on the card |
| character_type | VARCHAR(16) | ELRIC or JANE |
| player_hp | FLOAT | carried resources ↓ |
| personal_contamination_pct | FLOAT | |
| global_contamination_pct | FLOAT | |
| inventory_json | TEXT | carried items |
| objectives_json | TEXT | objective progress at save time |
| saved_at | TIMESTAMP | |

**Why this is separate from `save_state`.** `save_state` is permanent account
progression and only moves forward; a `save_slot` is a restorable point in time
that can be overwritten, deleted and reloaded. Merging them would make it
impossible to load an old slot without also rolling back unlocks the player has
legitimately earned — so loading a slot deliberately never touches `save_state`.

Inventory and objectives are JSON text rather than child tables: the data is only
ever read and written whole, and a relational breakdown would force a schema
migration every time an item type is added.

### match
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | created by host at session start |
| host_player_id | UUID FK → player | |
| mode | VARCHAR(8) | `SOLO` / `COOP` |
| started_at | TIMESTAMP | |
| ended_at | TIMESTAMP NULL | |
| result | VARCHAR(10) | `VICTORY` / `DEFEAT` / `ABORTED` |
| final_level_reached | INT | 1–3 |

### match_participant
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| match_id | UUID FK → match | |
| player_id | UUID FK → player | |
| character | VARCHAR(8) | `ELRIC` / `JANE` |
| damage_dealt | INT | |
| villagers_rescued | INT | |
| samples_collected | INT | |
| medicine_delivered | INT | |
| barricades_placed | INT | |
| sanitations_done | INT | |
| times_downed | INT | |
| revives_done | INT | |
| UNIQUE(match_id, player_id) | | |

### level_result
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| match_id | UUID FK → match | |
| level_number | INT | 1–3 |
| cleared | BOOLEAN | |
| clear_time_sec | INT NULL | |
| final_contamination_pct | INT | 0–100 global meter at end |
| retries | INT DEFAULT 0 | |
| UNIQUE(match_id, level_number) | | |

### leaderboard_entry  (denormalized bests, upserted on match complete)
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| player_id | UUID FK → player | |
| board | VARCHAR(24) | `FASTEST_L1..L3`, `LOWEST_CONTAM_L1..L3`, `BOSS_TIME` |
| value | INT | seconds or percent (lower = better for all boards) |
| match_id | UUID FK | provenance |
| achieved_at | TIMESTAMP | |
| UNIQUE(player_id, board) | keep only personal best | |

## 4. REST API

Base path `/api/v1`. All except `/auth/**` and `/health` require `Authorization: Bearer <JWT>`.

### Auth
| Method | Path | Body → Response |
|---|---|---|
| POST | `/auth/register` | `{username, displayName, password}` → `201 {playerId}` |
| POST | `/auth/login` | `{username, password}` → `{token, expiresIn, player}` |

### Players & Saves
| Method | Path | Purpose |
|---|---|---|
| GET | `/players/me` | profile + aggregate stats |
| GET | `/players/me/save` | current SaveState (used by both laptops to sync after match) |
| PUT | `/players/me/save` | upload save (offline-mode reconciliation); server keeps latest `updatedAt` |

### Manual save slots (Load Game screen)
| Method | Path | Purpose |
|---|---|---|
| GET | `/players/me/slots` | all 9 slots, empty ones included, in slot order |
| GET | `/players/me/slots/{n}` | one slot (1..9) |
| PUT | `/players/me/slots/{n}` | save / overwrite that slot |
| DELETE | `/players/me/slots/{n}` | clear that slot (no-op if already empty) |

The player id always comes from the JWT, never the request body — otherwise one
player could read or overwrite another's saves by sending a different id.

### Matches (host calls these)
| Method | Path | Purpose |
|---|---|---|
| POST | `/matches` | `{mode, participants:[{playerId, character}]}` → `{matchId}` at session start |
| POST | `/matches/{id}/level-result` | per-level result after each level (idempotent per level_number) |
| POST | `/matches/{id}/complete` | `{result, finalLevel, participants:[stats...], saves:[...]}` — writes match, participants, updates both SaveStates, upserts leaderboard bests, all in one transaction |
| GET | `/players/me/matches?limit=20` | match history for Profile screen |

### Leaderboards
| Method | Path | Purpose |
|---|---|---|
| GET | `/leaderboards/{board}?limit=20` | ranked entries `{rank, displayName, value, achievedAt}` |

### Misc
| Method | Path | Purpose |
|---|---|---|
| GET | `/health` | unauthenticated liveness check (lobby shows backend status) |

## 5. Key DTOs (in `shared/` module)

```java
record MatchCompleteRequest(
    String result,                 // VICTORY | DEFEAT | ABORTED
    int finalLevelReached,
    List<ParticipantStats> participants,
    List<SaveUpdate> saves) {}

record ParticipantStats(UUID playerId, String character,
    int damageDealt, int villagersRescued, int samplesCollected,
    int medicineDelivered, int barricadesPlaced, int sanitationsDone,
    int timesDowned, int revivesDone) {}

record SaveUpdate(UUID playerId, int highestLevelUnlocked,
    int storyProgress, long playtimeDeltaSec) {}
```

## 6. Security & Config

- BCrypt password hashing; JWT HS256, 12h expiry, secret from `application.yml` env override
- CORS: allow all origins on LAN (class-project scope)
- Validation: `@Valid` on all request bodies; username `^[a-zA-Z0-9_]{3,24}$`
- Server binds `0.0.0.0:8080` so the client laptop can reach it
- **Database:** MySQL, `jdbc:mysql://localhost:3306/infected_hour`. Credentials via env vars `DB_USERNAME` / `DB_PASSWORD` (defaults: `root`/`root` for dev). `ddl-auto=update`.
- Tests use H2 in-memory (MySQL compatibility mode) — no MySQL instance needed to run tests.

## 7. Business Rules

1. `POST /matches/{id}/complete` is the single source of truth: updates saves for **all participants**, so the client laptop only needs a follow-up `GET /players/me/save`.
2. Leaderboard upsert: replace entry only if new `value` < stored `value`.
3. `highest_level_unlocked` only increases (max of old/new).
4. A `SOLO` match still writes history and leaderboards (flagged by `mode` for optional separate boards later).
5. Aborted matches store participant stats but never touch leaderboards or level unlocks.

## 8. MySQL Setup (Demo Day)

On the host laptop, before running `./gradlew :backend:bootRun`:

```sql
CREATE DATABASE IF NOT EXISTS infected_hour;
```

Or just run the backend — `createDatabaseIfNotExist=true` in the JDBC URL handles it
automatically if the MySQL user has CREATE privileges.

Default credentials (dev): `root` / `root`. Override for demo:
```bash
export DB_USERNAME=your_user
export DB_PASSWORD=your_password
./gradlew :backend:bootRun
```
