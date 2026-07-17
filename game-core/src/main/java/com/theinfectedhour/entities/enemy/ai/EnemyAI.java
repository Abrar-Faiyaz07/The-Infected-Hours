package com.theinfectedhour.entities.enemy.ai;

/** Strategy contract: decides an enemy's next action from a restricted view of the world. */
public interface EnemyAI {

    Action decideAction(WorldContext context);
}
