package cz.nox.skgame.api.game.model;

import java.util.HashMap;
import java.util.Map;

public class GameMap {
    String id;
    String name;
    Object arena;
    Map<String, Object> info;

    public GameMap(String id, String name, Object arena, Map<String, Object> info) {
        this.id = id;
        this.name = name;
        this.arena = arena;
        this.info = info;
    }

    public GameMap(String id) {
        this(id,null,null, new HashMap<>());
    }

}