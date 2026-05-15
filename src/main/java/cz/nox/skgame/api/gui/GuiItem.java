package cz.nox.skgame.api.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class GuiItem {

    private final ItemStack item;
    private @Nullable ClickHandler onClick;
    private @Nullable ClickHandler onLeftClick;
    private @Nullable ClickHandler onRightClick;
    private @Nullable ClickHandler onShiftClick;

    private GuiItem(ItemStack item) {
        this.item = item.clone();
    }

    public static GuiItem of(ItemStack item) {
        return new GuiItem(item);
    }

    public static GuiItem of(Material material) {
        return new GuiItem(new ItemStack(material));
    }

    public GuiItem name(Component displayName) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(displayName);
            item.setItemMeta(meta);
        }
        return this;
    }

    public GuiItem name(String legacyName) {
        return name(LegacyComponentSerializer.legacyAmpersand().deserialize(legacyName));
    }

    public GuiItem lore(List<Component> lines) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.lore(lines);
            item.setItemMeta(meta);
        }
        return this;
    }

    public GuiItem lore(Component... lines) {
        return lore(Arrays.asList(lines));
    }

    public GuiItem amount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public GuiItem onClick(ClickHandler handler) {
        this.onClick = handler;
        return this;
    }

    public GuiItem onLeftClick(ClickHandler handler) {
        this.onLeftClick = handler;
        return this;
    }

    public GuiItem onRightClick(ClickHandler handler) {
        this.onRightClick = handler;
        return this;
    }

    public GuiItem onShiftClick(ClickHandler handler) {
        this.onShiftClick = handler;
        return this;
    }

    public ItemStack getItem() {
        return item.clone();
    }

    public @Nullable ClickHandler getOnClick() { return onClick; }
    public @Nullable ClickHandler getOnLeftClick() { return onLeftClick; }
    public @Nullable ClickHandler getOnRightClick() { return onRightClick; }
    public @Nullable ClickHandler getOnShiftClick() { return onShiftClick; }
}
