package cz.nox.skgame.api.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;

public class GuiHolder implements InventoryHolder {

    private final Map<Integer, GuiItem> slots;
    private Inventory inventory;

    GuiHolder(Map<Integer, GuiItem> slots) {
        this.slots = Collections.unmodifiableMap(slots);
    }

    /** Back-ref set by GuiBuilder after Bukkit creates the Inventory. */
    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public Map<Integer, GuiItem> getSlots() {
        return slots;
    }
}
