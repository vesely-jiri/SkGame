package cz.nox.skgame.api.game.model;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A selectable kit a minigame offers in the team-picker GUI. Skript-free.
 *
 * @param id          stable kit identifier (what the chosen-kit session value stores)
 * @param displayName shown name; legacy {@code &} colour codes allowed; may be null → falls back to id
 * @param icon        GUI icon; may be null → the picker uses a default
 */
public record KitOption(String id, @Nullable String displayName, @Nullable ItemStack icon) {

    /** Name to show, falling back to the id when none was given. */
    public String resolvedName() {
        return (displayName != null && !displayName.isEmpty()) ? displayName : id;
    }

    /** YAML-safe map for persistence (icon is a ConfigurationSerializable ItemStack). */
    public Map<String, Object> serialize() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        if (displayName != null) m.put("name", displayName);
        if (icon != null) m.put("icon", icon.clone());
        return m;
    }

    public static KitOption deserialize(Map<String, Object> m) {
        String id = m.get("id") != null ? m.get("id").toString() : null;
        String name = m.get("name") != null ? m.get("name").toString() : null;
        Object ic = m.get("icon");
        ItemStack icon = ic instanceof ItemStack is ? is.clone() : null;
        return new KitOption(id, name, icon);
    }
}
