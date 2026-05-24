package cz.nox.skgame.core.gui.services;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.gui.GuiBuilder;
import cz.nox.skgame.api.gui.GuiHolder;
import cz.nox.skgame.api.gui.GuiItem;
import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.api.statistics.LeaderboardEntry;
import cz.nox.skgame.core.game.MiniGameManager;
import cz.nox.skgame.core.storage.DatabaseManager;
import cz.nox.skgame.core.storage.GameResultsRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class LeaderboardGuiService implements Listener {

    public enum SortMode { WINS, PLAYS, WIN_RATE }

    private static LeaderboardGuiService instance;

    private final Map<UUID, SortMode> playerSortMode = new HashMap<>();

    private LeaderboardGuiService() {}

    public static synchronized LeaderboardGuiService getInstance() {
        if (instance == null) instance = new LeaderboardGuiService();
        return instance;
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    public void openMinigamePicker(Player viewer) {
        viewer.openInventory(buildPickerInventory(viewer));
    }

    public void openLeaderboard(Player viewer, String minigameId, SortMode mode) {
        playerSortMode.put(viewer.getUniqueId(), mode);
        viewer.openInventory(buildLoadingInventory(viewer, minigameId, mode));
        CompletableFuture
                .supplyAsync(() -> queryLeaderboard(minigameId, mode, 10))
                .thenAccept(entries -> Bukkit.getScheduler().runTask(SkGame.getInstance(), () -> {
                    if (viewer.isOnline()) {
                        viewer.openInventory(buildLeaderboardInventory(viewer, minigameId, mode, entries));
                    }
                }));
    }

    // ─── Event listeners ──────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof GuiHolder) {
            playerSortMode.remove(event.getPlayer().getUniqueId());
        }
    }

    // ─── GUI construction ─────────────────────────────────────────────────────

    private Inventory buildPickerInventory(Player viewer) {
        MiniGame[] minigames = MiniGameManager.getInstance().getAllMiniGames();

        GuiBuilder builder = new GuiBuilder()
                .size(3)
                .title(Messages.getComponent("gui.leaderboard.picker.title", viewer));

        GuiItem gray = GuiItem.of(Material.GRAY_STAINED_GLASS_PANE).name(Component.space());
        for (int i = 0; i < 9; i++)  builder.slot(i, gray);
        for (int i = 18; i < 27; i++) builder.slot(i, gray);

        builder.slot(4, GuiItem.of(Material.NETHER_STAR)
                .name(Messages.getComponent("gui.leaderboard.picker.label", viewer)));

        builder.slot(26, GuiItem.of(Material.SPRUCE_DOOR)
                .name(legacy("&c&lClose"))
                .onClick(e -> e.getWhoClicked().closeInventory()));

        for (int i = 0; i < Math.min(minigames.length, 9); i++) {
            MiniGame mg = minigames[i];
            String mgId = mg.getId();
            Object nameObj = mg.getValue("name");
            String mgName = nameObj != null ? nameObj.toString() : mgId;

            Object iconObj = mg.getValue("icon");
            Material iconMat = Material.PAPER;
            if (iconObj instanceof ItemStack is) iconMat = is.getType();

            builder.slot(9 + i, GuiItem.of(iconMat)
                    .name("&6" + mgName)
                    .lore(Messages.getComponent("gui.leaderboard.picker.click-hint", viewer))
                    .onClick(e -> openLeaderboard((Player) e.getWhoClicked(), mgId, SortMode.WINS)));
        }

        return builder.build();
    }

    private Inventory buildLoadingInventory(Player viewer, String minigameId, SortMode mode) {
        GuiBuilder builder = new GuiBuilder()
                .size(5)
                .title(Messages.getComponent("gui.leaderboard.title", viewer, minigameId));

        buildLeaderboardHeader(builder, viewer, minigameId, mode);

        builder.slot(22, GuiItem.of(Material.BARRIER)
                .name(Messages.getComponent("gui.leaderboard.loading", viewer)));

        return builder.build();
    }

    private Inventory buildLeaderboardInventory(Player viewer, String minigameId, SortMode mode,
                                                List<LeaderboardEntry> entries) {
        GuiBuilder builder = new GuiBuilder()
                .size(5)
                .title(Messages.getComponent("gui.leaderboard.title", viewer, minigameId));

        buildLeaderboardHeader(builder, viewer, minigameId, mode);

        GuiItem black = GuiItem.of(Material.BLACK_STAINED_GLASS_PANE).name(Component.space());
        for (int s = 9; s < 45; s++) builder.slot(s, black);

        if (entries.isEmpty()) {
            builder.slot(22, GuiItem.of(Material.BARRIER)
                    .name(Messages.getComponent("gui.leaderboard.empty", viewer)));
        } else {
            for (int i = 0; i < Math.min(entries.size(), 10); i++) {
                LeaderboardEntry e = entries.get(i);
                builder.slot(9 + i, buildEntryItem(viewer, i + 1, e, mode));
            }
        }

        return builder.build();
    }

    private void buildLeaderboardHeader(GuiBuilder builder, Player viewer, String minigameId, SortMode mode) {
        GuiItem gray = GuiItem.of(Material.GRAY_STAINED_GLASS_PANE).name(Component.space());
        for (int i = 0; i < 9; i++) builder.slot(i, gray);

        // Sort buttons — highlighted with yellow glass if active
        Material winsMat  = mode == SortMode.WINS     ? Material.YELLOW_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
        Material playsMat = mode == SortMode.PLAYS    ? Material.YELLOW_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
        Material rateMat  = mode == SortMode.WIN_RATE ? Material.YELLOW_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;

        builder.slot(0, GuiItem.of(winsMat)
                .name(Messages.getComponent("gui.leaderboard.sort.wins", viewer))
                .onClick(e -> openLeaderboard((Player) e.getWhoClicked(), minigameId, SortMode.WINS)));

        builder.slot(1, GuiItem.of(playsMat)
                .name(Messages.getComponent("gui.leaderboard.sort.plays", viewer))
                .onClick(e -> openLeaderboard((Player) e.getWhoClicked(), minigameId, SortMode.PLAYS)));

        builder.slot(2, GuiItem.of(rateMat)
                .name(Messages.getComponent("gui.leaderboard.sort.win-rate", viewer))
                .onClick(e -> openLeaderboard((Player) e.getWhoClicked(), minigameId, SortMode.WIN_RATE)));

        builder.slot(4, GuiItem.of(Material.NETHER_STAR)
                .name(legacy("&6" + minigameId)));

        builder.slot(8, GuiItem.of(Material.SPRUCE_DOOR)
                .name(Messages.getComponent("gui.leaderboard.back", viewer))
                .onClick(e -> openMinigamePicker((Player) e.getWhoClicked())));
    }

    private GuiItem buildEntryItem(Player viewer, int rank, LeaderboardEntry entry, SortMode mode) {
        String statLine = switch (mode) {
            case WINS     -> Messages.get("gui.leaderboard.entry.wins", viewer, entry.wins());
            case PLAYS    -> Messages.get("gui.leaderboard.entry.plays", viewer, entry.plays());
            case WIN_RATE -> Messages.get("gui.leaderboard.entry.win-rate", viewer,
                    String.format("%.1f%%", entry.winRate() * 100));
        };

        ItemStack skull = buildSkull(Bukkit.getOfflinePlayer(entry.playerUuid()));
        return GuiItem.of(skull)
                .name("&e#" + rank + " &f" + entry.playerName())
                .lore(
                        legacy(statLine),
                        Messages.getComponent("gui.leaderboard.entry.wins-lore", viewer, entry.wins()),
                        Messages.getComponent("gui.leaderboard.entry.plays-lore", viewer, entry.plays())
                );
    }

    private ItemStack buildSkull(org.bukkit.OfflinePlayer op) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null && op.isOnline()) {
            Player online = op.getPlayer();
            if (online != null) meta.setPlayerProfile(online.getPlayerProfile());
            skull.setItemMeta(meta);
        }
        return skull;
    }

    // ─── Query ────────────────────────────────────────────────────────────────

    private List<LeaderboardEntry> queryLeaderboard(String minigameId, SortMode mode, int limit) {
        GameResultsRepository repo = GameResultsRepository.getInstance();
        return switch (mode) {
            case WINS     -> repo.getTopPlayersByWins(minigameId, limit);
            case PLAYS    -> repo.getTopPlayersByPlays(minigameId, limit);
            case WIN_RATE -> repo.getTopPlayersByWinRate(minigameId, limit, 1);
        };
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
