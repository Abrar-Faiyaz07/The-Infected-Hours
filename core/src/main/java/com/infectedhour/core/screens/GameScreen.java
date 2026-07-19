package com.infectedhour.core.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.core.net.GameClient;
import com.infectedhour.core.systems.ContaminationSystem;
import com.infectedhour.core.systems.ObjectiveSystem;
import com.infectedhour.core.ui.Hud;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.network.InputCommand;

/**
 * Core gameplay loop screen (App Flow §2): explore -> objectives -> manage
 * contamination -> clear/fail. Named GameScreen per TRD §3's package layout
 * (not "GameplayScreen" — matches the doc exactly so file names line up
 * with what Claude Code will be told to open).
 */
public class GameScreen implements Screen {

    private final Game game;
    private final GameClient client;
    private final GameBridge bridge;
    private final int levelNumber;

    private final Hud hud = new Hud();
    private final ObjectiveSystem objectiveSystem = new ObjectiveSystem();
    private boolean paused = false;

    public GameScreen(Game game, GameClient client, GameBridge bridge, int levelNumber) {
        this.game = game;
        this.client = client;
        this.bridge = bridge;
        this.levelNumber = levelNumber;
    }

    @Override
    public void show() {
        // ================ TEAMMATE TASK: LEVEL SETUP ================
        // TODO(screens): load the level and create the world:
        //  1. var def = new LevelLoader().loadDefinition(levelNumber);
        //     levelLoader.loadMap(def);   // implement core/level first!
        //  2. Register objectives: def.objectives().forEach(o ->
        //         objectiveSystem.register(o.id(),
        //             ObjectiveSystem.ObjectiveType.valueOf(o.type()), o.target()));
        //  3. Create SpriteBatch, OrthographicCamera + FitViewport(1280,720),
        //     and an OrthogonalTiledMapRenderer for the map.
        //  4. Load the texture atlas for players/enemies from assets/
        //     (record every pack's license in ASSETS_CREDITS.md).
        // ============================================================
        hud.show();
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            paused = !paused;
        }

        Gdx.gl.glClearColor(0.05f, 0.06f, 0.08f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (!paused) {
            InputCommand input = readLocalInput();
            client.sendInputIfDue(input, delta);

            var snapshot = client.getInterpolatedSnapshot(System.currentTimeMillis());
            // ================ TEAMMATE TASK: WORLD RENDERING ================
            // TODO(screens): draw the world from the interpolated snapshot:
            //  1. mapRenderer.setView(camera); mapRenderer.render();
            //  2. batch.begin();
            //       - each snapshot.players entry at (x, y): pick Elric/Jane
            //         sprite by PlayerState.character, animate by movement.
            //       - each snapshot.enemies entry; villagers; pickups; stations.
            //     batch.end();
            //  3. Contamination overlay: every tile of every cloud gets a
            //     translucent toxic-purple (#7B4FA6) quad + subtle pulse.
            //  4. Camera follows the LOCAL player's snapshot position.
            //  Note: null snapshot before first packet arrives — guard it.
            // ================================================================

            checkWinLoseConditions();
        }

        hud.render(delta);
        // ============== TEAMMATE TASK: PAUSE OVERLAY ==============
        // TODO(screens): UI/UX doc par.2 screen 12.
        //  - Dim the world (translucent black quad), panel with: Resume,
        //    Settings (volume only), Abandon Match (host) / Leave (client).
        //  - Multiplayer: send EventMessage("PAUSE"/"RESUME") so the other
        //    screen shows "Host paused" — only the HOST truly pauses the sim.
        // ==========================================================
    }

    private InputCommand readLocalInput() {
        InputCommand input = new InputCommand();
        input.moveX = (Gdx.input.isKeyPressed(Input.Keys.D) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.A) ? 1 : 0);
        input.moveY = (Gdx.input.isKeyPressed(Input.Keys.W) ? 1 : 0) - (Gdx.input.isKeyPressed(Input.Keys.S) ? 1 : 0);
        input.attackPressed = Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
        input.interactHeld = Gdx.input.isKeyPressed(Input.Keys.E);
        input.interactPressed = Gdx.input.isKeyJustPressed(Input.Keys.E);
        input.abilityPressed = Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT);
        input.dropPressed = Gdx.input.isKeyJustPressed(Input.Keys.Q);
        return input;
    }

    private void checkWinLoseConditions() {
        // ============== TEAMMATE TASK: WIN / LOSE FLOW ==============
        // TODO(screens): App Flow par.2 — per-level end conditions:
        //  WIN:  objectiveSystem.areAllObjectivesComplete()
        //    -> Level Complete overlay (objective checklist + stats)
        //    -> host POSTs /matches/{id}/level-result (BackendClient)
        //    -> StoryPanelScreen (AFTER_LEVEL_1 / AFTER_LEVEL_2)
        //    -> next LevelBriefingScreen
        //  FAIL: global contamination lethal OR both players downed
        //    -> Level Failed overlay + LESSON TEXT explaining why
        //       (UX rule 4, e.g. "Barricades slow the spread") + Retry/Quit.
        //  Only the HOST decides win/lose; the client just reacts to the
        //  LevelTransition / EventMessage the host broadcasts.
        // ============================================================
    }

    @Override public void resize(int width, int height) { hud.resize(width, height); }
    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        hud.dispose();
    }
}
