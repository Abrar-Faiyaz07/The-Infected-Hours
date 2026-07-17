package com.theinfectedhour.entities.player;

import com.theinfectedhour.entities.AbilitySet;

/** The Local Scout playable role — mobility/recon behavior comes from its injected AbilitySet. */
public class LocalScout extends Player {

    public LocalScout(AbilitySet abilitySet) {
        super(PlayerRole.LOCAL_SCOUT, abilitySet);
    }
}
