package cz.nox.skgame.api.game.model;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts arbitrary value objects to/from a form that Bukkit's YAML (SnakeYAML) can emit <b>and</b>
 * re-parse, with no Skript dependency.
 *
 * <p>Motivation: storing a raw object SnakeYAML doesn't recognise (a {@code BlockData}/{@code CraftBlockData},
 * a {@code Material} or any enum, …) makes it emit a <b>global tag</b> like
 * {@code !!org.bukkit.craftbukkit...CraftBlockData}. Bukkit refuses to load such a tag
 * ({@code Global tag is not allowed}) — so the next read fails and the file is lost. This class is the
 * single choke point that guarantees the invariant: <b>never put an object into a YAML map that cannot
 * be emitted and read back.</b>
 *
 * <p>Pass-through (already YAML-safe): {@code null}, String, Number, Boolean, and Bukkit
 * {@link ConfigurationSerializable} (Location, ItemStack — emitted with a {@code ==} alias). Awkward
 * types are wrapped in a self-describing tagged map ({@value #TYPE_KEY}/{@value #VAL_KEY}). Lists recurse.
 * Anything genuinely unknown degrades to its string form rather than corrupting the file.
 */
public final class ValueSerialization {

    /** Marker key identifying a tagged value; chosen to never collide with real config keys. */
    public static final String TYPE_KEY = "__sgtype";
    public static final String VAL_KEY = "__sgval";
    public static final String CLASS_KEY = "__sgclass";

    private ValueSerialization() {}

    /** Encode a value into a YAML-safe representation. */
    public static Object encode(Object v) {
        if (v == null) return null;
        if (v instanceof String || v instanceof Number || v instanceof Boolean) return v;
        if (v instanceof BlockData bd) return tagged("blockdata", bd.getAsString());
        if (v instanceof Material m) return tagged("material", m.name());
        if (v instanceof Enum<?> e) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put(TYPE_KEY, "enum");
            map.put(CLASS_KEY, e.getDeclaringClass().getName());
            map.put(VAL_KEY, e.name());
            return map;
        }
        if (v instanceof ConfigurationSerializable) return v; // Location / ItemStack — Bukkit-safe
        if (v instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object e : list) out.add(encode(e));
            return out;
        }
        if (v instanceof Object[] arr) {
            List<Object> out = new ArrayList<>(arr.length);
            for (Object e : arr) out.add(encode(e));
            return out;
        }
        // Unknown / not natively YAML-safe (e.g. a Skript object). Never store raw — it would corrupt
        // the file with a global tag. Degrade to a string so the save always round-trips.
        Bukkit.getLogger().warning("[SkGame] Value of type " + v.getClass().getName()
                + " is not natively YAML-serializable; stored as text to protect the file. Value: " + v);
        return tagged("string", String.valueOf(v));
    }

    /** Inverse of {@link #encode(Object)}. Non-tagged values (primitives, ConfigurationSerializable) pass through. */
    public static Object decode(Object v) {
        if (v == null) return null;
        Map<String, Object> m = asMap(v);
        if (m != null && m.containsKey(TYPE_KEY)) {
            String kind = String.valueOf(m.get(TYPE_KEY));
            Object val = m.get(VAL_KEY);
            switch (kind) {
                case "blockdata" -> {
                    try { return Bukkit.createBlockData(String.valueOf(val)); }
                    catch (RuntimeException e) { return null; }
                }
                case "material" -> {
                    Material mat = Material.matchMaterial(String.valueOf(val));
                    if (mat == null) {
                        try { mat = Material.valueOf(String.valueOf(val)); } catch (IllegalArgumentException ignored) {}
                    }
                    return mat;
                }
                case "enum" -> {
                    try {
                        Class<?> cls = Class.forName(String.valueOf(m.get(CLASS_KEY)));
                        @SuppressWarnings({"unchecked", "rawtypes"})
                        Object e = Enum.valueOf((Class<? extends Enum>) cls.asSubclass(Enum.class), String.valueOf(val));
                        return e;
                    } catch (ReflectiveOperationException | IllegalArgumentException e) { return null; }
                }
                case "string" -> {
                    return val == null ? null : String.valueOf(val);
                }
                default -> {
                    return val;
                }
            }
        }
        if (v instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object e : list) out.add(decode(e));
            return out;
        }
        return v;
    }

    private static Map<String, Object> tagged(String kind, Object value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(TYPE_KEY, kind);
        map.put(VAL_KEY, value);
        return map;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object v) {
        if (v instanceof MemorySection sec) return sec.getValues(false);
        if (v instanceof Map<?, ?> m) return (Map<String, Object>) m;
        return null;
    }
}
