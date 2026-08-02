package com.infectedhour.core.level;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads static level data: the objective/spec definition and the walkability
 * grid the host simulation collides against.
 *
 * <p><b>Why not Tiled yet.</b> TRD §4 specifies a Tiled collision layer, but no
 * {@code .tmx} exists in the repo and {@code gdx-maps} is not on the core
 * classpath. This loader therefore reads a plain-text {@code .map} grid from
 * {@code core/src/main/resources/maps/}. That keeps the format hand-editable,
 * keeps level loading free of libGDX (so the host sim and its tests stay
 * headless), and confines the eventual Tiled swap to {@link #loadTileMap} —
 * everything downstream only ever sees a {@link TileMap}.
 */
public class LevelLoader {

    private static final Logger LOG = Logger.getLogger(LevelLoader.class.getName());

    /** Used when a level's map resource is missing, so a bad asset never hard-crashes a demo. */
    private static final int FALLBACK_WIDTH = 60;
    private static final int FALLBACK_HEIGHT = 40;

    private static final Pattern SUBDIVISIONS_HEADER = Pattern.compile("subdivisions:\\s*(\\d+)");

    private TileMap tileMap;

    public LevelDefinition loadDefinition(int levelNumber) {
        return switch (levelNumber) {
            case 1 -> LevelDefinition.level1();
            case 2 -> LevelDefinition.level2();
            case 3 -> LevelDefinition.level3Boss();
            default -> throw new IllegalArgumentException("Unknown level: " + levelNumber);
        };
    }

    /** Loads (and caches) the collision grid for {@code definition}. */
    public TileMap loadMap(LevelDefinition definition) {
        this.tileMap = loadTileMap(definition.collisionMapPath());
        return this.tileMap;
    }

    /**
     * Reads one {@code .map} resource into a {@link TileMap}. Falls back to an
     * open 60x40 field if the resource is absent or malformed — the level is
     * still playable, just without obstacles, which beats crashing mid-demo.
     */
    public TileMap loadTileMap(String resourcePath) {
        String classpathPath = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
        try (InputStream in = LevelLoader.class.getResourceAsStream(classpathPath)) {
            if (in == null) {
                LOG.warning(() -> "Map resource not found: " + classpathPath + " — falling back to an open field");
                return TileMap.allWalkable(FALLBACK_WIDTH, FALLBACK_HEIGHT);
            }
            ParsedMap parsed = readRows(in);
            return TileMap.fromRows(parsed.rows, parsed.subdivisions);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read map resource " + classpathPath, e);
        } catch (IllegalArgumentException e) {
            LOG.warning(() -> "Malformed map " + classpathPath + " (" + e.getMessage()
                    + ") — falling back to an open field");
            return TileMap.allWalkable(FALLBACK_WIDTH, FALLBACK_HEIGHT);
        }
    }

    private record ParsedMap(List<String> rows, int subdivisions) { }

    /**
     * Blank lines and {@code //} comments are skipped so maps can be annotated.
     * A comment of the form {@code // subdivisions: N} declares how many
     * collision cells make up one tile edge; absent, the file is read at tile
     * resolution.
     */
    private static ParsedMap readRows(InputStream in) throws IOException {
        List<String> rows = new ArrayList<>();
        int subdivisions = 1;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = stripTrailingWhitespace(stripByteOrderMark(line));
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.startsWith("//")) {
                    Matcher matcher = SUBDIVISIONS_HEADER.matcher(trimmed);
                    if (matcher.find()) {
                        subdivisions = Integer.parseInt(matcher.group(1));
                    }
                    continue;
                }
                rows.add(trimmed);
            }
        }
        return new ParsedMap(rows, subdivisions);
    }

    /** A BOM on the first line would otherwise make row 1 one character too wide. */
    private static String stripByteOrderMark(String line) {
        return !line.isEmpty() && line.charAt(0) == '﻿' ? line.substring(1) : line;
    }

    /** Trailing whitespace is invisible in an editor but would make a row look ragged. */
    private static String stripTrailingWhitespace(String line) {
        int end = line.length();
        while (end > 0 && Character.isWhitespace(line.charAt(end - 1))) {
            end--;
        }
        return line.substring(0, end);
    }

    /** @return the grid from the last {@link #loadMap}, or {@code null} if none has been loaded. */
    public TileMap getTileMap() {
        return tileMap;
    }

    public int getMapWidthInTiles() {
        return tileMap != null ? tileMap.getWidth() : FALLBACK_WIDTH;
    }

    public int getMapHeightInTiles() {
        return tileMap != null ? tileMap.getHeight() : FALLBACK_HEIGHT;
    }
}
