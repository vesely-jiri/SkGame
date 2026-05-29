package cz.nox.skgame.core.game.lifecycle;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.event.GamePlayerSessionJoin;
import cz.nox.skgame.api.game.event.GamePlayerSessionLeave;
import cz.nox.skgame.api.game.event.GameStartEvent;
import cz.nox.skgame.api.game.event.GameStopEvent;
import cz.nox.skgame.api.game.event.LobbyEnterEvent;
import cz.nox.skgame.api.game.event.PlayerRoleChangeEvent;
import cz.nox.skgame.api.game.event.SessionCreateEvent;
import cz.nox.skgame.api.game.event.SessionDisbandEvent;
import cz.nox.skgame.api.game.event.SpectatorJoinEvent;
import cz.nox.skgame.api.game.lifecycle.SessionLifecycleManager;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.DisbandReason;
import cz.nox.skgame.api.game.model.type.TeamAssignmentMode;
import cz.nox.skgame.api.game.model.type.GameStartReason;
import cz.nox.skgame.api.game.model.type.SessionRole;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.core.util.GamePlayerKeys;
import cz.nox.skgame.api.region.Region;
import cz.nox.skgame.core.game.PlayerManager;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.region.ArenaSlot;
import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.core.team.TeamPickerItem;
import cz.nox.skgame.util.PlayerResetter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import cz.nox.skgame.core.game.RejoinSnapshot;
import cz.nox.skgame.core.storage.GameResultsRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SessionLifecycleManagerImpl implements SessionLifecycleManager, Listener {

    private static SessionLifecycleManagerImpl instance;

    private final SessionManager sessionManager;
    private final PlayerManager playerManager;
    private final SkGame plugin;
    private final PartyManager partyManager;
    private final Map<UUID, RejoinSnapshot> rejoinSnapshots = new ConcurrentHashMap<>();
    private static final int PICKER_SLOT = 4;
    private final Map<UUID, ItemStack> pickerSlotBackup = new ConcurrentHashMap<>();

    private SessionLifecycleManagerImpl(SkGame plugin) {
        this.sessionManager = SessionManager.getInstance();
        this.playerManager = PlayerManager.getInstance();
        this.plugin = plugin;
        this.partyManager = new PartyManager(this, sessionManager, plugin);
    }

    public static synchronized SessionLifecycleManagerImpl getInstance() {
        if (instance == null) {
            instance = new SessionLifecycleManagerImpl(SkGame.getInstance());
        }
        return instance;
    }

    @Override
    public @Nullable Session createSession(Player host) {
        String id = UUID.randomUUID().toString();
        Session session = sessionManager.createSession(id); // fires SessionCreateEvent, registers in map
        session.setHost(host);
        session.addLobbyMember(host); // fires GamePlayerSessionJoin
        LobbyEnterEvent lobbyEvent = new LobbyEnterEvent(host, session);
        Bukkit.getPluginManager().callEvent(lobbyEvent);
        if (lobbyEvent.isCancelled()) {
            disbandSession(session, DisbandReason.EXPLICIT_DISBAND);
            return null;
        }
        partyManager.registerActivity(session);
        return session;
    }

    @Override
    public boolean joinSession(Player player, Session session) {
        if (session.getRole(player) != null) {
            plugin.getLogUtil().info("joinSession: " + player.getName() + " already in session " + session.getId());
            return false;
        }
        if (session.getState() != SessionState.LOBBY) {
            Messages.send(player, "session.error.game-in-progress");
            return false;
        }
        session.addLobbyMember(player); // fires GamePlayerSessionJoin
        LobbyEnterEvent event = new LobbyEnterEvent(player, session);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            session.removeLobbyMember(player); // fires GamePlayerSessionLeave
            return false;
        }
        partyManager.registerActivity(session);
        return true;
    }

    @Override
    public boolean joinAsSpectator(Player player, Session session) {
        if (session.getRole(player) != null) {
            plugin.getLogUtil().info("joinAsSpectator: " + player.getName() + " already in session, use setRole");
            return false;
        }
        SpectatorJoinEvent joinEvent = new SpectatorJoinEvent(player, session);
        Bukkit.getPluginManager().callEvent(joinEvent);
        if (joinEvent.isCancelled()) return false;

        // addSpectators does NOT fire GamePlayerSessionJoin — fire explicitly
        session.addSpectators(player);
        Bukkit.getPluginManager().callEvent(new GamePlayerSessionJoin(player, session));

        player.setGameMode(resolveSpectatorGameMode());
        Location spawn = resolveSpectatorSpawn(session);
        if (spawn != null) player.teleport(spawn);
        partyManager.registerActivity(session);
        return true;
    }

    @Override
    public void leaveSession(Player player) {
        leaveSessionInternal(player, true);
    }

    // BUG 8: server disconnect — explicit=false so mid-game host stays null (Phase 9 design)
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        if (plugin.getConfig().getBoolean("session.rejoin.enabled", true)) {
            Session session = sessionManager.getSession(player);
            if (session != null) {
                SessionRole role = session.getRole(player);
                SessionState state = session.getState();
                if (role == SessionRole.PLAYER
                        && (state == SessionState.STARTING || state == SessionState.STARTED
                            || state == SessionState.PREPARATION)) {
                    rejoinSnapshots.put(player.getUniqueId(),
                            new RejoinSnapshot(player.getUniqueId(), session.getId(), System.currentTimeMillis()));
                }
            }
        }
        leaveSessionInternal(player, false);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        if (!plugin.getConfig().getBoolean("session.rejoin.enabled", true)) return;
        Player player = e.getPlayer();
        RejoinSnapshot snapshot = rejoinSnapshots.get(player.getUniqueId());
        if (snapshot == null) return;
        long windowSeconds = plugin.getConfig().getLong("session.rejoin.window-seconds", 60L);
        if ((System.currentTimeMillis() - snapshot.disconnectTime()) / 1000L > windowSeconds) {
            rejoinSnapshots.remove(player.getUniqueId());
            return;
        }
        Session session = sessionManager.getSessionById(snapshot.sessionId());
        if (session == null
                || (session.getState() != SessionState.STARTING && session.getState() != SessionState.STARTED
                    && session.getState() != SessionState.PREPARATION)) {
            rejoinSnapshots.remove(player.getUniqueId());
            return;
        }
        String sessionId = snapshot.sessionId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            String offerText = Messages.get("session.rejoin.offer", player, sessionId);
            Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(offerText)
                    .append(Component.text(" [Click to rejoin]")
                            .color(NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.runCommand("/game rejoin " + sessionId)));
            player.sendMessage(message);
        }, 40L);
    }

    public void rejoinSession(Player player, String sessionId) {
        RejoinSnapshot snapshot = rejoinSnapshots.get(player.getUniqueId());
        if (snapshot == null || !snapshot.sessionId().equals(sessionId)) {
            Messages.send(player, "session.rejoin.expired");
            return;
        }
        long windowSeconds = plugin.getConfig().getLong("session.rejoin.window-seconds", 60L);
        if ((System.currentTimeMillis() - snapshot.disconnectTime()) / 1000L > windowSeconds) {
            rejoinSnapshots.remove(player.getUniqueId());
            Messages.send(player, "session.rejoin.expired");
            return;
        }
        Session session = sessionManager.getSessionById(sessionId);
        if (session == null
                || (session.getState() != SessionState.STARTING && session.getState() != SessionState.STARTED
                    && session.getState() != SessionState.PREPARATION)) {
            rejoinSnapshots.remove(player.getUniqueId());
            Messages.send(player, "session.rejoin.expired");
            return;
        }
        rejoinSnapshots.remove(player.getUniqueId());
        if (joinAsSpectator(player, session)) {
            playerManager.getPlayer(player).setValue(GamePlayerKeys.JOIN_PARTY_AFTER_GAME, true, true);
            Messages.send(player, "session.rejoin.success");
        }
    }

    private void leaveSessionInternal(Player player, boolean explicitLeave) {
        Session session = sessionManager.getSession(player);
        if (session == null) return;

        SessionRole role = session.getRole(player);
        if (role == null) return;

        SessionState stateBeforeLeave = session.getState();

        switch (role) {
            case LOBBY -> session.removeLobbyMember(player);  // fires GamePlayerSessionLeave
            case PLAYER -> session.removePlayers(player);      // fires GamePlayerSessionLeave
            case SPECTATOR -> {
                // removeSpectators does not fire leave event — fire explicitly
                session.removeSpectators(player);
                Bukkit.getPluginManager().callEvent(new GamePlayerSessionLeave(player, session));
            }
        }

        if (stateBeforeLeave == SessionState.PREPARATION && role == SessionRole.LOBBY) {
            removePicker(player);
            session.setTeam(player, null);
        }

        if (role == SessionRole.PLAYER || role == SessionRole.SPECTATOR) {
            PlayerResetter.reset(player, plugin.getDefaultGameMode());
            Location lobbySpawn = plugin.getLobbySpawn();
            if (lobbySpawn != null) player.teleport(lobbySpawn);
        }

        playerManager.getPlayer(player).removeValues(true);

        Messages.send(player, "session.leave.notification");

        if (!partyManager.tryPromoteHost(session, player, explicitLeave)) {
            if (stateBeforeLeave == SessionState.STARTED) endGame(session, "abandoned");
            else if (stateBeforeLeave == SessionState.PREPARATION) cancelPreparation(session);
            disbandSession(session, partyManager.disbandReasonForHostLeave());
            return;
        }
        if (partyManager.shouldAutoDisband(session)) {
            if (stateBeforeLeave == SessionState.STARTED) endGame(session, "abandoned");
            else if (stateBeforeLeave == SessionState.PREPARATION) cancelPreparation(session);
            disbandSession(session, DisbandReason.EMPTY_PARTY);
        }
    }

    @Override
    public boolean startGame(Session session, GameStartReason reason, @Nullable Long countdownTicks) {
        if (session.getState() != SessionState.LOBBY) return false;
        MiniGame miniGame = session.getMiniGame();
        GameMap gameMap = session.getGameMap();
        if (miniGame == null || gameMap == null) return false;
        if (!gameMap.supportsMiniGame(miniGame)) return false;

        if (plugin.isMaintenanceMode()) {
            for (Player p : session.getLobbyMembers()) {
                Messages.send(p, "session.start.maintenance");
            }
            return false;
        }

        if (needsPreparation(session)) { enterPreparation(session); return true; }

        if (reason != GameStartReason.AUTO_NEXT_ROUND) {
            session.setCurrentRound(1);
        }

        if (countdownTicks != null && countdownTicks > 0) {
            startWithCountdown(session, countdownTicks);
        } else {
            startImmediately(session);
        }
        return true;
    }

    private void startImmediately(Session session) {
        session.clearWinners(); // fresh winners list for each game / round
        // Snapshot before mutation
        Set<Player> lobbySnapshot = session.getLobbyMembers();

        // Transition LOBBY → PLAYER (fires PlayerRoleChangeEvent per member)
        for (Player p : lobbySnapshot) {
            session.setRole(p, SessionRole.PLAYER);
        }

        autoAssignTeams(session);

        session.setState(SessionState.STARTED);
        session.setStartedAt(System.currentTimeMillis());

        // Claim arena slot if applicable
        GameMap gameMap = session.getGameMap();
        if (gameMap != null && gameMap.hasArenaSlots()) {
            ArenaSlot slot = gameMap.claimSlot(session.getId());
            if (slot != null) {
                session.setClaimedSlot(slot);
                session.setArenaRegion(gameMap.getSlotRegion(slot));
            }
        }

        Bukkit.getPluginManager().callEvent(new GameStartEvent(session, session.getMiniGame(), gameMap));
    }

    private void startWithCountdown(Session session, long ticks) {
        session.setState(SessionState.STARTING);
        String sessionId = session.getId();
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                sessionManager.cancelCountdownTask(sessionId);
                Session current = sessionManager.getSessionById(sessionId);
                if (current == null || current.getState() != SessionState.STARTING) return;
                startImmediately(current);
            }
        }.runTaskLater(plugin, ticks);
        sessionManager.setCountdownTask(sessionId, task);
    }

    private boolean needsPreparation(Session session) {
        MiniGame mg = session.getMiniGame();
        if (mg == null || mg.getTeams().isEmpty()) return false;
        TeamAssignmentMode mode = mg.getTeamAssignment();
        return mode == TeamAssignmentMode.SELF_SELECT || mode == TeamAssignmentMode.BOTH;
    }

    private void autoAssignTeams(Session session) {
        MiniGame mg = session.getMiniGame();
        if (mg == null || mg.getTeams().isEmpty()) return;
        if (mg.getTeamAssignment() != TeamAssignmentMode.AUTO) return;
        List<String> teams = new ArrayList<>(mg.getTeams());
        Map<String, Integer> counts = new HashMap<>();
        teams.forEach(t -> counts.put(t, 0));
        for (Player p : new ArrayList<>(session.getPlayers())) {
            String smallest = teams.stream()
                    .min(Comparator.comparingInt(counts::get))
                    .orElseThrow();
            session.setTeam(p, smallest);
            counts.merge(smallest, 1, Integer::sum);
        }
    }

    private void enterPreparation(Session session) {
        session.setState(SessionState.PREPARATION);
        for (Player p : session.getLobbyMembers()) givePicker(p);

        long windowSeconds = plugin.getConfig().getLong("session.preparation.window-seconds", 20L);
        String sessionId = session.getId();
        final long[] remaining = {windowSeconds};

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                Session current = sessionManager.getSessionById(sessionId);
                if (current == null || current.getState() != SessionState.PREPARATION) {
                    cancel(); return;
                }
                if (remaining[0] <= 0) {
                    cancel();
                    finishPreparation(current);
                    return;
                }
                long secs = remaining[0];
                if (secs <= 5 || secs % 10 == 0) {
                    Component bar = LegacyComponentSerializer.legacyAmpersand()
                            .deserialize("&eTeam selection: &a" + secs + "s &eremaining");
                    for (Player p : current.getLobbyMembers()) p.sendActionBar(bar);
                }
                remaining[0]--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
        sessionManager.setCountdownTask(sessionId, task);
    }

    // PREPARATION → LOBBY (cancel / all-leave / under-min)
    private void cancelPreparation(Session session) {
        sessionManager.cancelCountdownTask(session.getId());
        for (Player p : new HashSet<>(session.getLobbyMembers())) removePicker(p);
        session.clearTeams();
        session.setState(SessionState.LOBBY);
    }

    @Override
    public void finishPreparation(Session session) {
        sessionManager.cancelCountdownTask(session.getId());
        for (Player p : new HashSet<>(session.getLobbyMembers())) removePicker(p);
        autoFillUnpickedTeams(session);
        startImmediately(session);
    }

    private void autoFillUnpickedTeams(Session session) {
        MiniGame mg = session.getMiniGame();
        if (mg == null || mg.getTeams().isEmpty()) return;
        List<String> teams = new ArrayList<>(mg.getTeams());
        Map<String, Integer> counts = new HashMap<>();
        teams.forEach(t -> counts.put(t, 0));
        for (Player p : session.getLobbyMembers()) {
            String team = session.getTeam(p);
            if (team != null && counts.containsKey(team)) counts.merge(team, 1, Integer::sum);
        }
        for (Player p : new ArrayList<>(session.getLobbyMembers())) {
            if (session.getTeam(p) != null) continue;
            String smallest = teams.stream().min(Comparator.comparingInt(counts::get)).orElseThrow();
            session.setTeam(p, smallest);
            counts.merge(smallest, 1, Integer::sum);
        }
    }

    private void givePicker(Player player) {
        TeamPickerItem picker = TeamPickerItem.getInstance();
        ItemStack existing = player.getInventory().getItem(PICKER_SLOT);
        if (picker.isPicker(existing)) return; // already has it (e.g. re-enter prep)
        pickerSlotBackup.put(player.getUniqueId(), existing); // null if slot was empty
        player.getInventory().setItem(PICKER_SLOT, picker.create());
    }

    private void removePicker(Player player) {
        TeamPickerItem picker = TeamPickerItem.getInstance();
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            if (picker.isPicker(player.getInventory().getItem(i))) {
                player.getInventory().setItem(i, null);
            }
        }
        // Restore slot 4 to its original content (null = empty slot)
        ItemStack backup = pickerSlotBackup.remove(player.getUniqueId());
        player.getInventory().setItem(PICKER_SLOT, backup);
    }

    @Override
    public void endGame(Session session, String reason) {
        sessionManager.cancelCountdownTask(session.getId());

        Region arena = session.getArenaRegion();

        // Release arena slot
        if (session.getClaimedSlot() != null && session.getGameMap() != null) {
            session.getGameMap().releaseSlot(session.getId());
            session.setClaimedSlot(null);
            session.setArenaRegion(null);
        }

        session.setState(SessionState.LOBBY);

        // Fire GameStopEvent while players are still in PLAYER role (preserve pre-Phase9 handler visibility)
        MiniGame miniGame = session.getMiniGame();
        if (miniGame != null) {
            Bukkit.getPluginManager().callEvent(new GameStopEvent(miniGame, session, reason));
        }

        if ("admin".equals(reason)) {
            for (Player member : session.getMembers()) {
                Messages.send(member, "session.stop.admin-ended");
            }
        }

        // Broadcast winners (set by scripts during GameStopEvent)
        var sessionWinners = session.getWinners();

        // Capture game result data before clearWinners() and role transitions
        {
            long endTime = System.currentTimeMillis();
            long startTime = session.getStartedAt();
            String sessionId = session.getId();
            String minigameId = miniGame != null ? miniGame.getId() : "";
            String gamemapId = session.getGameMap() != null ? session.getGameMap().getId() : "";
            Set<UUID> playerUuids = session.getPlayers().stream()
                    .map(Player::getUniqueId).collect(Collectors.toSet());
            Set<UUID> winnerUuids = sessionWinners.stream()
                    .map(Player::getUniqueId).collect(Collectors.toSet());
            GameResultsRepository.getInstance().recordAsync(
                    plugin, sessionId, minigameId, gamemapId,
                    startTime, endTime, reason, playerUuids, winnerUuids);
        }

        if (!sessionWinners.isEmpty()) {
            StringBuilder names = new StringBuilder();
            for (Player w : sessionWinners) {
                if (names.length() > 0) names.append(", ");
                names.append(w.getName());
            }
            String winKey = sessionWinners.size() == 1 ? "game.winners.single" : "game.winners.multiple";
            for (Player member : session.getMembers()) {
                Messages.send(member, winKey, names.toString());
            }
        }
        session.clearWinners();
        session.clearTeams();

        // Auto-cleanup after scripts have run, before role transitions
        if (arena != null && arena.getWorld() != null) {
            boolean allEntities  = plugin.getConfig().getBoolean("arena.cleanup.entities", true);
            boolean droppedItems = plugin.getConfig().getBoolean("arena.cleanup.dropped-items", true);
            boolean primedTnt    = plugin.getConfig().getBoolean("arena.cleanup.primed-tnt", true);
            if (allEntities) {
                arena.clearEntities(Entity.class);
            } else {
                if (droppedItems) arena.clearEntities(Item.class);
                if (primedTnt)    arena.clearEntities(TNTPrimed.class);
            }
        }

        // Snapshot before mutations — setRole removes from live set, causing CME otherwise
        Set<Player> activePlayers    = new HashSet<>(session.getPlayers());
        Set<Player> activeSpectators = new HashSet<>(session.getSpectators());

        // Clear temp values after handlers have run
        for (Player p : activePlayers) {
            playerManager.getPlayer(p).removeValues(true);
        }
        session.removeValues(true);

        // Transition PLAYER → LOBBY (stays in party)
        for (Player p : activePlayers) {
            session.setRole(p, SessionRole.LOBBY); // fires PlayerRoleChangeEvent
            Bukkit.getPluginManager().callEvent(new LobbyEnterEvent(p, session));
        }

        Location lobbySpawn = plugin.getLobbySpawn();

        // SPECTATOR → opt-in (join_party_after_game flag set) becomes LOBBY; otherwise evicted
        for (Player p : activeSpectators) {
            boolean wantsJoin = Boolean.TRUE.equals(
                    playerManager.getPlayer(p).getValue(GamePlayerKeys.JOIN_PARTY_AFTER_GAME, true));
            playerManager.getPlayer(p).removeValues(true);
            if (wantsJoin) {
                session.setRole(p, SessionRole.LOBBY); // fires PlayerRoleChangeEvent
                Bukkit.getPluginManager().callEvent(new LobbyEnterEvent(p, session));
                PlayerResetter.reset(p, plugin.getDefaultGameMode());
                if (lobbySpawn != null) p.teleport(lobbySpawn);
            } else {
                session.removeSpectators(p);
                Bukkit.getPluginManager().callEvent(new GamePlayerSessionLeave(p, session));
                PlayerResetter.reset(p, plugin.getDefaultGameMode());
                if (lobbySpawn != null) p.teleport(lobbySpawn);
                Messages.send(p, "session.leave.notification");
            }
        }

        // Reset and teleport active players (now in LOBBY role)
        for (Player p : activePlayers) {
            PlayerResetter.reset(p, plugin.getDefaultGameMode());
            if (lobbySpawn != null) p.teleport(lobbySpawn);
        }

        partyManager.registerActivity(session);

        int cur = session.getCurrentRound();
        int total = session.getTotalRounds();
        if (!plugin.isMaintenanceMode() && cur > 0 && cur < total) {
            session.setCurrentRound(cur + 1);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (sessionManager.getSessionById(session.getId()) != null) {
                    startGame(session, GameStartReason.AUTO_NEXT_ROUND, null);
                }
            }, 40L);
        } else {
            session.setCurrentRound(0);
        }
    }

    /** Called from SkGame.setMaintenanceMode(true): server-wide broadcast + clear lobby ready states. */
    public void onMaintenanceEnabled(@Nullable CommandSender exclude) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.equals(exclude)) continue;
            Messages.send(player, "maintenance.broadcast.enabled");
        }
        for (Session session : sessionManager.getAllSessions()) {
            if (session.getState() == SessionState.LOBBY) {
                for (Player member : session.getLobbyMembers()) {
                    playerManager.getPlayer(member).setValue(GamePlayerKeys.READY, false, true);
                }
            }
        }
    }

    /** Called from SkGame.setMaintenanceMode(false): server-wide broadcast. */
    public void onMaintenanceDisabled(@Nullable CommandSender exclude) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.equals(exclude)) continue;
            Messages.send(player, "maintenance.broadcast.disabled");
        }
    }

    @Override
    public void disbandSession(Session session, DisbandReason reason) {
        for (Player member : session.getMembers()) {
            if (reason == DisbandReason.SHUTDOWN) {
                Messages.send(member, "session.disband.shutdown");
            } else {
                String reasonName = reason.name().toLowerCase().replace('_', ' ');
                Messages.send(member, "session.disband.notification", reasonName);
            }
        }
        partyManager.onSessionDisbanded(session.getId());
        Bukkit.getPluginManager().callEvent(new SessionDisbandEvent(session, reason));
        sessionManager.deleteSession(session.getId());
    }

    /** Called from SkGame.onDisable — ends running games then disbands all sessions. */
    public void shutdown() {
        for (Session session : sessionManager.getAllSessions()) {
            SessionState state = session.getState();
            if (state == SessionState.STARTED || state == SessionState.STARTING
                    || state == SessionState.PREPARATION) {
                endGame(session, "SHUTDOWN");
            }
        }
        partyManager.shutdown();
    }

    @Override
    public @Nullable SessionRole getRole(Player player) {
        Session session = sessionManager.getSession(player);
        if (session == null) return null;
        return session.getRole(player);
    }

    private GameMode resolveSpectatorGameMode() {
        String raw = plugin.getConfig().getString("spectators.gamemode", "spectator");
        try {
            return GameMode.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return GameMode.SPECTATOR;
        }
    }

    @Nullable
    public static Location resolveSpectatorSpawn(Session session) {
        GameMap map = session.getGameMap();
        if (map != null) {
            Object spawn = map.getValue("spectator_spawn");
            if (spawn instanceof Location loc) return loc;
        }
        Region region = session.getArenaRegion();
        if (region != null) {
            Location center = region.getCenter();
            if (center != null) return center;
        }
        return SkGame.getInstance().getLobbySpawn();
    }
}
