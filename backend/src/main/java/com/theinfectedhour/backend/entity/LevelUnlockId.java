package com.theinfectedhour.backend.entity;

import java.io.Serializable;
import java.util.Objects;

/** Composite key (user_id, level_id) for LevelUnlock. */
public class LevelUnlockId implements Serializable {

    private Long userId;
    private Integer levelId;

    public LevelUnlockId() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LevelUnlockId other)) {
            return false;
        }
        return Objects.equals(userId, other.userId) && Objects.equals(levelId, other.levelId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, levelId);
    }
}
