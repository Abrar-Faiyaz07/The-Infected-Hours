package com.theinfectedhour.save;

/** Contract for anything whose state can be captured to and restored from SaveData. */
public interface Saveable {

    SaveData toSaveData();

    void fromSaveData(SaveData data);
}
