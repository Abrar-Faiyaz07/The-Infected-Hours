package com.infectedhour.core.content;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Single source of truth for story dialogue and its future voice-over files.
 *
 * <p>Text and audio are deliberately data-only. Screens decide when a line is
 * shown; {@link com.infectedhour.core.audio.AudioDirector} decides whether a
 * referenced voice file is currently available. This lets writers change copy
 * without editing rendering code and lets artists/audio teammates add files
 * without touching the story flow.</p>
 */
public final class DialogueCatalog {

    public enum Scene {
        INTRO,
        AFTER_LEVEL_1,
        AFTER_LEVEL_2,
        ENDING,
        BOSS
    }

    // TODO(dialogue): Add future scenes here first, then add their lines in
    // buildLines() and matching titles in panelTitles(). Keep scene names
    // stable once dialogue is recorded so save/replay data remains compatible.

    /** A single panel/line of dialogue. Voice files are relative to assets/. */
    public record Line(String id, String speaker, String text, String voiceAsset) {
        public Line {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("Dialogue id is required");
            if (speaker == null || speaker.isBlank()) throw new IllegalArgumentException("Dialogue speaker is required");
            if (text == null) throw new IllegalArgumentException("Dialogue text is required");
            if (voiceAsset == null) throw new IllegalArgumentException("Voice asset path is required");
        }
    }

    private static final Map<Scene, List<Line>> LINES = buildLines();

    private DialogueCatalog() {
    }

    public static List<Line> lines(Scene scene) {
        List<Line> result = LINES.get(scene);
        if (result == null) throw new IllegalArgumentException("Unknown dialogue scene: " + scene);
        return result;
    }

    public static Line line(Scene scene, int panelIndex) {
        List<Line> sceneLines = lines(scene);
        if (panelIndex < 0 || panelIndex >= sceneLines.size()) {
            throw new IndexOutOfBoundsException("Dialogue panel " + panelIndex + " is outside " + scene);
        }
        return sceneLines.get(panelIndex);
    }

    /** Returns a fresh array so a screen cannot mutate the catalog. */
    public static String[] panelText(Scene scene) {
        return lines(scene).stream().map(Line::text).toArray(String[]::new);
    }

    /** Titles stay separate from body copy so UI layout can change independently. */
    public static String[] panelTitles(Scene scene) {
        return switch (scene) {
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
                    "RESEARCH CELL ZERO: ELENA VANCE"
            };
            case BOSS -> new String[]{"THE VIRUS HEART"};
        };
    }

    private static Map<Scene, List<Line>> buildLines() {
        EnumMap<Scene, List<Line>> result = new EnumMap<>(Scene.class);

        // TODO(dialogue): Add new lines with this pattern:
        // line("stable-id", "Speaker", "Subtitle text", "audio/voice/file.ogg")
        // Keep one line per panel and place the matching .ogg under
        // assets/audio/voice/. Do not move story text into screen classes.
        result.put(Scene.INTRO, List.of(
                line("intro-01", "Narrator",
                        "Elric arrives in Ashgrove as an elite bio-containment operative for the private Oscorp Organization. A weaponized pathogen -- engineered inside Oscorp's black-budget research laboratories -- was stolen by a rogue insider and released into the civilian population.",
                        "audio/voice/intro_01.ogg"),
                line("intro-02", "Narrator",
                        "The contagion breached containment at midnight. Oscorp's directive is uncompromising: destroy the viral core at all costs before dawn, or the entire regional population will transform into ravenous mutated infected.",
                        "audio/voice/intro_02.ogg"),
                line("intro-03", "Narrator",
                        "Inside Ashgrove Hospital, an operative named Jane lies senseless. Unbeknownst to Oscorp, Jane is an undercover intelligence agent deployed by the government to monitor Oscorp's illegal bioweapon testing. Surviving the night requires an uneasy alliance.",
                        "audio/voice/intro_03.ogg")
        ));
        result.put(Scene.AFTER_LEVEL_1, List.of(
                line("after-level-1-01", "Narrator",
                        "Jane has been revived and the stranded hospital villagers guided to safety. Jane confirms the terrible truth: this is no ordinary virus -- it is an engineered extinction weapon that induces hyper-aggressive cellular mutations.",
                        "audio/voice/after_level_1_01.ogg"),
                line("after-level-1-02", "Jane",
                        "The road ahead cuts through the heart of the overrun village. Another wounded agent and three civilians are trapped near the municipal power relay. Secure the relay grid to reach the subterranean facility.",
                        "audio/voice/after_level_1_02.ogg")
        ));
        result.put(Scene.AFTER_LEVEL_2, List.of(
                line("after-level-2-01", "Narrator",
                        "With the wounded field agent and three villagers rescued from the ruins, the roadside power relays hum to life, unlocking the blast doors of the subterranean drainage corridor.",
                        "audio/voice/after_level_2_01.ogg"),
                line("after-level-2-02", "Narrator",
                        "Directly beneath Ashgrove lies Oscorp's covert biological research laboratory. In the deepest containment vault waits the primary bio-organism: the mutated Virus Heart that controls the outbreak.",
                        "audio/voice/after_level_2_02.ogg")
        ));
        result.put(Scene.ENDING, List.of(
                line("ending-01", "Narrator",
                        "The gargantuan Virus Heart shudders and collapses into smoldering biological embers. In the shattered containment chamber, Elric recovers an emergency keycard and the sole remaining vial of the synthesized Prototype Antidote.",
                        "audio/voice/ending_01.ogg"),
                line("ending-02", "Narrator",
                        "Elric unlocks the sealed observation cell at the back of the lab. Behind the shattered glass lies Elena Vance -- his closest friend and lead biochemist. With the facility neutralized and the synthesized antidote in hand, the team secures the survivors and signals emergency evacuation. Ashgrove is safe.",
                        "audio/voice/ending_02.ogg")
                /*
                // Temporarily commented out: friendship vs humanity choice & Jane duel
                line("ending-03", "Narrator",
                        "There is only ONE vial of the Antidote. Two irreconcilable choices stand before Elric:\n\n[1] SAVE ELENA -- Administer the antidote immediately to save your dearest friend.\n\n[2] SECURE FOR OSCORP -- Sacrifice Elena and deliver the antidote to Oscorp Corporation to synthesize a cure for humanity.\n\nPress [1] or [2] to decide.",
                        "audio/voice/ending_03.ogg"),
                line("ending-04", "Elric",
                        "Elric presses the injector into Elena's trembling arm. The mutagenic seizure subsides, and her fever breaks. For one tender moment, Elena opens her eyes and whispers: 'Thank you, Elric... you came for me.' She smiles softly and passes away peacefully in his arms, spared from becoming a monster. No combat takes place. Ashgrove is silent at last.",
                        "audio/voice/ending_04.ogg"),
                line("ending-05", "Jane",
                        "Elric turns away and locks the antidote canister into his tactical harness for Oscorp transport. Behind him, the unsheathing of a katana echoes. Agent Jane stands in the doorway, her federal intelligence badge gleaming.\n\nJane: 'Oscorp created this plague, Elric! I cannot let you deliver that weapon back to the corporate board. Hand over the antidote, or neither of us walks out of here alive!'",
                        "audio/voice/ending_05.ogg")
                */
        ));
        result.put(Scene.BOSS, List.of(
                line("boss-title", "Narrator",
                        "The source of the Ashgrove outbreak is awake.",
                        "audio/voice/boss_title.ogg")
        ));
        return Map.copyOf(result);
    }

    private static Line line(String id, String speaker, String text, String voiceAsset) {
        return new Line(id, speaker, text, voiceAsset);
    }
}
