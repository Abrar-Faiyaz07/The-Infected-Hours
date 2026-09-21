package com.infectedhour.core.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogueCatalogTest {

    @Test
    void everyStoryPanelHasStableIdAndVoicePath() {
        for (DialogueCatalog.Scene scene : DialogueCatalog.Scene.values()) {
            assertFalse(DialogueCatalog.lines(scene).isEmpty(), scene + " has no dialogue");
            DialogueCatalog.lines(scene).forEach(line -> {
                assertFalse(line.id().isBlank());
                assertFalse(line.speaker().isBlank());
                assertFalse(line.text().isBlank());
                assertTrue(line.voiceAsset().startsWith("audio/voice/"));
                assertTrue(line.voiceAsset().endsWith(".ogg"));
                assertTrue(line.imageAsset().endsWith(".png"));
                assertFalse(line.text().contains("Elena"));
                assertFalse(line.text().contains("Oscorp"));
            });
            assertEquals(DialogueCatalog.lines(scene).size(), DialogueCatalog.panelText(scene).length);
            assertEquals(DialogueCatalog.lines(scene).size(), DialogueCatalog.panelTitles(scene).length);
        }
    }

    @Test
    void panelLookupRejectsInvalidIndexes() {
        assertEquals("intro-01", DialogueCatalog.line(DialogueCatalog.Scene.INTRO, 0).id());
        org.junit.jupiter.api.Assertions.assertThrows(
                IndexOutOfBoundsException.class,
                () -> DialogueCatalog.line(DialogueCatalog.Scene.INTRO, 99)
        );
    }

    @Test
    void generatedCinematicSequenceHasFourteenStoryPanelsAfterTheMenu() {
        int storyPanelCount = java.util.Arrays.stream(DialogueCatalog.Scene.values())
                .filter(scene -> scene != DialogueCatalog.Scene.BOSS)
                .mapToInt(scene -> DialogueCatalog.lines(scene).size())
                .sum();

        assertEquals(14, storyPanelCount);
    }
}
