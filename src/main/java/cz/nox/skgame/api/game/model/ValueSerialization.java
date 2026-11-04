package cz.nox.skgame.api.game.model;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts arbitrary value objects to/from a form that Bukkit's YAML (SnakeYAML) can emit <b>and</b>
 * re-parse, losslessly, with no Skript dependency.
 *
 * <p>The invariant: <b>never put an object into a YAML map that cannot be emitted and read back.</b>
 * A raw object SnakeYAML doesn't recognise (a {@code CraftBlockData}, an enum) makes it emit a global
 * tag ({@code !!...}) that Bukkit refuses to load. Equally bad is the opposite failure this class
 * previously committed: <b>stringifying structured data</b> (a region {@link ConfigurationSection})
 * into {@code "MemorySection[...]"}, which is irrecoverable. So structured data is always recursed,
 * never flattened to text, and anything that genuinely cannot be represented losslessly is refused
 * loudly ({@link UnrepresentableException}) rather than silently mangled.
 *
 * <p>Precedence: null; primitive scalar; {@link ConfigurationSection}/{@link Map} (recurse);
 * {@link Collection}/array (recurse); {@link ConfigurationSerializable} (Location, ItemStack — pass
 * through so Bukkit emits its {@code ==} form); {@link BlockData} (string); {@link Enum} (class+name);
 * otherwise refuse.
 */
public final class ValueSerialization {

    /** Marker key identifying a tagged scalar; chosen to never collide with real config keys. */
    public static final String TYPE_KEY = "__sgtype";
    public static final String VAL_KEY = "__sgval";
    public static final String CLASS_KEY = "__sgclass";

    private ValueSerialization() {}

    /** Thrown when a value cannot be represented losslessly. Callers skip the field and keep the rest. */
    public static final class UnrepresentableException extends RuntimeException {
        public UnrepresentableException(String path, Object value) {
            super("Value at '" + path + "' of type "
                    + (value == null ? "null" : value.getClass().getName())
                    + " cannot be serialized losslessly (value: " + value + ")");
        }
    }

    public static Object encode(Object v) { return encode(v, "value"); }

    /** Encode into a YAML-safe, lossless representation. Throws {@link UnrepresentableException} if impossible. */
    public static Object encode(Object v, String path) {
        if (v == null) return null;
        if (v instanceof String || v instanceof Number || v instanceof Boolean || v instanceof Character) {
            return v;
        }
        // Structured data — ALWAYS recurse, NEVER stringify. (A region is a ConfigurationSection/Map.)
        if (v instanceof ConfigurationSection sec) {
            return encodeMap(sec.getValues(false), path);
        }
        if (v instanceof Map<?, ?> m) {
            return encodeMap(m, path);
        }
        if (v instanceof Collection<?> c) {
            return encodeList(c, path);
        }
        if (v instanceof Object[] arr) {
            return encodeList(java.util.Arrays.asList(arr), path);
        }
        // Bukkit ConfigurationSerializable (Location, ItemStack) — emitted with a safe `==` alias.
        if (v instanceof ConfigurationSerializable) {
            return v;
        }
        if (v instanceof BlockData bd) {
            return tagged("blockdata", bd.getAsString());
        }
        if (v instanceof Enum<?> e) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put(TYPE_KEY, "enum");
            map.put(CLASS_KEY, e.getDeclaringClass().getName());
            map.put(VAL_KEY, e.name());
            return map;
        }
        // Not representable losslessly. Refuse — never stringify silently. The exception message
        // carries the field path + runtime type; the save loop logs it LOUD, named, and skips only
        // this field (no server dependency here, so this works in standalone and in tests too).
        throw new UnrepresentableException(path, v);
    }

    /** Inverse of {@link #encode}. Tagged scalars reconstruct; structured maps/lists recurse; everything else passes through. */
    public static Object decode(Object v) {
        if (v == null) return null;
        Map<String, Object> m = asMap(v);
        if (m != null) {
            if (m.containsKey(TYPE_KEY)) {
                return decodeTagged(m);
            }
            // A `==` map is a ConfigurationSerializable; by the time we see it Bukkit has normally
            // already reconstructed it, but if not, leave it untouched for Bukkit to handle.
            if (m.containsKey("==")) {
                return v;
            }
            // Plain structural map (e.g. a region map) — recurse values, keep the structure.
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : m.entrySet()) {
                out.put(e.getKey(), decode(e.getValue()));
            }
            return out;
        }
        if (v instanceof List<?> list) {
            List<Object> out = new ArrayList<>(list.size());
            for (Object e : list) out.add(decode(e));
            return out;
        }
        return v;
    }

    private static Object decodeTagged(Map<String, Object> m) {
        String kind = String.valueOf(m.get(TYPE_KEY));
        Object val = m.get(VAL_KEY);
        switch (kind) {
            case "blockdata" -> {
                try { return Bukkit.createBlockData(String.valueOf(val)); }
                catch (RuntimeException e) { return null; }
            }
            case "enum" -> {
                String cls = String.valueOf(m.get(CLASS_KEY));
                String name = String.valueOf(val);
                try {
                    Class<?> c = Class.forName(cls);
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Object e = Enum.valueOf((Class<? extends Enum>) c.asSubclass(Enum.class), name);
                    return e;
                } catch (ReflectiveOperationException | IllegalArgumentException e) {
                    // Materials can be renamed across versions — try the alias matcher before giving up.
                    if (Material.class.getName().equals(cls)) {
                        Material mat = Material.matchMaterial(name);
                        if (mat != null) return mat;
                    }
                    return null;
                }
            }
            case "string" -> {           // legacy e160024 degradation — honoured on read only, never written
                return val == null ? null : String.valueOf(val);
            }
            default -> {
                return val;
            }
        }
    }

    private static Map<String, Object> encodeMap(Map<?, ?> m, String path) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : m.entrySet()) {
            String key = String.valueOf(e.getKey());
            out.put(key, encode(e.getValue(), path + "." + key));
        }
        return out;
    }

    private static List<Object> encodeList(Collection<?> c, String path) {
        List<Object> out = new ArrayList<>(c.size());
        int i = 0;
        for (Object e : c) out.add(encode(e, path + "[" + (i++) + "]"));
        return out;
    }

    private static Map<String, Object> tagged(String kind, Object value) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(TYPE_KEY, kind);
        map.put(VAL_KEY, value);
        return map;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object v) {
        if (v instanceof ConfigurationSection sec) return sec.getValues(false);
        if (v instanceof Map<?, ?> m) return (Map<String, Object>) m;
        return null;
    }
}
