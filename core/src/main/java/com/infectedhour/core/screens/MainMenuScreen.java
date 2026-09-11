package com.infectedhour.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.infectedhour.core.InfectedHourGame;
import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.core.net.GameClient;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.dto.SaveSlotDto;
import com.infectedhour.shared.network.CharacterType;

import java.util.List;

/**
 * Native in-game Main Menu rendered directly inside the primary game window.
 * Eliminates multi-window spawning, ensuring Discord/OBS streaming and
 * screenshot tools capture the menu and gameplay seamlessly in 1 window.
 */
public class MainMenuScreen implements Screen {

    public static final float VIRTUAL_WIDTH = 1280f;
    public static final float VIRTUAL_HEIGHT = 720f;

    private enum MenuState {
        MAIN,
        CHARACTER_SELECT,
        LOAD_GAME,
        CONTROLS
    }

    private final InfectedHourGame game;
    private final GameClient client;
    private final GameBridge bridge;

    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    private ShapeRenderer shapes;

    private BitmapFont titleFont;
    private BitmapFont subtitleFont;
    private BitmapFont font;
    private BitmapFont buttonFont;
    private GlyphLayout layout;

    private Texture bgTexture;
    private Texture elricTexture;
    private Texture janeTexture;

    private MenuState state = MenuState.MAIN;
    private List<SaveSlotDto> saveSlots;

    public MainMenuScreen(InfectedHourGame game, GameClient client, GameBridge bridge) {
        this.game = game;
        this.client = client;
        this.bridge = bridge;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        camera.position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0);

        batch = new SpriteBatch();
        shapes = new ShapeRenderer();
        layout = new GlyphLayout();

        font = new BitmapFont();
        font.setColor(Color.WHITE);

        buttonFont = new BitmapFont();
        buttonFont.getData().setScale(1.2f);

        subtitleFont = new BitmapFont();
        subtitleFont.getData().setScale(1.3f);

        titleFont = new BitmapFont();
        titleFont.getData().setScale(2.5f);

        bgTexture = loadTextureSafely("bg_menu.png");
        if (bgTexture == null) {
            bgTexture = loadTextureSafely("map.png");
        }

        elricTexture = loadTextureSafely("male_character_select.jpg");
        janeTexture = loadTextureSafely("female_character_select.jpg");

        if (bridge != null) {
            saveSlots = bridge.getSaveSlots();
        }
    }

    @Override
    public void render(float delta) {
        game.stepSimulation(delta);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapes.setProjectionMatrix(camera.combined);

        Gdx.gl.glClearColor(0.055f, 0.078f, 0.125f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Vector3 mouse = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(mouse);
        boolean clicked = Gdx.input.isButtonJustPressed(Input.Buttons.LEFT);

        // ── 1. Background Art ──
        batch.begin();
        if (bgTexture != null) {
            batch.setColor(0.45f, 0.48f, 0.55f, 1f);
            batch.draw(bgTexture, 0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
            batch.setColor(Color.WHITE);
        }
        batch.end();

        // ── 2. Atmospheric Dark Overlay ──
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.04f, 0.06f, 0.10f, 0.82f);
        shapes.rect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // ── 3. State-Specific Content ──
        switch (state) {
            case MAIN -> renderMainMenu(mouse, clicked);
            case CHARACTER_SELECT -> renderCharacterSelect(mouse, clicked);
            case LOAD_GAME -> renderLoadGame(mouse, clicked);
            case CONTROLS -> renderControls(mouse, clicked);
        }
    }

    private void renderMainMenu(Vector3 mouse, boolean clicked) {
        float centerX = VIRTUAL_WIDTH / 2f;

        // Title Header
        batch.begin();
        titleFont.setColor(0.910f, 0.690f, 0.165f, 1f);
        layout.setText(titleFont, "THE INFECTED HOUR");
        titleFont.draw(batch, "THE INFECTED HOUR", centerX - (layout.width / 2f), VIRTUAL_HEIGHT - 90f);

        subtitleFont.setColor(0.80f, 0.85f, 0.90f, 0.9f);
        layout.setText(subtitleFont, "EPIDEMIC CONTAINMENT & SURVIVAL CAMPAIGN");
        subtitleFont.draw(batch, "EPIDEMIC CONTAINMENT & SURVIVAL CAMPAIGN", centerX - (layout.width / 2f), VIRTUAL_HEIGHT - 145f);
        batch.end();

        // Menu Buttons
        float btnW = 380f;
        float btnH = 54f;
        float startY = VIRTUAL_HEIGHT - 260f;
        float spacing = 72f;

        if (drawButton(centerX - btnW / 2f, startY, btnW, btnH, "1.  STORY MODE (NEW RUN)", mouse, clicked)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1)) {
            state = MenuState.CHARACTER_SELECT;
        }

        if (drawButton(centerX - btnW / 2f, startY - spacing, btnW, btnH, "2.  LOAD SAVED GAME", mouse, clicked)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_2)) {
            if (bridge != null) saveSlots = bridge.getSaveSlots();
            state = MenuState.LOAD_GAME;
        }

        if (drawButton(centerX - btnW / 2f, startY - spacing * 2, btnW, btnH, "3.  CONTROLS & GUIDE", mouse, clicked)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUM_3) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_3)) {
            state = MenuState.CONTROLS;
        }

        if (drawButton(centerX - btnW / 2f, startY - spacing * 3, btnW, btnH, "4.  QUIT GAME", mouse, clicked)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUM_4) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_4)) {
            Gdx.app.exit();
        }

        // Footer Info
        batch.begin();
        font.setColor(Color.GRAY);
        font.draw(batch, "Native Single-Window Mode  |  v1.0  |  Press [1-4] or Click to Select", 40f, 40f);
        batch.end();
    }

    private void renderCharacterSelect(Vector3 mouse, boolean clicked) {
        float centerX = VIRTUAL_WIDTH / 2f;

        batch.begin();
        titleFont.setColor(0.910f, 0.690f, 0.165f, 1f);
        layout.setText(titleFont, "CHOOSE YOUR SURVIVOR");
        titleFont.draw(batch, "CHOOSE YOUR SURVIVOR", centerX - (layout.width / 2f), VIRTUAL_HEIGHT - 60f);

        subtitleFont.setColor(0.78f, 0.82f, 0.88f, 0.9f);
        layout.setText(subtitleFont, "Select your operative for the containment mission.");
        subtitleFont.draw(batch, "Select your operative for the containment mission.", centerX - (layout.width / 2f), VIRTUAL_HEIGHT - 105f);
        batch.end();

        float cardW = 340f;
        float cardH = 460f;
        float cardY = 140f;
        float card1X = centerX - cardW - 35f;
        float card2X = centerX + 35f;

        // ── Elric Card ──
        boolean elricChosen = drawCharacterCard(
                card1X, cardY, cardW, cardH,
                "ELRIC",
                "ROLE: FIELD MEDIC",
                new Color(0.22f, 0.74f, 0.97f, 1f),
                "Bio-containment specialist. Immune response & healing surge.\nHigh toxin resistance and emergency medical kit.",
                elricTexture,
                "[ 1 ] SELECT ELRIC",
                mouse, clicked
        ) || Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1);

        if (elricChosen) {
            launchGameWithCharacter(CharacterType.ELRIC);
            return;
        }

        // ── Jane Card ──
        boolean janeChosen = drawCharacterCard(
                card2X, cardY, cardW, cardH,
                "JANE",
                "ROLE: LOCAL SCOUT",
                new Color(0.20f, 0.83f, 0.60f, 1f),
                "Veteran street survivor. High agility and rapid evasion.\nDeadly machete expertise and swift field maneuvers.",
                janeTexture,
                "[ 2 ] SELECT JANE",
                mouse, clicked
        ) || Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_2);

        if (janeChosen) {
            launchGameWithCharacter(CharacterType.JANE);
            return;
        }

        // Back Button
        float backW = 280f;
        float backH = 46f;
        if (drawButton(centerX - backW / 2f, 50f, backW, backH, "BACK TO MAIN MENU (ESC)", mouse, clicked)
                || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
            state = MenuState.MAIN;
        }
    }

    private boolean drawCharacterCard(float x, float y, float w, float h,
                                      String name, String role, Color roleColor,
                                      String description, Texture portrait,
                                      String buttonLabel, Vector3 mouse, boolean clicked) {
        boolean hovered = mouse.x >= x && mouse.x <= x + w && mouse.y >= y && mouse.y <= y + h;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(hovered ? new Color(0.12f, 0.18f, 0.28f, 0.95f) : new Color(0.08f, 0.12f, 0.19f, 0.92f));
        shapes.rect(x, y, w, h);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(hovered ? new Color(0.910f, 0.690f, 0.165f, 1f) : new Color(0.22f, 0.30f, 0.42f, 0.8f));
        shapes.rect(x, y, w, h);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        // Portrait Image
        if (portrait != null) {
            float imgW = 160f;
            float imgH = 220f;
            float imgX = x + (w - imgW) / 2f;
            float imgY = y + h - imgH - 18f;
            batch.draw(portrait, imgX, imgY, imgW, imgH);
        }

        // Name & Role
        subtitleFont.setColor(0.910f, 0.690f, 0.165f, 1f);
        subtitleFont.draw(batch, name, x, y + 190f, w, Align.center, false);

        font.setColor(roleColor);
        font.draw(batch, role, x, y + 162f, w, Align.center, false);

        font.setColor(0.80f, 0.84f, 0.90f, 1f);
        font.draw(batch, description, x + 16f, y + 130f, w - 32f, Align.center, true);
        batch.end();

        // Action Button
        float btnW = w - 40f;
        float btnH = 42f;
        float btnX = x + 20f;
        float btnY = y + 16f;
        return drawButton(btnX, btnY, btnW, btnH, buttonLabel, mouse, clicked);
    }

    private void renderLoadGame(Vector3 mouse, boolean clicked) {
        float centerX = VIRTUAL_WIDTH / 2f;

        batch.begin();
        titleFont.setColor(0.910f, 0.690f, 0.165f, 1f);
        layout.setText(titleFont, "LOAD SAVED RUN");
        titleFont.draw(batch, "LOAD SAVED RUN", centerX - (layout.width / 2f), VIRTUAL_HEIGHT - 60f);

        subtitleFont.setColor(0.78f, 0.82f, 0.88f, 0.9f);
        layout.setText(subtitleFont, "Select a checkpoint slot to resume.");
        subtitleFont.draw(batch, "Select a checkpoint slot to resume.", centerX - (layout.width / 2f), VIRTUAL_HEIGHT - 100f);
        batch.end();

        float cardW = 340f, cardH = 120f;
        float gapX = 40f, gapY = 22f;
        float totalGridW = 3 * cardW + 2 * gapX;
        float startX = (VIRTUAL_WIDTH - totalGridW) / 2f;
        float startY = VIRTUAL_HEIGHT - 145f;

        int selectedSlotIndex = -1;

        for (int i = 0; i < GameConstants.SAVE_SLOT_COUNT; i++) {
            int col = i % 3;
            int row = i / 3;
            float x = startX + col * (cardW + gapX);
            float y = startY - (row + 1) * cardH - row * gapY;

            boolean hovered = mouse.x >= x && mouse.x <= x + cardW && mouse.y >= y && mouse.y <= y + cardH;
            if (hovered && clicked) selectedSlotIndex = i;

            SaveSlotDto slot = (saveSlots != null && i < saveSlots.size()) ? saveSlots.get(i) : SaveSlotDto.empty(i + 1);

            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(hovered ? new Color(0.18f, 0.24f, 0.35f, 0.95f) : (slot != null && slot.occupied() ? new Color(0.1f, 0.14f, 0.22f, 0.9f) : new Color(0.07f, 0.09f, 0.14f, 0.85f)));
            shapes.rect(x, y, cardW, cardH);
            shapes.end();

            shapes.begin(ShapeRenderer.ShapeType.Line);
            shapes.setColor(hovered ? new Color(0.910f, 0.690f, 0.165f, 1f) : (slot != null && slot.occupied() ? new Color(0.3f, 0.45f, 0.65f, 0.8f) : new Color(0.2f, 0.25f, 0.35f, 0.5f)));
            shapes.rect(x, y, cardW, cardH);
            shapes.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);

            batch.begin();
            font.setColor(new Color(0.91f, 0.69f, 0.16f, 1f));
            font.draw(batch, "SLOT " + (i + 1), x + 14f, y + cardH - 14f);

            if (slot == null || !slot.occupied()) {
                font.setColor(Color.GRAY);
                font.draw(batch, "— Empty Slot —", x + 14f, y + cardH - 45f);
                font.setColor(Color.DARK_GRAY);
                font.draw(batch, "Start a new run to save here", x + 14f, y + 26f);
            } else {
                font.setColor(Color.WHITE);
                font.draw(batch, "Level " + slot.levelNumber() + (slot.levelName() == null ? "" : " — " + slot.levelName()), x + 14f, y + cardH - 38f);
                font.setColor(Color.LIGHT_GRAY);
                font.draw(batch, slot.checkpointName() == null ? "Checkpoint: —" : slot.checkpointName(), x + 14f, y + cardH - 60f);
                font.setColor(new Color(0.22f, 0.74f, 0.97f, 1f));
                font.draw(batch, String.format("%s   HP %.0f   [%s]", slot.formattedPlaytime(), slot.playerHp(), slot.characterType() == null ? "ELRIC" : slot.characterType()), x + 14f, y + 26f);
            }
            batch.end();
        }

        // Numeric Keypad
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1)) selectedSlotIndex = 0;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_2)) selectedSlotIndex = 1;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_3)) selectedSlotIndex = 2;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_4)) selectedSlotIndex = 3;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_5)) selectedSlotIndex = 4;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_6) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_6)) selectedSlotIndex = 5;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_7) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_7)) selectedSlotIndex = 6;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_8) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_8)) selectedSlotIndex = 7;
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_9) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_9)) selectedSlotIndex = 8;

        if (selectedSlotIndex >= 0 && saveSlots != null && selectedSlotIndex < saveSlots.size()) {
            SaveSlotDto slot = saveSlots.get(selectedSlotIndex);
            if (slot != null && slot.occupied()) {
                CharacterType c = "JANE".equalsIgnoreCase(slot.characterType()) ? CharacterType.JANE : CharacterType.ELRIC;
                if (game.getServer() != null) {
                    game.getServer().setHostCharacter(c);
                    game.getServer().restoreFrom(slot);
                }
                game.setScreen(new LevelBriefingScreen(game, client, bridge, slot.levelNumber()));
                return;
            }
        }

        // Back Button
        float backW = 280f;
        float backH = 44f;
        if (drawButton(centerX - backW / 2f, 32f, backW, backH, "BACK TO MAIN MENU (ESC)", mouse, clicked)
                || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            state = MenuState.MAIN;
        }
    }

    private void renderControls(Vector3 mouse, boolean clicked) {
        float centerX = VIRTUAL_WIDTH / 2f;

        batch.begin();
        titleFont.setColor(0.910f, 0.690f, 0.165f, 1f);
        layout.setText(titleFont, "CONTROLS & HOW TO PLAY");
        titleFont.draw(batch, "CONTROLS & HOW TO PLAY", centerX - (layout.width / 2f), VIRTUAL_HEIGHT - 70f);
        batch.end();

        float boxW = 680f;
        float boxH = 420f;
        float boxX = centerX - boxW / 2f;
        float boxY = 160f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.08f, 0.12f, 0.18f, 0.92f);
        shapes.rect(boxX, boxY, boxW, boxH);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(0.25f, 0.35f, 0.50f, 0.8f);
        shapes.rect(boxX, boxY, boxW, boxH);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        float textX = boxX + 40f;
        float textY = boxY + boxH - 40f;
        float lineGap = 42f;

        buttonFont.setColor(0.910f, 0.690f, 0.165f, 1f);
        buttonFont.draw(batch, "KEYBOARD & MOUSE CONTROLS:", textX, textY);

        font.setColor(Color.WHITE);
        font.draw(batch, "•  W / A / S / D          — Move Operative in 4 directions", textX, textY - lineGap);
        font.draw(batch, "•  LEFT SHIFT            — Sprint / Fast Evasion (Uses Stamina)", textX, textY - lineGap * 2);
        font.draw(batch, "•  SPACEBAR              — Primary Attack (Machete Slash or Bomb Throw)", textX, textY - lineGap * 3);
        font.draw(batch, "•  E                     — Interact with Doors, Terminals, Keys & Evacuation", textX, textY - lineGap * 4);
        font.draw(batch, "•  H  or  [3]            — Healing Surge (+35 HP & 15s immunity, 12s cooldown)", textX, textY - lineGap * 5);
        font.draw(batch, "•  ESC                   — Pause Game & Open Save Slots Overlay", textX, textY - lineGap * 6);
        font.draw(batch, "•  1 / 2 (Keys)          — Switch between Machete & Bomb in gameplay", textX, textY - lineGap * 7);
        batch.end();

        // Back Button
        float backW = 280f;
        float backH = 46f;
        if (drawButton(centerX - backW / 2f, 65f, backW, backH, "BACK TO MAIN MENU (ESC)", mouse, clicked)
                || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            state = MenuState.MAIN;
        }
    }

    private boolean drawButton(float x, float y, float w, float h, String text, Vector3 mouse, boolean clicked) {
        boolean hovered = mouse.x >= x && mouse.x <= x + w && mouse.y >= y && mouse.y <= y + h;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(hovered ? new Color(0.20f, 0.28f, 0.40f, 0.95f) : new Color(0.10f, 0.14f, 0.22f, 0.90f));
        shapes.rect(x, y, w, h);
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Line);
        shapes.setColor(hovered ? new Color(0.910f, 0.690f, 0.165f, 1f) : new Color(0.28f, 0.36f, 0.48f, 0.75f));
        shapes.rect(x, y, w, h);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();
        buttonFont.setColor(hovered ? new Color(0.910f, 0.690f, 0.165f, 1f) : Color.WHITE);
        layout.setText(buttonFont, text);
        buttonFont.draw(batch, text, x + (w - layout.width) / 2f, y + (h + layout.height) / 2f);
        batch.end();

        return hovered && clicked;
    }

    private void launchGameWithCharacter(CharacterType character) {
        if (game.getServer() != null) {
            game.getServer().setHostCharacter(character);
        }
        game.setScreen(new LevelBriefingScreen(game, client, bridge, 1));
    }

    private Texture loadTextureSafely(String path) {
        try {
            if (Gdx.files.internal(path).exists()) {
                Texture t = new Texture(Gdx.files.internal(path));
                t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                return t;
            }
        } catch (Exception ignored) { }
        return null;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override public void pause() { }
    @Override public void resume() { }
    @Override public void hide() { }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (shapes != null) shapes.dispose();
        if (font != null) font.dispose();
        if (buttonFont != null) buttonFont.dispose();
        if (subtitleFont != null) subtitleFont.dispose();
        if (titleFont != null) titleFont.dispose();
        if (bgTexture != null) bgTexture.dispose();
        if (elricTexture != null) elricTexture.dispose();
        if (janeTexture != null) janeTexture.dispose();
    }
}
