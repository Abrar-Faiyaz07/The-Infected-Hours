package com.infectedhour.core.audio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    @Test
    void combatEffectFallbacksArePackagedWithTheGame() {
        assertEquals("music/machete.mp3", SoundtrackCatalog.Effect.MELEE_HIT.fallbackAssetPath());
        assertEquals("music/swing.mp3", SoundtrackCatalog.Effect.MELEE_SWING.fallbackAssetPath());
        assertEquals("music/bomb.mp3", SoundtrackCatalog.Effect.BOMB_EXPLOSION.fallbackAssetPath());

        ClassLoader assets = SoundtrackCatalogTest.class.getClassLoader();
        assertNotNull(assets.getResource("music/machete.mp3"));
        assertNotNull(assets.getResource("music/swing.mp3"));
        assertNotNull(assets.getResource("music/bomb.mp3"));
    }
}
