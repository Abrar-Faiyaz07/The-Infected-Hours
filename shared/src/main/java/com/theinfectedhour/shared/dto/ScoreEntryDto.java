package com.theinfectedhour.shared.dto;

import java.io.Serializable;

/** Leaderboard row exchanged with the backend (time, deaths per level). */
public class ScoreEntryDto implements Serializable {

    public long userId;
    public int levelId;
    public int timeSeconds;
    public int deaths;
}
