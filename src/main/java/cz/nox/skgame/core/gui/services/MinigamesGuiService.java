package cz.nox.skgame.core.gui.services;

import cz.nox.skgame.api.game.event.GamePlayerSessionLeave;
import cz.nox.skgame.api.game.event.GameStartEvent;
import cz.nox.skgame.api.game.event.MiniGameRegisterEvent;
import cz.nox.skgame.api.game.event.MiniGameUnregisterEvent;
import cz.nox.skgame.api.game.event.SessionDisbandEvent;
import cz.nox.skgame.api.game.event.SessionSettingsChangedEvent;
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

import cz.nox.skgame.api.game.model.MinigameTag;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

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
    private final Map<UUID, Consumer<Player>> returnCallbacks = new ConcurrentHashMap<>();

    private MinigamesGuiService() {}

    public static synchronized MinigamesGuiService getInstance() {
        if (instance == null) instance = new MinigamesGuiService();
        return instance;
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    public void openFor(Player player) {
        openFor(player, null);
    }

    public void openFor(Player player, @Nullable Consumer<Player> onReturn) {
        if (onReturn != null) returnCallbacks.put(player.getUniqueId(), onReturn);
        Session session = SessionManager.getInstance().getSession(player);
        if (session == null) return;
        MinigamesGuiOpenEvent guiEvent = new MinigamesGuiOpenEvent(player, session);
        Bukkit.getPluginManager().callEvent(guiEvent);
        if (guiEvent.isCancelled()) return;
        player.openInventory(buildFor(player, session));
        viewers.computeIfAbsent(session.getId(), k -> new HashSet<>()).add(player.getUniqueId());
    }

    private void returnToCaller(Player p) {
        Consumer<Player> cb = returnCallbacks.remove(p.getUniqueId());
        if (cb != null) cb.accept(p);
        else SessionGuiService.getInstance().openFor(p);
    }

    public int getTotalViewerCount() { return viewers.values().stream().mapToInt(Set::size).sum(); }

    public void closeFor(Session session) {
        Set<UUID> vset = viewers.remove(session.getId());
        if (vset == null) return;
        for (UUID uuid : vset) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) p.closeInventory();
        }
    }

    // ─── Event listeners ──────────────────────────────────────────────────────

    @EventHandler public void onSessionDisband(SessionDisbandEvent e)        { closeFor(e.getSession()); }
    @EventHandler public void onGameStart(GameStartEvent e)                   { closeFor(e.getSession()); }
    @EventHandler public void onMinigameRegister(MiniGameRegisterEvent e)     { updateAll(); }
    @EventHandler public void onMinigameUnregister(MiniGameUnregisterEvent e) { updateAll(); }

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

    private void updateAll() {
        viewers.forEach((sessionId, uuids) -> new HashSet<>(uuids).forEach(uuid -> {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) { uuids.remove(uuid); return; }
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof GuiHolder) openFor(p);
            else uuids.remove(uuid);
        }));
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
                .onClick(e -> returnToCaller((Player) e.getWhoClicked())));

        // Dynamic minigame list
        MiniGameManager mgm = MiniGameManager.getInstance();
        MiniGame[] minigames = mgm.getAllMiniGames();
        for (int i = 0; i < Math.min(minigames.length, ITEM_SLOTS.length); i++) {
            MiniGame mg = minigames[i];
            String mgId = mg.getId();
            boolean disabled = mgm.isMinigameDisabled(mgId);
            builder.slot(ITEM_SLOTS[i], buildMinigameItem(mg, disabled).onClick(e -> {
                Player p = (Player) e.getWhoClicked();
                if (mgm.isMinigameDisabled(mgId)) {
                    Messages.send(p, "session.error.minigame-disabled");
                    return;
                }
                Session s = SessionManager.getInstance().getSession(p);
                if (s == null) return;
                MiniGame clicked = mgm.getMiniGameById(mgId);
                if (clicked == null) return;
                s.setMiniGame(clicked);
                Bukkit.getPluginManager().callEvent(new SessionSettingsChangedEvent(s, "minigame"));
                SessionGuiService.getInstance().update(s);
                returnToCaller(p);
            }));
        }

        return builder.build();
    }

    /** Shared lore: description + author + tags. Used by both the picker and the session-GUI display. */
    static List<Component> buildMinigameLore(MiniGame mg) {
        List<Component> lore = new ArrayList<>();
        Object descObj = mg.getValue("description");
        if (descObj instanceof String s && !s.isEmpty()) {
            lore.add(legacy("&7" + s));
        } else if (descObj instanceof Object[] arr) {
            for (Object line : arr) {
                String str = String.valueOf(line);
                if (!str.isEmpty()) lore.add(legacy("&7" + str));
            }
        }
        Object authorObj = mg.getValue("author");
        if (authorObj != null) {
            lore.add(legacy("&8by &f" + authorObj));
        }
        Set<MinigameTag> tags = mg.getTags();
        if (!tags.isEmpty()) {
            String tagStr = tags.stream()
                    .map(t -> t.name().toLowerCase())
                    .collect(Collectors.joining(", "));
            lore.add(legacy("&8Tags: &f" + tagStr));
        }
        return lore;
    }

    private GuiItem buildMinigameItem(MiniGame mg, boolean disabled) {
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

        List<Component> lore = new ArrayList<>(buildMinigameLore(mg));
        if (disabled) lore.add(legacy("&c[Disabled]"));
        GuiItem item = GuiItem.of(mat).name(disabled ? "&8" + displayName : displayName);
        if (!lore.isEmpty()) item.lore(lore);
        return item;
    }

    private static Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
