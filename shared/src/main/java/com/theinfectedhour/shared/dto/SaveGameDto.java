package com.theinfectedhour.shared.dto;

import java.io.Serializable;

/** REST payload mirroring the save_games row a player syncs with the backend. */
public class SaveGameDto implements Serializable {

    public long userId;
    public int levelReached;
    public String contaminationStats; // JSON blob, schema owned by game-core SaveData
}
