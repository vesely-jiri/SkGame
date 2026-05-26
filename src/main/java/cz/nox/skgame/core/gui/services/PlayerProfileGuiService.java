package cz.nox.skgame.core.gui.services;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.gui.GuiBuilder;
import cz.nox.skgame.api.gui.GuiHolder;
import cz.nox.skgame.api.gui.GuiItem;
import cz.nox.skgame.api.gui.event.PlayerProfileGuiOpenEvent;
import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.api.statistics.GameResult;
import cz.nox.skgame.core.game.MiniGameManager;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerProfileGuiService implements Listener {

    private static final int   SLOT_SKULL       = 4;
    private static final int[] STAT_SLOTS        = {10, 11, 12, 13, 14};
    private static final int[] MINIGAME_SLOTS    = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
    private static final int[] RECENT_SLOTS      = {37, 38, 39, 40, 41, 42, 43};
    private static final int   SLOT_CLOSE        = 45;
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private record ProfileData(int totalGames, int totalWins, String favoriteId,
                               Map<String, int[]> byMinigame, List<GameResult> recent) {}

    private static PlayerProfileGuiService instance;
    private final Set<UUID> activeViewers = ConcurrentHashMap.newKeySet();

    private PlayerProfileGuiService() {}

    public static synchronized PlayerProfileGuiService getInstance() {
        if (instance == null) instance = new PlayerProfileGuiService();
        return instance;
    }

    public void openFor(Player viewer, OfflinePlayer subject) {
        PlayerProfileGuiOpenEvent event = new PlayerProfileGuiOpenEvent(viewer, subject);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        viewer.openInventory(buildLoading(viewer, subject));
        activeViewers.add(viewer.getUniqueId());

        SkGame plugin = SkGame.getInstance();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            GameResultsRepository repo = GameResultsRepository.getInstance();
            UUID uuid = subject.getUniqueId();
            int totalGames   = repo.getTotalGames(uuid);
            int totalWins    = repo.getTotalWins(uuid);
            String favoriteId = repo.getFavoriteMinigameId(uuid);
            Map<String, int[]> byMg = repo.getStatsByMinigame(uuid);
            List<GameResult> recent = repo.getGameResults(uuid, null, RECENT_SLOTS.length);
            ProfileData data = new ProfileData(totalGames, totalWins, favoriteId, byMg, recent);

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!activeViewers.contains(viewer.getUniqueId())) return;
                if (!(viewer.getOpenInventory().getTopInventory().getHolder() instanceof GuiHolder)) return;
                viewer.openInventory(buildFull(viewer, subject, data));
            });
        });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof GuiHolder) {
            activeViewers.remove(event.getPlayer().getUniqueId());
        }
    }

    // ─── GUI builders ─────────────────────────────────────────────────────────

    private Inventory buildLoading(Player viewer, OfflinePlayer subject) {
        String name = subjectName(subject);
        GuiBuilder builder = new GuiBuilder().size(6)
                .title(Messages.getComponent("gui.profile.title", viewer, name));
        applyBorder(builder);
        builder.slot(SLOT_SKULL, buildSkull(subject));
        builder.slot(STAT_SLOTS[0], GuiItem.of(Material.GRAY_STAINED_GLASS_PANE)
                .name(Messages.get("gui.profile.loading", viewer)));
        builder.slot(SLOT_CLOSE, buildClose());
        return builder.build();
    }

    private Inventory buildFull(Player viewer, OfflinePlayer subject, ProfileData data) {
        String name = subjectName(subject);
        GuiBuilder builder = new GuiBuilder().size(6)
                .title(Messages.getComponent("gui.profile.title", viewer, name));
        applyBorder(builder);
        builder.slot(SLOT_SKULL, buildSkull(subject));
        builder.slot(SLOT_CLOSE, buildClose());

        if (data.totalGames == 0) {
            builder.slot(STAT_SLOTS[0], GuiItem.of(Material.BARRIER)
                    .name(Messages.get("gui.profile.no-data", viewer)));
            return builder.build();
        }

        int losses = data.totalGames - data.totalWins;
        String winRateStr = data.totalGames > 0
                ? String.format("%.1f%%", data.totalWins * 100.0 / data.totalGames)
                : "0.0%";

        String favName = data.favoriteId != null ? mgDisplayName(data.favoriteId) : null;

        builder.slot(STAT_SLOTS[0], GuiItem.of(Material.BOOK)
                .name(Messages.get("gui.profile.total-games", viewer, data.totalGames)));
        builder.slot(STAT_SLOTS[1], GuiItem.of(Material.GOLDEN_SWORD)
                .name(Messages.get("gui.profile.total-wins", viewer, data.totalWins)));
        builder.slot(STAT_SLOTS[2], GuiItem.of(Material.REDSTONE)
                .name(Messages.get("gui.profile.total-losses", viewer, losses)));
        builder.slot(STAT_SLOTS[3], GuiItem.of(Material.NETHER_STAR)
                .name(Messages.get("gui.profile.win-rate", viewer, winRateStr)));
        if (favName != null) {
            builder.slot(STAT_SLOTS[4], GuiItem.of(Material.COMPASS)
                    .name(Messages.get("gui.profile.favorite", viewer, favName)));
        }

        // Per-minigame breakdown
        int mgIdx = 0;
        for (Map.Entry<String, int[]> entry : data.byMinigame.entrySet()) {
            if (mgIdx >= MINIGAME_SLOTS.length) break;
            String mgId = entry.getKey();
            int[] stats = entry.getValue(); // [wins, plays]
            double rate = stats[1] > 0 ? (stats[0] * 100.0 / stats[1]) : 0;
            List<Component> lore = new ArrayList<>();
            lore.add(legacy("&7Plays: &f" + stats[1]));
            lore.add(legacy("&6Wins: &f" + stats[0]));
            lore.add(legacy("&bWin rate: &f" + String.format("%.1f%%", rate)));
            builder.slot(MINIGAME_SLOTS[mgIdx++], GuiItem.of(mgMaterial(mgId))
                    .name("&7" + mgDisplayName(mgId))
                    .lore(lore));
        }

        // Recent games
        int recentIdx = 0;
        for (GameResult gr : data.recent) {
            if (recentIdx >= RECENT_SLOTS.length) break;
            String mgName = mgDisplayName(gr.minigameId());
            String date = DATE_FMT.format(Instant.ofEpochMilli(gr.endTime()));
            List<Component> lore = new ArrayList<>();
            lore.add(legacy("&7Map: &f" + gr.gamemapId()));
            lore.add(legacy("&7Date: &f" + date));
            builder.slot(RECENT_SLOTS[recentIdx++],
                    GuiItem.of(gr.winner() ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE)
                            .name((gr.winner() ? "&a✓ " : "&c✗ ") + mgName)
                            .lore(lore));
        }

        return builder.build();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void applyBorder(GuiBuilder builder) {
        GuiItem glass = GuiItem.of(Material.BLACK_STAINED_GLASS_PANE).name(Component.space());
        for (int i = 0; i < 9; i++)  builder.slot(i, glass);
        for (int i = 45; i < 54; i++) builder.slot(i, glass);
        for (int row = 1; row < 5; row++) {
            builder.slot(row * 9,     glass);
            builder.slot(row * 9 + 8, glass);
        }
    }

    private GuiItem buildSkull(OfflinePlayer subject) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        if (subject instanceof Player online) {
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setPlayerProfile(online.getPlayerProfile());
                skull.setItemMeta(meta);
            }
        }
        return GuiItem.of(skull).name("&3" + subjectName(subject));
    }

    private GuiItem buildClose() {
        return GuiItem.of(Material.SPRUCE_DOOR)
                .name("&c&lClose")
                .onClick(e -> e.getWhoClicked().closeInventory());
    }

    private String subjectName(OfflinePlayer p) {
        return p.getName() != null ? p.getName() : "Unknown";
    }

    private String mgDisplayName(String mgId) {
        MiniGame mg = MiniGameManager.getInstance().getMiniGameById(mgId);
        if (mg == null) return mgId;
        Object nameObj = mg.getValue("name");
        return nameObj != null ? nameObj.toString() : mgId;
    }

    private Material mgMaterial(String mgId) {
        MiniGame mg = MiniGameManager.getInstance().getMiniGameById(mgId);
        if (mg == null) return Material.PAPER;
        Object iconObj = mg.getValue("icon");
        if (iconObj instanceof ItemStack stack) {
            if (stack.getType() != Material.AIR) return stack.getType();
        } else if (iconObj != null) {
            Material m = Material.matchMaterial(iconObj.toString());
            if (m != null && m != Material.AIR) return m;
        }
        return Material.PAPER;
    }

    private static Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
