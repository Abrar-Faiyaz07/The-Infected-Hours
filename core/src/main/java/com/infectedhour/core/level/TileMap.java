package com.infectedhour.core.level;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Walkability data for one level, at two resolutions.
 *
 * <p><b>Why two.</b> Gameplay reasons in whole tiles — the contamination cloud
 * BFS grows tile by tile and ships tile indices over the wire. Collision needs
 * something finer: the level art is hand-drawn and its walls do not land neatly
 * on any uniform tile grid, so a one-bit-per-tile mask either eats a walkable
 * strip of corridor or lets players clip into a wall. This class therefore
 * stores a fine <em>collision cell</em> grid ({@code subdivisions} cells per
 * tile edge) and derives the coarse tile grid from it.
 *
 * <p><b>Indexing.</b> Both grids are row-major, {@code y * width + x}, matching
 * the tile-index convention {@code ContaminationZone} uses on the wire.
 * {@code y == 0} is the <em>bottom</em> row, matching libGDX's y-up world space
 * — {@link #fromRows(List, int)} flips the file rows for you.
 *
 * <p><b>World space.</b> One <em>tile</em> is one world unit, so entity
 * positions and speeds are unaffected by the collision resolution. Tile
 * {@code (tx, ty)} covers {@code [tx, tx+1) x [ty, ty+1)}.
 *
 * <p>No libGDX dependency, so the host simulation and its tests run headless.
 */
public final class TileMap {

    /** Character marking a blocked cell in a {@code .map} resource. */
    public static final char BLOCKED = '#';

    private final int width;
    private final int height;
    private final int subdivisions;
    /** Fine collision grid, {@code (width * subdivisions) x (height * subdivisions)}. */
    private final boolean[] walkableCells;
    /** Coarse gameplay grid, derived from the fine one. */
    private final boolean[] walkableTiles;

    public TileMap(int width, int height, int subdivisions, boolean[] walkableCells) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Map dimensions must be positive, got " + width + "x" + height);
        }
        if (subdivisions <= 0) {
            throw new IllegalArgumentException("subdivisions must be positive, got " + subdivisions);
        }
        Objects.requireNonNull(walkableCells, "walkableCells");
        int expected = width * subdivisions * height * subdivisions;
        if (walkableCells.length != expected) {
            throw new IllegalArgumentException(
                    "collision grid is " + walkableCells.length + " cells, expected " + expected);
        }
        this.width = width;
        this.height = height;
        this.subdivisions = subdivisions;
        this.walkableCells = walkableCells.clone();
        this.walkableTiles = deriveTiles();
    }

    /**
     * A tile counts as walkable for gameplay if most of it is walkable. Using
     * "any cell walkable" would let clouds flow along a wall's anti-aliased
     * edge; "all cells walkable" would stop them entering any doorway.
     */
    private boolean[] deriveTiles() {
        boolean[] tiles = new boolean[width * height];
        int perTile = subdivisions * subdivisions;
        int cellsWide = width * subdivisions;
        for (int ty = 0; ty < height; ty++) {
            for (int tx = 0; tx < width; tx++) {
                int open = 0;
                for (int sy = 0; sy < subdivisions; sy++) {
                    for (int sx = 0; sx < subdivisions; sx++) {
                        if (walkableCells[(ty * subdivisions + sy) * cellsWide + (tx * subdivisions + sx)]) {
                            open++;
                        }
                    }
                }
                tiles[ty * width + tx] = open * 2 > perTile;
            }
        }
        return tiles;
    }

    /** An open field of the given size, enclosed by the implicit out-of-bounds wall. */
    public static TileMap allWalkable(int width, int height) {
        boolean[] cells = new boolean[width * height];
        Arrays.fill(cells, true);
        return new TileMap(width, height, 1, cells);
    }

    /**
     * Parses a {@code .map} text grid. Rows are given <em>top-down</em> as they
     * read in a text editor and are flipped so {@code y == 0} is the bottom row.
     * {@code '#'} is blocked; every other character is walkable.
     *
     * <p>Rows describe <em>collision cells</em>, so the file is
     * {@code subdivisions} times larger than the tile grid in each direction.
     *
     * @throws IllegalArgumentException if the grid is empty, ragged, or its
     *         dimensions are not a whole number of tiles — all of which are typos,
     *         and padding them would silently punch a hole in a wall.
     */
    public static TileMap fromRows(List<String> rowsTopDown, int subdivisions) {
        Objects.requireNonNull(rowsTopDown, "rowsTopDown");
        if (rowsTopDown.isEmpty()) {
            throw new IllegalArgumentException("Map has no rows");
        }
        if (subdivisions <= 0) {
            throw new IllegalArgumentException("subdivisions must be positive, got " + subdivisions);
        }

        int cellsHigh = rowsTopDown.size();
        int cellsWide = rowsTopDown.get(0).length();
        if (cellsWide % subdivisions != 0 || cellsHigh % subdivisions != 0) {
            throw new IllegalArgumentException("Map is " + cellsWide + "x" + cellsHigh
                    + " cells, which is not a whole number of tiles at " + subdivisions + " subdivisions");
        }

        boolean[] cells = new boolean[cellsWide * cellsHigh];
        for (int row = 0; row < cellsHigh; row++) {
            String line = rowsTopDown.get(row);
            if (line.length() != cellsWide) {
                throw new IllegalArgumentException("Ragged map: row " + (row + 1) + " is "
                        + line.length() + " columns, expected " + cellsWide);
            }
            int y = cellsHigh - 1 - row; // file reads top-down, world space is y-up
            for (int x = 0; x < cellsWide; x++) {
                cells[y * cellsWide + x] = line.charAt(x) != BLOCKED;
            }
        }
        return new TileMap(cellsWide / subdivisions, cellsHigh / subdivisions, subdivisions, cells);
    }

    /** Single-resolution convenience overload. */
    public static TileMap fromRows(List<String> rowsTopDown) {
        return fromRows(rowsTopDown, 1);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getSubdivisions() {
        return subdivisions;
    }

    public int getCollisionWidth() {
        return width * subdivisions;
    }

    public int getCollisionHeight() {
        return height * subdivisions;
    }

    /** Row-major index used by {@code ContaminationZone} and {@code WorldSnapshot.CloudFrontierDelta}. */
    public int tileIndex(int tileX, int tileY) {
        return tileY * width + tileX;
    }

    public boolean isInBounds(int tileX, int tileY) {
        return tileX >= 0 && tileX < width && tileY >= 0 && tileY < height;
    }

    /** Coarse, gameplay-resolution walkability. Out-of-bounds is blocked. */
    public boolean isWalkable(int tileX, int tileY) {
        return isInBounds(tileX, tileY) && walkableTiles[tileY * width + tileX];
    }

    /** @see #isWalkable(int, int) — index form, for tile-index consumers like the cloud BFS. */
    public boolean isWalkable(int tileIndex) {
        return tileIndex >= 0 && tileIndex < walkableTiles.length && walkableTiles[tileIndex];
    }

    /** Fine, collision-resolution walkability. Out-of-bounds is blocked. */
    public boolean isCellWalkable(int cellX, int cellY) {
        int cellsWide = width * subdivisions;
        int cellsHigh = height * subdivisions;
        return cellX >= 0 && cellX < cellsWide && cellY >= 0 && cellY < cellsHigh
                && walkableCells[cellY * cellsWide + cellX];
    }

    /** Walkability at a continuous world position, at collision resolution. */
    public boolean isWalkableAt(float worldX, float worldY) {
        return isCellWalkable(toCell(worldX), toCell(worldY));
    }

    /** World coordinate to collision-cell coordinate. */
    public int toCell(float worldCoordinate) {
        return (int) Math.floor(worldCoordinate * subdivisions);
    }

    /** Size of one collision cell in world units. */
    public float getCellSize() {
        return 1f / subdivisions;
    }

    /** World coordinate to tile coordinate. Floors toward negative infinity, so -0.3 is tile -1. */
    public static int toTile(float worldCoordinate) {
        return (int) Math.floor(worldCoordinate);
    }
}
