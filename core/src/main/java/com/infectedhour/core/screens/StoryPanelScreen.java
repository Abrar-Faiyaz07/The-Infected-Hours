package com.infectedhour.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.infectedhour.core.InfectedHourGame;
import com.infectedhour.core.bridge.GameBridge;
import com.infectedhour.core.net.GameClient;
import com.infectedhour.shared.constants.GameConstants;
import com.infectedhour.shared.network.MatchMode;

/**
 * Full-screen illustrated story panels (UI/UX doc §6). Advances only when
 * BOTH players press E (UX rule 5) with a 20s auto-ready fallback — mirrors
 * the LevelBriefingScreen ready-gate pattern, and for the same reason: a
 * co-op story beat that advances on one screen but not the other desyncs the
 * whole session.
 */
public class StoryPanelScreen implements Screen {

    public enum Sequence {
        INTRO, AFTER_LEVEL_1, AFTER_LEVEL_2, ENDING
    }

    private static final String STORY_ADVANCE_EVENT = "STORY_ADVANCE";

    private final InfectedHourGame game;
    private final GameClient client;
    private final GameBridge bridge;
    private final Sequence sequence;
    private final int justCompletedOrUpcomingLevel;

    private SpriteBatch batch;
    private BitmapFont font;
    private BitmapFont titleFont;
    private Texture storyBackground;
    private Texture overlayPixel;

    private String[] panels;
    private String[] panelTitles;
    private int currentPanelIndex = 0;
    private float typewriterElapsed = 0f;
    private float panelElapsed = 0f;
    private boolean localAdvanceRequested = false;
    private volatile boolean partnerAdvanceRequested = false;

    private static final float TYPEWRITER_CHARS_PER_SEC = 40f; // UI/UX doc §6
    private static final float AUTO_READY_FALLBACK_SECONDS = 20f;

    private int moralChoice = 0; // 0 = undecided, 1 = save Elena, 2 = deliver to Oscorp

    public StoryPanelScreen(InfectedHourGame game, GameClient client, GameBridge bridge,
                            Sequence sequence, int levelContext) {
        this.game = game;
        this.client = client;
        this.bridge = bridge;
        this.sequence = sequence;
        this.justCompletedOrUpcomingLevel = levelContext;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.15f);
        titleFont = new BitmapFont();
        titleFont.getData().setScale(2.05f);
        panels = panelsFor(sequence);
        panelTitles = panelTitlesFor(sequence);

        if (Gdx.files.internal("story_intro.png").exists()) {
            storyBackground = new Texture(Gdx.files.internal("story_intro.png"));
            storyBackground.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }

        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        overlayPixel = new Texture(pixel);
        pixel.dispose();

        client.setOnEvent(event -> {
            if (STORY_ADVANCE_EVENT.equals(event.type)) {
                partnerAdvanceRequested = true;
            } else if ("STORY_CHOICE".equals(event.type)) {
                try {
                    moralChoice = Integer.parseInt(event.payload);
                } catch (Exception ignored) { }
            }
        });
    }

    @Override
    public void render(float delta) {
        game.stepSimulation(delta);

        typewriterElapsed += delta;
        panelElapsed += delta;

        // Friendship / Humanity choice temporarily commented out
        boolean isEndingChoicePanel = false;
        /*
        boolean isEndingChoicePanel = (sequence == Sequence.ENDING && currentPanelIndex == 2);

        if (isEndingChoicePanel && moralChoice == 0) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1)) {
                moralChoice = 1;
                client.sendEvent("STORY_CHOICE", "1");
                nextPanel();
                return;
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_2)) {
                moralChoice = 2;
                client.sendEvent("STORY_CHOICE", "2");
                nextPanel();
                return;
            }
        }
        */

        String text = panels[currentPanelIndex];
        int visibleChars = Math.min(text.length(), (int) (typewriterElapsed * TYPEWRITER_CHARS_PER_SEC));
        boolean fullyRevealed = visibleChars >= text.length();

        if (!isEndingChoicePanel && (Gdx.input.isKeyJustPressed(Input.Keys.E)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isButtonJustPressed(Input.Buttons.LEFT))) {
            if (!fullyRevealed) {
                typewriterElapsed = text.length() / TYPEWRITER_CHARS_PER_SEC + 1f; // reveal the rest instantly
            } else if (!localAdvanceRequested) {
                localAdvanceRequested = true;
                client.sendEvent(STORY_ADVANCE_EVENT, sequence.name() + ":" + currentPanelIndex);
            }
        }

        // Nobody is held hostage by a partner who walked away (UX rule 5).
        boolean partnerOk = partnerAdvanceRequested
                || panelElapsed >= AUTO_READY_FALLBACK_SECONDS
                || client.getMatchMode() == MatchMode.SOLO
                || (game.getSession() != null && game.getSession().debugSplitScreen())
                || (game.getServer() != null && game.getServer().getConnectedPlayerCount() <= 1);

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        float marginX = Math.max(54f, screenWidth * 0.055f);
        float contentWidth = Math.min(720f, screenWidth * 0.42f);
        float top = screenHeight - Math.max(64f, screenHeight * 0.075f);

        batch.begin();
        if (storyBackground != null) {
            batch.setColor(Color.WHITE);
            batch.draw(storyBackground, 0f, 0f, screenWidth, screenHeight);
        }

        // 1. Cinematic gradient blend from left to right (eliminates harsh 50/50 vertical split)
        batch.setColor(0.01f, 0.02f, 0.035f, 0.25f);
        batch.draw(overlayPixel, 0f, 0f, screenWidth, screenHeight);

        float overlayWidth = Math.min(screenWidth * 0.58f, 980f);
        int gradientSlices = 36;
        float sliceW = overlayWidth / gradientSlices;
        for (int i = 0; i < gradientSlices; i++) {
            float t = (float) i / gradientSlices;
            float alpha = 0.88f * (1.0f - (float) Math.pow(t, 1.55));
            batch.setColor(0.012f, 0.022f, 0.038f, alpha);
            batch.draw(overlayPixel, i * sliceW, 0f, sliceW + 1f, screenHeight);
        }

        // 2. Tactical Briefing Card Background & Framing
        float cardX = marginX - 20f;
        float cardY = 88f;
        float cardW = contentWidth + 40f;
        float cardH = (top - cardY) + 20f;

        // Semi-transparent acrylic glass
        batch.setColor(0.018f, 0.028f, 0.045f, 0.65f);
        batch.draw(overlayPixel, cardX, cardY, cardW, cardH);

        // Muted Card Border
        batch.setColor(0.18f, 0.24f, 0.32f, 0.70f);
        batch.draw(overlayPixel, cardX, cardY, cardW, 1f);
        batch.draw(overlayPixel, cardX, cardY + cardH, cardW, 1f);
        batch.draw(overlayPixel, cardX, cardY, 1f, cardH);
        batch.draw(overlayPixel, cardX + cardW, cardY, 1f, cardH);

        // Tactical Corner Brackets (Biohazard Amber / Gold)
        batch.setColor(0.910f, 0.690f, 0.165f, 1f);
        float bLen = 16f;
        float bThick = 2f;
        // Top-Left
        batch.draw(overlayPixel, cardX, cardY + cardH - bThick, bLen, bThick);
        batch.draw(overlayPixel, cardX, cardY + cardH - bLen, bThick, bLen);
        // Top-Right
        batch.draw(overlayPixel, cardX + cardW - bLen, cardY + cardH - bThick, bLen, bThick);
        batch.draw(overlayPixel, cardX + cardW - bThick, cardY + cardH - bLen, bThick, bLen);
        // Bottom-Left
        batch.draw(overlayPixel, cardX, cardY, bLen, bThick);
        batch.draw(overlayPixel, cardX, cardY, bThick, bLen);
        // Bottom-Right
        batch.draw(overlayPixel, cardX + cardW - bLen, cardY, bLen, bThick);
        batch.draw(overlayPixel, cardX + cardW - bThick, cardY, bThick, bLen);

        // 3. Header & Classification Stamps
        batch.setColor(0.910f, 0.690f, 0.165f, 1f);
        batch.draw(overlayPixel, marginX, top - 30f, 120f, 2f);
        batch.setColor(Color.WHITE);

        font.setColor(0.910f, 0.690f, 0.165f, 1f);
        font.draw(batch, "[ // " + sequenceLabel(sequence) + " // ]", marginX, top);

        font.setColor(0.55f, 0.62f, 0.72f, 0.9f);
        font.draw(batch, "SECURITY CLEARANCE: LEVEL-4 RESTRICTED", marginX + contentWidth - 280f, top);

        titleFont.setColor(Color.WHITE);
        titleFont.draw(batch, panelTitles[currentPanelIndex], marginX, top - 52f,
                contentWidth, Align.left, true);

        // 4. Body Copy (Sanitized against missing glyphs)
        String sanitizedText = sanitize(text);
        int safeChars = Math.min(sanitizedText.length(), visibleChars);
        font.setColor(0.88f, 0.91f, 0.94f, 1f);
        font.draw(batch, sanitizedText.substring(0, safeChars), marginX, top - 138f,
                contentWidth, Align.left, true);

        // 5. Interactive Footer: Stylized Keycap Badge & Segmented Progress
        if (isEndingChoicePanel) {
            titleFont.setColor(0.910f, 0.690f, 0.165f, 1f);
            font.setColor(0.88f, 0.90f, 0.92f, 1f);
            font.draw(batch, "[1] Save Elena (Friendship)   |   [2] Deliver to Oscorp (Humanity)", marginX, 56f);
        } else {
            // Keycap button badge for [ E ]
            float keyBadgeX = marginX;
            float keyBadgeY = 40f;
            float keyBadgeW = 28f;
            float keyBadgeH = 26f;

            batch.setColor(0.12f, 0.16f, 0.24f, 0.95f);
            batch.draw(overlayPixel, keyBadgeX, keyBadgeY, keyBadgeW, keyBadgeH);
            batch.setColor(0.45f, 0.55f, 0.70f, 0.9f);
            batch.draw(overlayPixel, keyBadgeX, keyBadgeY, keyBadgeW, 1f);
            batch.draw(overlayPixel, keyBadgeX, keyBadgeY + keyBadgeH, keyBadgeW, 1f);
            batch.draw(overlayPixel, keyBadgeX, keyBadgeY, 1f, keyBadgeH);
            batch.draw(overlayPixel, keyBadgeX + keyBadgeW, keyBadgeY, 1f, keyBadgeH);
            batch.setColor(Color.WHITE);

            font.setColor(Color.WHITE);
            font.draw(batch, "E", keyBadgeX + 9f, keyBadgeY + 18f);

            font.setColor(0.72f, 0.78f, 0.86f, 1f);
            String promptText = !fullyRevealed ? "REVEAL ALL" : (!localAdvanceRequested ? "CONTINUE DIRECTIVE" : (partnerOk ? "PROCEEDING..." : "WAITING FOR PARTNER..."));
            font.draw(batch, promptText, keyBadgeX + keyBadgeW + 12f, keyBadgeY + 18f);
        }

        // Segmented Progress Pip: [ ■ ■ □ ] PAGE 01 / 03
        StringBuilder pips = new StringBuilder();
        for (int i = 0; i < panels.length; i++) {
            pips.append(i <= currentPanelIndex ? "■ " : "□ ");
        }
        font.setColor(0.60f, 0.68f, 0.78f, 1f);
        font.draw(batch, String.format("[ %s]   PAGE %02d / %02d", pips.toString(), currentPanelIndex + 1, panels.length),
                marginX + contentWidth - 170f, 56f);
        batch.end();

        if (localAdvanceRequested && partnerOk) {
            nextPanel();
        }
    }

    private static String sanitize(String text) {
        if (text == null) return "";
        return text.replace("—", " -- ")
                .replace("–", " - ")
                .replace("…", "...")
                .replace("“", "\"")
                .replace("”", "\"")
                .replace("‘", "'")
                .replace("’", "'");
    }

    private static String sequenceLabel(Sequence sequence) {
        return switch (sequence) {
            case INTRO -> "OPERATION ASHGROVE: OSCORP BIO-CONTAINMENT";
            case AFTER_LEVEL_1 -> "FIELD REPORT 01: THE EVACUATION ROUTE";
            case AFTER_LEVEL_2 -> "FIELD REPORT 02: THE SUBTERRANEAN BREACH";
            case ENDING -> "FINAL REPORT: PROJECT EXTINCTION";
        };
    }

    private static String[] panelTitlesFor(Sequence sequence) {
        return switch (sequence) {
            case INTRO -> new String[]{
                    "OSCORP BIO-CONTAINMENT DIRECTIVE",
                    "A WEAPONIZED PATHOGEN LOOSE",
                    "THE UNDERCOVER OPERATIVE"
            };
            case AFTER_LEVEL_1 -> new String[]{
                    "BEYOND ASHGROVE HOSPITAL",
                    "THE ROAD TO THE OUTPOST"
            };
            case AFTER_LEVEL_2 -> new String[]{
                    "THE UNDERGROUND DRAINAGE TUNNEL",
                    "OSCORP SECRET FACILITY ZERO"
            };
            case ENDING -> new String[]{
                    "THE VIRUS HEART FALLS SILENT",
                    "RESEARCH CELL ZERO: ELENA VANCE",
                    "THE MOMENT OF TRUTH: MORAL CHOICE",
                    "A PEACEFUL FAREWELL",
                    "JANE'S INTERVENTION: CLIMAX DUEL"
            };
        };
    }

    private String footerHint(boolean fullyRevealed, boolean partnerOk) {
        if (!fullyRevealed) {
            return "[E] skip text";
        }
        if (!localAdvanceRequested) {
            return "[E] continue   (" + (currentPanelIndex + 1) + "/" + panels.length + ")";
        }
        return partnerOk ? "" : "Waiting for your partner...";
    }

    private void nextPanel() {
        localAdvanceRequested = false;
        partnerAdvanceRequested = false;
        typewriterElapsed = 0f;
        panelElapsed = 0f;

        /*
        // Moral choice branching and Jane duel transition temporarily commented out
        if (sequence == Sequence.ENDING) {
            if (currentPanelIndex == 2) {
                // After choice panel
                if (moralChoice == 1) {
                    currentPanelIndex = 3; // Peaceful farewell panel
                    return;
                } else if (moralChoice == 2) {
                    currentPanelIndex = 4; // Jane confrontation panel
                    return;
                }
            } else if (currentPanelIndex == 3) {
                // Choice 1 completed: Elena passes peacefully, victory!
                bridge.notifyMatchEnded(new GameBridge.MatchOutcome("VICTORY", GameConstants.BOSS_LEVEL_NUMBER));
                if (bridge.hasLauncher()) {
                    bridge.requestReturnToLauncher(() -> Gdx.app.postRunnable(Gdx.app::exit));
                } else {
                    game.setScreen(new MainMenuScreen(game, client, bridge));
                }
                return;
            } else if (currentPanelIndex == 4) {
                // Choice 2: Transition to boss duel against Agent Jane!
                game.setScreen(new JaneDuelScreen(game, client, bridge));
                return;
            }
        }
        */

        currentPanelIndex++;

        if (currentPanelIndex < panels.length) {
            return;
        }

        if (sequence == Sequence.ENDING) {
            bridge.notifyMatchEnded(new GameBridge.MatchOutcome("VICTORY", GameConstants.BOSS_LEVEL_NUMBER));
            if (bridge.hasLauncher()) {
                bridge.requestReturnToLauncher(() -> Gdx.app.postRunnable(Gdx.app::exit));
            } else {
                game.setScreen(new MainMenuScreen(game, client, bridge));
            }
            return;
        }
        if (sequence == Sequence.INTRO) {
            game.setScreen(new GameScreen(game, client, bridge, justCompletedOrUpcomingLevel));
        } else {
            game.setScreen(new LevelBriefingScreen(game, client, bridge, justCompletedOrUpcomingLevel + 1));
        }
    }

    private static String[] panelsFor(Sequence sequence) {
        return switch (sequence) {
            case INTRO -> new String[]{
                    "Elric arrives in Ashgrove as an elite bio-containment operative for the private Oscorp Organization. A weaponized pathogen -- engineered inside Oscorp's black-budget research laboratories -- was stolen by a rogue insider and released into the civilian population.",
                    "The contagion breached containment at midnight. Oscorp's directive is uncompromising: destroy the viral core at all costs before dawn, or the entire regional population will transform into ravenous mutated infected.",
                    "Inside Ashgrove Hospital, an operative named Jane lies senseless. Unbeknownst to Oscorp, Jane is an undercover intelligence agent deployed by the government to monitor Oscorp's illegal bioweapon testing. Surviving the night requires an uneasy alliance."
            };
            case AFTER_LEVEL_1 -> new String[]{
                    "Jane has been revived and the stranded hospital villagers guided to safety. Jane confirms the terrible truth: this is no ordinary virus -- it is an engineered extinction weapon that induces hyper-aggressive cellular mutations.",
                    "The road ahead cuts through the heart of the overrun village. Jane warns that another wounded agent and three civilians are trapped near the municipal power relay. They must secure the relay grid to reach the subterranean facility."
            };
            case AFTER_LEVEL_2 -> new String[]{
                    "With the wounded field agent and three villagers rescued from the ruins, the roadside power relays hum to life, unlocking the blast doors of the subterranean drainage corridor.",
                    "Directly beneath Ashgrove lies Oscorp's covert biological research laboratory. In the deepest containment vault waits the primary bio-organism: the mutated Virus Heart that controls the outbreak."
            };
            case ENDING -> new String[]{
                    "The gargantuan Virus Heart shudders and collapses into smoldering biological embers. In the shattered containment chamber, Elric recovers an emergency keycard and the sole remaining vial of the synthesized Prototype Antidote.",
                    "Elric unlocks the sealed observation cell at the back of the lab. Behind the shattered glass lies Elena Vance -- his closest friend and lead biochemist, who disappeared two months ago investigating Oscorp's weaponization program. Elena is infected, slipping into cellular necrosis.",
                    "There is only ONE vial of the Antidote. Two irreconcilable choices stand before Elric:\n\n[1] SAVE ELENA -- Administer the antidote immediately to save your dearest friend.\n\n[2] SECURE FOR OSCORP -- Sacrifice Elena and deliver the antidote to Oscorp Corporation to synthesize a cure for humanity.\n\nPress [1] or [2] to decide.",
                    "Elric presses the injector into Elena's trembling arm. The mutagenic seizure subsides, and her fever breaks. For one tender moment, Elena opens her eyes and whispers: 'Thank you, Elric... you came for me.' She smiles softly and passes away peacefully in his arms, spared from becoming a monster. No combat takes place. Ashgrove is silent at last.",
                    "Elric turns away and locks the antidote canister into his tactical harness for Oscorp transport. Behind him, the unsheathing of a katana echoes. Agent Jane stands in the doorway, her federal intelligence badge gleaming.\n\nJane: 'Oscorp created this plague, Elric! I can't let you deliver that weapon back to the corporate board. Hand over the antidote, or neither of us walks out of here alive!'"
            };
        };
    }

    @Override public void resize(int width, int height) { }
    @Override public void pause() { }
    @Override public void resume() { }

    @Override
    public void hide() {
        client.setOnEvent(null);
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
        if (titleFont != null) titleFont.dispose();
        if (storyBackground != null) storyBackground.dispose();
        if (overlayPixel != null) overlayPixel.dispose();
    }
}
