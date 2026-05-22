package cz.nox.skgame.core.gui.services;

import cz.nox.skgame.api.game.event.GameStartEvent;
import cz.nox.skgame.api.game.event.GameStopEvent;
import cz.nox.skgame.api.game.event.SessionCreateEvent;
import cz.nox.skgame.api.game.event.SessionDisbandEvent;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.api.gui.GuiBuilder;
import cz.nox.skgame.api.gui.GuiHolder;
import cz.nox.skgame.api.gui.GuiItem;
import cz.nox.skgame.api.gui.event.MainGuiOpenEvent;
import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.game.lifecycle.SessionLifecycleManagerImpl;
import cz.nox.skgame.core.gui.services.SessionGuiService;
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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class MainGuiService implements Listener {

    private static final int[] GRAY_SLOTS    = {0, 2, 8, 10, 18, 20, 26, 28, 36, 38, 44, 46, 50, 52};
    private static final int[] RED_SLOTS     = {4, 12, 22, 30, 40, 48};
    // Slot 51 excluded — .sk sets black glass then immediately overwrites with BOOK; net = BOOK.
    private static final int[] BLACK_SLOTS   = {1, 3, 5, 7, 9, 11, 13, 17, 21, 27, 29, 31, 35, 37, 39, 47, 49, 53};
    private static final int[] SESSION_SLOTS = {14, 15, 16, 23, 24, 25, 32, 33, 34, 41, 42, 43};
    private static final String CREATE_PERMISSION = "skript.game.create_session";

    private static MainGuiService instance;
    private final Set<UUID> activeViewers = new HashSet<>();

    private MainGuiService() {}

    public static synchronized MainGuiService getInstance() {
        if (instance == null) instance = new MainGuiService();
        return instance;
    }

    public void openFor(Player viewer) {
        MainGuiOpenEvent guiEvent = new MainGuiOpenEvent(viewer);
        Bukkit.getPluginManager().callEvent(guiEvent);
        if (guiEvent.isCancelled()) return;
        viewer.openInventory(buildFor(viewer));
        activeViewers.add(viewer.getUniqueId());
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

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof GuiHolder) {
            activeViewers.remove(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onSessionCreate(SessionCreateEvent event) { update(); }

    @EventHandler
    public void onSessionDisband(SessionDisbandEvent event) { update(); }

    @EventHandler
    public void onGameStart(GameStartEvent event) { update(); }

    @EventHandler
    public void onGameStop(GameStopEvent event) { update(); }

    private Inventory buildFor(Player viewer) {
        SessionLifecycleManagerImpl lifecycle = SessionLifecycleManagerImpl.getInstance();
        SessionManager sm = SessionManager.getInstance();

        GuiItem grayGlass  = GuiItem.of(Material.GRAY_STAINED_GLASS_PANE).name(Component.space());
        GuiItem redGlass   = GuiItem.of(Material.RED_STAINED_GLASS_PANE).name(Component.space());
        GuiItem blackGlass = GuiItem.of(Material.BLACK_STAINED_GLASS_PANE).name(Component.space());

        GuiBuilder builder = new GuiBuilder()
                .size(6)
                .title(Messages.getComponent("gui.main.title", viewer));

        for (int s : GRAY_SLOTS)  builder.slot(s, grayGlass);
        for (int s : RED_SLOTS)   builder.slot(s, redGlass);
        for (int s : BLACK_SLOTS) builder.slot(s, blackGlass);

        // Slot 6 — Create session (permission: skript.game.create_session)
        builder.slot(6, GuiItem.of(Material.LIME_STAINED_GLASS_PANE)
                .name("&a&lCreate session")
                .onClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (sm.getSession(p) != null) {
                        Messages.send(p, "session.error.already-in-session");
                        return;
                    }
                    if (!p.hasPermission(CREATE_PERMISSION)) {
                        Messages.send(p, "command.error.no-permission");
                        return;
                    }
                    Session created = lifecycle.createSession(p);
                    if (created != null) {
                        SessionGuiService.getInstance().openFor(p);
                    }
                }));

        // Slot 19 — Server event placeholder
        // NOTE: .sk broadcasts to all; Java port sends to clicking player only.
        builder.slot(19, GuiItem.of(Material.BARRIER)
                .name("&cThis function was not implemented yet")
                .onClick(e -> e.getWhoClicked().sendMessage(
                        legacy("&cThis function was not implemented yet"))));

        // Slot 45 — Close
        builder.slot(45, GuiItem.of(Material.SPRUCE_DOOR)
                .name("&c&lClose")
                .onClick(e -> e.getWhoClicked().closeInventory()));

        // Slot 51 — Open current session
        builder.slot(51, GuiItem.of(Material.BOOK)
                .name("&a&lOpen current session")
                .onClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (sm.getSession(p) != null) {
                        SessionGuiService.getInstance().openFor(p);
                    }
                    // else: player not in session — no-op (matches .sk implicit stop)
                }));

        // Dynamic session list — LOBBY only, sorted oldest-first (intentional deviation from .sk)
        List<Session> lobbySessions = Arrays.stream(sm.getAllSessions())
                .filter(s -> s.getState() == SessionState.LOBBY)
                .sorted(Comparator.comparingLong(Session::getCreatedAt))
                .collect(Collectors.toList());

        for (int i = 0; i < Math.min(lobbySessions.size(), SESSION_SLOTS.length); i++) {
            builder.slot(SESSION_SLOTS[i], buildSessionItem(lobbySessions.get(i), lifecycle, sm));
        }

        return builder.build();
    }

    private GuiItem buildSessionItem(Session session, SessionLifecycleManagerImpl lifecycle,
                                     SessionManager sm) {
        Player host = session.getHost();
        String hostName = host != null ? host.getName() : "?";
        String sessionId = session.getId();

        List<Component> lore = session.getLobbyMembers().stream()
                .map(p -> legacy("&7" + p.getName()))
                .collect(Collectors.toList());

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        if (host != null) {
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) {
                meta.setPlayerProfile(host.getPlayerProfile());
                skull.setItemMeta(meta);
            }
        }

        return GuiItem.of(skull)
                .name("&3" + hostName)
                .lore(lore)
                .onClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    Session clicked = sm.getSessionById(sessionId);
                    if (clicked == null) return;

                    Session cur = sm.getSession(p);
                    if (cur == null) {
                        boolean joined = lifecycle.joinSession(p, clicked);
                        if (joined) {
                            SessionGuiService.getInstance().openFor(p);
                        }
                    } else if (cur.getId().equals(sessionId)) {
                        SessionGuiService.getInstance().openFor(p);
                    } else {
                        Messages.send(p, "session.error.already-in-session");
                    }
                });
    }

    private static Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
