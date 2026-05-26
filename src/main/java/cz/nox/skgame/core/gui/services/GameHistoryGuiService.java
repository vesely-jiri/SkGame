package cz.nox.skgame.core.gui.services;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.gui.GuiBuilder;
import cz.nox.skgame.api.gui.GuiHolder;
import cz.nox.skgame.api.gui.GuiItem;
import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.api.statistics.GameResult;
import cz.nox.skgame.core.storage.DatabaseManager;
import cz.nox.skgame.core.storage.GameResultsRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class GameHistoryGuiService implements Listener {

    private static final int PAGE_SIZE = 36;
    private static final int[] RESULT_SLOTS = buildRange(9, 44);

    private record HistoryState(UUID targetUuid, String targetName,
                                @Nullable String minigameFilter,
                                List<String> availableMinigames,
                                int offset) {
        HistoryState withFilter(@Nullable String filter) {
            return new HistoryState(targetUuid, targetName, filter, availableMinigames, 0);
        }
        HistoryState withOffset(int newOffset) {
            return new HistoryState(targetUuid, targetName, minigameFilter, availableMinigames, newOffset);
        }
        HistoryState withMinigames(List<String> mgs) {
            return new HistoryState(targetUuid, targetName, minigameFilter, mgs, offset);
        }
    }

    private static GameHistoryGuiService instance;
    private final java.util.Map<UUID, HistoryState> viewerState = new ConcurrentHashMap<>();

    private GameHistoryGuiService() {}

    public static synchronized GameHistoryGuiService getInstance() {
        if (instance == null) instance = new GameHistoryGuiService();
        return instance;
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    public void openFor(Player viewer, OfflinePlayer target) {
        if (!DatabaseManager.getInstance().isAvailable()) {
            viewer.sendMessage(legacy("&cDatabase is not available"));
            return;
        }
        String name = target.getName() != null ? target.getName() : target.getUniqueId().toString().substring(0, 8);
        HistoryState state = new HistoryState(target.getUniqueId(), name, null, List.of(), 0);
        viewerState.put(viewer.getUniqueId(), state);
        showLoading(viewer, state);
        loadAndShow(viewer, state);
    }

    // ─── Internal ─────────────────────────────────────────────────────────────

    private void loadAndShow(Player viewer, HistoryState state) {
        CompletableFuture.supplyAsync(() -> {
            GameResultsRepository repo = GameResultsRepository.getInstance();
            List<GameResult> results = repo.getGameResults(
                    state.targetUuid(), state.minigameFilter(), PAGE_SIZE, state.offset());
            int total = repo.countGamesForPlayer(state.targetUuid(), state.minigameFilter());
            List<String> mgs = (state.availableMinigames().isEmpty() && state.minigameFilter() == null)
                    ? repo.getDistinctMinigames(state.targetUuid())
                    : state.availableMinigames();
            return new Object[]{results, total, mgs};
        }).thenAccept(data -> Bukkit.getScheduler().runTask(SkGame.getInstance(), () -> {
            if (!viewer.isOnline()) return;
            @SuppressWarnings("unchecked")
            List<GameResult> results = (List<GameResult>) data[0];
            int total = (int) data[1];
            @SuppressWarnings("unchecked")
            List<String> mgs = (List<String>) data[2];
            HistoryState updated = state.withMinigames(mgs);
            viewerState.put(viewer.getUniqueId(), updated);
            viewer.openInventory(buildPage(viewer, updated, results, total));
        }));
    }

    private void showLoading(Player viewer, HistoryState state) {
        GuiBuilder builder = new GuiBuilder()
                .size(6)
                .title(legacy("&3&lHistory: &f" + state.targetName()));
        builder.slot(22, GuiItem.of(Material.CLOCK)
                .name(Messages.getComponent("gui.history.loading", viewer)));
        viewer.openInventory(builder.build());
    }

    private Inventory buildPage(Player viewer, HistoryState state, List<GameResult> results, int total) {
        GuiBuilder builder = new GuiBuilder()
                .size(6)
                .title(legacy("&3&lHistory: &f" + state.targetName()));

        GuiItem border = GuiItem.of(Material.BLACK_STAINED_GLASS_PANE).name(Component.space());
        for (int s : new int[]{0,1,2,3,4,5,6,7,8,45,46,47,48,49,50,51,52,53})
            builder.slot(s, border);

        for (int i = 0; i < Math.min(results.size(), RESULT_SLOTS.length); i++) {
            builder.slot(RESULT_SLOTS[i], buildResultCard(results.get(i)));
        }

        if (results.isEmpty()) {
            builder.slot(22, GuiItem.of(Material.GRAY_STAINED_GLASS_PANE)
                    .name(Messages.getComponent("gui.history.empty", viewer)));
        }

        // ← Previous page
        boolean hasPrev = state.offset() > 0;
        builder.slot(45, GuiItem.of(hasPrev ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE)
                .name(legacy(hasPrev ? "&a← Previous page" : "&7← Previous page"))
                .onClick(e -> {
                    if (!hasPrev) return;
                    Player p = (Player) e.getWhoClicked();
                    HistoryState s = viewerState.get(p.getUniqueId());
                    if (s == null) return;
                    HistoryState next = s.withOffset(Math.max(0, s.offset() - PAGE_SIZE));
                    viewerState.put(p.getUniqueId(), next);
                    showLoading(p, next);
                    loadAndShow(p, next);
                }));

        // → Next page
        boolean hasNext = state.offset() + results.size() < total;
        builder.slot(53, GuiItem.of(hasNext ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE)
                .name(legacy(hasNext ? "&aNext page →" : "&7Next page →"))
                .onClick(e -> {
                    if (!hasNext) return;
                    Player p = (Player) e.getWhoClicked();
                    HistoryState s = viewerState.get(p.getUniqueId());
                    if (s == null) return;
                    HistoryState next = s.withOffset(s.offset() + PAGE_SIZE);
                    viewerState.put(p.getUniqueId(), next);
                    showLoading(p, next);
                    loadAndShow(p, next);
                }));

        // Filter cycle
        Component filterName = state.minigameFilter() != null
                ? Messages.getComponent("gui.history.filter.active", viewer, state.minigameFilter())
                : Messages.getComponent("gui.history.filter.all", viewer);
        builder.slot(49, GuiItem.of(Material.HOPPER)
                .name(filterName)
                .lore(List.of(legacy("&7Click to cycle minigame filter")))
                .onClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    HistoryState s = viewerState.get(p.getUniqueId());
                    if (s == null) return;
                    HistoryState updated = s.withFilter(nextFilter(s));
                    viewerState.put(p.getUniqueId(), updated);
                    showLoading(p, updated);
                    loadAndShow(p, updated);
                }));

        // Page info
        int page = total == 0 ? 1 : state.offset() / PAGE_SIZE + 1;
        int totalPages = Math.max(1, (int) Math.ceil((double) total / PAGE_SIZE));
        builder.slot(47, GuiItem.of(Material.PAPER)
                .name(legacy("&7Page &f" + page + "&7/&f" + totalPages))
                .lore(List.of(legacy("&7Total: &f" + total + " &7games"))));

        // Close
        builder.slot(51, GuiItem.of(Material.BARRIER)
                .name(legacy("&c&lClose"))
                .onClick(e -> e.getWhoClicked().closeInventory()));

        return builder.build();
    }

    private GuiItem buildResultCard(GameResult result) {
        Material mat = result.winner() ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE;
        String prefix = result.winner() ? "&a✔ &f" : "&c✗ &f";
        long durSec = (result.endTime() - result.startTime()) / 1000;

        List<Component> lore = new ArrayList<>();
        lore.add(legacy("&7Map: &f" + result.gamemapId()));
        lore.add(legacy("&7Duration: &f" + formatDuration(durSec)));
        lore.add(legacy("&8" + formatRelativeTime(result.endTime())));

        return GuiItem.of(mat)
                .name(legacy(prefix + result.minigameId()))
                .lore(lore);
    }

    @Nullable
    private String nextFilter(HistoryState state) {
        List<String> mgs = state.availableMinigames();
        if (mgs.isEmpty()) return null;
        if (state.minigameFilter() == null) return mgs.get(0);
        int idx = mgs.indexOf(state.minigameFilter());
        return (idx < 0 || idx >= mgs.size() - 1) ? null : mgs.get(idx + 1);
    }

    // ─── Listener ─────────────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof GuiHolder) {
            viewerState.remove(event.getPlayer().getUniqueId());
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static String formatDuration(long seconds) {
        long h = seconds / 3600, m = (seconds % 3600) / 60, s = seconds % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s);
    }

    private static String formatRelativeTime(long epochMillis) {
        long secs = (System.currentTimeMillis() - epochMillis) / 1000;
        if (secs < 60) return secs + "s ago";
        long mins = secs / 60;
        if (mins < 60) return mins + "m ago";
        long hrs = mins / 60;
        if (hrs < 24) return hrs + "h ago";
        return (hrs / 24) + "d ago";
    }

    private static Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    private static int[] buildRange(int from, int to) {
        int[] arr = new int[to - from + 1];
        for (int i = 0; i < arr.length; i++) arr[i] = from + i;
        return arr;
    }
}
