package com.theinfectedhour.world;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;

/**
 * Loads levels authored in the Tiled editor (.tmx) via libGDX's TmxMapLoader.
 * Expected object layers per map: "spawns" (player/enemy spawn points),
 * "sections" (rectangles with a requiredMission property — RE-style map
 * parts), "gates" (doors between sections), "contamination_zones".
 */
public class MapLoader {

    private final TmxMapLoader tmxLoader = new TmxMapLoader();

    public Level load(String mapId) {
        // TODO: TiledMap map = tmxLoader.load("maps/" + mapId + ".tmx");
        // TODO: parse "sections" object layer into MapSection instances
        // TODO: parse "spawns" layer into Level.playerSpawns
        // TODO: parse "contamination_zones" layer into ContaminationZone objects
        return null;
    }
}
