package cz.nox.skgame.api.game.model;

import ch.njol.skript.classes.ClassInfo;
import cz.nox.skgame.api.game.model.type.CustomValuePlurality;
import cz.nox.skgame.api.game.model.type.ValueType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Definition of a configurable value on a minigame (gamemap- or session-scoped).
 *
 * <p>Phase 2: the authoritative type is the native {@link ValueType}. (De)serialization and GUI
 * editing use {@code ValueType} only — no {@code ch.njol.skript.*} symbol is linked on that path,
 * so typed value defs work in standalone mode. The legacy Skript {@link ClassInfo} is still exposed
 * via {@link #getType()}/{@link #setSkriptType(ClassInfo)} for the Skript syntax layer; it is derived
 * lazily from the {@code ValueType} through a bridge installed only in addon mode
 * ({@code SkriptTypeAdapter.install()}), so it is never touched in standalone.
 *
 * <p>Note the native and Skript setters have <b>distinct names</b> on purpose: a single overloaded
 * {@code setType} would force javac to load {@code ClassInfo} to resolve any call, breaking
 * compilation of Skript-free extensions. Native extensions call {@link #setValueType(ValueType)}.
 */
public class CustomValue implements ConfigurationSerializable {
    String name;
    /** Legacy Skript type — addon-only, derived lazily from {@link #valueType}. Never used standalone. */
    ClassInfo<?> type;
    /** Native, Skript-independent authoritative type. */
    ValueType valueType;
    Object defaultValue;
    String description;
    CustomValuePlurality plurality;
    private List<String> allowedValues = new ArrayList<>();
    private Number minValue;
    private Number maxValue;

    // --- Skript bridge (installed only in addon mode by SkriptTypeAdapter.install()) -------------
    // Held as java.util.function references so this api class never calls into the skript/ layer.
    private static Function<ValueType, ClassInfo<?>> toClassInfo;
    private static Function<ClassInfo<?>, ValueType> toValueType;

    /** Installed by the addon-only SkriptTypeAdapter to bridge ValueType ↔ Skript ClassInfo. */
    public static void installSkriptTypeBridge(Function<ValueType, ClassInfo<?>> toClassInfo,
                                               Function<ClassInfo<?>, ValueType> toValueType) {
        CustomValue.toClassInfo = toClassInfo;
        CustomValue.toValueType = toValueType;
    }

    public CustomValue() {}
    public CustomValue(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    /** Native type token — authoritative, Skript-independent. */
    public ValueType getValueType() {
        return valueType;
    }

    /**
     * Set the native type (the Skript-free entry point extensions use). Clears the lazily-derived
     * Skript {@link ClassInfo}. Named distinctly from {@link #setSkriptType(ClassInfo)} so calling it
     * never forces javac to load {@code ClassInfo}.
     */
    public void setValueType(ValueType valueType) {
        this.valueType = valueType;
        this.type = null; // re-derived lazily on getType() in addon mode
    }

    /**
     * Legacy Skript type. Derived lazily from {@link #valueType} via the addon bridge.
     * Returns null in standalone mode (no bridge installed) — callers must use {@link #getValueType()}.
     */
    public ClassInfo<?> getType() {
        if (type == null && valueType != null && toClassInfo != null) {
            type = toClassInfo.apply(valueType);
        }
        return type;
    }

    /** Addon-only setter (Skript syntax). Also derives the native {@link ValueType} via the bridge. */
    public void setSkriptType(ClassInfo<?> type) {
        this.type = type;
        if (type != null && toValueType != null) {
            this.valueType = toValueType.apply(type);
        } else if (type == null) {
            this.valueType = null;
        }
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

    /** True when Skript is present. Used only to guard the TIMESPAN default conversion. */
    private static boolean skriptPresent() {
        return Bukkit.getPluginManager().getPlugin("Skript") != null;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("value-type", valueType != null ? valueType.getCode() : null);
        map.put("name", name);
        map.put("description", description);
        map.put("plurality", plurality != null ? plurality.name() : null);
        if (defaultValue != null) {
            Object serDef = serializeDefault(defaultValue);
            if (serDef != null) map.put("defaultValue", serDef);
        }
        if (allowedValues != null && !allowedValues.isEmpty()) {
            map.put("allowed-values", new ArrayList<>(allowedValues));
        }
        if (minValue != null) map.put("min", minValue);
        if (maxValue != null) map.put("max", maxValue);
        return map;
    }

    /**
     * Native default serialization: primitives and Bukkit {@link ConfigurationSerializable}
     * (ItemStack/Location) round-trip through YamlConfiguration as-is. The one non-YAML type a def
     * default can hold is a Skript {@code Timespan}, stored as ticks (addon only — guarded so no
     * {@code ch.njol.skript.*} symbol is linked in standalone).
     */
    private Object serializeDefault(Object def) {
        if (valueType == ValueType.TIMESPAN && skriptPresent()
                && def instanceof ch.njol.skript.util.Timespan ts) {
            return ts.getAs(ch.njol.skript.util.Timespan.TimePeriod.TICK);
        }
        return def;
    }

    public static CustomValue deserialize(Map<String, Object> map) {
        CustomValue cv = new CustomValue();
        cv.name = (String) map.get("name");

        Object vt = map.get("value-type");
        if (vt instanceof String s) {
            cv.valueType = ValueType.fromCode(s);
        }

        Object def = map.get("defaultValue");
        cv.defaultValue = deserializeDefault(def, cv.valueType);

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

    /** Inverse of {@link #serializeDefault(Object)}. TIMESPAN ticks → Timespan (addon only). */
    private static Object deserializeDefault(Object def, ValueType vt) {
        if (def == null) return null;
        if (vt == ValueType.TIMESPAN && skriptPresent() && def instanceof Number n) {
            return new ch.njol.skript.util.Timespan(ch.njol.skript.util.Timespan.TimePeriod.TICK, n.longValue());
        }
        return def;
    }
}
