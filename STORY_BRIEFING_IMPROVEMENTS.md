# STORY & BRIEFING SCREEN REDESIGN & ISSUE TRACKER

## 1. Root Cause of the Box ("Tofu" []) Characters
In `core/src/main/java/com/infectedhour/core/screens/StoryPanelScreen.java`:
- Line 290:
  `"Elric arrives in Ashgrove as an elite bio-containment operative for the private Oscorp Organization. A weaponized pathogen — engineered inside Oscorp's black-budget research laboratories — was stolen by a rogue insider and released into the civilian population."`
- The issue: The text contains Unicode em dashes (`—` / `\u2014`).
  LibGDX's built-in `new BitmapFont()` only contains standard ASCII glyphs (32–126).
  Missing Unicode characters render as the `.notdef` fallback glyph (empty square boxes).
- Other affected lines in StoryPanelScreen:
  - Line 295: `"...no ordinary virus — it is an engineered extinction weapon..."`
  - Line 304: `"...Elena Vance — his closest friend..."`
  - Line 305: `"[1] SAVE ELENA — Administer the antidote..."`
  - Line 305: `"[2] SECURE FOR OSCORP — Sacrifice Elena..."`
  - Line 230: `"Waiting for your partner…"` (unicode ellipsis `…`)

### Solution:
1. Replace all unicode em dashes (`—`) with double hyphens `--` or spaced hyphens ` - `.
2. Replace unicode ellipsis `…` with `...`.
3. Add a sanitizing method in `StoryPanelScreen.java` to auto-clean all future text strings.

---

## 2. Visual & Aesthetic Redesign Plan for StoryPanelScreen

### Current Issues:
1. **Hard 50/50 Screen Division**:
   - `StoryPanelScreen.java` line 161 draws a solid dark overlay rectangle across the left 54% of the screen.
   - This creates a harsh vertical split rather than an atmospheric cinematic scene.
2. **Plain Floating Text**:
   - The briefing text floats in an empty black rectangle with excessive negative space underneath.
3. **Missing "Oscorp Classified Dossier" Aesthetic**:
   - Oscorp is a black-budget military/biohazard corporation, but the briefing looks like plain debug text.
4. **Basic Footer**:
   - Plain `[E] continue (1/3)` and `01 / 03`.

### Visual & Architectural Enhancements:
1. **Background Blending**:
   - Allow `story_intro.png` to bleed across the screen with a smooth horizontal fade rather than a hard cut.
2. **Tactical Dossier Card**:
   - Wrap the story content inside a semi-transparent HUD panel (`rgba(10, 14, 22, 0.85)`).
   - Add biohazard amber (`#EAB308` / `Color(0.910f, 0.690f, 0.165f, 1f)`) corner brackets:
     `┌ ┐ └ ┘` to give it a tactical military interface appearance.
3. **Mission Metadata Headers**:
   - Category badge: `[ // OSCORP BIO-CONTAINMENT DIRECTIVE // ]`
   - Classification badge: `CLEARANCE: LEVEL-4 RESTRICTED`
   - Sub-header: `DIRECTIVE 04-ASHGROVE | STATUS: CRITICAL`
4. **Improved Typography & Hierarchy**:
   - Increase text leading / line spacing.
   - Draw an accent divider bar under the title.
5. **Interactive Key Prompt & Tactical Pagination**:
   - Draw a stylized keycap badge for `[ E ] CONTINUE`.
   - Draw a segmented progress bar: `[ ■ ■ □ ] PAGE 01 / 03`.

---

## 3. 🧟 Level 1 Hospital: Hallway Zombie Ambush Timing Fix

### Reported Problem:
In the hallway outside the X-Ray room, two zombies are standing there right from the beginning of Level 1 (visible dormant on map and as yellow radar dots).

### Desired Behavior:
- The hallway should be **empty** at the start of Level 1.
- When the player rescues the villagers (Ward villagers), the zombies should **instantly appear / ambush** in the hallway (identical to the key pickup swarm ambush).

### Code Changes (GameScreen.java):
1. **Lines 496–499 (Level 1 initialization)**:
   - Remove initial pre-spawning of `sleepingZombies`.
   - Keep `sleepingZombies.clear()`.
2. **Lines 1090–1107 (Villager Rescue Interaction)**:
   - When a villager is rescued and `!sleepingZombiesAwakened`:
     ```java
     sleepingZombiesAwakened = true;
     sleepingZombies.clear();
     SleepingZombie z1 = new SleepingZombie(19.0f, 6.0f);
     z1.sleeping = false;
     sleepingZombies.add(z1);

     SleepingZombie z2 = new SleepingZombie(19.0f, 9.5f);
     z2.sleeping = false;
     sleepingZombies.add(z2);

     showBanner("AMBUSH: Hallway infected emerged to cut off the escape route!");
     ```

---

## 4. 👥 Level 2 Continuity: Jane, Villagers, Grenade & Items Persistence

### Reported Problems:
When advancing to Level 2 (Floor 2 / Roadside):
1. **Jane is missing** completely.
2. **Rescued villagers** from Level 1 are missing completely.
3. **Coins reset to 0**.
4. **Grenade (`hasBomb`) is missing** from inventory (screenshot confirms only Machete exists, Grenade disappeared).

### Solution:
1. **Global Campaign Persistence (`CampaignSquadState`)**:
   - `coins`: Carried over across all levels.
   - `hasBomb`: Preserved into Level 2 inventory.
   - `hasMachete` & `isMacheteEquipped`: Preserved into Level 2.
   - `isJaneRevived`: Carried over into Level 2 (with her Katana and health).
   - `rescuedVillagers`: Dr. Ramirez and Nurse Claire carried over into Level 2 squad.
2. **Level 2 Spawn Positioning**:
   - Spawn Jane right beside the player at the Reception area.
   - Spawn the rescued villagers right behind the player in squad formation.
3. **Generalize AI & Rendering**:
   - Update `updateLevel1AlliesAndVillagers` -> `updateAlliesAndVillagers` and remove `if (levelNumber == 1)` so Jane and villagers follow and fight across all levels.

---

## 5. 🎮 New Feature: "DEBUG CO-OP (SPLIT SCREEN)" Mode

### Requested Behavior:
- Add a dedicated menu button in the Launcher (`MainMenu.fxml` / `MainMenuController.java`): **`DEBUG CO-OP (SPLIT SCREEN)`**.
- When selected:
  - Game boots into **Split Screen mode** on a single computer.
  - Left screen: **Player 1 (Elric)** controlled by `W, A, S, D` (Space attack, E interact).
  - Right screen: **Player 2 (Jane)** controlled by Number Keys / Numpad / Arrow keys:
    - `5` or `8` or `Up Arrow`: Move Up
    - `2` or `Down Arrow`: Move Down
    - `4` or `Left Arrow`: Move Left
    - `6` or `Right Arrow`: Move Right
    - `1` / `Space` (P2): Attack (Katana slash)
    - `3` / `0` (P2): Interact
