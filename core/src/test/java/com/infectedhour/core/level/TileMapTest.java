package com.infectedhour.core.level;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TileMapTest {

    @Test
    void fromRowsFlipsFileRowsIntoYUpWorldSpace() {
        // File reads top-down; world space is y-up, so the LAST file row is y == 0.
        TileMap map = TileMap.fromRows(List.of(
                "###",
                "#.#",
                "..."));

        assertTrue(map.isWalkable(0, 0), "bottom-left of the file should land at y=0");
        assertTrue(map.isWalkable(1, 1), "the open middle tile should be walkable");
        assertFalse(map.isWalkable(0, 2), "the top file row is solid, so y=2 must be blocked");
    }

    @Test
    void outOfBoundsIsBlockedSoTheMapEdgeNeedsNoWall() {
        TileMap map = TileMap.allWalkable(4, 4);

        assertFalse(map.isWalkable(-1, 0), "negative x is outside the world");
        assertFalse(map.isWalkable(0, -1), "negative y is outside the world");
        assertFalse(map.isWalkable(4, 0), "x == width is one past the last column");
        assertFalse(map.isWalkable(0, 4), "y == height is one past the last row");
    }

    @Test
    void tileIndexIsRowMajorMatchingTheContaminationCloudWireFormat() {
        TileMap map = TileMap.allWalkable(60, 40);

        // ContaminationZone / WorldSnapshot.CloudFrontierDelta both assume y * width + x.
        assertEquals(0, map.tileIndex(0, 0));
        assertEquals(63, map.tileIndex(3, 1), "row-major: y=1 starts at index 60");
    }

    @Test
    void toTileFloorsTowardNegativeInfinity() {
        assertEquals(0, TileMap.toTile(0.9f));
        assertEquals(3, TileMap.toTile(3.0f));
        assertEquals(-1, TileMap.toTile(-0.3f), "casting to int would wrongly give 0 here");
    }

    @Test
    void raggedRowsAreRejectedRatherThanPadded() {
        // Silently padding a short row would punch an invisible hole in a wall.
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> TileMap.fromRows(List.of("####", "##")));
        assertTrue(error.getMessage().contains("row 2"), "the error should name the offending row");
    }

    @Test
    void constructorRejectsAGridThatDoesNotMatchItsDimensions() {
        assertThrows(IllegalArgumentException.class,
                () -> new TileMap(3, 3, 1, new boolean[4]));
    }

    @Test
    void gridIsCopiedSoCallersCannotMutateALoadedLevel() {
        boolean[] source = { true, true, true, true };
        TileMap map = new TileMap(2, 2, 1, source);

        source[0] = false;

        assertTrue(map.isWalkable(0, 0), "TileMap must be immutable once constructed");
    }

    // --- collision cells: finer than tiles, because hand-drawn walls do not
    // --- land on the tile grid (see the class javadoc).

    @Test
    void subdividedRowsGiveCellResolutionCollisionOverTileResolutionGameplay() {
        // 2x2 tiles at 3 subdivisions = a 6x6 cell grid.
        TileMap map = TileMap.fromRows(List.of(
                "......",
                "......",
                "......",
                "###...",
                "###...",
                "###..."), 3);

        assertEquals(2, map.getWidth(), "6 cells / 3 subdivisions = 2 tiles");
        assertEquals(2, map.getHeight());
        assertEquals(6, map.getCollisionWidth());

        // Bottom-left tile is fully blocked, everything else open.
        assertFalse(map.isWalkable(0, 0));
        assertTrue(map.isWalkable(1, 0));
        assertTrue(map.isWalkable(0, 1));
    }

    @Test
    void collisionResolvesInsideASingleTile() {
        // One tile whose left third is wall - impossible to express at tile resolution.
        TileMap map = TileMap.fromRows(List.of(
                "#..",
                "#..",
                "#.."), 3);

        assertEquals(1, map.getWidth(), "one tile wide");
        assertFalse(map.isWalkableAt(0.1f, 0.5f), "the left third of the tile is wall");
        assertTrue(map.isWalkableAt(0.5f, 0.5f), "the middle of the tile is open");
        assertTrue(map.isWalkableAt(0.9f, 0.5f), "the right third is open");
    }

    @Test
    void aTileIsWalkableForGameplayWhenMostOfItIs() {
        // Majority rule: "any cell open" would let clouds seep along a wall edge,
        // "all cells open" would stop them entering any doorway.
        TileMap mostlyOpen = TileMap.fromRows(List.of("#..", "...", "..."), 3);
        TileMap mostlyBlocked = TileMap.fromRows(List.of("##.", "##.", "###"), 3);

        assertTrue(mostlyOpen.isWalkable(0, 0), "8 of 9 cells open");
        assertFalse(mostlyBlocked.isWalkable(0, 0), "7 of 9 cells blocked");
    }

    @Test
    void rowCountsThatAreNotAWholeNumberOfTilesAreRejected() {
        // 5 rows at 3 subdivisions is 1.67 tiles - always a typo.
        assertThrows(IllegalArgumentException.class,
                () -> TileMap.fromRows(List.of("...", "...", "...", "...", "..."), 3));
    }
}
