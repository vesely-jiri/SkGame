package cz.nox.skgame.core.admin;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class AdminWand {

    private final NamespacedKey key;

    public AdminWand(NamespacedKey key) {
        this.key = key;
    }

    public ItemStack create() {
        ItemStack item = new ItemStack(Material.GOLDEN_AXE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6Map creation wand");
            meta.setLore(List.of(
                    "§eLeft-click §7> §eset position 1",
                    "§eRight-click §7> §eset position 2",
                    "§cDrop the wand to stop edit mode"
            ));
            meta.getPersistentDataContainer().set(key, PersistentDataType.BOOLEAN, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isWand(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(key, PersistentDataType.BOOLEAN);
    }
}
