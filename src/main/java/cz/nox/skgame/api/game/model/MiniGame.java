package cz.nox.skgame.api.game.model;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.variables.SerializedVariable;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MiniGame implements ConfigurationSerializable {
    private String id;
    private Map<String, Object> values;
    private Map<String, CustomValue> gameMapValueDefs = new LinkedHashMap<>();

    public MiniGame(String id,Map<String, Object> values) {
        this.id = id;
        this.values = values;
    }
    public MiniGame(String id) {
        this(id.toLowerCase(), new HashMap<>());
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
    public String[] getKeys() {
        return values.keySet().toArray(new String[0]);
    }
    public Object[] getValues() {
        return values.values().toArray();
    }
    public void setValue(String key, Object o) {
        values.put(key,o);
    }
    public void setValues(Map<String, Object> values) {
        this.values = values;
    }
    public void removeValue(String key) {
        this.values.remove(key);
    }
    public void removeValues() {
        this.values.clear();
    }

    public Map<String, CustomValue> getGameMapValueDefs() {
        return gameMapValueDefs;
    }
    public @Nullable CustomValue getGameMapValueDef(String key) {
        return gameMapValueDefs.get(key);
    }
    public void setGameMapValueDef(String key, @Nullable CustomValue cv) {
        if (cv == null) gameMapValueDefs.remove(key);
        else gameMapValueDefs.put(key, cv);
    }
    public void setGameMapValueDefs(Map<String, CustomValue> defs) {
        this.gameMapValueDefs = (defs != null) ? defs : new LinkedHashMap<>();
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> gm = new HashMap<>();
        gm.put("id", this.id);

        Map<String, Object> serializedValues = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : this.values.entrySet()) {
            Object v = entry.getValue();
            SerializedVariable.Value serialized = Classes.serialize(v);
            if (serialized != null) {
                Map<String, Object> ser = new HashMap<>();
                ser.put("type", serialized.type);
                ser.put("data", serialized.data);
                serializedValues.put(entry.getKey(), ser);
            } else {
                serializedValues.put(entry.getKey(), v);
            }
        }
        gm.put("values", serializedValues);

        if (!gameMapValueDefs.isEmpty()) {
            Map<String, Object> defs = new LinkedHashMap<>();
            for (Map.Entry<String, CustomValue> e : gameMapValueDefs.entrySet()) {
                defs.put(e.getKey(), e.getValue().serialize());
            }
            gm.put("gamemap-values", defs);
        }
        return gm;
    }

    @SuppressWarnings("unchecked")
    public static MiniGame deserialize(Map<String, Object> gm) {
        if (gm == null) return null;
        String id = (String) gm.get("id");
        Object rawValues = gm.get("values");
        Map<String, Object> rawMap = new HashMap<>();
        if (rawValues instanceof Map) {
            rawMap = (Map<String, Object>) rawValues;
        } else if (rawValues instanceof MemorySection sec) {
            rawMap = sec.getValues(false);
        }

        Map<String, Object> values = new HashMap<>();
        for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
            Object raw = entry.getValue();
            boolean decoded = false;
            if (raw instanceof MemorySection rawSection) {
                Map<String, Object> data = rawSection.getValues(false);
                String typeObj = (String) data.get("type");
                Object rawBytes = data.get("data");
                if (typeObj != null && rawBytes instanceof byte[] bytes) {
                    ClassInfo<?> classInfo = Classes.getClassInfoNoError(typeObj);
                    if (classInfo != null) {
                        Serializer<?> ser = classInfo.getSerializer();
                        if (ser != null) {
                            Object obj = Classes.deserialize(classInfo, bytes);
                            if (obj != null) {
                                values.put(entry.getKey(), obj);
                                decoded = true;
                            }
                        }
                    }
                }
            }
            if (!decoded) {
                values.put(entry.getKey(), raw);
            }
        }

        MiniGame newGm = new MiniGame(id);
        newGm.setValues(values);

        Object rawDefs = gm.get("gamemap-values");
        Map<String, Object> defsMap = null;
        if (rawDefs instanceof MemorySection sec) {
            defsMap = sec.getValues(false);
        } else if (rawDefs instanceof Map<?, ?> m) {
            //noinspection unchecked
            defsMap = (Map<String, Object>) m;
        }
        if (defsMap != null) {
            Map<String, CustomValue> defs = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : defsMap.entrySet()) {
                Map<String, Object> cvMap = null;
                if (entry.getValue() instanceof MemorySection cvSec) {
                    cvMap = cvSec.getValues(false);
                } else if (entry.getValue() instanceof Map<?, ?> m) {
                    //noinspection unchecked
                    cvMap = (Map<String, Object>) m;
                }
                if (cvMap != null) {
                    defs.put(entry.getKey(), CustomValue.deserialize(cvMap));
                }
            }
            newGm.setGameMapValueDefs(defs);
        }

        return newGm;
    }
}
