package cz.nox.skgame.core.gui.services;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.DisbandReason;
import cz.nox.skgame.api.game.model.type.GameStartReason;
import cz.nox.skgame.api.game.model.type.SessionRole;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.api.gui.GuiBuilder;
import cz.nox.skgame.api.gui.GuiHolder;
import cz.nox.skgame.api.gui.GuiItem;
import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.game.lifecycle.SessionLifecycleManagerImpl;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AdminPanelGuiService implements Listener {

    // Panel filter values
    private static final String FILTER_ALL     = "all";
    private static final String FILTER_LOBBY   = "lobby";
    private static final String FILTER_RUNNING = "running";

    // Slots for session cards in the panel (rows 2–5, all 9 columns = 36 slots)
    private static final int[] PANEL_CARD_SLOTS = buildRange(9, 44);
    // Slots for member cards in the detail view (rows 2–5)
    private static final int[] DETAIL_MEMBER_SLOTS = buildRange(9, 44);

    private static AdminPanelGuiService instance;

    // per-viewer filter state for the panel
    private final java.util.Map<UUID, String> panelFilters = new ConcurrentHashMap<>();

    private AdminPanelGuiService() {}

    public static synchronized AdminPanelGuiService getInstance() {
        if (instance == null) instance = new AdminPanelGuiService();
        return instance;
    }

    // ─── Panel (session list) ────────────────────────────────────────────────

    public void openPanel(Player admin) {
        admin.openInventory(buildPanel(admin));
    }

    private Inventory buildPanel(Player admin) {
        SessionManager sm = SessionManager.getInstance();
        String filter = panelFilters.getOrDefault(admin.getUniqueId(), FILTER_ALL);

        Session[] sessions = Arrays.stream(sm.getAllSessions())
                .filter(s -> switch (filter) {
                    case FILTER_LOBBY   -> s.getState() == SessionState.LOBBY;
                    case FILTER_RUNNING -> s.getState() == SessionState.STARTING || s.getState() == SessionState.STARTED;
                    default             -> true;
                })
                .toArray(Session[]::new);

        GuiBuilder builder = new GuiBuilder()
                .size(6)
                .title(Messages.getComponent("gui.admin-panel.title", admin));

        GuiItem border = GuiItem.of(Material.BLACK_STAINED_GLASS_PANE).name(Component.space());
        for (int s : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 45, 46, 47, 48, 49, 50, 51, 52, 53})
            builder.slot(s, border);

        // Row 6 filter buttons
        builder.slot(46, filterButton(admin, FILTER_ALL,     Material.COMPASS,      "&f&lAll sessions",   filter));
        builder.slot(47, filterButton(admin, FILTER_LOBBY,   Material.OAK_DOOR,     "&e&lLobby only",     filter));
        builder.slot(48, filterButton(admin, FILTER_RUNNING, Material.LIME_CONCRETE, "&a&lRunning only",  filter));
        builder.slot(53, GuiItem.of(Material.BARRIER)
                .name("&c&lClose")
                .onClick(e -> e.getWhoClicked().closeInventory()));

        if (sessions.length == 0) {
            builder.slot(22, GuiItem.of(Material.GRAY_STAINED_GLASS_PANE)
                    .name(Messages.getComponent("gui.admin-panel.no-sessions", admin)));
        }

        for (int i = 0; i < Math.min(sessions.length, PANEL_CARD_SLOTS.length); i++) {
            builder.slot(PANEL_CARD_SLOTS[i], buildSessionCard(sessions[i], admin));
        }

        return builder.build();
    }

    private GuiItem filterButton(Player admin, String value, Material mat, String label, String active) {
        boolean isActive = value.equals(active);
        return GuiItem.of(isActive ? Material.LIME_STAINED_GLASS_PANE : mat)
                .name((isActive ? "&a" : "&7") + label.replace("&f&l", "&a&l").replace("&e&l", "&a&l").replace("&7&l", "&7"))
                .onClick(e -> {
                    panelFilters.put(e.getWhoClicked().getUniqueId(), value);
                    openPanel((Player) e.getWhoClicked());
                });
    }

    private GuiItem buildSessionCard(Session session, Player admin) {
        Player host = session.getHost();
        String hostName = host != null ? host.getName() : "?";

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        if (host != null) {
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) { meta.setPlayerProfile(host.getPlayerProfile()); skull.setItemMeta(meta); }
        }

        List<Component> lore = new ArrayList<>();
        lore.add(legacy("&7State: &f" + session.getState().name()));

        MiniGame mg = session.getMiniGame();
        if (mg != null) {
            Object mgName = mg.getValue("name");
            lore.add(Messages.getComponent("gui.admin-panel.session.game", admin,
                    mgName != null ? mgName.toString() : mg.getId()));
        }
        if (session.getGameMap() != null)
            lore.add(Messages.getComponent("gui.admin-panel.session.map", admin, session.getGameMap().getId()));

        int players = session.getPlayers().size() + session.getLobbyMembers().size();
        lore.add(Messages.getComponent("gui.admin-panel.session.players", admin, players, session.getTotalRounds()));

        if (session.getState() == SessionState.STARTED && session.getStartedAt() > 0) {
            long elapsed = (System.currentTimeMillis() - session.getStartedAt()) / 1000;
            lore.add(Messages.getComponent("gui.admin-panel.session.uptime", admin, formatDuration(elapsed)));
        }
        lore.add(Messages.getComponent("gui.admin-panel.session.rounds", admin,
                session.getCurrentRound(), session.getTotalRounds()));

        lore.add(Component.empty());
        lore.add(legacy("&eLeft-click: &7Detail view"));
        lore.add(legacy("&cRight-click: &7Force disband"));
        if (session.getState() == SessionState.STARTED)
            lore.add(legacy("&cShift+right: &7Force end game"));

        String sessionId = session.getId();
        return GuiItem.of(skull)
                .name("&3" + hostName + "'s session")
                .lore(lore)
                .onClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    Session s = SessionManager.getInstance().getSessionById(sessionId);
                    if (s == null) { openPanel(p); return; }

                    if (e.isShiftClick() && e.getClick().isRightClick()) {
                        if (s.getState() == SessionState.STARTED) {
                            SessionLifecycleManagerImpl.getInstance().endGame(s, "admin");
                        }
                        openPanel(p);
                    } else if (e.getClick().isRightClick()) {
                        SessionLifecycleManagerImpl.getInstance().disbandSession(s, DisbandReason.EXPLICIT_DISBAND);
                        openPanel(p);
                    } else {
                        openDetailFor(p, s);
                    }
                });
    }

    // ─── Detail view (per-session) ───────────────────────────────────────────

    public void openDetailFor(Player admin, Session session) {
        admin.openInventory(buildDetail(admin, session));
    }

    private Inventory buildDetail(Player admin, Session session) {
        SessionLifecycleManagerImpl lifecycle = SessionLifecycleManagerImpl.getInstance();
        String sessionId = session.getId();

        GuiBuilder builder = new GuiBuilder()
                .size(6)
                .title(legacy("&3&lSession: &f" + session.getId()));

        GuiItem border = GuiItem.of(Material.BLACK_STAINED_GLASS_PANE).name(Component.space());
        for (int s : new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 45, 46, 47, 48, 49, 50, 51, 52, 53})
            builder.slot(s, border);

        // Row 1 controls
        builder.slot(0, GuiItem.of(Material.ARROW)
                .name("&a&lBack to panel")
                .onClick(e -> openPanel((Player) e.getWhoClicked())));

        // Force start (LOBBY only)
        if (session.getState() == SessionState.LOBBY) {
            builder.slot(46, GuiItem.of(Material.LIME_CONCRETE)
                    .name(Messages.getComponent("gui.admin-panel.force-start", admin))
                    .onClick(e -> {
                        Player p = (Player) e.getWhoClicked();
                        Session s = SessionManager.getInstance().getSessionById(sessionId);
                        if (s != null) lifecycle.startGame(s, GameStartReason.ADMIN_FORCE, null);
                        openPanel(p);
                    }));
        }

        // Force end (STARTED only)
        if (session.getState() == SessionState.STARTED) {
            builder.slot(48, GuiItem.of(Material.RED_CONCRETE)
                    .name(Messages.getComponent("gui.admin-panel.force-end", admin))
                    .onClick(e -> {
                        Player p = (Player) e.getWhoClicked();
                        Session s = SessionManager.getInstance().getSessionById(sessionId);
                        if (s != null) lifecycle.endGame(s, "admin");
                        openPanel(p);
                    }));
        }

        // Rounds − and +
        builder.slot(49, GuiItem.of(Material.RED_STAINED_GLASS_PANE)
                .name("&c&l− Round")
                .lore(List.of(legacy("&7Current: &f" + session.getTotalRounds())))
                .onClick(e -> {
                    Session s = SessionManager.getInstance().getSessionById(sessionId);
                    if (s != null && s.getTotalRounds() > 1) s.setTotalRounds(s.getTotalRounds() - 1);
                    if (s != null) openDetailFor((Player) e.getWhoClicked(), s);
                }));
        builder.slot(50, GuiItem.of(Material.LIME_STAINED_GLASS_PANE)
                .name("&a&l+ Round")
                .lore(List.of(legacy("&7Current: &f" + session.getTotalRounds())))
                .onClick(e -> {
                    Session s = SessionManager.getInstance().getSessionById(sessionId);
                    if (s != null) s.setTotalRounds(s.getTotalRounds() + 1);
                    if (s != null) openDetailFor((Player) e.getWhoClicked(), s);
                }));

        // Force disband
        builder.slot(53, GuiItem.of(Material.BARRIER)
                .name(Messages.getComponent("gui.admin-panel.force-disband", admin))
                .onClick(e -> {
                    Session s = SessionManager.getInstance().getSessionById(sessionId);
                    if (s != null) lifecycle.disbandSession(s, DisbandReason.EXPLICIT_DISBAND);
                    openPanel((Player) e.getWhoClicked());
                }));

        // Member list
        List<Player> members = new ArrayList<>();
        members.addAll(session.getLobbyMembers());
        members.addAll(session.getPlayers());
        members.addAll(session.getSpectators());

        for (int i = 0; i < Math.min(members.size(), DETAIL_MEMBER_SLOTS.length); i++) {
            builder.slot(DETAIL_MEMBER_SLOTS[i], buildMemberCard(members.get(i), session, admin));
        }

        return builder.build();
    }

    private GuiItem buildMemberCard(Player member, Session session, Player admin) {
        SessionRole role = session.getRole(member);
        String roleStr = role != null ? role.name() : "?";
        boolean isHost = member.equals(session.getHost());

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) { meta.setPlayerProfile(member.getPlayerProfile()); skull.setItemMeta(meta); }

        String sessionId = session.getId();
        return GuiItem.of(skull)
                .name("&f" + member.getName() + (isHost ? " &6[Host]" : ""))
                .lore(List.of(legacy("&7Role: &f" + roleStr),
                        legacy("&eLeft-click: &7View profile"),
                        legacy("&cRight-click: &7Kick from session")))
                .onClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (e.getClick().isRightClick()) {
                        if (member.isOnline()) {
                            SessionLifecycleManagerImpl.getInstance().leaveSession(member);
                        }
                        Session s = SessionManager.getInstance().getSessionById(sessionId);
                        if (s != null) openDetailFor(p, s); else openPanel(p);
                    } else {
                        PlayerProfileGuiService.getInstance().openFor(p, member);
                    }
                });
    }

    // ─── Listener ────────────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof GuiHolder)) return;
        // Panel filter state is intentionally preserved across open/close
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static String formatDuration(long seconds) {
        long h = seconds / 3600, m = (seconds % 3600) / 60, s = seconds % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s);
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
