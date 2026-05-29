package cz.nox.skgame.core.team;

import cz.nox.skgame.SkGame;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * The hotbar item given to lobby members during PREPARATION when map voting is active.
 * Identified via PersistentDataContainer — pattern from TeamPickerItem.
 */
public class MapVoteItem {

    private static MapVoteItem instance;
    private final NamespacedKey key;

    private MapVoteItem() {
        this.key = new NamespacedKey(SkGame.getInstance(), "map_vote");
    }

    public static synchronized MapVoteItem getInstance() {
        if (instance == null) instance = new MapVoteItem();
        return instance;
    }

    public ItemStack create() {
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§bMap Vote");
            meta.setLore(List.of("§7Right-click to vote for a map"));
            meta.getPersistentDataContainer().set(key, PersistentDataType.BOOLEAN, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isVoteItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(key, PersistentDataType.BOOLEAN);
    }
}
