# Codex Change Log — The Infected Hour

**Owner:** Codex  
**Date:** 2026-09-18  
**Branch:** faiyaz  
**Purpose:** coordinate safely with Gemini and prevent overlapping edits.

## Coordination rule

Gemini should avoid editing the files listed under **Codex-owned files** while
this work is being reviewed. New art and audio can be added independently under
their own folders. If a shared file must be changed, coordinate first and
preserve both sides of the diff.

## Codex-owned files in this change

### New code

- core/src/main/java/com/infectedhour/core/content/DialogueCatalog.java
- core/src/main/java/com/infectedhour/core/audio/SoundtrackCatalog.java
- core/src/main/java/com/infectedhour/core/audio/AudioDirector.java
- core/src/test/java/com/infectedhour/core/content/DialogueCatalogTest.java
- core/src/test/java/com/infectedhour/core/audio/SoundtrackCatalogTest.java

### Shared integration points

- core/src/main/java/com/infectedhour/core/InfectedHourGame.java
- core/src/main/java/com/infectedhour/core/screens/MainMenuScreen.java
- core/src/main/java/com/infectedhour/core/screens/LevelBriefingScreen.java
- core/src/main/java/com/infectedhour/core/screens/StoryPanelScreen.java
- core/src/main/java/com/infectedhour/core/screens/GameScreen.java
- core/src/main/java/com/infectedhour/core/screens/BossScreen.java

The integration points only request music/voice cues. They do not change
network authority, collision, boss damage, level objectives, or map logic.

## What was added

### Dialogue

DialogueCatalog is the single source for story panel text, speaker names,
stable dialogue IDs, panel titles, and future voice-over paths. It currently
contains the intro, post-Level-1, post-Level-2, ending, and boss title scenes.

Voice files are expected at:

    assets/audio/voice/*.ogg

No voice files were added yet.

### Music and sound effects

SoundtrackCatalog lists themed tracks and one-shot effects. It currently defines
menu, hospital, roadside village, hidden laboratory, Virus Heart boss, Jane
duel, story, victory, boss-phase, telegraph, objective, and player-hit cues.

Audio files are expected at:

    assets/audio/music/*.ogg
    assets/audio/sfx/*.ogg

No audio files were added yet.

### Future content placeholders

Use these comments as the hand-off points for future work:

- **Dialogue:** add a stable line ID, speaker, subtitle text, and voice path
  in `DialogueCatalog`; then place the matching `.ogg` in
  `assets/audio/voice/`.
- **Music:** add a track entry in `SoundtrackCatalog.Track`, place the `.ogg`
  in `assets/audio/music/`, and request it from the relevant screen.
- **Sound effects:** add an entry in `SoundtrackCatalog.Effect`, place the
  `.ogg` in `assets/audio/sfx/`, and trigger it through `AudioDirector`.
- **Voice-over timing:** keep one voice file per dialogue line so subtitles,
  replay data, and recorded audio remain aligned.

Do not replace these placeholders with direct `Music`/`Sound` calls in screens;
extend the catalogs and keep playback centralized in `AudioDirector`.

### Runtime playback

AudioDirector is owned by InfectedHourGame and shared by all screens. It:

- loops music and replaces it when the screen/theme changes;
- plays future dialogue voice-over and one-shot effects;
- caches short sound effects;
- exposes separate music, voice, and SFX volume controls;
- stops and disposes audio during game shutdown;
- logs missing audio once without crashing the game.

Music hooks were added to the main menu, level briefing, gameplay, and boss
screen. Story panels request the matching voice-over when a panel starts or
changes. The boss title requests its voice-over when the encounter opens.

## Conflict boundaries

- Do not rename dialogue IDs after voice recording begins; IDs are the stable
  link between text, voice files, subtitles, and future save/replay data.
- Add new dialogue by appending a new Line to DialogueCatalog; do not put story
  strings back into screen classes.
- Add music/effects by extending SoundtrackCatalog; do not load Music or Sound
  directly from gameplay screens.
- Do not commit generated audio previews or editor cache files into
  assets/audio/.
- If an audio asset is unavailable, leave its catalog path intact. The runtime
  intentionally handles missing files safely.

## Verification

The catalog tests validate stable IDs, voice path conventions, scene/panel
counts, and track/effect path conventions. Run:

    gradlew.bat :core:test

## Files intentionally not touched

Existing map, collision, boss, sprite, and Gemini-created image files were not
modified by this audio/dialogue change.

## Teammate ownership — Level 6 / `level_final`

The user has assigned the complete final level to a teammate and will fully
accept that teammate's implementation. Their Level 6 work is the source of
truth.

- Preserve the teammate's `level_final` map, final-boss logic, gameplay, and
  assets without rewriting or reverting them.
- In shared-file conflicts, keep the teammate's Level 6-specific branches and
  adapt Levels 1–5 around them.
- Do not modify `core/src/main/resources/maps/level_final.map`,
  `assets/map_final.png`, `BossScreen.java`, or Level 6-specific code unless
  the user explicitly requests it.
- If tool or model limits interrupt the work, continue on other levels and
  leave the final level untouched for the teammate.
