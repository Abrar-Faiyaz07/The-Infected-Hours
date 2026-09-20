package com.infectedhour.core.audio;

/**
 * Asset manifest for music and sound effects. Audio files are intentionally
 * optional during development; add the referenced OGG files under assets/audio
 * when they are ready and the game will pick them up automatically.
 */
public final class SoundtrackCatalog {

    // TODO(audio): Add future music cues here with a descriptive enum name
    // and an assets/audio/music/*.ogg path. Keep gameplay screens dependent on
    // this catalog instead of loading Music directly.

    public enum Track {
        MAIN_MENU("audio/music/main_menu.ogg"),
        HOSPITAL("audio/music/level_1_hospital.ogg"),
        ROADSIDE_VILLAGE("audio/music/level_2_roadside_village.ogg"),
        HIDDEN_LABORATORY("audio/music/level_3_hidden_laboratory.ogg"),
        VIRUS_HEART_BOSS("audio/music/boss_virus_heart.ogg"),
        JANE_DUEL("audio/music/jane_duel.ogg"),
        STORY("audio/music/story_interlude.ogg"),
        VICTORY("audio/music/victory.ogg");

        private final String assetPath;

        Track(String assetPath) {
            this.assetPath = assetPath;
        }

        public String assetPath() {
            return assetPath;
        }
    }

    public enum Effect {
        // TODO(audio): Add future one-shot cues here with an
        // assets/audio/sfx/*.ogg path (for example, puzzle solved or survivor
        // rescued). Missing files are safe and will be logged by AudioDirector.
        STORY_ADVANCE("audio/sfx/story_advance.ogg"),
        LEVEL_START("audio/sfx/level_start.ogg"),
        BOSS_PHASE_CHANGE("audio/sfx/boss_phase_change.ogg"),
        BOSS_TELEGRAPH("audio/sfx/boss_telegraph.ogg"),
        BOSS_DEFEATED("audio/sfx/boss_defeated.ogg"),
        OBJECTIVE_COMPLETE("audio/sfx/objective_complete.ogg"),
        PLAYER_HIT("audio/sfx/player_hit.ogg");

        private final String assetPath;

        Effect(String assetPath) {
            this.assetPath = assetPath;
        }

        public String assetPath() {
            return assetPath;
        }
    }

    private SoundtrackCatalog() {
    }

    public static Track forLevel(int levelNumber) {
        return switch (levelNumber) {
            case 1 -> Track.HOSPITAL;
            case 2, 4, 5 -> Track.ROADSIDE_VILLAGE;
            case 3, 6 -> Track.HIDDEN_LABORATORY;
            default -> Track.STORY;
        };
    }
}
