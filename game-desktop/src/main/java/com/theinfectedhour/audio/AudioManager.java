package com.theinfectedhour.audio;

import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.Vector2;
import com.theinfectedhour.entities.Updatable;

/**
 * Plays music and sound effects in response to game events, with positional
 * panning: pitch/pan of world sounds (enemy screeches, boss roars) are derived
 * from the sound source's distance and direction relative to the local player.
 */
public class AudioManager implements Updatable {

    private Music currentMusic;

    /** Simple UI/global playback, no spatialization. */
    public void play(Sound sound, float volume) {
        // TODO: sound.play(volume)
    }

    /**
     * Positional playback: pan in [-1 left .. 1 right] from horizontal offset,
     * volume falls off with distance from the listener (local player).
     */
    public void playSpatial(Sound sound, Vector2 sourcePosition, Vector2 listenerPosition) {
        // TODO: compute pan/volume from (sourcePosition - listenerPosition),
        //       then sound.play(volume, pitch, pan)
    }

    public void playMusic(String trackPath, boolean looping) {
        // TODO: stop currentMusic, load and play new track
    }

    @Override
    public void update(float deltaTime) {
        // TODO: fade transitions between music tracks
    }
}
