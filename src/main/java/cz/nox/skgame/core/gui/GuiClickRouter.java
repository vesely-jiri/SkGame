package cz.nox.skgame.core.gui;

import cz.nox.skgame.api.gui.ClickHandler;
import cz.nox.skgame.api.gui.GuiHolder;
import cz.nox.skgame.api.gui.GuiItem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public class GuiClickRouter implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GuiHolder holder)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        GuiItem item = holder.getSlots().get(slot);
        if (item == null) return;

        ClickType click = event.getClick();

        // Dispatch priority: shift > left > right > generic
        if (click.isShiftClick() && item.getOnShiftClick() != null) {
            item.getOnShiftClick().handle(event);
        } else if (click == ClickType.LEFT && item.getOnLeftClick() != null) {
            item.getOnLeftClick().handle(event);
        } else if (click == ClickType.RIGHT && item.getOnRightClick() != null) {
            item.getOnRightClick().handle(event);
        } else if (item.getOnClick() != null) {
            item.getOnClick().handle(event);
        }
    }
}
