package cz.nox.skgame.api.game.model;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.variables.SerializedVariable;
import cz.nox.skgame.api.game.model.type.CustomValuePlurality;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ch.njol.skript.registrations.Classes.getClassInfoNoError;

public class CustomValue implements ConfigurationSerializable {
    String name;
    ClassInfo<?> type;
    Object defaultValue;
    String description;
    CustomValuePlurality plurality;
    private List<String> allowedValues = new ArrayList<>();
    private Number minValue;
    private Number maxValue;

    public CustomValue() {}
    public CustomValue(String name) {
        this.name = name;
    }
    public CustomValue(String name, ClassInfo<?> type, Object defaultValue, String description, CustomValuePlurality plur) {
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.description = description;
        this.plurality = plur;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public ClassInfo<?> getType() {
        return type;
    }
    public void setType(ClassInfo<?> type) {
        this.type = type;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }
    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public CustomValuePlurality getPlurality() {
        return (plurality == null) ? CustomValuePlurality.SINGLE : plurality;
    }
    public void setPlurality(CustomValuePlurality plur) {
        this.plurality = plur;
    }

    public List<String> getAllowedValues() {
        return allowedValues != null ? allowedValues : new ArrayList<>();
    }
    public void setAllowedValues(List<String> vals) {
        this.allowedValues = new ArrayList<>(vals);
    }
    public boolean hasAllowedValues() {
        return allowedValues != null && !allowedValues.isEmpty();
    }

    public Number getMinValue() { return minValue; }
    public void setMinValue(Number min) { this.minValue = min; }
    public Number getMaxValue() { return maxValue; }
    public void setMaxValue(Number max) { this.maxValue = max; }
    public boolean hasBounds() { return minValue != null || maxValue != null; }

    /** Clamp a numeric value to [min, max]. Returns original if not a Number or no bounds. */
    public Object clamp(Object value) {
        if (!(value instanceof Number n)) return value;
        if (!hasBounds()) return value;
        double v = n.doubleValue();
        if (minValue != null && v < minValue.doubleValue()) v = minValue.doubleValue();
        else if (maxValue != null && v > maxValue.doubleValue()) v = maxValue.doubleValue();
        else return value;
        // Preserve integer type when original was integral
        return (value instanceof Long || value instanceof Integer) ? (long) v : v;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("type", type != null ? type.getCodeName() : null);
        map.put("name", name);
        map.put("description", description);
        map.put("plurality", plurality != null ? plurality.name() : null);
        if (defaultValue != null && type != null) {
            SerializedVariable.Value val = Classes.serialize(defaultValue);
            if (val != null) {
                Map<String, Object> ser = new HashMap<>();
                ser.put("type", val.type);
                ser.put("data", val.data);
                map.put("defaultValue", ser);
            }
        }
        if (allowedValues != null && !allowedValues.isEmpty()) {
            map.put("allowed-values", new ArrayList<>(allowedValues));
        }
        if (minValue != null) map.put("min", minValue);
        if (maxValue != null) map.put("max", maxValue);
        return map;
    }

    public static CustomValue deserialize(Map<String, Object> map) {
        CustomValue cv = new CustomValue();
        cv.name = (String) map.get("name");
        Object typeObj = map.get("type");
        if (typeObj instanceof String code) {
            cv.type = getClassInfoNoError(code);
        }
        Object def = map.get("defaultValue");
        Map<String, Object> defMap = null;
        if (def instanceof MemorySection defSec) {
            defMap = defSec.getValues(false);
        } else if (def instanceof Map<?, ?> m) {
            //noinspection unchecked
            defMap = (Map<String, Object>) m;
        }
        if (defMap != null) {
            String typeCode = (String) defMap.get("type");
            Object rawData = defMap.get("data");
            byte[] data = null;
            if (rawData instanceof byte[] b) {
                data = b;
            } else if (rawData instanceof java.util.List<?> list) {
                data = new byte[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    data[i] = ((Number) list.get(i)).byteValue();
                }
            }
            if (typeCode != null && data != null) {
                cv.defaultValue = Classes.deserialize(typeCode, data);
            }
        }
        cv.description = (String) map.get("description");
        Object pl = map.get("plurality");
        if (pl != null) {
            cv.plurality = CustomValuePlurality.valueOf(pl.toString());
        }
        Object av = map.get("allowed-values");
        if (av instanceof List<?> avList) {
            List<String> allowed = new ArrayList<>();
            for (Object item : avList) {
                if (item instanceof String s) allowed.add(s);
            }
            cv.allowedValues = allowed;
        }
        Object minRaw = map.get("min");
        if (minRaw instanceof Number n) cv.minValue = n;
        Object maxRaw = map.get("max");
        if (maxRaw instanceof Number n) cv.maxValue = n;
        return cv;
    }
}
