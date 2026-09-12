package com.infectedhour.core.bridge;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameBridgeTest {

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
