package cz.nox.skgame.api.game.model;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.variables.SerializedVariable;
import cz.nox.skgame.api.game.model.type.CustomValuePlurality;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static ch.njol.skript.registrations.Classes.getClassInfoNoError;

public class CustomValue implements ConfigurationSerializable {
    String name;
    ClassInfo<?> type;
    Object defaultValue;
    String description;
    CustomValuePlurality plurality;

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
        return plurality;
    }
    public void setPlurality(CustomValuePlurality plur) {
        this.plurality = plur;
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
        if (def instanceof Map<?, ?> ser) {
            String typeCode = (String) ser.get("type");
            byte[] data = (byte[]) ser.get("data");
            if (typeCode != null && data != null) {
                cv.defaultValue = Classes.deserialize(typeCode, data);
            }
        }
        cv.description = (String) map.get("description");
        Object pl = map.get("plurality");
        if (pl != null) {
            cv.plurality = CustomValuePlurality.valueOf(pl.toString());
        }
        return cv;
    }
}
