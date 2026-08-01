package com.infectedhour.core.level;

public class LevelLoader {

    private boolean[][] walkableGrid;
    // Faking a 60x40 tile grid for your server to use temporarily
    private int mapWidthInTiles = 60;
    private int mapHeightInTiles = 40;

    public LevelDefinition loadDefinition(int levelNumber) {
        return switch (levelNumber) {
            case 1 -> LevelDefinition.level1();
            case 2 -> LevelDefinition.level2();
            case 3 -> LevelDefinition.level3Boss();
            default -> throw new IllegalArgumentException("Unknown level: " + levelNumber);
        };
    }

    public void loadMap(LevelDefinition definition) {
        // FAKE COLLISION GRID: Since we only have a PNG, we tell the game everything is walkable.
        walkableGrid = new boolean[mapWidthInTiles][mapHeightInTiles];
        for (int x = 0; x < mapWidthInTiles; x++) {
            for (int y = 0; y < mapHeightInTiles; y++) {
                walkableGrid[x][y] = true;
            }
        }
    }

    public boolean[][] getWalkableGrid() {
        return walkableGrid;
    }

    public int getMapWidthInTiles() {
        return mapWidthInTiles;
    }

    public int getMapHeightInTiles() {
        return mapHeightInTiles;
    }
}