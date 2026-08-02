package com.infectedhour.core.systems;

/**
 * 3-phase boss state machine for "The Virus Heart" (PRD §8). Reuses
 * ObjectiveSystem/CombatSystem/AISystem rather than bespoke boss code,
 * per TRD's risk mitigation for boss complexity.
 *
 * Phase 1 (Shield): Elric places cure samples at 3 injection points while
 *   Jane defends — modeled as an ObjectiveSystem with 3 SAMPLE-type targets.
 * Phase 2 (Exposure): shield drops for a 15s window; both players attack
 *   the core. If the window expires before core HP threshold is hit,
 *   shield returns (weaker) and it's back to Phase 1.
 * Phase 3 (Core Destruction): core HP < 25% — requires simultaneous
 *   Elric cure-charge + Jane strike within a short window.
 */
public class BossPhaseSystem {

    public enum Phase {
        SHIELD, EXPOSURE, CORE_DESTRUCTION, DEFEATED
    }

    private static final float EXPOSURE_WINDOW_SECONDS = 15f;
    private static final float CORE_DESTRUCTION_HP_THRESHOLD_PCT = 0.25f;
    private static final int INJECTION_POINTS_PER_SHIELD_CYCLE = 3;

    private Phase currentPhase = Phase.SHIELD;
    private float coreHpPct = 1.0f;
    private float exposureTimerRemaining;
    private int shieldCycleCount = 0;

    public Phase getCurrentPhase() {
        return currentPhase;
    }

    public float getCoreHpPct() {
        return coreHpPct;
    }

    /** Called when all 3 injection points for the current shield cycle are complete. */
    public void onShieldWeakened() {
        if (currentPhase != Phase.SHIELD) return;
        currentPhase = Phase.EXPOSURE;
        exposureTimerRemaining = EXPOSURE_WINDOW_SECONDS;
    }

    /** Tick while in EXPOSURE; call with damage applied this frame from both players. */
    public void tickExposure(float delta, float damageDealtThisTick) {
        if (currentPhase != Phase.EXPOSURE) return;

        coreHpPct = Math.max(0f, coreHpPct - damageDealtThisTick);
        exposureTimerRemaining -= delta;

        if (coreHpPct <= CORE_DESTRUCTION_HP_THRESHOLD_PCT) {
            currentPhase = Phase.CORE_DESTRUCTION;
            return;
        }
        if (exposureTimerRemaining <= 0) {
            // Window expired without enough damage — shield returns, weaker next time.
            shieldCycleCount++;
            currentPhase = Phase.SHIELD;
        }
    }

    /** Both players' simultaneous-strike QTE result for Phase 3 (PRD §8). */
    public void resolveCoreDestructionAttempt(boolean elricCureChargeReady, boolean janeStrikeReady) {
        if (currentPhase != Phase.CORE_DESTRUCTION) return;
        if (elricCureChargeReady && janeStrikeReady) {
            currentPhase = Phase.DEFEATED;
        }
        // ================ TEAMMATE TASK: QTE PARTIAL-FAIL ================
        // TODO(boss): feedback when only ONE player was ready.
        //  - Fire an EventMessage("QTE_PARTIAL_FAIL", whoMissed) so BOTH
        //    screens can flash the prompt red + play a fail SFX.
        //  - Do NOT punish with damage — the punishment is time (arena
        //    contamination keeps rising). UX rule 4: "failure teaches".
        // =================================================================
    }

    public int getShieldCycleCount() {
        return shieldCycleCount;
    }
}
