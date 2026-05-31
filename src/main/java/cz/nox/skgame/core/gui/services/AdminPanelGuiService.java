package cz.nox.skgame.core.gui.services;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.event.GameStartEvent;
import cz.nox.skgame.api.game.event.GameStopEvent;
import cz.nox.skgame.api.game.event.SessionCreateEvent;
import cz.nox.skgame.api.game.event.SessionDisbandEvent;
import cz.nox.skgame.api.game.event.SessionSettingsChangedEvent;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.SessionVisibility;
import cz.nox.skgame.api.game.model.type.DisbandReason;
import cz.nox.skgame.api.game.model.type.GameStartReason;
import cz.nox.skgame.api.game.model.type.MapSelectionMode;
import cz.nox.skgame.api.game.model.type.SessionRole;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.api.gui.GuiBuilder;
import cz.nox.skgame.api.gui.GuiHolder;
import cz.nox.skgame.api.gui.GuiItem;
import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.game.lifecycle.SessionLifecycleManagerImpl;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
    // tracks which admins currently have the panel list open for live refresh
    private final Set<UUID> panelViewers = ConcurrentHashMap.newKeySet();
    // stores the exact Inventory ref for each viewer's open panel — used to distinguish
    // the panel-list from sub-GUIs (detail view, player profile) during live refresh
    private final java.util.Map<UUID, Inventory> panelInventories = new ConcurrentHashMap<>();
    // admin UUID → session ID they're force-ending with custom message
    private final Map<UUID, String> pendingForceEndSessions = new ConcurrentHashMap<>();

    private AdminPanelGuiService() {}

    public boolean isPendingForceEnd(Player p) { return pendingForceEndSessions.containsKey(p.getUniqueId()); }

    public static synchronized AdminPanelGuiService getInstance() {
        if (instance == null) instance = new AdminPanelGuiService();
        return instance;
    }

    // ─── Panel (session list) ────────────────────────────────────────────────

    public void openPanel(Player admin) {
        panelViewers.add(admin.getUniqueId());
        Inventory inv = buildPanel(admin);
        panelInventories.put(admin.getUniqueId(), inv);
        admin.openInventory(inv);
    }

    private Inventory buildPanel(Player admin) {
        SessionManager sm = SessionManager.getInstance();
        String filter = panelFilters.getOrDefault(admin.getUniqueId(), FILTER_ALL);

        Session[] sessions = Arrays.stream(sm.getAllSessions())
                .filter(s -> switch (filter) {
                    case FILTER_LOBBY   -> s.getState() == SessionState.LOBBY;
                    case FILTER_RUNNING -> s.getState() == SessionState.STARTING || s.getState() == SessionState.STARTED
                            || s.getState() == SessionState.ENDED;
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
        Player storedHost = session.getHost();
        java.util.UUID hostUuid = storedHost != null ? storedHost.getUniqueId() : null;

        // Resolve current online player by UUID (single authoritative lookup for both name + skull)
        Player onlineHost = hostUuid != null ? Bukkit.getPlayer(hostUuid) : null;

        // Name: current online > offline last-known > session-ID prefix fallback
        String hostName;
        if (onlineHost != null) {
            hostName = onlineHost.getName();
        } else if (hostUuid != null) {
            String offlineName = Bukkit.getOfflinePlayer(hostUuid).getName();
            hostName = offlineName != null ? offlineName : session.getId().substring(0, Math.min(8, session.getId().length())) + "…";
        } else {
            hostName = session.getId().substring(0, Math.min(8, session.getId().length())) + "…";
        }

        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        if (onlineHost != null) {
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) { meta.setPlayerProfile(onlineHost.getPlayerProfile()); skull.setItemMeta(meta); }
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

        // Force start (LOBBY only) / force-finish prep (PREPARATION only)
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
        if (session.getState() == SessionState.PREPARATION) {
            builder.slot(46, GuiItem.of(Material.LIME_CONCRETE)
                    .name(Messages.getComponent("gui.admin-panel.force-start", admin))
                    .lore(List.of(legacy("&7Bypasses the ready-check")))
                    .onClick(e -> {
                        Player p = (Player) e.getWhoClicked();
                        Session s = SessionManager.getInstance().getSessionById(sessionId);
                        if (s != null && s.getState() == SessionState.PREPARATION) lifecycle.finishPreparation(s);
                        openPanel(p);
                    }));
        }

        // Force end (STARTED only) — opens chat-input flow for custom broadcast message
        if (session.getState() == SessionState.STARTED) {
            builder.slot(48, GuiItem.of(Material.RED_CONCRETE)
                    .name(Messages.getComponent("gui.admin-panel.force-end", admin))
                    .lore(List.of(legacy("&7Type a message to broadcast to session")))
                    .onClick(e -> {
                        Player p = (Player) e.getWhoClicked();
                        Session s = SessionManager.getInstance().getSessionById(sessionId);
                        if (s == null || s.getState() != SessionState.STARTED) { openPanel(p); return; }
                        pendingForceEndSessions.put(p.getUniqueId(), sessionId);
                        p.closeInventory();
                        p.sendMessage(legacy("&eType a broadcast message (or &7cancel&e to abort):"));
                    }));
        }

        // Slot 1 — Map swap (admin override): opens map browser; applies immediately (LOBBY/PREP) or deferred (STARTED/ENDED)
        builder.slot(1, GuiItem.of(Material.FILLED_MAP)
                .name("&b&lMap Swap")
                .lore(List.of(
                        legacy("&7Current: &f" + (session.getGameMap() != null ? session.getGameMap().getId() : "none")),
                        legacy("&eClick: &7Open map browser")))
                .onClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    Session s = SessionManager.getInstance().getSessionById(sessionId);
                    if (s == null) { openPanel(p); return; }
                    MapsGuiService.getInstance().openFor(p);
                }));

        // Slot 47 — Map selection mode cycle (admin override; deferred during STARTED/ENDED)
        MapSelectionMode currentMode = session.getMapSelectionMode();
        String modeLabel = switch (currentMode) {
            case SPECIFIC -> "&7SPECIFIC";
            case VOTE     -> "&eVOTE";
            case RANDOM   -> "&aRANDOM";
        };
        builder.slot(47, GuiItem.of(Material.COMPASS)
                .name("&b&lMap Mode")
                .lore(List.of(
                        legacy("&7Current: " + modeLabel),
                        legacy("&eClick: &7Cycle SPECIFIC→VOTE→RANDOM"),
                        session.hasPendingAdminChanges() ? legacy("&6Pending change queued") : Component.empty()))
                .onClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    Session s = SessionManager.getInstance().getSessionById(sessionId);
                    if (s == null) { openPanel(p); return; }
                    MapSelectionMode next = switch (s.getMapSelectionMode()) {
                        case SPECIFIC -> MapSelectionMode.VOTE;
                        case VOTE     -> MapSelectionMode.RANDOM;
                        case RANDOM   -> MapSelectionMode.SPECIFIC;
                    };
                    if (s.getState() == SessionState.STARTED || s.getState() == SessionState.ENDED) {
                        s.setPendingAdminChange("map-mode", next);
                        p.sendMessage(legacy("&eMap mode queued — applies at round end."));
                    } else {
                        s.setMapSelectionMode(next);
                    }
                    SessionGuiService.getInstance().update(s);
                    openDetailFor(p, s);
                }));

        // Slot 51 — Persistent toggle (immediate always — non-disruptive)
        builder.slot(51, GuiItem.of(session.isPersistent() ? Material.NETHER_STAR : Material.GRAY_DYE)
                .name("&b&lPersistent")
                .lore(List.of(legacy(session.isPersistent() ? "&aEnabled" : "&7Disabled")))
                .onClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    Session s = SessionManager.getInstance().getSessionById(sessionId);
                    if (s == null) { openPanel(p); return; }
                    s.setPersistent(!s.isPersistent());
                    openDetailFor(p, s);
                }));

        // Slot 52 — Visibility cycle (immediate always — non-disruptive)
        SessionVisibility vis = session.getVisibility();
        String visLabel = switch (vis) {
            case PUBLIC      -> "&aPublic";
            case INVITE_ONLY -> "&eInvite-only";
            case CODE        -> "&6Code";
            default          -> vis.name();
        };
        builder.slot(52, GuiItem.of(Material.OMINOUS_TRIAL_KEY)
                .name("&b&lVisibility")
                .lore(List.of(legacy("&7Current: " + visLabel)))
                .onClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    Session s = SessionManager.getInstance().getSessionById(sessionId);
                    if (s == null) { openPanel(p); return; }
                    SessionVisibility nextVis = switch (s.getVisibility()) {
                        case PUBLIC      -> SessionVisibility.INVITE_ONLY;
                        case INVITE_ONLY -> SessionVisibility.CODE;
                        default          -> SessionVisibility.PUBLIC;
                    };
                    s.setVisibility(nextVis);
                    Bukkit.getPluginManager().callEvent(new SessionSettingsChangedEvent(s, "visibility"));
                    openDetailFor(p, s);
                }));

        // Rounds − and +
        builder.slot(49, GuiItem.of(Material.RED_STAINED_GLASS_PANE)
                .name("&c&l− Round")
                .lore(List.of(legacy("&7Current: &f" + session.getTotalRounds())))
                .onClick(e -> {
                    Session s = SessionManager.getInstance().getSessionById(sessionId);
                    if (s != null && s.getTotalRounds() > 1) s.setTotalRounds(s.getTotalRounds() - 1);
                    if (s != null) {
                        SessionGuiService.getInstance().update(s);
                        openDetailFor((Player) e.getWhoClicked(), s);
                    }
                }));
        builder.slot(50, GuiItem.of(Material.LIME_STAINED_GLASS_PANE)
                .name("&a&l+ Round")
                .lore(List.of(legacy("&7Current: &f" + session.getTotalRounds())))
                .onClick(e -> {
                    Session s = SessionManager.getInstance().getSessionById(sessionId);
                    if (s != null) s.setTotalRounds(s.getTotalRounds() + 1);
                    if (s != null) {
                        SessionGuiService.getInstance().update(s);
                        openDetailFor((Player) e.getWhoClicked(), s);
                    }
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
                        legacy("&cRight-click: &7Kick from session"),
                        legacy("&cShift+right-click: &7Ban from session")))
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
                })
                .onShiftClick(e -> {
                    if (e.getClick() != ClickType.SHIFT_RIGHT) return;
                    Player p = (Player) e.getWhoClicked();
                    Session s = SessionManager.getInstance().getSessionById(sessionId);
                    if (s == null) { openPanel(p); return; }
                    s.addBan(member.getUniqueId(), member.getName());
                    if (member.isOnline()) {
                        Messages.send(member, "session.ban.banned");
                        SessionLifecycleManagerImpl.getInstance().leaveSession(member);
                    }
                    Session refreshed = SessionManager.getInstance().getSessionById(sessionId);
                    if (refreshed != null) openDetailFor(p, refreshed); else openPanel(p);
                });
    }

    public int getPanelViewerCount() { return panelViewers.size(); }

    // ─── Listener ────────────────────────────────────────────────────────────

    @EventHandler public void onSessionCreate(SessionCreateEvent e)        { refreshPanelViewers(); }
    @EventHandler public void onSessionDisband(SessionDisbandEvent e)      { refreshPanelViewers(); }
    @EventHandler public void onGameStart(GameStartEvent e)                { refreshPanelViewers(); }
    @EventHandler public void onGameStop(GameStopEvent e)                  { refreshPanelViewers(); }
    @EventHandler public void onSettingsChanged(SessionSettingsChangedEvent e) { refreshPanelViewers(); }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof GuiHolder)) return;
        // OPEN_NEW: inventory replaced programmatically (refresh/navigation) — keep tracking.
        // Genuine closes (PLAYER/PLUGIN/DISCONNECT) still untrack.
        if (event.getReason() == org.bukkit.event.inventory.InventoryCloseEvent.Reason.OPEN_NEW) return;
        UUID uuid = event.getPlayer().getUniqueId();
        panelViewers.remove(uuid);
        panelInventories.remove(uuid);  // prevent stale Inventory ref leak on genuine close
        // Panel filter state is intentionally preserved across open/close
        // If admin closes GUI while pending a force-end message, cancel the pending input
        pendingForceEndSessions.remove(uuid);
    }

    // ─── Chat handlers for force-end-with-message flow ───────────────────────

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPaperChatForceEnd(AsyncChatEvent event) {
        if (!pendingForceEndSessions.containsKey(event.getPlayer().getUniqueId())) return;
        event.setCancelled(true);
        event.viewers().clear();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    @SuppressWarnings("deprecation")
    public void onLegacyChatForceEnd(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        Player p = event.getPlayer();
        String sessionId = pendingForceEndSessions.get(p.getUniqueId());
        if (sessionId == null) return;
        event.setCancelled(true);
        event.getRecipients().clear();
        String msg = event.getMessage().trim();
        Bukkit.getScheduler().runTask(SkGame.getInstance(), () -> {
            pendingForceEndSessions.remove(p.getUniqueId());
            if ("cancel".equalsIgnoreCase(msg)) { openPanel(p); return; }
            Session s = SessionManager.getInstance().getSessionById(sessionId);
            if (s != null && s.getState() == SessionState.STARTED)
                SessionLifecycleManagerImpl.getInstance().endGame(s, "admin:" + msg);
            openPanel(p);
        });
    }

    private void refreshPanelViewers() {
        for (UUID uuid : new HashSet<>(panelViewers)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) { panelViewers.remove(uuid); continue; }
            Inventory tracked = panelInventories.get(uuid);
            if (tracked == null) { panelViewers.remove(uuid); continue; } // null-guard: no stored ref
            if (p.getOpenInventory().getTopInventory() == tracked) {
                // Admin is viewing the panel list specifically — safe to rebuild
                openPanel(p);
            } else {
                // Admin moved to a sub-GUI (detail view, player profile, etc.) — skip, don't yank
                panelViewers.remove(uuid);
            }
        }
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
