package com.infectedhour.core.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundtrackCatalogTest {

    @Test
    void gameplayLevelsUseTheirThemedTracks() {
        assertEquals(SoundtrackCatalog.Track.HOSPITAL, SoundtrackCatalog.forLevel(1));
        assertEquals(SoundtrackCatalog.Track.ROADSIDE_VILLAGE, SoundtrackCatalog.forLevel(2));
        assertEquals(SoundtrackCatalog.Track.HIDDEN_LABORATORY, SoundtrackCatalog.forLevel(3));
    }

    @Test
    void everyTrackAndEffectUsesAnOggAssetPath() {
        for (SoundtrackCatalog.Track track : SoundtrackCatalog.Track.values()) {
            assertTrue(track.assetPath().startsWith("audio/music/"));
            assertTrue(track.assetPath().endsWith(".ogg"));
        }
        for (SoundtrackCatalog.Effect effect : SoundtrackCatalog.Effect.values()) {
            assertTrue(effect.assetPath().startsWith("audio/sfx/"));
            assertTrue(effect.assetPath().endsWith(".ogg"));
        }
    }
}
