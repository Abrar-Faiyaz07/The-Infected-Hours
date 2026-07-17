package com.theinfectedhour.world;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Vector2;

/**
 * One playable map: the Tiled map data, its named sections (Resident-Evil-style
 * parts that unlock as missions complete), spawn points, and placed objects.
 */
public class Level {

    private final String levelId;
    /** Backing TiledMap authored in the Tiled editor (.tmx), loaded by MapLoader. */
    private final TiledMap tiledMap;
    private final List<MapSection> sections = new ArrayList<>();
    private final List<Vector2> playerSpawns = new ArrayList<>();

    public Level(String levelId, TiledMap tiledMap) {
        this.levelId = levelId;
        this.tiledMap = tiledMap;
    }

    public String getLevelId() {
        return levelId;
    }

    public TiledMap getTiledMap() {
        return tiledMap;
    }

    public List<MapSection> getSections() {
        return sections;
    }

    public List<Vector2> getPlayerSpawns() {
        return playerSpawns;
    }

    /** The section a world position falls inside, or null if outside all sections. */
    public MapSection sectionAt(Vector2 worldPosition) {
        // TODO: implement
        return null;
    }
}
