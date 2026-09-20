# Changelog — The Infected Hour
### Started: September 16, 2026
### Author: Faiyaz & Antigravity

This file maintains an ongoing log of all architectural, performance, and gameplay changes made to The Infected Hour.

---

## [2026-09-16] — Memory Optimization & Performance Tuning
### 1. Root Cause Analysis of Memory & Load Issues
* **Default JVM Heap Sizing:** Java 21 (64-bit) automatically assigns up to 25% of system physical RAM per JVM process if unbounded. Running Backend + Game + Launcher simultaneously caused total memory usage to climb towards 8GB.
* **Cold Gradle Startup:** Subproject evaluation and compilation overhead on every `run` execution added 15-30s delay.
* **Synchronous Texture Loading:** Large textures (4.5MB `map.png`, 2.3MB `bg_menu.png`) were uncompressing into ~64MB raw VRAM/RAM on the main render thread inside `show()`.

### 2. Changes Applied:
* **JVM Heap Caps (`-Xmx512m`):**
  * Added `tasks.withType<JavaExec>` in `build.gradle.kts` setting `maxHeapSize = "512m"` and `jvmArgs("-Xms128m", "-Xmx512m", "-XX:+UseG1GC")`.
  * Added Spring Boot `bootRun` heap configuration in `backend/build.gradle.kts` capping heap to 512MB.
  * Reduced memory footprint per process from ~4-8GB ceiling down to ~300MB-512MB (saving up to 85% RAM across processes).
* **Build Acceleration:**
  * Enabled `org.gradle.parallel=true` and `org.gradle.caching=true` in `gradle.properties` to drastically cut cold startup times and prevent redundant subproject recompilations.

---

## [2026-09-16] — Part 1: Home Page Correction Plans & COLLECTION Screen
### 1. New COLLECTION / Operative Dossiers System
* **Native LibGDX Game Menu (`MainMenuScreen.java`):**
  * Added `MenuState.COLLECTION` state and a dedicated `3. COLLECTION (OPERATIVE DOSSIERS)` button to the home screen.
  * Number keys `[1-6]` updated for streamlined keyboard navigation.
  * Implemented `renderCollection(...)` and `drawCollectionDossier(...)` with a high-tech dark sci-fi dossier layout, custom division separators, interactive deployment buttons, and quick ESC return.
* **JavaFX Launcher (`fx-launcher`):**
  * Added `COLLECTION / DOSSIERS` block to `menuRail` in `MainMenu.fxml`.
  * Connected action in `MainMenuController.java`.
  * Created `CollectionView.java` with responsive operative dossier cards, personnel backgrounds, tactical perk breakdowns, and direct deployment buttons.

### 2. Operative Profiles & Character Outfits Updated
* **Elric — Field Striker:**
  * **Role:** Melee Combat & Containment
  * **Affiliation:** Oscorp Private Security Operative
  * **Bio:** *"A resilient survivor who thrives under pressure. Built for close-quarters clearing and holding choke points against infected swarms."*
  * **Perks & Stats:**
    * Combat Focus: +20% Melee attack speed and knockback
    * Hazard Resistance: 30% slower contamination meter buildup in toxic zones
    * Melee Power: 50 Damage per hit
    * Healing Surge: 35 HP restore
  * **Portrait:** `male_character_select.jpg` (matching in-game blue hoodie appearance)
* **Jane — Tactical Scout:**
  * **Role:** High Agility & Objective Runner
  * **Affiliation:** Undercover Government Agent
  * **Bio:** *"Analytical and razor-sharp. Uses superior agility and tactical blade strikes to secure quarantine zones and rescue survivors before time runs out."*
  * **Perks & Stats:**
    * Agility Focus: +15% base movement speed and fast dash recovery
    * Field Expertise: 35% faster sanitation node & objective interactions
    * Tactical Katana: 30 Damage per strike + knockback
    * Field Chemist: Produces 1 Medkit every 60s (50 HP heal capacity)
  * **Portrait:** Updated from old crop-top artwork to `female/female_character_select_v3.jpg` (matching tactical long-sleeve, side blade, and glasses outfit).

## [2026-09-16] — Part 2: Level 1 Hospital Expansion & Tactical Mechanics
### 1. Senseless Jane & Revive Sequence
* **Location:** Pharmacy chamber at tile `(42.0, 5.5)` (Pink marker).
* **State:** Starts locked and senseless with a dormant tint. Cannot act until revived.
* **Mixed Herb Revive Kit Crafting:**
  * Herb Part 1 placed at `(3.5, 6.5)` (Storage Ward).
  * Extra Floor Medkit placed at `(4.5, 6.5)`.
  * Herb Part 2 placed at `(38.0, 14.0)` (Central East Hallway).
  * Gathering both parts constructs the `Mixed Herb Revive Kit` in inventory.
* **Awakening Perks & Rewards:**
  * Reviving Jane immediately awards **+3 Coins**.
  * Jane awakens equipped with a Tactical Katana (30 Damage per strike + knockback).
  * Synthesizes 1 Medkit every 60 seconds (50 HP healing capacity).
  * Follows Elric and autonomously attacks nearby infected.

### 2. Villager Protection & Escort System
* **Locations:**
  * Villager 1: `(18.5, 28.5)` (North-West Ward).
  * Villager 2: `(35.5, 28.5)` (North-East Ward).
* **Mechanics:**
  * Interacting prompts them to follow the player/Jane.
  * Rescuing Villager 1 awakens **2 Sleeping Hallway Zombies** at `(19.0, 6.0)` and `(19.0, 9.5)`.
  * Active zombies prioritize attacking and biting escorted villagers.
  * Villagers display a real-time health bar above their heads.
* **First-Aid System (`[H]` key):**
  * Approaching an injured villager and pressing `[H]` consumes a medkit to heal them:
    * **Jane:** Restores **+50 HP**.
    * **Elric:** Restores **+35 HP**.
  * If no villager is injured within range, the player heals themselves.

### 3. Key Relocation & Two-Phase Climax Sequence
* **Staff Room Key:** Relocated to Blue Dot at `(24.5, 26.5)`.
* **Corridor Ambush Swarm:**
  * Picking up the key spawns active corridor zombies at Gold Marker coordinates:
    * `(41.0, 21.0)`, `(43.0, 21.0)`, `(35.5, 19.5)`, `(38.0, 13.0)`, `(30.5, 13.5)`.
  * New objective triggers: eliminate all swarm zombies.
* **Mutated Boss Zombie:**
  * Clearing the swarm unlocks the Red Objective marker at `(40.5, 25.5)`.
  * Spawns a scaled (1.45x) Mutated Boss Zombie with crimson bio-aura and 220 HP.
  * Slaying the boss awards **+5 Coins** and unlocks the evacuation staircase at `(42.0, 30.5)`.

### 4. Economy & Objective HUD
* **Coin Economy:**
  * +1 Coin per zombie killed.
  * +3 Coins for reviving Senseless Jane.
  * +5 Coins per surviving villager at level clear (+10 Coins if both survive).
  * Persistent `COINS: X` counter displayed on the top-left HUD.
* **Tactical Objective Overlay (`[O]` key):**
  * Toggles an in-game HUD checklist displaying real-time progress:
    * Herb parts collected (X/2)
    * Senseless Jane revive status
    * Villagers rescued and escorted (X/2)
    * Staff Room Key acquisition
    * Corridor swarm extermination
    * Mutated Boss elimination
* **Minimap Blips:**
  * Pink: Senseless Jane
  * Yellow: Herb parts and sleeping zombies
  * Blue: Rescuable villagers
  * Cyan: Staff Room Key
  * Gold: Ambush swarm zombies
  * Red: Mutated Boss Zombie

---

## [2026-09-16] — Part 3: Story Narrative, Moral Choice & Jane Boss Duel
### 1. Narrative & Lore Re-alignment (`StoryPanelScreen.java`)
* **Oscorp Bio-Weapon Lore:**
  * Elric established as an elite bio-security operative for the private Oscorp Organization.
  * A weaponized bio-pathogen was stolen from Oscorp's high-containment laboratories by a rogue employee.
  * Containment directive: eradicate the virus and destroy the Virus Heart or the entire population transforms.
  * Jane revealed as an undercover government intelligence agent sent to investigate Oscorp's black-budget bio-warfare testing.
* **Level 2 & 3 Narrative Bridges:**
  * Level 2: Rescue another wounded operative and 3 villagers, repairing the power grid to unseal the subterranean drainage tunnel.
  * Level 3: Infiltrate the covert underground laboratory beneath Ashgrove to destroy the Virus Heart.

### 2. Climax Ending & Elena Vance Moral Dilemma
* **The Missing Researcher:**
  * Slaying the final boss yields a high-security keycard and the sole remaining Prototype Antidote vial.
  * Elric unlocks Quarantine Cell Zero to find Elena Vance — his closest friend and senior researcher who vanished 2 months ago after investigating the outbreak. Elena is infected and slipping into cellular necrosis.
* **The Two-Path Moral Choice:**
  * **[1] SAVE ELENA (Friendship):**
    * Elric administers the antidote to Elena.
    * Her fever subsides and mutations cease; she whispers her gratitude and passes away peacefully in Elric's arms.
    * No combat takes place. Operation concludes in peaceful victory.
  * **[2] SACRIFICE ELENA / SECURE FOR OSCORP (Humanity):**
    * Elric secures the antidote for corporate transport.
    * Agent Jane draws her tactical blade, reveals her federal badge, and challenges Elric to stop Oscorp from reclaiming the bioweapon.

### 3. Interactive Boss Duel (`JaneDuelScreen.java`)
* **Combat Specifications:**
  * **Jane (Boss):** 300 HP, deals **60 Damage per hit**.
  * **Elric (Player):** 200 HP, deals **50 Damage per hit**.
* **Parry & Dodge Mechanics:**
  * **Parry (`[F]` / `[Q]`):** 0.38s active window. Timed against Jane's strike telegraph to block 100% damage and stun Jane for 1.3s with bonus counter-attack damage.
  * **Dodge Roll (`[SHIFT]` / `[X]`):** 0.4s evasive roll providing full invulnerability frames.
  * **Jane AI:** High agility tracking, windup strike telegraph with red warning flash, recovery backward step, and stunned reaction.
  * **Duel Outcomes:**
    * Victory: Subdues Jane, secures antidote for Oscorp, concludes operation.
    * Defeat: Player downed, option to press `[R]` to retry the duel.

---

## [2026-09-16] — Infinite Time Tuning & Tactical Briefing Screen Overhaul
### 1. Root Cause Analysis: "Why Showing I'm Dead"
* **Countdown Clock Expiration:** The simulation tracked `currentImmunityTime` initialized to 120s which counted down each frame. When `currentImmunityTime <= 0f`, line 925 in `GameScreen.java` forcibly executed the death condition `if (me.downed || me.hp <= 0f || currentImmunityTime <= 0f)` and locked the game with `"You are dead"`.
* **Cross-Level Damage Persistence:** When a player entered a level after taking damage or dying, `GameServer.java` did not reset player vitals or downed state in `configureLevel()`.

### 2. Infinite Time & Health Initialization Fixes Applied
* **Infinite Immunity Timer:**
  * Updated `maxImmunityTime = 999999f` and `currentImmunityTime = 999999f` in `GameScreen.java`.
  * Removed `|| currentImmunityTime <= 0f` check from the death condition on line 925.
  * Reset `currentImmunityTime = 999999f` on `show()`.
  * HUD updated: Circular immunity ring stays 100% full and renders `TIME: INFINITE` in cyan.
* **Level Start Player Health Guarantee:**
  * In `GameServer.java` (`configureLevel()`), added explicit revival and healing: `connected.entity.revive()`, `connected.entity.heal(100f)`, and `connected.entity.cleanseContamination()`.
  * In `GameScreen.java` (`show()`), dispatched `PLAYER_HEAL 100` event so local and remote players begin each level in full health.

### 3. LevelBriefingScreen Complete UI Redesign
* **Cinematic Sci-Fi Atmosphere:** Replaced the minimal blank void `#0E1420` with `story_intro.png` under a translucent dark vignette.
* **Unicode Box Glyph Fix:** Eliminated missing character boxes `[]` by converting non-ASCII characters (`—`, `•`, `…`) to clean ASCII equivalents (`-`, `>`, `...`).
* **High-Tech Two-Column Layout:**
  * **Left Column (Mission Command):** Header banner `OPERATION ASHGROVE // LEVEL 01 - HOSPITAL CONTAINMENT`, directives cards with badges (`[01] SYNTHESIS`, `[02] RESCUE`, `[03] EXTERMINATION`), and complete tactical field controls guide.
  * **Right Column (Operative Dossier & Ready Gate):** Real-time operative portrait (`male_character_select.jpg` for Elric / `female_character_select_v3.jpg` for Jane), role, affiliation, equipment details, and glowing pulsing Ready/Deploy button `[ PRESS E OR ENTER TO DEPLOY ]`.
  * **Bottom Bar:** Encrypted military uplink terminal status bar.

---

## [2026-09-16] — Debug Co-Op Bypass & Level Briefing Ready Gate Unblock
### 1. Root Cause Analysis: "Stuck in this Screen" & "Debug Button Co-Op Don't Load This"
* **Briefing Gate Hang:**
  * When entering `LevelBriefingScreen`, pressing `[E]` or clicking Deploy set `localReady = true`.
  * The advance condition required `partnerGateOpen = partnerReadyReceived || client.getMatchMode() == MatchMode.SOLO`.
  * When launching via launcher or hosting, `client.getMatchMode()` defaulted to `COOP` or had dummy P2 registered on server, while `partnerReadyReceived` never arrived over the network because dummy P2 doesn't transmit network packets.
  * As a result, the screen was permanently stuck displaying:
    `>> [ READY FOR INSERTION ] <<` and `Waiting for squad partner...`.
* **Debug Co-Op Flow:**
  * The "Debug Co-Op" button in `MainMenuController.java` (`onDebugCoop()`) launches local split-screen co-op on a single machine with Elric (P1) and Jane (P2).
  * Loading the tactical briefing screen for debug testing was unnecessary and blocked immediate testing of the 2-player local split-screen controls.

### 2. Solutions Implemented
* **Debug Co-Op Direct Boot (`InfectedHourGame.java`):**
  * Added conditional bypass in `InfectedHourGame.create()`:
    ```java
    if (session.debugSplitScreen()) {
        setScreen(new GameScreen(this, client, bridge, session.startingLevel()));
    } else if (bridge.hasLauncher() || session.isLoadingSave()) {
        setScreen(new LevelBriefingScreen(this, client, bridge, session.startingLevel()));
    }
    ```
  * Pressing the "DEBUG CO-OP" button now bypasses `LevelBriefingScreen` completely and boots directly into `GameScreen` with local split-screen active.

* **LevelBriefingScreen Ready Gate Overhaul (`LevelBriefingScreen.java`):**
  * **Single-Player Auto-Detection:**
    ```java
    boolean isSinglePlayer = (client.getMatchMode() == MatchMode.SOLO)
            || (game.getSession() != null && game.getSession().debugSplitScreen())
            || (game.getServer() != null && game.getServer().getConnectedPlayerCount() <= 1);
    ```
    If playing solo or testing locally, clicking Deploy or pressing `[E]` instantly advances into the mission without waiting.
  * **Force-Deploy Override:**
    If already ready, pressing `[E]`, `[ENTER]`, `[SPACE]`, or clicking the Deploy button again acts as an immediate Force Deploy.
  * **Auto-Advance Timeout:**
    Added `AUTO_ADVANCE_SECONDS = 2.0f`. Once `localReady` is true, the screen auto-advances after 2 seconds even if a network partner's packet was delayed.
  * **Navigation & Return:**
    Pressing `[ESC]` or `[Q]` gracefully requests return to the launcher window.

* **Story Panel Partner Safety (`StoryPanelScreen.java`):**
  * Extended `partnerOk` check to include `isSinglePlayer` and `debugSplitScreen` guarantees, allowing seamless panel transitions with `[E]`, `[SPACE]`, `[ENTER]`, or Left Mouse Click.

