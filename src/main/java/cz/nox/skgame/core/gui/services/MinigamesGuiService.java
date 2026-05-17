package cz.nox.skgame.core.gui.services;

import cz.nox.skgame.api.game.event.GamePlayerSessionLeave;
import cz.nox.skgame.api.game.event.GameStartEvent;
import cz.nox.skgame.api.game.event.SessionDisbandEvent;
import cz.nox.skgame.api.gui.event.MinigamesGuiOpenEvent;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.gui.GuiBuilder;
import cz.nox.skgame.api.gui.GuiHolder;
import cz.nox.skgame.api.gui.GuiItem;
import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.core.game.MiniGameManager;
import cz.nox.skgame.core.game.SessionManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MinigamesGuiService implements Listener {

    // Identical border pattern to maps GUI (both use all-black border)
    private static final int[] BLACK_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 17, 18, 26, 27, 35, 36, 44,
            45, 46, 47, 48, 49, 50, 51, 52
    };
    private static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static MinigamesGuiService instance;
    private final Map<String, Set<UUID>> viewers = new HashMap<>();

    private MinigamesGuiService() {}

    public static synchronized MinigamesGuiService getInstance() {
        if (instance == null) instance = new MinigamesGuiService();
        return instance;
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    public void openFor(Player player) {
        Session session = SessionManager.getInstance().getSession(player);
        if (session == null) return;
        MinigamesGuiOpenEvent guiEvent = new MinigamesGuiOpenEvent(player, session);
        Bukkit.getPluginManager().callEvent(guiEvent);
        if (guiEvent.isCancelled()) return;
        player.openInventory(buildFor(player, session));
        viewers.computeIfAbsent(session.getId(), k -> new HashSet<>()).add(player.getUniqueId());
    }

    public void closeFor(Session session) {
        Set<UUID> vset = viewers.remove(session.getId());
        if (vset == null) return;
        for (UUID uuid : vset) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) p.closeInventory();
        }
    }

    // ─── Event listeners ──────────────────────────────────────────────────────

    @EventHandler public void onSessionDisband(SessionDisbandEvent e) { closeFor(e.getSession()); }
    @EventHandler public void onGameStart(GameStartEvent e)           { closeFor(e.getSession()); }

    @EventHandler
    public void onPlayerLeave(GamePlayerSessionLeave e) {
        Set<UUID> vset = viewers.get(e.getSession().getId());
        if (vset != null) vset.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof GuiHolder) {
            UUID uuid = e.getPlayer().getUniqueId();
            viewers.values().forEach(vset -> vset.remove(uuid));
        }
    }

    // ─── GUI construction ─────────────────────────────────────────────────────

    private Inventory buildFor(Player viewer, Session session) {
        GuiBuilder builder = new GuiBuilder()
                .size(6)
                .title(Messages.getComponent("gui.minigames.title", viewer));

        GuiItem blackGlass = GuiItem.of(Material.BLACK_STAINED_GLASS_PANE).name(Component.empty());
        for (int s : BLACK_SLOTS) builder.slot(s, blackGlass);

        // Slot 53 — back to session lobby
        builder.slot(53, GuiItem.of(Material.SPRUCE_DOOR)
                .name("&c&lBack to lobby")
                .onClick(e -> SessionGuiService.getInstance().openFor((Player) e.getWhoClicked())));

        // Dynamic minigame list
        MiniGame[] minigames = MiniGameManager.getInstance().getAllMiniGames();
        for (int i = 0; i < Math.min(minigames.length, ITEM_SLOTS.length); i++) {
            MiniGame mg = minigames[i];
            String mgId = mg.getId();
            builder.slot(ITEM_SLOTS[i], buildMinigameItem(mg).onClick(e -> {
                Player p = (Player) e.getWhoClicked();
                Session s = SessionManager.getInstance().getSession(p);
                if (s == null) return;
                MiniGame clicked = MiniGameManager.getInstance().getMiniGameById(mgId);
                if (clicked == null) return;
                s.setMiniGame(clicked);
                SessionGuiService.getInstance().update(s);
                SessionGuiService.getInstance().openFor(p);
            }));
        }

        return builder.build();
    }

    private GuiItem buildMinigameItem(MiniGame mg) {
        Object iconObj = mg.getValue("icon");
        Object nameObj = mg.getValue("name");
        String displayName = nameObj != null ? nameObj.toString() : mg.getId();

        Material mat = null;
        if (iconObj instanceof ItemStack stack) {
            mat = stack.getType();
        } else if (iconObj != null) {
            mat = Material.matchMaterial(iconObj.toString());
        }
        if (mat == null || mat == Material.AIR) mat = Material.BARRIER;

        return GuiItem.of(mat).name(displayName);
    }
}
