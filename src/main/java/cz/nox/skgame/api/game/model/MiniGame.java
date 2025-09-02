package cz.nox.skgame.api.game.model;

import ch.njol.skript.lang.util.common.AnyNamed;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MiniGame implements ConfigurationSerializable, AnyNamed {
    private String id;
    private String name;
    private Map<String, Object> values;

    public MiniGame(String id, String name, Map<String, Object> values) {
        this.id = id;
        this.name = name;
        this.values = values;
    }
    public MiniGame(String id) {
        this(id,null,new HashMap<>());
    }

    @Override
    public @UnknownNullability String name() {
        return this.name;
    }
    @Override
    public boolean supportsNameChange() {
        return true;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
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
        gm.put("name", this.name);
        gm.put("values", this.values);
        return gm;
    }

    @SuppressWarnings("unchecked")
    public static MiniGame deserialize(Map<String, Object> gm) {
        if (gm == null) return null;
        String id = (String) gm.get("id");
        String name = (String) gm.get("name");
        Map<String, Object> values = new HashMap<>();
        Object rawValues = gm.get("values");
        if (rawValues instanceof Map) {
            values = (Map<String, Object>) rawValues;
        } else if (rawValues instanceof org.bukkit.configuration.MemorySection) {
            values = ((org.bukkit.configuration.MemorySection) rawValues).getValues(false);
        }
        MiniGame newGm = new MiniGame(id);
        newGm.setName(name);
        newGm.setValues(values);
        return newGm;
    }
}
