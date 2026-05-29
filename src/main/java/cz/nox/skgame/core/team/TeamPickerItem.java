package cz.nox.skgame.core.team;

import cz.nox.skgame.SkGame;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * The hotbar item given to lobby members during the PREPARATION team-select window.
 * Identified via PersistentDataContainer — pattern from AdminWand.
 */
public class TeamPickerItem {

    private static TeamPickerItem instance;
    private final NamespacedKey key;

    private TeamPickerItem() {
        this.key = new NamespacedKey(SkGame.getInstance(), "team_picker");
    }

    public static synchronized TeamPickerItem getInstance() {
        if (instance == null) instance = new TeamPickerItem();
        return instance;
    }

    public ItemStack create() {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§eTeam Selection");
            meta.setLore(List.of("§7Right-click to choose your team"));
            meta.getPersistentDataContainer().set(key, PersistentDataType.BOOLEAN, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isPicker(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(key, PersistentDataType.BOOLEAN);
    }
}
