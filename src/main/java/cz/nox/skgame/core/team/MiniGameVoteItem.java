package cz.nox.skgame.core.team;

import cz.nox.skgame.SkGame;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Hotbar item given to lobby members during PREPARATION when minigame voting is active.
 * Identified via PersistentDataContainer — pattern from MapVoteItem.
 */
public class MiniGameVoteItem {

    private static MiniGameVoteItem instance;
    private final NamespacedKey key;

    private MiniGameVoteItem() {
        this.key = new NamespacedKey(SkGame.getInstance(), "minigame_vote");
    }

    public static synchronized MiniGameVoteItem getInstance() {
        if (instance == null) instance = new MiniGameVoteItem();
        return instance;
    }

    public ItemStack create() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§dMinigame Vote");
            meta.setLore(List.of("§7Right-click to vote for a minigame"));
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
