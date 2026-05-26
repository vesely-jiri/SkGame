package cz.nox.skgame.core.gui.services;

import cz.nox.skgame.api.game.model.MinigameTag;
import cz.nox.skgame.api.gui.GuiBuilder;
import cz.nox.skgame.api.gui.GuiHolder;
import cz.nox.skgame.api.gui.GuiItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FilterPickerGuiService implements Listener {

    // Tag items placed at slots 10–16 in a 3-row GUI
    private static final int[] TAG_SLOTS = {10, 11, 12, 13, 14, 15, 16};
    private static final int SLOT_BACK  = 18;
    private static final int SLOT_CLEAR = 22;
    private static final int SLOT_APPLY = 26;

    private static FilterPickerGuiService instance;
    private final Map<UUID, Set<MinigameTag>> pendingTagFilters = new ConcurrentHashMap<>();

    private FilterPickerGuiService() {}

    public static synchronized FilterPickerGuiService getInstance() {
        if (instance == null) instance = new FilterPickerGuiService();
        return instance;
    }

    public void openFor(Player player, Set<MinigameTag> currentTags) {
        pendingTagFilters.put(player.getUniqueId(), new HashSet<>(currentTags));
        player.openInventory(buildFor(player));
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof GuiHolder)) return;
        // OPEN_NEW means we opened a replacement — pending state intentionally kept
        if (event.getReason() == InventoryCloseEvent.Reason.OPEN_NEW) return;
        // For all other closes (PLAYER, DISCONNECT, PLUGIN), discard pending if not already removed
        pendingTagFilters.remove(event.getPlayer().getUniqueId());
    }

    private Inventory buildFor(Player player) {
        Set<MinigameTag> selected = pendingTagFilters.getOrDefault(player.getUniqueId(), Set.of());

        GuiBuilder builder = new GuiBuilder()
                .size(3)
                .title("&eFilter by Tag");

        GuiItem bg = GuiItem.of(Material.BLACK_STAINED_GLASS_PANE).name(Component.space());
        builder.fill(0, 26, bg);

        MinigameTag[] tags = MinigameTag.values();
        for (int i = 0; i < tags.length && i < TAG_SLOTS.length; i++) {
            MinigameTag tag = tags[i];
            boolean active = selected.contains(tag);
            Material mat = active ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
            builder.slot(TAG_SLOTS[i], GuiItem.of(mat)
                    .name((active ? "&a&l" : "&7") + tag.displayName())
                    .onClick(e -> {
                        Set<MinigameTag> cur = pendingTagFilters.computeIfAbsent(
                                player.getUniqueId(), k -> new HashSet<>());
                        if (!cur.remove(tag)) cur.add(tag);
                        player.openInventory(buildFor(player));
                    }));
        }

        builder.slot(SLOT_BACK, GuiItem.of(Material.ARROW)
                .name("&c&lBack")
                .lore(legacy("&7Discard changes"))
                .onClick(e -> {
                    pendingTagFilters.remove(player.getUniqueId());
                    player.closeInventory();
                    MainGuiService.getInstance().openFor(player);
                }));

        builder.slot(SLOT_CLEAR, GuiItem.of(Material.BARRIER)
                .name("&c&lClear")
                .lore(legacy("&7Remove all tag filters"))
                .onClick(e -> {
                    pendingTagFilters.put(player.getUniqueId(), new HashSet<>());
                    player.openInventory(buildFor(player));
                }));

        builder.slot(SLOT_APPLY, GuiItem.of(Material.EMERALD)
                .name("&a&lApply")
                .onClick(e -> {
                    Set<MinigameTag> chosen = pendingTagFilters.remove(player.getUniqueId());
                    MainGuiService.getInstance().setTagFilter(player, chosen != null ? chosen : Set.of());
                    player.closeInventory();
                    MainGuiService.getInstance().openFor(player);
                }));

        return builder.build();
    }

    private static Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
