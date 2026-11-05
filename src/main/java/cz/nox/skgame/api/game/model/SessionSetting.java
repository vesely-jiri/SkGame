package cz.nox.skgame.api.game.model;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A host-toggleable boolean setting a minigame declares, surfaced in the session settings GUI.
 * Skript-free. Boolean-only for now.
 *
 * @param key          session-value key the toggle writes (e.g. {@code "bb_pvp"}); read it back with
 *                     {@code session.getValue(key, false)}
 * @param displayName  shown name; legacy {@code &} colour codes allowed; may be null → falls back to key
 * @param defaultValue state shown (and assumed) until the host toggles it
 */
public record SessionSetting(String key, @Nullable String displayName, boolean defaultValue) {

    /** Name to show, falling back to the key when none was given. */
    public String resolvedName() {
        return (displayName != null && !displayName.isEmpty()) ? displayName : key;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        if (displayName != null) m.put("name", displayName);
        m.put("default", defaultValue);
        return m;
    }

    public static SessionSetting deserialize(Map<String, Object> m) {
        String key = m.get("key") != null ? m.get("key").toString() : null;
        String name = m.get("name") != null ? m.get("name").toString() : null;
        boolean def = Boolean.TRUE.equals(m.get("default"));
        return new SessionSetting(key, name, def);
    }
}
