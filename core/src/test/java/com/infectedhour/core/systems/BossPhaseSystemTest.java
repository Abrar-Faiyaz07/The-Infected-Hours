package com.infectedhour.core.systems;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BossPhaseSystemTest {

    @Test
    void startsInShieldPhase() {
        BossPhaseSystem boss = new BossPhaseSystem();
        assertEquals(BossPhaseSystem.Phase.SHIELD, boss.getCurrentPhase());
    }

    @Test
    void shieldWeakenedMovesToExposure() {
        BossPhaseSystem boss = new BossPhaseSystem();
        boss.onShieldWeakened();
        assertEquals(BossPhaseSystem.Phase.EXPOSURE, boss.getCurrentPhase());
    }

    @Test
    void exposureWindowExpiringReturnsToShield() {
        BossPhaseSystem boss = new BossPhaseSystem();
        boss.onShieldWeakened();
        boss.tickExposure(20f, 0f); // window is 15s, no damage dealt

        assertEquals(BossPhaseSystem.Phase.SHIELD, boss.getCurrentPhase());
        assertEquals(1, boss.getShieldCycleCount());
    }

    @Test
    void enoughDamageMovesToCoreDestruction() {
        BossPhaseSystem boss = new BossPhaseSystem();
        boss.onShieldWeakened();
        boss.tickExposure(1f, 0.8f); // pushes coreHpPct below 25% threshold

        assertEquals(BossPhaseSystem.Phase.CORE_DESTRUCTION, boss.getCurrentPhase());
    }

    @Test
    void simultaneousStrikeDefeatsBoss() {
        BossPhaseSystem boss = new BossPhaseSystem();
        boss.onShieldWeakened();
        boss.tickExposure(1f, 0.8f);

        boss.resolveCoreDestructionAttempt(true, true);

        assertEquals(BossPhaseSystem.Phase.DEFEATED, boss.getCurrentPhase());
    }

    @Test
    void oneSidedStrikeDoesNotDefeatBoss() {
        BossPhaseSystem boss = new BossPhaseSystem();
        boss.onShieldWeakened();
        boss.tickExposure(1f, 0.8f);

        boss.resolveCoreDestructionAttempt(true, false);

        assertEquals(BossPhaseSystem.Phase.CORE_DESTRUCTION, boss.getCurrentPhase());
    }
}
