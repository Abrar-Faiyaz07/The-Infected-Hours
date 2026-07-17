package com.theinfectedhour.entities.boss.phases;

import com.theinfectedhour.entities.boss.VirusHeart;

/** State-pattern contract for one discrete phase of the VirusHeart fight. */
public interface BossPhase {

    void onEnter(VirusHeart boss);

    void update(float deltaTime);

    void onExit();
}
