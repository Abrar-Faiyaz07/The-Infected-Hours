package com.infectedhour.core.bridge;

import com.infectedhour.core.state.CampaignSquadState;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameBridgeTest {

    @Test
    void cinematicSubtitlesDefaultToCenteredAndCanBeChangedByLauncherSettings() {
        GameBridge bridge = new GameBridge();

        assertTrue(bridge.isCinematicSubtitleCentered());

        bridge.setCinematicSubtitleCentered(false);

        assertFalse(bridge.isCinematicSubtitleCentered());
    }

    @Test
    void launcherAudioVolumesReachTheGameAndStayWithinValidRange() {
        GameBridge bridge = new GameBridge();

        bridge.setMusicVolume(0.35f);
        bridge.setSfxVolume(-1f);
        bridge.setSubtitleVoiceVolume(2f);

        assertEquals(0.35f, bridge.getMusicVolume());
        assertEquals(0f, bridge.getSfxVolume());
        assertEquals(1f, bridge.getSubtitleVoiceVolume());
    }

    @Test
    void matchOutcomeCapturesCoinsAndFinalizedSurvivorReport() {
        CampaignSquadState.reset();
        CampaignSquadState.coins = 27;
        CampaignSquadState.finalizeSurvivorReport(3);

        GameBridge.MatchOutcome outcome = new GameBridge.MatchOutcome("VICTORY", 6);

        assertEquals(27, outcome.coinsCollected());
        assertEquals(3, outcome.npcsSaved());
        assertEquals(1, outcome.npcsFailed());
        assertTrue(outcome.totalGameTimeSeconds() >= 0L);

        CampaignSquadState.reset();
    }

    @Test
    void gameReadySignalReachesLauncherCallback() {
        GameBridge bridge = new GameBridge();
        AtomicInteger calls = new AtomicInteger();
        bridge.setOnGameReady(calls::incrementAndGet);

        bridge.notifyGameReady();

        assertEquals(1, calls.get());
    }

    @Test
    void nullGameReadyCallbackFallsBackToNoOp() {
        GameBridge bridge = new GameBridge();
        bridge.setOnGameReady(null);

        bridge.notifyGameReady();
    }

    @Test
    void returnRequestWaitsForLauncherToReleaseGameWindow() {
        GameBridge bridge = new GameBridge();
        AtomicInteger closeCalls = new AtomicInteger();
        AtomicInteger launcherCalls = new AtomicInteger();

        bridge.setOnReturnToLauncherRequested(closeGameWindow -> {
            launcherCalls.incrementAndGet();
            assertEquals(0, closeCalls.get());
            closeGameWindow.run();
        });

        bridge.requestReturnToLauncher(closeCalls::incrementAndGet);

        assertEquals(1, launcherCalls.get());
        assertEquals(1, closeCalls.get());
    }
}
