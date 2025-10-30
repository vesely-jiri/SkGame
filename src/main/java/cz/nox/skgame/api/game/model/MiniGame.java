package cz.nox.skgame.api.game.model;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MiniGame implements ConfigurationSerializable {
    private String id;
    private Map<String, Object> values;

    public MiniGame(String id,Map<String, Object> values) {
        this.id = id.toLowerCase();
        this.values = values;
    }
    public MiniGame(String id) {
        this(id,new HashMap<>());
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id.toLowerCase();
    }

    public Object getValue(String key) {
        return values.get(key);
    }
    public Collection<String> getKeys() {
        return values.keySet();
    }
    public Object[] getValues() {
        return values.values().toArray();
    }
    public void setValue(String key, Object o) {
        if (o == null) {
            values.remove(key);
        } else {
            values.put(key,o);
        }
    }
    public void setValues(Map<String, Object> values) {
        this.values = values;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> gm = new HashMap<>();
        gm.put("id", this.id);
        gm.put("values", this.values);
        return gm;
    }

    @SuppressWarnings("unchecked")
    public static MiniGame deserialize(Map<String, Object> gm) {
        if (gm == null) return null;
        String id = (String) gm.get("id");
        Object rawValues = gm.get("values");
        Map<String, Object> values = new HashMap<>();
        if (rawValues instanceof Map) {
            values = (Map<String, Object>) rawValues;
        } else if (rawValues instanceof org.bukkit.configuration.MemorySection) {
            values = ((org.bukkit.configuration.MemorySection) rawValues).getValues(false);
        }
        MiniGame newGm = new MiniGame(id);
        newGm.setValues(values);
        return newGm;
    }
}
