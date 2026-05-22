package cz.nox.skgame.core.gui.services;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.event.GamePlayerSessionJoin;
import cz.nox.skgame.api.game.event.GamePlayerSessionLeave;
import cz.nox.skgame.api.game.event.GameStartEvent;
import cz.nox.skgame.api.game.event.PlayerRoleChangeEvent;
import cz.nox.skgame.api.game.event.SessionDisbandEvent;
import cz.nox.skgame.api.gui.event.SessionGuiOpenEvent;
import cz.nox.skgame.api.game.model.GameMap;
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
import cz.nox.skgame.core.game.PlayerManager;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.game.lifecycle.SessionLifecycleManagerImpl;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SessionGuiService implements Listener {

    private static final int[] GRAY_SLOTS   = {6, 8, 15, 16, 17, 42, 44, 51, 52};
    private static final int[] RED_SLOTS    = {5, 13, 23, 31, 41, 49};
    private static final int[] WHITE_SLOTS  = {4, 14, 22, 32, 40, 50};
    private static final int[] BLACK_SLOTS  = {0, 3, 9, 12, 21, 30, 36, 39, 45, 48};
    private static final int[] PLAYER_SLOTS = {1, 2, 10, 11, 19, 20, 28, 29, 37, 38, 46, 47};
    private static final int MAX_ROUNDS = 10;

    private static SessionGuiService instance;
    private final Map<String, Set<UUID>> viewers = new HashMap<>();

    private SessionGuiService() {}

    public static synchronized SessionGuiService getInstance() {
        if (instance == null) instance = new SessionGuiService();
        return instance;
    }

    // ─── Public API ───────────────────────────────────────────────────────────

    public void openFor(Player player) {
        Session session = SessionManager.getInstance().getSession(player);
        if (session == null) return;
        SessionGuiOpenEvent guiEvent = new SessionGuiOpenEvent(player, session);
        Bukkit.getPluginManager().callEvent(guiEvent);
        if (guiEvent.isCancelled()) return;
        player.openInventory(buildFor(player, session));
        viewers.computeIfAbsent(session.getId(), k -> new HashSet<>()).add(player.getUniqueId());
    }

    public void update(Session session) {
        Set<UUID> vset = viewers.get(session.getId());
        if (vset == null || vset.isEmpty()) return;
        for (UUID uuid : new HashSet<>(vset)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) { vset.remove(uuid); continue; }
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof GuiHolder) {
                openFor(p);
            } else {
                vset.remove(uuid);
            }
        }
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
    @EventHandler public void onRoleChange(PlayerRoleChangeEvent e)   { update(e.getSession()); }
    @EventHandler public void onPlayerJoin(GamePlayerSessionJoin e)   { update(e.getSession()); }

    @EventHandler
    public void onPlayerLeave(GamePlayerSessionLeave e) {
        Set<UUID> vset = viewers.get(e.getSession().getId());
        if (vset != null) vset.remove(e.getPlayer().getUniqueId());
        Player leaving = e.getPlayer();
        if (leaving.isOnline()
                && leaving.getOpenInventory().getTopInventory().getHolder() instanceof GuiHolder) {
            leaving.closeInventory();
        }
        update(e.getSession());
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
        SessionLifecycleManagerImpl lifecycle = SessionLifecycleManagerImpl.getInstance();
        PlayerManager pm = PlayerManager.getInstance();
        SkGame plugin = SkGame.getInstance();

        Player host = session.getHost();
        String hostName = host != null ? host.getName() : "?";

        GuiBuilder builder = new GuiBuilder()
                .size(6)
                .title(Messages.getComponent("gui.session.title", viewer, hostName));

        for (int s : GRAY_SLOTS)  builder.slot(s, GuiItem.of(Material.GRAY_STAINED_GLASS_PANE).name(Component.space()));
        for (int s : RED_SLOTS)   builder.slot(s, GuiItem.of(Material.RED_STAINED_GLASS_PANE).name(Component.space()));
        for (int s : WHITE_SLOTS) builder.slot(s, GuiItem.of(Material.WHITE_STAINED_GLASS_PANE).name(Component.space()));
        for (int s : BLACK_SLOTS) builder.slot(s, GuiItem.of(Material.BLACK_STAINED_GLASS_PANE).name(Component.space()));

        // Slot 34 — visual separator (standalone; not in any border batch; static)
        builder.slot(34, GuiItem.of(Material.LIGHT_GRAY_STAINED_GLASS_PANE).name(Component.space()));

        // Slot 7 — Spectators count + toggle
        builder.slot(7, buildSpectatorsSlot(session, viewer));

        // Slot 24 — Shuffle players (host-only)
        builder.slot(24, GuiItem.of(Material.WIND_CHARGE)
                .name(Messages.get("gui.session.shuffle.title", viewer))
                .lore(Messages.getComponent(session.isShuffle()
                        ? "gui.session.shuffle.lore-on"
                        : "gui.session.shuffle.lore-off", viewer))
                .onClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (isMidGameLocked(session, p)) return;
                    if (!isHostOnly(p, session)) return;
                    session.setShuffle(!session.isShuffle());
                    update(session);
                }));

        // Slot 25 — Minigames (host-only)
        builder.slot(25, buildMinigameSlot(session).onClick(e -> {
            Player p = (Player) e.getWhoClicked();
            if (isMidGameLocked(session, p)) return;
            if (!isHostOnly(p, session)) return;
            MinigamesGuiService.getInstance().openFor(p);
        }));

        // Slot 26 — Lobby mode toggle (host-only)
        String mode = session.getValue("mode", false) instanceof String s ? s : "public";
        builder.slot(26, GuiItem.of(Material.OMINOUS_TRIAL_KEY)
                .name("&7&lLobby mode")
                .lore(legacy("public".equals(mode) ? "&aPublic&7/Private" : "&7Public/&aPrivate"))
                .onClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (isMidGameLocked(session, p)) return;
                    if (!isHostOnly(p, session)) return;
                    String cur = session.getValue("mode", false) instanceof String str ? str : "public";
                    session.setValue("mode", "public".equals(cur) ? "private" : "public", false);
                    update(session);
                }));

        // Slot 33 — Maps (host-only; no-op if minigame not yet selected)
        builder.slot(33, buildMapsSlot(session).onClick(e -> {
            Player p = (Player) e.getWhoClicked();
            if (isMidGameLocked(session, p)) return;
            if (!isHostOnly(p, session)) return;
            MapsGuiService.getInstance().openFor(p);
        }));

        // Slot 35 — Rounds (host-only; left=+1, right=-1)
        builder.slot(35, GuiItem.of(Material.REPEATER)
                .name("&7&lRounds")
                .amount(getRounds(session))
                .onLeftClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (isMidGameLocked(session, p)) return;
                    if (!isHostOnly(p, session)) return;
                    session.setTotalRounds(Math.min(getRounds(session) + 1, MAX_ROUNDS));
                    update(session);
                })
                .onRightClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (isMidGameLocked(session, p)) return;
                    if (!isHostOnly(p, session)) return;
                    session.setTotalRounds(Math.max(getRounds(session) - 1, 1));
                    update(session);
                }));

        // Slot 43 — Ready toggle (shows viewer's own ready state)
        boolean viewerReady = Boolean.TRUE.equals(pm.getPlayer(viewer).getValue("ready", true));
        builder.slot(43, GuiItem.of(viewerReady ? Material.LIME_STAINED_GLASS_PANE : Material.ORANGE_STAINED_GLASS_PANE)
                .name(viewerReady ? "&aReady" : "&cNot ready")
                .onClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    if (session.getMiniGame() == null) {
                        Messages.send(p, "gui.session.error.no-minigame");
                        return;
                    }
                    if (session.getGameMap() == null) {
                        Messages.send(p, "gui.session.error.no-map");
                        return;
                    }
                    boolean wasReady = Boolean.TRUE.equals(pm.getPlayer(p).getValue("ready", true));
                    pm.getPlayer(p).setValue("ready", !wasReady, true);
                    update(session);
                    // Ready-check uses LOBBY members only — spectators do not count (per design)
                    if (allLobbyMembersReady(session, pm)) {
                        lifecycle.startGame(session, GameStartReason.AUTO_READY);
                    }
                }));

        // Slot 53 — Back / Leave / Disband
        builder.slot(53, GuiItem.of(Material.SPRUCE_DOOR)
                .name("&c&lBack to main menu")
                .lore(
                        legacy("&c- Left click: Back to main menu"),
                        legacy("&c- Right click: Leave"),
                        legacy("&c- Shift + Rightclick: Disband session")
                )
                .onLeftClick(e -> MainGuiService.getInstance().openFor((Player) e.getWhoClicked()))
                .onRightClick(e -> lifecycle.leaveSession((Player) e.getWhoClicked()))
                .onShiftClick(e -> {
                    if (e.getClick() != ClickType.SHIFT_RIGHT) return;
                    Player p = (Player) e.getWhoClicked();
                    if (!p.equals(session.getHost())) {
                        Messages.send(p, "gui.session.error.not-host");
                        return;
                    }
                    lifecycle.disbandSession(session, DisbandReason.EXPLICIT_DISBAND);
                }));

        // Dynamic player head slots
        // LOBBY state: show lobby members with ready indicator.
        // STARTING/STARTED state: show active players (lobby is empty during a running game).
        SessionState state = session.getState();
        Set<Player> displayMembers = (state == SessionState.LOBBY)
                ? session.getLobbyMembers()
                : session.getPlayers();
        int idx = 0;
        for (Player member : displayMembers) {
            if (idx >= PLAYER_SLOTS.length) break;
            boolean isHost = member.equals(session.getHost());
            boolean rawReady = Boolean.TRUE.equals(pm.getPlayer(member).getValue("ready", true));
            boolean isReady = state != SessionState.LOBBY || rawReady;
            boolean applyGlow = state == SessionState.LOBBY && rawReady;
            builder.slot(PLAYER_SLOTS[idx++], buildPlayerHead(member, isHost, isReady, applyGlow, viewer));
        }

        return builder.build();
    }

    private GuiItem buildSpectatorsSlot(Session session, Player viewer) {
        PlayerManager pm = PlayerManager.getInstance();
        boolean viewerIsHost = viewer.equals(session.getHost());
        Set<Player> spectators = session.getSpectators();
        int count = Math.max(1, Math.min(spectators.size(), 64));

        List<Component> lore = spectators.stream()
                .map(p -> legacy("&7" + p.getName()))
                .collect(Collectors.toList());

        // Opt-in status lore (STARTED+SPECTATOR viewer only)
        boolean viewerIsSpectator = session.getRole(viewer) == SessionRole.SPECTATOR;
        if (viewerIsSpectator && session.getState() == SessionState.STARTED) {
            boolean wantsJoin = Boolean.TRUE.equals(pm.getPlayer(viewer).getValue("join_party_after_game", true));
            lore.add(Component.empty());
            lore.add(legacy(wantsJoin ? "&aWill join party after this game" : "&7Click to join party after this game"));
        }

        // Action hints
        lore.add(Component.empty());
        lore.add(legacy("&7Left-click: become spectator/player"));
        if (viewerIsHost) {
            boolean allowSpectate = session.isAllowSpectate();
            lore.add(legacy("&7Right-click: toggle allow-spectate (currently: "
                    + (allowSpectate ? "&aON" : "&cOFF") + "&7)"));
        }

        return GuiItem.of(Material.SPYGLASS)
                .name("&7&lSpectators")
                .lore(lore)
                .amount(count)
                .onLeftClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    Session s = SessionManager.getInstance().getSession(p);
                    if (s == null) return;
                    SessionRole role = s.getRole(p);
                    if (role == SessionRole.SPECTATOR) {
                        if (s.getState() == SessionState.STARTED) {
                            // Queue opt-in for next round; stay spectator for the current game
                            boolean current = Boolean.TRUE.equals(pm.getPlayer(p).getValue("join_party_after_game", true));
                            pm.getPlayer(p).setValue("join_party_after_game", !current, true);
                            Messages.send(p, !current ? "session.spectator.queued-join" : "session.spectator.queued-leave");
                            update(s);
                        } else {
                            s.setRole(p, SessionRole.LOBBY);
                            // PlayerRoleChangeEvent fires → onRoleChange → update(session)
                        }
                    } else if (role == SessionRole.LOBBY) {
                        s.setRole(p, SessionRole.SPECTATOR);
                        if (s.getState() == SessionState.STARTED) {
                            p.setGameMode(GameMode.SPECTATOR);
                        }
                        // PlayerRoleChangeEvent fires → onRoleChange → update(session)
                    }
                })
                .onRightClick(e -> {
                    Player p = (Player) e.getWhoClicked();
                    Session s = SessionManager.getInstance().getSession(p);
                    if (s == null) return;
                    if (!p.equals(s.getHost())) {
                        Messages.send(p, "gui.session.error.not-host");
                        return;
                    }
                    s.setAllowSpectate(!s.isAllowSpectate());
                    update(s);
                });
    }

    private GuiItem buildMinigameSlot(Session session) {
        MiniGame mg = session.getMiniGame();
        if (mg != null) {
            Object iconObj = mg.getValue("icon");
            Object nameObj = mg.getValue("name");
            String displayName = nameObj != null ? nameObj.toString() : mg.getId();
            Material mat = null;
            if (iconObj instanceof ItemStack stack) {
                mat = stack.getType();
            } else if (iconObj != null) {
                mat = Material.matchMaterial(iconObj.toString());
            }
            if (mat != null && mat != Material.AIR) {
                return GuiItem.of(mat).name(displayName);
            }
        }
        return GuiItem.of(Material.LIGHT_GRAY_BUNDLE).name("&7&lMinigames");
    }

    private GuiItem buildMapsSlot(Session session) {
        GameMap map = session.getGameMap();
        if (map != null) {
            Object nameObj = map.getValue("name");
            String mapName = nameObj != null ? nameObj.toString() : "?";
            return GuiItem.of(Material.PAPER).name("&7&l" + mapName);
        }
        if (session.getMiniGame() == null) {
            return GuiItem.of(Material.BARRIER).name("&7&lMaps").lore(legacy("&c- Choose an addon"));
        }
        return GuiItem.of(Material.BARRIER).name("&7&lMaps").lore(legacy("&c- Choose a map"));
    }

    private GuiItem buildPlayerHead(Player member, boolean isHost, boolean isReady, boolean applyGlow, Player viewer) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setPlayerProfile(member.getPlayerProfile());
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            if (applyGlow) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            }
            skull.setItemMeta(meta);
        }
        GuiItem item = GuiItem.of(skull)
                .name((isReady ? "&a" : "&c") + member.getName());
        if (isHost) {
            item.lore(Messages.getComponent("gui.session.host-label", viewer));
        }
        return item;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private boolean isMidGameLocked(Session session, Player player) {
        SessionState st = session.getState();
        if (st != SessionState.STARTING && st != SessionState.STARTED) return false;
        if (SkGame.getInstance().isAllowMidGameChanges()) return false;
        Messages.send(player, "session.error.mid-game-locked");
        return true;
    }

    private boolean isHostOnly(Player player, Session session) {
        if (player.equals(session.getHost())) return true;
        Messages.send(player, "gui.session.error.not-host");
        return false;
    }

    private boolean allLobbyMembersReady(Session session, PlayerManager pm) {
        Set<Player> lobby = session.getLobbyMembers();
        if (lobby.isEmpty()) return false;
        return lobby.stream().allMatch(p -> Boolean.TRUE.equals(pm.getPlayer(p).getValue("ready", true)));
    }

    private int getRounds(Session session) {
        return session.getTotalRounds();
    }

    private static Component legacy(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}
