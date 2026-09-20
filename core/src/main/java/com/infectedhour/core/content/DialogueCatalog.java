package com.infectedhour.core.content;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Single source of truth for cinematic images, subtitles, and optional voice-over.
 *
 * <p>Every line represents one full-screen cinematic panel. Voice assets are
 * optional while the game is in development; {@code AudioDirector} safely
 * ignores a missing OGG file.</p>
 */
public final class DialogueCatalog {

    public enum Scene {
        INTRO,
        LEVEL_2_START,
        LEVEL_3_START,
        LEVEL_4_START,
        LEVEL_5_START,
        LEVEL_6_START,
        ENDING,
        BOSS
    }

    /** Asset paths are relative to the repo-root {@code assets/} directory. */
    public record Line(
            String id,
            String speaker,
            String title,
            String text,
            String voiceAsset,
            String imageAsset
    ) {
        public Line {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("Dialogue id is required");
            if (speaker == null || speaker.isBlank()) throw new IllegalArgumentException("Dialogue speaker is required");
            if (title == null || title.isBlank()) throw new IllegalArgumentException("Dialogue title is required");
            if (text == null || text.isBlank()) throw new IllegalArgumentException("Dialogue text is required");
            if (voiceAsset == null) throw new IllegalArgumentException("Voice asset path is required");
            if (imageAsset == null || imageAsset.isBlank()) throw new IllegalArgumentException("Image asset path is required");
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

    public static String[] panelText(Scene scene) {
        return lines(scene).stream().map(Line::text).toArray(String[]::new);
    }

    public static String[] panelTitles(Scene scene) {
        return lines(scene).stream().map(Line::title).toArray(String[]::new);
    }

    private static Map<Scene, List<Line>> buildLines() {
        EnumMap<Scene, List<Line>> result = new EnumMap<>(Scene.class);

        result.put(Scene.INTRO, List.of(
                line("intro-01", "NARRATOR", "ASHGROVE FIELD OPERATIVE",
                        "Elric was one of Ashgrove Corporation's highest-ranked field agents. "
                                + "When classified intelligence surfaced from St. Mercy Hospital, Ashgrove sent him to investigate.",
                        "audio/voice/intro_elric.ogg", "cinematics/intro_elric.png"),
                line("intro-02", "NARRATOR / JANE RECORDING", "THE MISSING AGENT",
                        "Three weeks earlier, Agent Jane disappeared during an operation inside the same facility.\n\n"
                                + "JANE: If anyone receives this transmission... the experiment is still alive. Do not trust the official report.",
                        "audio/voice/intro_jane.ogg", "cinematics/intro_jane.png"),
                line("intro-03", "NARRATOR / ELRIC", "ST. MERCY HOSPITAL",
                        "Jane's final signal led Elric to St. Mercy Hospital, a medical center connected to Ashgrove's biological research division. "
                                + "Somewhere inside, Jane was still alive.\n\nELRIC: Hold on, Jane. I'm coming.",
                        "audio/voice/map1_starting.ogg", "cinematics/map1_starting.png")
        ));

        result.put(Scene.LEVEL_2_START, List.of(
                line("level-2-start", "NARRATOR / JANE / ELRIC", "THE UPPER WING",
                        "Elric found Jane injured but alive and helped her recover. Together, they entered the hospital's upper wing.\n\n"
                                + "JANE: Laboratory assistants are trapped near the emergency entrance.\n"
                                + "ELRIC: Then we clear the building and bring them out.",
                        "audio/voice/map1_part2_starting.ogg", "cinematics/map1_part2_starting.png")
        ));

        result.put(Scene.LEVEL_3_START, List.of(
                line("level-2-end", "NARRATOR / LAB ASSISTANT / JANE", "ESCAPE FROM ST. MERCY",
                        "Elric and Jane reached the trapped laboratory assistants, but another horde was closing on the entrance.\n\n"
                                + "LAB ASSISTANT: The ambulance is still operational!\n"
                                + "JANE: Stay between us. Keep moving.",
                        "audio/voice/map1_part2_escaping.ogg", "cinematics/map1_part2_escaping.png"),
                line("level-3-start", "NARRATOR / ELRIC / JANE", "THE AMBULANCE ROUTE",
                        "The survivors reached the hospital grounds, but the route to the ambulance was surrounded.\n\n"
                                + "ELRIC: Protect the assistants. I'll clear a path.\n"
                                + "JANE: You're not doing it alone.",
                        "audio/voice/map2_part1_starting.ogg", "cinematics/map2_part1_starting.png")
        ));

        result.put(Scene.LEVEL_4_START, List.of(
                line("level-3-end", "NARRATOR / LAB ASSISTANT / JANE", "THE FIRST WITNESSES",
                        "With the hospital grounds secured, the laboratory assistants escaped in the ambulance.\n\n"
                                + "LAB ASSISTANT: The outbreak began at another facility hidden beyond the forest.\n"
                                + "JANE: Then that is where we find the cure.",
                        "audio/voice/map2_part1_ending.ogg", "cinematics/map2_part1_ending.png"),
                line("level-4-start", "NARRATOR / JANE / ELRIC", "THE ABANDONED DISTRICT",
                        "The ambulance could go no farther. Elric and Jane continued on foot through the infected hospital district.\n\n"
                                + "JANE: There are too many ahead.\n"
                                + "ELRIC: Then we remove them and keep moving.",
                        "audio/voice/map2_part2_starting.ogg", "cinematics/map2_part2_starting.png")
        ));

        result.put(Scene.LEVEL_5_START, List.of(
                line("level-4-end", "NARRATOR / JANE / ELRIC", "THE DAMAGED SECURITY KEY",
                        "Among the remains of an Ashgrove security officer, Elric found a damaged laboratory access card. Its encrypted coordinates were still readable.\n\n"
                                + "JANE: This belongs to the original research laboratory.\n"
                                + "ELRIC: Then that is our next destination.",
                        "audio/voice/map2_part2_ending.ogg", "cinematics/map2_part2_ending.png"),
                line("level-5-start", "NARRATOR / JANE / ELRIC", "THE HIDDEN RESEARCH FACILITY",
                        "The damaged card led them deep into the forest, where an abandoned Ashgrove research facility remained powered.\n\n"
                                + "JANE: The missing authorization data must be inside this perimeter.\n"
                                + "ELRIC: We recover it and open that door.",
                        "audio/voice/map3_starting.ogg", "cinematics/map3_starting.png")
        ));

        result.put(Scene.LEVEL_6_START, List.of(
                line("level-5-end", "NARRATOR / JANE / ELRIC", "FULL AUTHORIZATION RESTORED",
                        "After breaking through the infected perimeter, Elric and Jane restored the access card. It identified the facility as the origin of the St. Mercy outbreak.\n\n"
                                + "JANE: The card will open the laboratory.\n"
                                + "ELRIC: Then Ashgrove's experiment ends now.",
                        "audio/voice/map3_ending.ogg", "cinematics/map3_ending.png"),
                line("level-6-start", "NARRATOR / SCIENTIST / JANE", "THE FINAL EXPERIMENT",
                        "Inside, they found the lead scientist transforming into the final mutation.\n\n"
                                + "SCIENTIST: This virus was never a mistake. It was evolution.\n"
                                + "JANE: You destroyed St. Mercy for this.",
                        "audio/voice/map3_final_starting.ogg", "cinematics/map3_final_starting.png")
        ));

        result.put(Scene.ENDING, List.of(
                line("ending-01", "NARRATOR / JANE / ELRIC", "THE LAST CURE",
                        "After a brutal battle, the final mutation was defeated. Elric recovered the last completed dose of the antidote.\n\n"
                                + "JANE: The antidote survived.\n"
                                + "ELRIC: Then we recover its research data and destroy every remaining sample.",
                        "audio/voice/map3_final_ending1.ogg", "cinematics/map3_final_ending1.png"),
                line("ending-02", "COMPUTER / NARRATOR / JANE / ELRIC", "VIRUS PURGE COMPLETE",
                        "COMPUTER: Antidote data verified. Virus purge initiated. All active samples destroyed.\n\n"
                                + "NARRATOR: The St. Mercy outbreak was over, but the truth could no longer remain buried.\n"
                                + "JANE: It's over.\nELRIC: No. Now we find out who authorized it.",
                        "audio/voice/map3_final_ending2.ogg", "cinematics/map3_final_ending2.png")
        ));

        result.put(Scene.BOSS, List.of(
                line("boss-title", "NARRATOR", "THE FINAL MUTATION",
                        "The creator of the virus became its strongest victim.",
                        "audio/voice/boss_title.ogg", "boss_load.png")
        ));

        return Map.copyOf(result);
    }

    private static Line line(String id, String speaker, String title, String text,
                             String voiceAsset, String imageAsset) {
        return new Line(id, speaker, title, text, voiceAsset, imageAsset);
    }
}
