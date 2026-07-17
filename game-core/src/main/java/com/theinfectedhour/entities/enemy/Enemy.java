package com.theinfectedhour.entities.enemy;

import com.theinfectedhour.entities.AbilitySet;
import com.theinfectedhour.entities.Character;
import com.theinfectedhour.entities.enemy.ai.EnemyAI;

/** Base for AI-controlled hostiles; behavior comes from an injected EnemyAI strategy, not from subclasses. */
public abstract class Enemy extends Character {

    protected EnemyAI ai;

    protected Enemy(AbilitySet abilitySet, EnemyAI ai) {
        super(abilitySet);
        this.ai = ai;
    }
}
