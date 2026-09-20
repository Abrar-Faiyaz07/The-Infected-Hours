package com.infectedhour.core.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.infectedhour.core.content.DialogueCatalog;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Safe, centralized music/voice/SFX playback for the desktop game.
 *
 * <p>Missing audio is a normal development state, not a fatal game error. The
 * director checks the asset first and logs a path once, so programmers can wire
 * dialogue and soundtrack cues before the audio team supplies the OGG files.</p>
 */
public final class AudioDirector implements AutoCloseable {

    /*
     * FUTURE AUDIO PLACEHOLDER
     * ------------------------
     * Add new playback behavior here when the related catalog entry exists:
     *   game.getAudioDirector().playMusic(SoundtrackCatalog.Track.NEW_TRACK);
     *   game.getAudioDirector().playVoice(DialogueCatalog.line(scene, index));
     *   game.getAudioDirector().playEffect(SoundtrackCatalog.Effect.NEW_EFFECT);
     * Keep asset loading centralized in this class so missing files never
     * interrupt a screen transition or crash the game.
     */

    private final Map<String, Sound> soundCache = new HashMap<>();
    private final Set<String> missingAssets = new HashSet<>();
    private Music music;
    private Sound activeVoice;
    private long activeVoiceId = -1L;
    private SoundtrackCatalog.Track currentTrack;
    private float musicVolume = 0.80f;
    private float voiceVolume = 1.00f;
    private float sfxVolume = 0.85f;

    public void playMusic(SoundtrackCatalog.Track track) {
        if (track == null || track == currentTrack) return;
        stopMusic();
        currentTrack = track;
        FileHandle file = asset(track.assetPath());
        if (file == null || Gdx.audio == null) return;
        try {
            music = Gdx.audio.newMusic(file);
            music.setLooping(true);
            music.setVolume(musicVolume);
            music.play();
        } catch (RuntimeException error) {
            reportMissing(track.assetPath(), error);
            music = null;
        }
    }

    public void playVoice(DialogueCatalog.Line line) {
        if (line == null || line.voiceAsset().isBlank()) return;
        stopVoice();
        Sound sound = sound(line.voiceAsset());
        if (sound != null) {
            activeVoice = sound;
            activeVoiceId = sound.play(voiceVolume);
        }
    }

    public void playEffect(SoundtrackCatalog.Effect effect) {
        if (effect == null) return;
        Sound sound = sound(effect.assetPath());
        if (sound != null) sound.play(sfxVolume);
    }

    public void stopMusic() {
        if (music != null) {
            music.stop();
            music.dispose();
            music = null;
        }
        currentTrack = null;
    }

    public void stopVoice() {
        if (activeVoice != null) {
            if (activeVoiceId >= 0L) activeVoice.stop(activeVoiceId);
            activeVoice = null;
            activeVoiceId = -1L;
        }
    }

    public void setMusicVolume(float volume) {
        musicVolume = clamp(volume);
        if (music != null) music.setVolume(musicVolume);
    }

    public void setVoiceVolume(float volume) {
        voiceVolume = clamp(volume);
    }

    public void setSfxVolume(float volume) {
        sfxVolume = clamp(volume);
    }

    public SoundtrackCatalog.Track getCurrentTrack() {
        return currentTrack;
    }

    public boolean isMusicPlaying() {
        return music != null && music.isPlaying();
    }

    public Set<String> getMissingAssets() {
        return Set.copyOf(missingAssets);
    }

    @Override
    public void close() {
        stopVoice();
        stopMusic();
        for (Sound sound : soundCache.values()) sound.dispose();
        soundCache.clear();
    }

    public void dispose() {
        close();
    }

    private Sound sound(String path) {
        Sound cached = soundCache.get(path);
        if (cached != null) return cached;
        FileHandle file = asset(path);
        if (file == null || Gdx.audio == null) return null;
        try {
            Sound loaded = Gdx.audio.newSound(file);
            soundCache.put(path, loaded);
            return loaded;
        } catch (RuntimeException error) {
            reportMissing(path, error);
            return null;
        }
    }

    private FileHandle asset(String path) {
        if (Gdx.files == null) return null;
        try {
            FileHandle file = Gdx.files.internal(path);
            if (file.exists()) return file;
            reportMissing(path, null);
        } catch (RuntimeException error) {
            reportMissing(path, error);
        }
        return null;
    }

    private void reportMissing(String path, RuntimeException error) {
        if (!missingAssets.add(path) || Gdx.app == null) return;
        if (error == null) Gdx.app.log("AudioDirector", "Audio asset not supplied yet: " + path);
        else Gdx.app.log("AudioDirector", "Could not load audio: " + path);
    }

    private static float clamp(float volume) {
        return Math.max(0f, Math.min(1f, volume));
    }
}
