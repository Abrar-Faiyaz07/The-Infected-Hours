package com.infectedhour.core.systems;

import com.infectedhour.core.entities.Player;
import com.infectedhour.shared.network.CharacterType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContaminationSystemTest {

    @Test
    void globalContaminationRisesOverTime() {
        ContaminationSystem system = new ContaminationSystem();
        system.tickGlobal(10f);
        assertTrue(system.getGlobalContaminationPct() > 0);
    }

    @Test
    void globalContaminationClampsAtMax() {
        ContaminationSystem system = new ContaminationSystem();
        system.tickGlobal(10_000f);
        assertEquals(100f, system.getGlobalContaminationPct());
        assertTrue(system.isGlobalContaminationLethal());
    }

    @Test
    void elricHasContaminationResistance() {
        ContaminationSystem system = new ContaminationSystem();
        Player elric = new Player("p1", CharacterType.ELRIC);
        Player jane = new Player("p2", CharacterType.JANE);

        system.tickPlayerInZone(elric, 1f);
        system.tickPlayerInZone(jane, 1f);

        assertTrue(elric.getPersonalContaminationPct() < jane.getPersonalContaminationPct(),
                "Elric's 30% passive resistance (PRD §4) should mean lower gain than Jane");
    }

    @Test
    void fullContaminationDrainsHp() {
        ContaminationSystem system = new ContaminationSystem();
        Player player = new Player("p1", CharacterType.JANE);
        player.addContamination(100f);

        float hpBefore = player.getHp();
        system.tickPlayerInZone(player, 1f);

        assertTrue(player.getHp() < hpBefore, "HP should drain once personal contamination is maxed");
    }

    @Test
    void sanitationCleansesContamination() {
        ContaminationSystem system = new ContaminationSystem();
        Player player = new Player("p1", CharacterType.ELRIC);
        player.addContamination(50f);

        system.cleanseAtSanitationStation(player);

        assertEquals(0f, player.getPersonalContaminationPct());
    }
}
