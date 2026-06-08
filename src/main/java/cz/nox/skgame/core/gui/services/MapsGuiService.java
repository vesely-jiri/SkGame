package cz.nox.skgame.core.gui.services;

import cz.nox.skgame.api.game.event.GameMapRegisterEvent;
import cz.nox.skgame.api.game.event.GameMapUnregisterEvent;
import cz.nox.skgame.api.game.event.GamePlayerSessionLeave;
import cz.nox.skgame.api.game.event.GameStartEvent;
import cz.nox.skgame.api.game.event.SessionDisbandEvent;
import cz.nox.skgame.api.game.event.SessionSettingsChangedEvent;
import cz.nox.skgame.api.gui.event.MapsGuiOpenEvent;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.gui.GuiBuilder;
import cz.nox.skgame.api.gui.GuiHolder;
import cz.nox.skgame.api.gui.GuiItem;
import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.core.game.GameMapManager;
import cz.nox.skgame.core.game.SessionManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.Arrays;
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

public class MapsGuiService implements Listener {

    // Same border pattern as MinigamesGuiService
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

    private static MapsGuiService instance;
    private final Map<String, Set<UUID>> viewers = new HashMap<>();
    private final Map<UUID, Consumer<Player>> returnCallbacks = new ConcurrentHashMap<>();

    private MapsGuiService() {}

    public static synchronized MapsGuiService getInstance() {
        if (instance == null) instance = new MapsGuiService();
        return instance;
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    /** Guard: session.getMiniGame() must be non-null (no-op if null). */
    public void openFor(Player player) {
        openFor(player, null);
    }

    public void openFor(Player player, @Nullable Consumer<Player> onReturn) {
        if (onReturn != null) returnCallbacks.put(player.getUniqueId(), onReturn);
        Session session = SessionManager.getInstance().getSession(player);
        if (session == null || session.getMiniGame() == null) return;
        MapsGuiOpenEvent guiEvent = new MapsGuiOpenEvent(player, session, session.getMiniGame());
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

    public void closeFor(Session session) {
        Set<UUID> vset = viewers.remove(session.getId());
        if (vset == null) return;
        for (UUID uuid : vset) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) p.closeInventory();
        }
    }

    // ─── Event listeners ──────────────────────────────────────────────────────

    @EventHandler public void onSessionDisband(SessionDisbandEvent e)   { closeFor(e.getSession()); }
    @EventHandler public void onGameStart(GameStartEvent e)              { closeFor(e.getSession()); }
    @EventHandler public void onMapRegister(GameMapRegisterEvent e)      { updateAll(); }
    @EventHandler public void onMapUnregister(GameMapUnregisterEvent e)  { updateAll(); }

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
        MiniGame miniGame = session.getMiniGame();

        GuiBuilder builder = new GuiBuilder()
                .size(6)
                .title(Messages.getComponent("gui.maps.title", viewer));

        GuiItem blackGlass = GuiItem.of(Material.BLACK_STAINED_GLASS_PANE).name(Component.empty());
        for (int s : BLACK_SLOTS) builder.slot(s, blackGlass);

        // Slot 53 — back to session lobby
        builder.slot(53, GuiItem.of(Material.SPRUCE_DOOR)
                .name("&c&lBack to lobby")
                .onClick(e -> returnToCaller((Player) e.getWhoClicked())));

        // Dynamic map list — filtered by current minigame
        // NOTE: .sk uses "a map" item (FILLED_MAP), not PAPER (spec deviation flagged in plan)
        List<GameMap> compatible = Arrays.stream(GameMapManager.getInstance().getGameMaps())
                .filter(m -> m.supportsMiniGame(miniGame))
                .collect(Collectors.toList());

        for (int i = 0; i < Math.min(compatible.size(), ITEM_SLOTS.length); i++) {
            GameMap map = compatible.get(i);
            String mapId = map.getId();
            Object nameObj = map.getValue("name");
            String displayName = nameObj != null ? nameObj.toString() : map.getId();

            builder.slot(ITEM_SLOTS[i], GuiItem.of(Material.FILLED_MAP)
                    .name("&7&l" + displayName)
                    .onClick(e -> {
                        Player p = (Player) e.getWhoClicked();
                        Session s = SessionManager.getInstance().getSession(p);
                        if (s == null) return;
                        GameMap clicked = GameMapManager.getInstance().getGameMapById(mapId);
                        if (clicked == null) return;
                        s.setMapVoting(false); // ensure mode = SPECIFIC when a specific map is chosen
                        s.setGameMap(clicked);
                        Bukkit.getPluginManager().callEvent(new SessionSettingsChangedEvent(s, "map"));
                        SessionGuiService.getInstance().update(s);
                        returnToCaller(p);
                    }));
        }

        return builder.build();
    }
}
