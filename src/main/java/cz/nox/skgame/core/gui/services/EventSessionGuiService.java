package cz.nox.skgame.core.gui.services;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.event.EventSessionOpenEvent;
import cz.nox.skgame.api.game.event.GamePlayerSessionLeave;
import cz.nox.skgame.api.game.event.SessionDisbandEvent;
import cz.nox.skgame.api.game.event.GameStartEvent;
import cz.nox.skgame.api.game.event.GameStopEvent;
import cz.nox.skgame.api.game.event.LobbyEnterEvent;
import cz.nox.skgame.api.game.event.PlayerRoleChangeEvent;
import cz.nox.skgame.api.game.event.SessionSettingsChangedEvent;
import cz.nox.skgame.api.game.event.SpectatorJoinEvent;
import cz.nox.skgame.api.game.model.CustomValue;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.SessionVisibility;
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
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EventSessionGuiService implements Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static EventSessionGuiService instance;
    private final Set<UUID> activeViewers = new HashSet<>();

    private EventSessionGuiService() {}

    public static synchronized EventSessionGuiService getInstance() {
        if (instance == null) instance = new EventSessionGuiService();
        return instance;
    }

    private Component c(String s) { return LEGACY.deserialize(s); }

    public void openFor(Player player) {
        Session session = SessionManager.getInstance().getEventSession();
        if (session == null) {
            if (!player.hasPermission("skgame.admin")) {
                Messages.send(player, "event.not-running");
                return;
            }
            session = SessionLifecycleManagerImpl.getInstance().createEventSession(player);
            if (session == null) {
                Messages.send(player, "event.not-running");
                return;
            }
        }
        boolean isNewViewer = !activeViewers.contains(player.getUniqueId());
        player.openInventory(buildFor(player, session));
        activeViewers.add(player.getUniqueId());
        if (isNewViewer) {
            // Refresh other open GUIs so they see the new viewer in the player head grid
            Bukkit.getScheduler().runTask(SkGame.getInstance(), () ->
                new HashSet<>(activeViewers).stream()
                    .filter(uuid -> !uuid.equals(player.getUniqueId()))
                    .map(Bukkit::getPlayer)
                    .filter(p -> p != null && p.isOnline())
                    .forEach(this::openFor)
            );
        }
    }

    private Inventory buildFor(Player player, Session session) {
        boolean isAdmin = player.hasPermission("skgame.admin");

        MiniGame mg = session.getMiniGame();
        GameMap map = session.getGameMap();
        String mgName = mg != null && mg.getValue("name") != null ? mg.getValue("name").toString() : "Not set";
        String mapName = map != null ? map.getId() : "Not set";
        int total = session.getLobbyMembers().size() + session.getPlayers().size();
        boolean unlocked = session.getVisibility() == SessionVisibility.PUBLIC;
        boolean started = session.getState() == SessionState.STARTED;
        boolean inPreparation = session.getState() == SessionState.PREPARATION;

        GuiBuilder builder = new GuiBuilder()
                .size(6)
                .title(Messages.get(isAdmin ? "gui.event.slot.gui-title" : "gui.event.player.title", player));

        // Black border top + bottom
        GuiItem glass = GuiItem.of(Material.BLACK_STAINED_GLASS_PANE).name(" ");
        builder.fill(0, 8, glass).fill(45, 53, glass);

        // Minigame selector (admin: clickable; player: display only)
        if (isAdmin) {
            builder.slot(1, GuiItem.of(mg != null ? Material.BOOK : Material.WRITABLE_BOOK)
                    .name(c("&eMiniGame: &f" + mgName))
                    .lore(c("&7Click to select"))
                    .onClick(e -> MinigamesGuiService.getInstance().openFor((Player) e.getWhoClicked(), EventSessionGuiService.getInstance()::openFor)));
        } else {
            builder.slot(1, GuiItem.of(mg != null ? Material.BOOK : Material.WRITABLE_BOOK)
                    .name(c("&eMiniGame: &f" + mgName)));
        }

        // Map selector (admin: clickable; player: display only)
        if (isAdmin) {
            builder.slot(3, GuiItem.of(map != null ? Material.MAP : Material.FILLED_MAP)
                    .name(c("&eMap: &f" + mapName))
                    .lore(c("&7Click to select"))
                    .onClick(e -> MapsGuiService.getInstance().openFor((Player) e.getWhoClicked(), EventSessionGuiService.getInstance()::openFor)));
        } else {
            builder.slot(3, GuiItem.of(map != null ? Material.MAP : Material.FILLED_MAP)
                    .name(c("&eMap: &f" + mapName)));
        }

        // Player count (admin: clickable → ban management; player: display only)
        if (isAdmin) {
            builder.slot(5, GuiItem.of(Material.PLAYER_HEAD)
                    .name(c("&ePlayers in lobby: &f" + total))
                    .lore(c("&7Click to manage bans"))
                    .onClick(e -> openBannedGui((Player) e.getWhoClicked(), session)));
        } else {
            builder.slot(5, GuiItem.of(Material.PLAYER_HEAD)
                    .name(c("&ePlayers in lobby: &f" + total)));
        }

        // Rounds (admin: clickable ±; player: display only)
        int rounds = session.getTotalRounds();
        if (isAdmin) {
            builder.slot(6, GuiItem.of(Material.CLOCK)
                    .name(c("&eRounds: &f" + rounds))
                    .lore(c("&aLeft-click: &f+1"), c("&cRight-click: &f-1"))
                    .onClick(e -> {
                        if (e.getClick().isRightClick()) {
                            session.setTotalRounds(session.getTotalRounds() - 1);
                        } else {
                            session.setTotalRounds(session.getTotalRounds() + 1);
                        }
                        Bukkit.getPluginManager().callEvent(new SessionSettingsChangedEvent(session, "rounds"));
                        openFor((Player) e.getWhoClicked());
                    }));
        } else {
            builder.slot(6, GuiItem.of(Material.CLOCK)
                    .name(c("&eRounds: &f" + rounds)));
        }

        // State
        builder.slot(7, GuiItem.of(Material.COMPASS)
                .name(c("&eState: &f" + session.getState().name()))
                .lore(c("&eVisibility: &f" + session.getVisibility().name())));

        // Middle slots 9-44 — Player heads (spectators shown separately via slot 46 lore)
        java.util.List<Player> allMembers = new java.util.ArrayList<>();
        allMembers.addAll(session.getLobbyMembers());
        allMembers.addAll(session.getPlayers());
        // Include admins currently viewing the GUI but not yet session members (locked phase)
        for (UUID viewerUuid : new HashSet<>(activeViewers)) {
            Player vp = Bukkit.getPlayer(viewerUuid);
            if (vp != null && vp.isOnline() && !allMembers.contains(vp)) allMembers.add(vp);
        }
        if (!allMembers.contains(player)) allMembers.add(player);
        int[] playerSlots = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        };
        for (int i = 0; i < Math.min(allMembers.size(), playerSlots.length); i++) {
            Player member = allMembers.get(i);
            boolean isHost = member.equals(session.getHost());
            String role = session.getLobbyMembers().contains(member) ? "Lobby"
                    : session.getPlayers().contains(member) ? "Playing" : "Spectator";
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            if (meta != null) { meta.setPlayerProfile(member.getPlayerProfile()); skull.setItemMeta(meta); }
            java.util.List<Component> headLore = new java.util.ArrayList<>();
            headLore.add(c("&7Role: &f" + role));
            if (isAdmin) {
                headLore.add(c("&cRight-click: &7Kick"));
                headLore.add(c("&4Shift+Right-click: &7Ban"));
            }
            GuiItem headItem = GuiItem.of(skull)
                    .name(c((isHost ? "&e✦ " : "&7") + member.getName()))
                    .lore(headLore);
            if (isAdmin) {
                final Player memberFinal = member;
                headItem.onClick(e -> {
                    if (!e.getClick().isRightClick()) return;
                    if (e.getClick().isShiftClick()) {
                        session.addBan(memberFinal.getUniqueId(), memberFinal.getName());
                        Messages.send(memberFinal, "event.banned");
                        SessionLifecycleManagerImpl.getInstance().leaveSession(memberFinal);
                    } else {
                        Messages.send(memberFinal, "event.kicked");
                        SessionLifecycleManagerImpl.getInstance().leaveSession(memberFinal);
                    }
                    openFor((Player) e.getWhoClicked());
                });
            }
            builder.slot(playerSlots[i], headItem);
        }

        // Slot 45 — Unlock / Start / Stop (admin only; non-admin: black glass from fill)
        if (isAdmin) {
            if (!unlocked) {
                if (mg == null || map == null) {
                    builder.slot(45, GuiItem.of(Material.GRAY_STAINED_GLASS_PANE)
                            .name(c("&7Unlock Event"))
                            .lore(c("&cSet minigame and map first"))
                            .onClick(e -> Messages.send((Player) e.getWhoClicked(), "event.unlock.missing-setup")));
                } else {
                    builder.slot(45, GuiItem.of(Material.LIME_STAINED_GLASS_PANE)
                            .name(c("&a&lUnlock Event"))
                            .lore(c("&7Opens the event to all players"))
                            .onClick(e -> unlockEvent((Player) e.getWhoClicked(), session)));
                }
            } else if (inPreparation) {
                builder.slot(45, GuiItem.of(Material.YELLOW_CONCRETE)
                        .name(c("&e&lPreparing..."))
                        .lore(c("&7Team selection / map vote in progress")));
            } else if (!started) {
                builder.slot(45, GuiItem.of(Material.LIME_CONCRETE)
                        .name(c("&a&lStart Game"))
                        .lore(c("&7Start the minigame now"))
                        .onClick(e -> {
                            SessionLifecycleManagerImpl.getInstance().startGame(session, GameStartReason.ADMIN_FORCE, null);
                            openFor((Player) e.getWhoClicked());
                        }));
            } else {
                builder.slot(45, GuiItem.of(Material.RED_CONCRETE)
                        .name(c("&c&lStop Game"))
                        .lore(c("&7Force-stop the running game"))
                        .onClick(e -> {
                            SessionLifecycleManagerImpl.getInstance().endGame(session, "admin");
                            openFor((Player) e.getWhoClicked());
                        }));
            }
        }

        // Slot 46 — Spectate (all; only when STARTED or PREPARATION)
        if (started || inPreparation) {
            Session playerSession = SessionManager.getInstance().getSession(player);
            boolean inThisSession = playerSession != null && playerSession.getId().equals(session.getId());
            boolean isSpectator = inThisSession && session.getSpectators().contains(player);
            boolean isLobbyMember = inThisSession && session.getLobbyMembers().contains(player);
            int spectatorCount = session.getSpectators().size();
            List<Component> spectLore = new ArrayList<>();

            if (isSpectator) {
                spectLore.add(c("&7You are spectating"));
                if (spectatorCount > 0) spectLore.add(c("&7Spectating: &f" + spectatorCount));
                builder.slot(46, GuiItem.of(Material.SPYGLASS)
                        .name(c("&b&lSpectating"))
                        .lore(spectLore));
            } else if (isLobbyMember) {
                spectLore.add(c("&7Switch to spectator"));
                if (spectatorCount > 0) spectLore.add(c("&7Spectating: &f" + spectatorCount));
                builder.slot(46, GuiItem.of(Material.SPYGLASS)
                        .name(c("&b&lSpectate"))
                        .lore(spectLore)
                        .onClick(e -> {
                            Player p = (Player) e.getWhoClicked();
                            session.setRole(p, SessionRole.SPECTATOR);
                            openFor(p);
                        }));
            } else if (!inThisSession) {
                spectLore.add(c("&7Watch the game as spectator"));
                if (spectatorCount > 0) spectLore.add(c("&7Spectating: &f" + spectatorCount));
                builder.slot(46, GuiItem.of(Material.SPYGLASS)
                        .name(c("&b&lSpectate"))
                        .lore(spectLore)
                        .onClick(e -> {
                            Player p = (Player) e.getWhoClicked();
                            SessionLifecycleManagerImpl.getInstance().joinAsSpectator(p, session);
                            openFor(p);
                        }));
            }
        }

        // Slot 47 — Disband (admin only; non-admin: black glass from fill)
        if (isAdmin) {
            builder.slot(47, GuiItem.of(Material.BARRIER)
                    .name(c("&c&lDisband Event Session"))
                    .lore(c("&7Removes the event session entirely"))
                    .onClick(e -> {
                        Player p = (Player) e.getWhoClicked();
                        // Remove from activeViewers before disband so the scheduled update() next tick
                        // does not see this player with a GuiHolder open and close the main GUI.
                        activeViewers.remove(p.getUniqueId());
                        SessionLifecycleManagerImpl.getInstance().disbandSession(session, DisbandReason.EXPLICIT_DISBAND);
                        MainGuiService.getInstance().openFor(p);
                    }));
        }

        // Slot 48 — session value defs (≥2: sub-GUI; 1: inline)
        if (mg != null && !mg.getSessionValueDefs().isEmpty()) {
            Map<String, CustomValue> svDefs = mg.getSessionValueDefs();
            if (svDefs.size() >= 2) {
                builder.slot(48, GuiItem.of(Material.COMPARATOR)
                    .name(c("&b&lGame Settings"))
                    .lore(c("&7Click to configure"), c("&8" + svDefs.size() + " settings"))
                    .onClick(e -> {
                        Player p = (Player) e.getWhoClicked();
                        SessionGuiService.openSessionValuesGui(p, session, isAdmin, () -> openFor(p));
                    }));
            } else {
                Map.Entry<String, CustomValue> svEntry = svDefs.entrySet().iterator().next();
                String svKey = svEntry.getKey();
                CustomValue svCv = svEntry.getValue();
                GuiItem svItem = SessionGuiService.buildSessionValueItem(svKey, svCv, session);
                if (isAdmin) {
                    svItem.onLeftClick(e -> {
                        SessionGuiService.advanceSessionValue(session, svKey, svCv, true);
                        openFor((Player) e.getWhoClicked());
                    }).onRightClick(e -> {
                        SessionGuiService.advanceSessionValue(session, svKey, svCv, false);
                        openFor((Player) e.getWhoClicked());
                    });
                }
                builder.slot(48, svItem);
            }
        }

        // Slot 53 — Back (left-click) / Leave event (right-click, any player)
        builder.slot(53, GuiItem.of(Material.SPRUCE_DOOR)
                .name(c("&c&lBack"))
                .lore(c("&7Left-click: &fBack"), c("&7Right-click: &cLeave event"))
                .onClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (e.getClick().isRightClick()) {
                        SessionLifecycleManagerImpl.getInstance().leaveSession(p);
                        // Open main GUI last — overrides the event GUI that update() re-opens
                        // synchronously inside leaveSession via onSessionLeave → update() → openFor().
                        MainGuiService.getInstance().openFor(p);
                        return;
                    }
                    MainGuiService.getInstance().openFor(p);
                }));

        return builder.build();
    }

    private void openBannedGui(Player admin, Session session) {
        GuiBuilder builder = new GuiBuilder()
                .size(3)
                .title(Messages.get("gui.event.banned.title", admin));

        GuiItem glass = GuiItem.of(Material.BLACK_STAINED_GLASS_PANE).name(" ");
        builder.fill(0, 8, glass).fill(18, 26, glass);

        Map<UUID, String> banned = session.getBannedEntries();
        if (banned.isEmpty()) {
            builder.slot(13, GuiItem.of(Material.LIME_CONCRETE)
                    .name(c(Messages.get("gui.event.banned.empty", admin))));
        } else {
            int[] bannedSlots = {9, 10, 11, 12, 13, 14, 15, 16, 17};
            List<Map.Entry<UUID, String>> entries = new ArrayList<>(banned.entrySet());
            for (int i = 0; i < Math.min(entries.size(), bannedSlots.length); i++) {
                UUID uuid = entries.get(i).getKey();
                String reason = entries.get(i).getValue();
                OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                String name = op.getName() != null ? op.getName() : uuid.toString().substring(0, 8);

                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                if (op.isOnline() && op.getPlayer() != null) {
                    SkullMeta meta = (SkullMeta) skull.getItemMeta();
                    if (meta != null) {
                        meta.setPlayerProfile(op.getPlayer().getPlayerProfile());
                        skull.setItemMeta(meta);
                    }
                }

                final UUID uuidFinal = uuid;
                builder.slot(bannedSlots[i], GuiItem.of(skull)
                        .name(c("&c" + name))
                        .lore(c("&7Reason: &f" + reason), c("&aClick to unban"))
                        .onClick(e -> {
                            session.removeBan(uuidFinal);
                            openBannedGui((Player) e.getWhoClicked(), session);
                        }));
            }
        }

        builder.slot(22, GuiItem.of(Material.SPRUCE_DOOR)
                .name(c("&c&lBack"))
                .onClick(e -> openFor((Player) e.getWhoClicked())));

        admin.openInventory(builder.build());
        activeViewers.add(admin.getUniqueId());
    }

    private void unlockEvent(Player admin, Session session) {
        session.setVisibility(SessionVisibility.PUBLIC);
        Bukkit.getPluginManager().callEvent(new SessionSettingsChangedEvent(session, "visibility"));
        Bukkit.getPluginManager().callEvent(new EventSessionOpenEvent(session));

        MiniGame mg = session.getMiniGame();
        String mgName = mg != null && mg.getValue("name") != null ? mg.getValue("name").toString() : "Event";
        GameMap map = session.getGameMap();
        String mapName = map != null ? map.getId() : "—";

        for (Player online : Bukkit.getOnlinePlayers()) {
            Messages.send(online, "event.broadcast", mgName, mapName);
            online.playSound(online.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        }
        openFor(admin);
    }

    public void update() {
        for (UUID uuid : new HashSet<>(activeViewers)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) { activeViewers.remove(uuid); continue; }
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof GuiHolder) {
                Session session = SessionManager.getInstance().getEventSession();
                if (session != null) openFor(p); else p.closeInventory();
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
    public void onLobbyEnter(LobbyEnterEvent event) {
        if (event.getSession().isEventSession()) update();
    }

    @EventHandler
    public void onSpectatorJoin(SpectatorJoinEvent event) {
        if (event.getSession().isEventSession()) update();
    }

    @EventHandler
    public void onRoleChange(PlayerRoleChangeEvent event) {
        if (event.getSession().isEventSession()) update();
    }

    @EventHandler
    public void onSessionSettingsChanged(SessionSettingsChangedEvent event) {
        if (event.getSession().isEventSession()) update();
    }

    @EventHandler
    public void onGameStart(GameStartEvent event) {
        if (event.getSession().isEventSession()) update();
    }

    @EventHandler
    public void onGameStop(GameStopEvent event) {
        if (event.getSession().isEventSession()) update();
    }

    @EventHandler
    public void onSessionLeave(GamePlayerSessionLeave event) {
        if (event.getSession().isEventSession()) update();
    }

    @EventHandler
    public void onSessionDisband(SessionDisbandEvent event) {
        if (!event.getSession().isEventSession()) return;
        Bukkit.getScheduler().runTask(SkGame.getInstance(), this::update);
    }
}
