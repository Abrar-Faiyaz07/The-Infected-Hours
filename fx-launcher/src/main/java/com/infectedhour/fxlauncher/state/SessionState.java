package com.infectedhour.fxlauncher.state;

import com.infectedhour.shared.dto.PlayerDto;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

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

    private static final String SUBTITLE_ALIGNMENT_KEY = "cinematicSubtitleAlignment";
    private static final String MUSIC_VOLUME_KEY = "musicVolumePercent";
    private static final String SFX_VOLUME_KEY = "sfxVolumePercent";
    private static final String SUBTITLE_VOICE_VOLUME_KEY = "subtitleVoiceVolumePercent";
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(SessionState.class);
    private static final SessionState INSTANCE = new SessionState();

    public enum SubtitleAlignment {
        CENTER,
        LEFT
    }

    private String jwtToken;
    private PlayerDto currentPlayer;
    private String backendUrl = "http://localhost:8080"; // overridden once host IP is known (Settings screen, advanced)
    private boolean offlineMode = false;
    /** Fullscreen is the normal presentation; Settings can opt into windowed mode. */
    private boolean fullscreen = true;
    /** Stored per Windows user so cinematic dialogue keeps the chosen alignment after restart. */
    private SubtitleAlignment subtitleAlignment = loadSubtitleAlignment();
    private double musicVolumePercent = loadVolume(MUSIC_VOLUME_KEY, 80.0);
    private double sfxVolumePercent = loadVolume(SFX_VOLUME_KEY, 85.0);
    private double subtitleVoiceVolumePercent = loadVolume(SUBTITLE_VOICE_VOLUME_KEY, 100.0);
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

    public boolean isFullscreen() {
        return fullscreen;
    }

    public void setFullscreen(boolean fullscreen) {
        this.fullscreen = fullscreen;
    }

    public SubtitleAlignment getSubtitleAlignment() {
        return subtitleAlignment;
    }

    public void setSubtitleAlignment(SubtitleAlignment subtitleAlignment) {
        this.subtitleAlignment = subtitleAlignment == null ? SubtitleAlignment.CENTER : subtitleAlignment;
        PREFERENCES.put(SUBTITLE_ALIGNMENT_KEY, this.subtitleAlignment.name());
        try {
            PREFERENCES.flush();
        } catch (BackingStoreException ignored) {
            // The in-memory setting still applies to the current launcher session.
        }
    }

    private static SubtitleAlignment loadSubtitleAlignment() {
        String saved = PREFERENCES.get(SUBTITLE_ALIGNMENT_KEY, SubtitleAlignment.CENTER.name());
        try {
            return SubtitleAlignment.valueOf(saved);
        } catch (IllegalArgumentException ignored) {
            return SubtitleAlignment.CENTER;
        }
    }

    public double getMusicVolumePercent() {
        return musicVolumePercent;
    }

    public void setMusicVolumePercent(double volumePercent) {
        musicVolumePercent = saveVolume(MUSIC_VOLUME_KEY, volumePercent);
    }

    public double getSfxVolumePercent() {
        return sfxVolumePercent;
    }

    public void setSfxVolumePercent(double volumePercent) {
        sfxVolumePercent = saveVolume(SFX_VOLUME_KEY, volumePercent);
    }

    public double getSubtitleVoiceVolumePercent() {
        return subtitleVoiceVolumePercent;
    }

    public void setSubtitleVoiceVolumePercent(double volumePercent) {
        subtitleVoiceVolumePercent = saveVolume(SUBTITLE_VOICE_VOLUME_KEY, volumePercent);
    }

    private static double loadVolume(String key, double defaultValue) {
        return clampPercent(PREFERENCES.getDouble(key, defaultValue));
    }

    private static double saveVolume(String key, double value) {
        double clamped = clampPercent(value);
        PREFERENCES.putDouble(key, clamped);
        try {
            PREFERENCES.flush();
        } catch (BackingStoreException ignored) {
            // The in-memory setting still applies to the current launcher session.
        }
        return clamped;
    }

    private static double clampPercent(double value) {
        return Math.max(0.0, Math.min(100.0, value));
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
