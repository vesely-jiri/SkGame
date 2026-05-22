package cz.nox.skgame.core.gui.services;

import cz.nox.skgame.api.game.event.GameStartEvent;
import cz.nox.skgame.api.game.event.GameStopEvent;
import cz.nox.skgame.api.game.event.PlayerRoleChangeEvent;
import cz.nox.skgame.api.game.event.SessionCreateEvent;
import cz.nox.skgame.api.game.event.SessionDisbandEvent;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.api.gui.GuiBuilder;
import cz.nox.skgame.api.gui.GuiHolder;
import cz.nox.skgame.api.gui.GuiItem;
import cz.nox.skgame.api.gui.event.SpectateGuiOpenEvent;
import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.game.lifecycle.SessionLifecycleManagerImpl;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SpectateGuiService implements Listener {

    // Mirror MainGuiService border pattern
    private static final int[] GRAY_SLOTS    = {0, 2, 8, 10, 18, 20, 26, 28, 36, 38, 44, 46, 50, 52};
    private static final int[] RED_SLOTS     = {4, 12, 22, 30, 40, 48};
    private static final int[] BLACK_SLOTS   = {1, 3, 5, 7, 9, 11, 13, 17, 21, 27, 29, 31, 35, 37, 39, 47, 49, 53};
    private static final int[] SESSION_SLOTS = {14, 15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43};

    private static SpectateGuiService instance;
    private final Set<UUID> activeViewers = new HashSet<>();

    private SpectateGuiService() {}

    public static synchronized SpectateGuiService getInstance() {
        if (instance == null) instance = new SpectateGuiService();
        return instance;
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    public void openFor(Player player) {
        SpectateGuiOpenEvent guiEvent = new SpectateGuiOpenEvent(player);
        Bukkit.getPluginManager().callEvent(guiEvent);
        if (guiEvent.isCancelled()) return;
        player.openInventory(buildFor(player));
        activeViewers.add(player.getUniqueId());
    }

    public void update() {
        for (UUID uuid : new HashSet<>(activeViewers)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) {
                activeViewers.remove(uuid);
                continue;
            }
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof GuiHolder) {
                openFor(p);
            } else {
                activeViewers.remove(uuid);
            }
        }
    }

    // ─── Event listeners ──────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof GuiHolder) {
            activeViewers.remove(e.getPlayer().getUniqueId());
        }
    }

    @EventHandler public void onSessionCreate(SessionCreateEvent e)  { update(); }
    @EventHandler public void onSessionDisband(SessionDisbandEvent e) { update(); }
    @EventHandler public void onGameStart(GameStartEvent e)           { update(); }
    @EventHandler public void onGameStop(GameStopEvent e)             { update(); }
    @EventHandler public void onRoleChange(PlayerRoleChangeEvent e)   { update(); }

    // ─── GUI construction ─────────────────────────────────────────────────────

    private Inventory buildFor(Player viewer) {
        SessionManager sm = SessionManager.getInstance();
        SessionLifecycleManagerImpl lifecycle = SessionLifecycleManagerImpl.getInstance();

        GuiBuilder builder = new GuiBuilder()
                .size(6)
                .title(Messages.getComponent("gui.spectate.title", viewer));

        GuiItem grayGlass  = GuiItem.of(Material.GRAY_STAINED_GLASS_PANE).name(Component.space());
        GuiItem redGlass   = GuiItem.of(Material.RED_STAINED_GLASS_PANE).name(Component.space());
        GuiItem blackGlass = GuiItem.of(Material.BLACK_STAINED_GLASS_PANE).name(Component.space());

        for (int s : GRAY_SLOTS)  builder.slot(s, grayGlass);
        for (int s : RED_SLOTS)   builder.slot(s, redGlass);
        for (int s : BLACK_SLOTS) builder.slot(s, blackGlass);

        // Slot 45 — Back to main menu
        builder.slot(45, GuiItem.of(Material.SPRUCE_DOOR)
                .name("&c&lBack to main menu")
                .onClick(e -> MainGuiService.getInstance().openFor((Player) e.getWhoClicked())));

        // Active spectatable sessions (STARTED + allowSpectate), sorted oldest-first
        List<Session> spectatable = Arrays.stream(sm.getAllSessions())
                .filter(s -> s.getState() == SessionState.STARTED && s.isAllowSpectate())
                .sorted(Comparator.comparingLong(Session::getCreatedAt))
                .collect(Collectors.toList());

        if (spectatable.isEmpty()) {
            builder.slot(24, GuiItem.of(Material.BARRIER)
                    .name(Messages.getComponent("gui.spectate.empty", viewer)));
        } else {
            for (int i = 0; i < Math.min(spectatable.size(), SESSION_SLOTS.length); i++) {
                Session s = spectatable.get(i);
                String sessionId = s.getId();
                builder.slot(SESSION_SLOTS[i], buildSessionCard(s, viewer).onClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    Session clicked = sm.getSessionById(sessionId);
                    if (clicked == null) return;
                    if (sm.getSession(p) != null) {
                        Messages.send(p, "spectator.already-in-session");
                        return;
                    }
                    boolean joined = lifecycle.joinAsSpectator(p, clicked);
                    if (!joined) {
                        Messages.send(p, "spectator.join-denied");
                    }
                }));
            }
        }

        return builder.build();
    }

    private GuiItem buildSessionCard(Session session, Player viewer) {
        Player host = session.getHost();
        String hostName = host != null ? host.getName() : "?";

        MiniGame mg = session.getMiniGame();
        GameMap map = session.getGameMap();
        Object mgNameObj = mg != null ? mg.getValue("name") : null;
        Object mapNameObj = map != null ? map.getValue("name") : null;
        String mgName  = mgNameObj  != null ? mgNameObj.toString()  : (mg  != null ? mg.getId()  : "?");
        String mapName = mapNameObj != null ? mapNameObj.toString() : (map != null ? map.getId() : "?");
        int playerCount    = session.getPlayers().size();
        int spectatorCount = session.getSpectators().size();

        return GuiItem.of(Material.SPYGLASS)
                .name("&3" + hostName)
                .lore(
                        Messages.getComponent("gui.spectate.session-card.minigame",    viewer, mgName),
                        Messages.getComponent("gui.spectate.session-card.map",         viewer, mapName),
                        Messages.getComponent("gui.spectate.session-card.players",     viewer, playerCount),
                        Messages.getComponent("gui.spectate.session-card.spectators",  viewer, spectatorCount)
                );
    }
}
