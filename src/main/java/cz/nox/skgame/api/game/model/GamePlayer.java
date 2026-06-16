package cz.nox.skgame.api.game.model;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class GamePlayer {
    private final Player player;
    private final Map<String, Object> tempValues = new HashMap<>();

    public GamePlayer(Player player) {
        this.player = player;
    }

    public Player getHandle() {
        return player;
    }
    public Object getValue(String key) {
        return tempValues.get(key);
    }
    public Object[] getValues() {
        return tempValues.values().toArray();
    }
    public String[] getKeys() {
        return tempValues.keySet().toArray(new String[0]);
    }
    public void setValue(String key, Object o) {
        tempValues.put(key, o);
    }
    public void removeValue(String key) {
        tempValues.remove(key);
    }
    public void removeValues() {
        tempValues.clear();
    }
}
