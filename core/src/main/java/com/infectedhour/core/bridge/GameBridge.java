package com.infectedhour.core.bridge;

import java.util.function.Consumer;

/**
 * Thread-safe boundary between the JavaFX Application Thread and the
 * libGDX render thread (TRD §2). Neither side touches the other's UI
 * toolkit directly — everything crosses through here.
 *
 * fx-launcher implements the FX-side callbacks (via GameLauncherBridge) and
 * hands this interface to InfectedHourGame when it boots the Lwjgl3Application
 * on its own thread.
 */
public class GameBridge {

    private Consumer<MatchOutcome> onMatchEnded = outcome -> {
    };
    private Runnable onGameWindowClosed = () -> {
    };

    /** Called by core (libGDX thread) when the match reaches Results. Must marshal to FX via Platform.runLater on the receiving side. */
    public void notifyMatchEnded(MatchOutcome outcome) {
        onMatchEnded.accept(outcome);
    }

    /** Called by core when the libGDX window is disposed, so fx-launcher can re-show its stage. */
    public void onGameWindowClosed() {
        onGameWindowClosed.run();
    }

    public void setOnMatchEnded(Consumer<MatchOutcome> callback) {
        this.onMatchEnded = callback;
    }

    public void setOnGameWindowClosed(Runnable callback) {
        this.onGameWindowClosed = callback;
    }

    /** Minimal payload core hands back to the launcher for the Results screen. */
    public record MatchOutcome(String result, int finalLevelReached) {
    }
}
