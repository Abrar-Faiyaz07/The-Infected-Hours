package com.infectedhour.core.screens;

import com.infectedhour.core.InfectedHourGame;
import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.core.net.GameClient;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.network.CharacterType;

/** Opens campaign gameplay directly, without the retired operative briefing card. */
public final class CampaignScreenRouter {

    private CampaignScreenRouter() {
    }

    public static void openLevel(InfectedHourGame game, GameClient client,
                                 GameBridge bridge, int levelNumber) {
        if (levelNumber == GameConstants.BOSS_LEVEL_NUMBER) {
            CharacterType preferred = game.getSession() == null
                    ? CharacterType.ELRIC
                    : game.getSession().preferredCharacter();
            game.setScreen(new BossScreen(game, client, bridge, preferred));
            return;
        }
        game.setScreen(new GameScreen(game, client, bridge, levelNumber));
    }
}
