package cz.nox.skgame.api.game.model;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Immutable descriptor for a single declared team in a minigame registration. */
public class TeamEntry {

    private final String id;
    @Nullable private final String displayName;
    @Nullable private final ItemStack icon;

    public TeamEntry(String id, @Nullable String displayName, @Nullable ItemStack icon) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon != null ? icon.clone() : null;
    }

    public String getId() { return id; }

    /** Returns displayName if explicitly set, otherwise falls back to id. */
    public String getDisplayName() { return displayName != null ? displayName : id; }

    /** Raw nullable field — null means no explicit name was declared. */
    public @Nullable String getRawDisplayName() { return displayName; }

    /** Returns a clone of the icon ItemStack, or null if none was declared. */
    public @Nullable ItemStack getIcon() { return icon != null ? icon.clone() : null; }
}
