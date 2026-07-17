package com.theinfectedhour.entities.player;

import com.theinfectedhour.entities.AbilitySet;

/** The Field Medic playable role — healing/curing behavior comes from its injected AbilitySet. */
public class FieldMedic extends Player {

    public FieldMedic(AbilitySet abilitySet) {
        super(PlayerRole.FIELD_MEDIC, abilitySet);
    }
}
