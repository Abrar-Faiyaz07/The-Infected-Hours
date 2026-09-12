package com.infectedhour.core.bridge;

import com.infectedhour.shared.dto.SaveSlotDto;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

    private volatile boolean hasLauncher = false;
    private volatile Consumer<MatchOutcome> onMatchEnded = outcome -> {
    };
    private volatile Runnable onGameReady = () -> {
    };
    private volatile Runnable onGameWindowClosed = () -> {
    };
    private volatile Consumer<Runnable> onReturnToLauncherRequested = completion -> completion.run();
    private volatile Consumer<String> onJoinFailed = reason -> {
    };
    private volatile Consumer<PartnerEvent> onPartnerEvent = event -> {
    };
    private volatile Supplier<List<SaveSlotDto>> slotProvider = List::of;
    private volatile BiConsumer<Integer, SaveSlotDto> onSaveRequested = (slot, dto) -> {
    };
    private volatile Consumer<Integer> onSaveConfirmed = slot -> {
    };

    /** Called by core (libGDX thread) when the match reaches Results. Must marshal to FX via Platform.runLater on the receiving side. */
    public void notifyMatchEnded(MatchOutcome outcome) {
        onMatchEnded.accept(outcome);
    }

    /**
     * Called after libGDX has rendered its first frame. The launcher keeps its
     * loading overlay visible until this signal, so the desktop never flashes
     * or disappears while the native game window is being created.
     */
    public void notifyGameReady() {
        onGameReady.run();
    }

    /** Called by core when the libGDX window is disposed, so fx-launcher can re-show its stage. */
    public void onGameWindowClosed() {
        onGameWindowClosed.run();
    }

    /**
     * Requests an orderly game-to-launcher handoff. The launcher invokes
     * {@code closeGameWindow} only after its stage has been restored and given
     * time to paint, preventing a desktop flash between the two windows.
     */
    public void requestReturnToLauncher(Runnable closeGameWindow) {
        Runnable completion = closeGameWindow != null ? closeGameWindow : () -> {};
        onReturnToLauncherRequested.accept(completion);
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

    public List<SaveSlotDto> getSaveSlots() {
        return slotProvider.get();
    }

    public void requestSave(int slot, SaveSlotDto dto) {
        onSaveRequested.accept(slot, dto);
    }

    public void notifySaveConfirmed(int slot) {
        onSaveConfirmed.accept(slot);
    }

    public void setOnMatchEnded(Consumer<MatchOutcome> callback) {
        this.onMatchEnded = callback;
    }

    public void setOnGameReady(Runnable callback) {
        this.onGameReady = callback != null ? callback : () -> {};
    }

    public void setOnGameWindowClosed(Runnable callback) {
        this.onGameWindowClosed = callback != null ? callback : () -> {};
    }

    public boolean hasLauncher() {
        return hasLauncher;
    }

    public void setHasLauncher(boolean hasLauncher) {
        this.hasLauncher = hasLauncher;
    }

    public void setOnReturnToLauncherRequested(Consumer<Runnable> callback) {
        this.onReturnToLauncherRequested = callback != null ? callback : completion -> completion.run();
        if (callback != null) {
            this.hasLauncher = true;
        }
    }

    public void setOnJoinFailed(Consumer<String> callback) {
        this.onJoinFailed = callback;
    }

    public void setOnPartnerEvent(Consumer<PartnerEvent> callback) {
        this.onPartnerEvent = callback;
    }

    public void setSlotProvider(Supplier<List<SaveSlotDto>> provider) {
        this.slotProvider = provider != null ? provider : List::of;
    }

    public void setOnSaveRequested(BiConsumer<Integer, SaveSlotDto> callback) {
        this.onSaveRequested = callback != null ? callback : (slot, dto) -> {};
    }

    public void setOnSaveConfirmed(Consumer<Integer> callback) {
        this.onSaveConfirmed = callback != null ? callback : slot -> {};
    }

    /** Minimal payload core hands back to the launcher for the Results screen. */
    public record MatchOutcome(String result, int finalLevelReached) {
    }

    /** @param type one of {@code GameConstants.EVENT_PARTNER_*} / {@code EVENT_CONVERTED_TO_SOLO}. */
    public record PartnerEvent(String type, String displayName) {
    }
}
