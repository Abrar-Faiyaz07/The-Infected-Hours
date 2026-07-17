package com.theinfectedhour.entities;

/** Strategy-style bundle of role-specific abilities composed into a Character — composition over inheritance (Architecture.md §6). */
public interface AbilitySet {

    void useAbility(Character owner);
}
