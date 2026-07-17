package com.theinfectedhour.entities.boss;

import com.theinfectedhour.entities.AbilitySet;
import com.theinfectedhour.entities.Character;
import com.theinfectedhour.entities.boss.phases.BossPhase;

/** The final boss; its fight rules live in the current BossPhase state object, never in this class. */
public class VirusHeart extends Character {

    private BossPhase currentPhase;

    public VirusHeart(AbilitySet abilitySet, BossPhase initialPhase) {
        super(abilitySet);
        this.currentPhase = initialPhase;
    }

    /** Transitions to the next phase (State pattern: Shield -> Exposure -> CoreDestruction). */
    public void advancePhase() {
        // TODO: implement
    }
}
