package com.infectedhour.fxlauncher.state;

import com.infectedhour.shared.dto.PlayerDto;

/**
 * In-memory session: JWT token, current player, and the backend URL both
 * laptops must agree on (App Flow par.4).
 *
 * TEAMMATE TASK (fx): offline save cache (App Flow par.6):
 *  - After every successful GET /players/me/save, also write the
 *    SaveStateDto to ~/.infectedhour/save.json.
 *  - In offline mode, read that file so solo play still knows which
 *    levels are unlocked; on next login, PUT it if newer (server's
 *    latest-updatedAt copy wins ties).
 */
public class SessionState {

    private static final SessionState INSTANCE = new SessionState();

    private String jwtToken;
    private PlayerDto currentPlayer;
    private String backendUrl = "http://localhost:8080"; // overridden once host IP is known (Settings screen, advanced)
    private boolean offlineMode = false;
    /** Set by the Load Game screen; the game reads it on boot to restore a checkpoint. Null = new run. */
    private com.infectedhour.shared.dto.SaveSlotDto loadedSlot;

    private SessionState() {
    }

    public static SessionState get() {
        return INSTANCE;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public PlayerDto getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(PlayerDto currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    public String getBackendUrl() {
        return backendUrl;
    }

    public void setBackendUrl(String backendUrl) {
        this.backendUrl = backendUrl;
    }

    public boolean isOfflineMode() {
        return offlineMode;
    }

    public void setOfflineMode(boolean offlineMode) {
        this.offlineMode = offlineMode;
    }

    public boolean isAuthenticated() {
        return jwtToken != null;
    }

    /** The slot the player chose in Load Game, or null for a fresh run. */
    public com.infectedhour.shared.dto.SaveSlotDto getLoadedSlot() {
        return loadedSlot;
    }

    public void setLoadedSlot(com.infectedhour.shared.dto.SaveSlotDto loadedSlot) {
        this.loadedSlot = loadedSlot;
    }

    public void clearLoadedSlot() {
        this.loadedSlot = null;
    }
}
