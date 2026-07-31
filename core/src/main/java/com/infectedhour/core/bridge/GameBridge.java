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
 *
 * <p>Callbacks are {@code volatile} because they are set on the FX thread but
 * invoked from the libGDX render thread and from KryoNet's listener thread.
 * Every receiver is responsible for marshalling onto its own toolkit's thread
 * ({@code Platform.runLater} on the FX side, {@code Gdx.app.postRunnable} on
 * the game side).
 */
public class GameBridge {

    private volatile Consumer<MatchOutcome> onMatchEnded = outcome -> {
    };
    private volatile Runnable onGameWindowClosed = () -> {
    };
    private volatile Consumer<String> onJoinFailed = reason -> {
    };
    private volatile Consumer<PartnerEvent> onPartnerEvent = event -> {
    };

    /** Called by core (libGDX thread) when the match reaches Results. Must marshal to FX via Platform.runLater on the receiving side. */
    public void notifyMatchEnded(MatchOutcome outcome) {
        onMatchEnded.accept(outcome);
    }

    /** Called by core when the libGDX window is disposed, so fx-launcher can re-show its stage. */
    public void onGameWindowClosed() {
        onGameWindowClosed.run();
    }

    /**
     * Called from the networking layer when this machine could not join or lost
     * the host: LOBBY_FULL, VERSION_MISMATCH, TIMEOUT, HOST_LOST
     * (see {@code GameConstants.REJECT_*}). The launcher re-shows itself and
     * explains — TRD §9 forbids surfacing network failures as crashes.
     */
    public void notifyJoinFailed(String reason) {
        onJoinFailed.accept(reason);
    }

    /** Partner joined / dropped / reconnected, so the lobby chip can change colour (UI/UX doc §7). */
    public void notifyPartnerEvent(PartnerEvent event) {
        onPartnerEvent.accept(event);
    }

    public void setOnMatchEnded(Consumer<MatchOutcome> callback) {
        this.onMatchEnded = callback;
    }

    public void setOnGameWindowClosed(Runnable callback) {
        this.onGameWindowClosed = callback;
    }

    public void setOnJoinFailed(Consumer<String> callback) {
        this.onJoinFailed = callback;
    }

    public void setOnPartnerEvent(Consumer<PartnerEvent> callback) {
        this.onPartnerEvent = callback;
    }

    /** Minimal payload core hands back to the launcher for the Results screen. */
    public record MatchOutcome(String result, int finalLevelReached) {
    }

    /** @param type one of {@code GameConstants.EVENT_PARTNER_*} / {@code EVENT_CONVERTED_TO_SOLO}. */
    public record PartnerEvent(String type, String displayName) {
    }
}
