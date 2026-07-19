package com.infectedhour.core.level;

/**
 * Wraps libGDX's TmxMapLoader to load a level's .tmx and populate the
 * screen's entities/collision from it (TRD §1 Maps row, §4 Collision row).
 */
public class LevelLoader {

    public LevelDefinition loadDefinition(int levelNumber) {
        return switch (levelNumber) {
            case 1 -> LevelDefinition.level1();
            case 2 -> LevelDefinition.level2();
            case 3 -> LevelDefinition.level3Boss();
            default -> throw new IllegalArgumentException("Unknown level: " + levelNumber);
        };
    }

    /**
     * TEAMMATE TASK: TILED MAP LOADING — this is build-order step #1;
     * MovementSystem collision and ContaminationSystem BFS both depend on it.
     * See docs/02_TRD1.md par.1 (Maps row) and par.4 (collision).
     */
    public void loadMap(LevelDefinition definition) {
        // ==================== TEAMMATE TASK: MAP LOADING ====================
        // TODO(level): implement real .tmx loading. Suggested steps:
        //  1. TiledMap map = new TmxMapLoader().load(definition.tmxPath());
        //     (put .tmx + tileset images under assets/maps/, licenses in
        //      ASSETS_CREDITS.md)
        //  2. Read the layer named "collision" -> build boolean[][] walkable;
        //     expose it via a getter — MovementSystem + ContaminationSystem
        //     BFS both consume this exact grid.
        //  3. Read an object layer named "spawns" for: player1_spawn,
        //     player2_spawn, enemy spawns, villager positions, sample nodes,
        //     sanitation stations, barricade slots, safe-zone rectangle.
        //  4. Store all of it so GameScreen.show() can create the entities.
        //  5. GameScreen renders the map with OrthogonalTiledMapRenderer.
        // ====================================================================
    }
}
