package com.theinfectedhour.entities.enemy;

import com.theinfectedhour.entities.AbilitySet;
import com.theinfectedhour.entities.enemy.ai.EnemyAI;

/** The standard infected enemy unit. */
public class MutatedCell extends Enemy {

    public MutatedCell(AbilitySet abilitySet, EnemyAI ai) {
        super(abilitySet, ai);
    }
}
