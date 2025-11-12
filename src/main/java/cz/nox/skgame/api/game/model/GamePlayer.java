package cz.nox.skgame.api.game.model;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class GamePlayer {
    private final Player player;
    private final Map<String, Object> values = new HashMap<>();
    private final Map<String, Object> tempValues = new HashMap<>();

    public GamePlayer(Player player) {
        this.player = player;
    }

    public Player getHandle() {
        return player;
    }
    public Object getValue(String key, boolean isTemp) {
        return getMap(isTemp).get(key);
    }
    public Object[] getValues(boolean isTemp) {
        return getMap(isTemp).values().toArray();
    }
    public String[] getKeys(boolean isTemp) {
        return getMap(isTemp).keySet().toArray(new String[0]);
    }
    public void setValue(String key, Object o, boolean isTemp) {
        getMap(isTemp).put(key,o);
    }
    public void removeValue(String key, boolean isTemp) {
        getMap(isTemp).remove(key);
    }
    public void removeValues(boolean isTemp) {
        getMap(isTemp).clear();
    }

    private Map<String, Object> getMap(boolean isTemporary) {
        return isTemporary ? tempValues : values;
    }
}
