package cz.nox.skgame.api.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.Map;

public class GuiBuilder {

    private int rows = 3;
    private Component title = Component.empty();
    private final Map<Integer, GuiItem> slots = new HashMap<>();

    public GuiBuilder size(int rows) {
        this.rows = rows;
        return this;
    }

    public GuiBuilder title(Component title) {
        this.title = title;
        return this;
    }

    public GuiBuilder title(String legacyTitle) {
        return title(LegacyComponentSerializer.legacyAmpersand().deserialize(legacyTitle));
    }

    public GuiBuilder slot(int index, GuiItem item) {
        slots.put(index, item);
        return this;
    }

    /** Fill slots [start, end] inclusive with the given item. */
    public GuiBuilder fill(int start, int end, GuiItem item) {
        for (int i = start; i <= end; i++) {
            slots.put(i, item);
        }
        return this;
    }

    /** Fill border slots of a rows×9 grid. */
    public GuiBuilder border(GuiItem item) {
        int size = rows * 9;
        // top row
        for (int i = 0; i < 9; i++) slots.put(i, item);
        // bottom row
        for (int i = size - 9; i < size; i++) slots.put(i, item);
        // left/right columns (middle rows)
        for (int row = 1; row < rows - 1; row++) {
            slots.put(row * 9, item);
            slots.put(row * 9 + 8, item);
        }
        return this;
    }

    /** Build a new Inventory snapshot. Safe to call multiple times for different players. */
    public Inventory build() {
        GuiHolder holder = new GuiHolder(new HashMap<>(slots));
        Inventory inv = Bukkit.createInventory(holder, rows * 9, title);
        holder.setInventory(inv);
        for (Map.Entry<Integer, GuiItem> entry : slots.entrySet()) {
            inv.setItem(entry.getKey(), entry.getValue().getItem());
        }
        return inv;
    }

    /** Build and open immediately for the player. */
    public void open(Player player) {
        player.openInventory(build());
    }
}
