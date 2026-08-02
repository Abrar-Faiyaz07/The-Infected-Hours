package com.infectedhour.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.infectedhour.core.InfectedHourGame;
import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.core.net.GameClient;
import com.infectedhour.core.systems.BossPhaseSystem;
import com.infectedhour.core.ui.Hud;

/**
 * Level 3: The Virus Heart (PRD §8). Shows the Boss Title Card first
 * (UI/UX doc §2 screen 15 — static card + roar SFX, no cinematic per
 * [STRETCH] cut), then runs the same gameplay loop as GameScreen driven
 * by BossPhaseSystem instead of a normal objective set.
 */
public class BossScreen implements Screen {

    private final InfectedHourGame game;
    private final GameClient client;
    private final GameBridge bridge;

    private final BossPhaseSystem bossPhaseSystem = new BossPhaseSystem();
    private final Hud hud = new Hud();
    private boolean titleCardDismissed = false;

    public BossScreen(InfectedHourGame game, GameClient client, GameBridge bridge) {
        this.game = game;
        this.client = client;
        this.bridge = bridge;
    }

    @Override
    public void show() {
        // ============ TEAMMATE TASK: BOSS TITLE CARD ============
        // TODO(boss): UI/UX doc par.2 screen 15 — static card, no cinematic.
        //  - Full-screen dark panel: "THE VIRUS HEART" in the pixel display
        //    font, accent-red (#E85A4F); play the boss roar .ogg once.
        //  - E / any key -> titleCardDismissed = true (see below).
        // ========================================================
        hud.show();
    }

    @Override
    public void render(float delta) {
        // The boss arena is still host-authoritative — keep the sim stepping.
        game.stepSimulation(delta);

        Gdx.gl.glClearColor(0.05f, 0.02f, 0.03f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (!titleCardDismissed) {
            if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.E)
                    || Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE)) {
                titleCardDismissed = true;
            }
            // TODO(boss): draw the title card and start the arena music stem.
            // Keep the ready-gate: BOTH players must dismiss (same EventMessage
            // pattern as LevelBriefingScreen).
            return;
        }

        switch (bossPhaseSystem.getCurrentPhase()) {
            case SHIELD -> {
                // TODO(boss) PHASE 1 — Shield (PRD par.8):
                //  - Render 3 injection points; Elric hold-interacts (E) at
                //    each to place a cure sample -> a 3-target objective.
                //  - Spawn mutation swarms for Jane to fight off.
                //  - All 3 placed -> bossPhaseSystem.onShieldWeakened()
            }
            case EXPOSURE -> {
                // TODO(boss) PHASE 2 — Exposure window (15s):
                //  - Show the countdown + the exposed core (hurt-able hitbox).
                //  - Sum both players' damage this frame and call
                //    bossPhaseSystem.tickExposure(delta, damageDealtPct);
                //  - Draw the core HP bar (accent-red) at the top.
            }
            case CORE_DESTRUCTION -> {
                // TODO(boss) PHASE 3 — Simultaneous-strike QTE:
                //  - Prompt BOTH screens: "[Hold E] TOGETHER!" with a ring.
                //  - Track each player's held state via EventMessage; when
                //    the host sees both held within the same ~1s window:
                //    bossPhaseSystem.resolveCoreDestructionAttempt(true, true)
                //  - Meanwhile the arena floods with infection waves.
            }
            case DEFEATED -> {
                bridge.notifyMatchEnded(new GameBridge.MatchOutcome("VICTORY", 3));
            }
        }

        hud.render(delta);
    }

    @Override public void resize(int width, int height) { hud.resize(width, height); }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }
    @Override public void dispose() { hud.dispose(); }
}
