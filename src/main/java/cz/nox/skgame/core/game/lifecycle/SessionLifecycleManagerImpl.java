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
import cz.nox.skgame.api.game.model.GamePlayer;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.DisbandReason;
import cz.nox.skgame.api.game.model.SessionVisibility;
import cz.nox.skgame.api.game.model.type.MapSelectionMode;
import cz.nox.skgame.api.game.model.type.TeamAssignmentMode;
import cz.nox.skgame.api.game.model.type.GameStartReason;
import cz.nox.skgame.api.game.model.type.SessionRole;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.core.util.GamePlayerKeys;
import cz.nox.skgame.core.util.SessionKeys;
import cz.nox.skgame.api.gui.GuiHolder;
import cz.nox.skgame.api.region.Region;
import cz.nox.skgame.core.game.PlayerManager;
import cz.nox.skgame.core.game.GameMapManager;
import cz.nox.skgame.core.game.MiniGameManager;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.region.ArenaSlot;
import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.core.team.MapVoteItem;
import cz.nox.skgame.core.team.TeamPickerItem;
import cz.nox.skgame.util.Debug;
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
import cz.nox.skgame.core.scoreboard.ScoreboardService;
import cz.nox.skgame.core.storage.GameResultsRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;
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
    private static final int VOTE_SLOT = 5;
    // Reasons that indicate forced teardown rather than natural round completion.
    private static final Set<String> TEARDOWN_REASONS = Set.of(
            "disband", "shutdown", "abandoned", "admin", "minigame-disabled", "no-players");

    /** True for any abort/teardown reason: exact match in TEARDOWN_REASONS, or "admin:" prefix variants. */
    private static boolean isAbortReason(String reason) {
        String lower = reason.toLowerCase();
        return lower.startsWith("admin:") || TEARDOWN_REASONS.contains(lower);
    }
    private final Map<UUID, ItemStack> pickerSlotBackup = new ConcurrentHashMap<>();
    private final Map<UUID, ItemStack> voteSlotBackup = new ConcurrentHashMap<>();

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
        Session session = sessionManager.registerSession(id); // registers in map, does NOT fire event yet
        session.setHost(host);
        session.addLobbyMember(host); // fires GamePlayerSessionJoin; host is LOBBY member before announce
        Bukkit.getPluginManager().callEvent(new SessionCreateEvent(session)); // host set + 1 member — fully formed
        LobbyEnterEvent lobbyEvent = new LobbyEnterEvent(host, session);
        Bukkit.getPluginManager().callEvent(lobbyEvent);
        if (lobbyEvent.isCancelled()) {
            disbandSession(session, DisbandReason.EXPLICIT_DISBAND);
            return null;
        }
        partyManager.registerActivity(session);
        return session;
    }

    /** Creates a server-managed event session. Returns null if one already exists. */
    public @Nullable Session createEventSession(Player admin) {
        if (sessionManager.getEventSession() != null) {
            plugin.getLogUtil().warning("createEventSession: event session already exists — ignoring");
            return null;
        }
        Session session = createSession(admin);
        if (session == null) return null;
        session.setEventSession(true);
        session.setPersistent(true);
        session.setVisibility(SessionVisibility.INVITE_ONLY);
        sessionManager.setEventSession(session);
        return session;
    }

    @Override
    public boolean joinSession(Player player, Session session) {
        if (session.getRole(player) != null) {
            plugin.getLogUtil().info("joinSession: " + player.getName() + " already in session " + session.getId());
            return false;
        }
        if (session.isBanned(player.getUniqueId())) {
            Messages.send(player, "session.join-denied.banned");
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
        if (session.getMiniGame() != null) {
            final MiniGame _mg = session.getMiniGame();
            Debug.logMiniGame(_mg.getId(), "player-join", () -> player.getName() + " session=" + session.getId());
        }
        return true;
    }

    @Override
    public boolean joinAsSpectator(Player player, Session session) {
        if (session.getRole(player) != null) {
            plugin.getLogUtil().info("joinAsSpectator: " + player.getName() + " already in session, use setRole");
            return false;
        }
        if (session.isBanned(player.getUniqueId())) {
            Messages.send(player, "session.join-denied.banned");
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

        // Restore player's scoreboard if they had a session board shown
        if (stateBeforeLeave == SessionState.STARTED || stateBeforeLeave == SessionState.ENDED) {
            ScoreboardService.getInstance().hideFromIfPresent(player, session);
        }

        if (stateBeforeLeave == SessionState.PREPARATION && role == SessionRole.LOBBY) {
            removePicker(player);
            removeVoteItem(player);
            session.setTeam(player, null);
            session.setMapVote(player, null);
        }

        if (role == SessionRole.PLAYER || role == SessionRole.SPECTATOR) {
            PlayerResetter.reset(player, plugin.getDefaultGameMode());
            Location lobbySpawn = plugin.getLobbySpawn();
            if (lobbySpawn != null) player.teleport(lobbySpawn);
        }

        playerManager.getPlayer(player).removeValues(true);

        if (session.getMiniGame() != null) {
            final MiniGame _mg = session.getMiniGame();
            final SessionRole _role = role;
            Debug.logMiniGame(_mg.getId(), "player-leave", () ->
                player.getName() + " role=" + _role + " explicit=" + explicitLeave + " session=" + session.getId());
        }

        Messages.send(player, "session.leave.notification");

        Player hostBefore = session.getHost();
        if (!partyManager.tryPromoteHost(session, player, explicitLeave)) {
            if (session.isPersistent()) { session.setHost(null); return; }
            if (stateBeforeLeave == SessionState.STARTED || stateBeforeLeave == SessionState.ENDED) endGame(session, "abandoned");
            else if (stateBeforeLeave == SessionState.PREPARATION) cancelPreparation(session);
            disbandSession(session, partyManager.disbandReasonForHostLeave());
            return;
        }
        // Notify on actual promotion (new host set, different from the one who just left)
        Player hostAfter = session.getHost();
        if (hostAfter != null && !hostAfter.equals(hostBefore)) {
            String leavingName = player.getName();
            for (Player member : session.getMembers()) {
                Messages.send(member, "session.host.left", leavingName);
            }
            if (hostAfter.isOnline()) Messages.send(hostAfter, "session.host.promoted");
        }
        if (partyManager.shouldAutoDisband(session)) {
            if (session.isPersistent()) return;
            if (stateBeforeLeave == SessionState.STARTED || stateBeforeLeave == SessionState.ENDED) endGame(session, "abandoned");
            else if (stateBeforeLeave == SessionState.PREPARATION) cancelPreparation(session);
            disbandSession(session, DisbandReason.EMPTY_PARTY);
        }
    }

    @Override
    public boolean startGame(Session session, GameStartReason reason, @Nullable Long countdownTicks) {
        if (session.getState() != SessionState.LOBBY) return false;
        MiniGame miniGame = session.getMiniGame();
        GameMap gameMap = session.getGameMap();
        if (miniGame == null) return false;
        if (MiniGameManager.getInstance().isMinigameDisabled(miniGame.getId())) {
            for (Player p : session.getLobbyMembers()) Messages.send(p, "session.error.minigame-disabled");
            return false;
        }
        if (gameMap == null && session.getMapSelectionMode() == MapSelectionMode.SPECIFIC) return false;
        if (gameMap != null && !gameMap.supportsMiniGame(miniGame)) return false;

        if (plugin.isMaintenanceMode()) {
            for (Player p : session.getLobbyMembers()) {
                Messages.send(p, "session.start.maintenance");
            }
            return false;
        }

        if (needsPreparation(session)) {
            if (session.isMapVoting() && getCandidateMaps(session).isEmpty()) {
                for (Player p : session.getLobbyMembers())
                    Messages.send(p, "session.error.no-maps-for-minigame");
                return false;
            }
            enterPreparation(session);
            return true;
        }

        if (session.getMapSelectionMode() == MapSelectionMode.RANDOM && session.getGameMap() == null) {
            List<GameMap> candidates = getCandidateMaps(session);
            if (candidates.isEmpty()) {
                for (Player p : session.getLobbyMembers())
                    Messages.send(p, "session.error.no-maps-for-minigame");
                return false;
            }
            Collections.shuffle(candidates);
            session.setGameMap(candidates.get(0));
        }

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

        // Reset per-round scores so each game starts from zero
        for (Player p : lobbySnapshot) {
            GamePlayer gp = playerManager.getPlayer(p);
            if (gp != null) gp.removeValue(GamePlayerKeys.SCORE, true);
        }
        session.removeValuesByPrefix(SessionKeys.TEAM_SCORE_PREFIX, true);

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

        // Reset all players before game starts — clears lobby inventory, effects, state
        for (Player p : session.getPlayers()) {
            PlayerResetter.reset(p, plugin.getDefaultGameMode());
        }
        Bukkit.getPluginManager().callEvent(new GameStartEvent(session, session.getMiniGame(), gameMap));
        if (session.getMiniGame() != null) {
            final MiniGame _mg = session.getMiniGame();
            final GameMap _map = gameMap;
            final int _cnt = session.getPlayers().size();
            Debug.logMiniGame(_mg.getId(), "game-start", () ->
                "session=" + session.getId() + " map=" + (_map != null ? _map.getId() : "none") + " players=" + _cnt);
            Debug.logMiniGame(_mg.getId(), "event-dispatch", () ->
                "GameStartEvent fired session=" + session.getId());
        }
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
        if (session.isMapVoting()) return true;
        MiniGame mg = session.getMiniGame();
        if (mg == null || mg.getTeams().isEmpty()) return false;
        TeamAssignmentMode mode = mg.getTeamAssignment();
        return mode == TeamAssignmentMode.SELF_SELECT || mode == TeamAssignmentMode.BOTH;
    }

    private boolean needsTeamPicker(Session session) {
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
        Debug.logMiniGame(mg.getId(), "team-assign", () -> {
            StringBuilder sb = new StringBuilder("auto:");
            for (Player p : session.getPlayers()) {
                String t = session.getTeam(p);
                sb.append(' ').append(p.getName()).append('→').append(t != null ? t : "?");
            }
            return sb.toString();
        });
    }

    private void enterPreparation(Session session) {
        session.setState(SessionState.PREPARATION);
        if (session.getMiniGame() != null) {
            final MiniGame _mg = session.getMiniGame();
            Debug.logMiniGame(_mg.getId(), "preparation-start", () -> "session=" + session.getId());
        }

        // Map vote setup (must happen before team picker so both can coexist)
        if (session.isMapVoting()) {
            List<GameMap> candidates = getCandidateMaps(session);
            if (candidates.size() == 1) {
                // Auto-pick the only map — no vote UI needed
                session.setGameMap(candidates.get(0));
                session.setMapVoting(false);
            } else {
                // 2+ candidates: clear any previous winner, give vote items
                session.setGameMap(null);
                for (Player p : session.getLobbyMembers()) giveVoteItem(p);
            }
        }

        // Team picker (only when self-select/both mode)
        if (needsTeamPicker(session)) {
            for (Player p : session.getLobbyMembers()) givePicker(p);
        }

        // Deferred close + hint — must be one tick later; start was inside an InventoryClickEvent
        Set<Player> prepMembers = new HashSet<>(session.getLobbyMembers());
        boolean showTeamHint = needsTeamPicker(session);
        boolean showMapHint = session.isMapVoting();
        Bukkit.getScheduler().runTask(plugin, () -> {
            prepMembers.forEach(Player::closeInventory);
            if (showTeamHint || showMapHint) {
                Title.Times times = Title.Times.times(
                        Duration.ofMillis(200), Duration.ofSeconds(2), Duration.ofMillis(500));
                prepMembers.forEach(p -> {
                    Component titleComp = (showTeamHint && showMapHint)
                            ? LegacyComponentSerializer.legacyAmpersand()
                                    .deserialize(Messages.get("session.preparation.pick-team", p))
                            : Component.empty();
                    Component subtitleComp = showMapHint
                            ? LegacyComponentSerializer.legacyAmpersand()
                                    .deserialize(Messages.get("session.preparation.pick-map", p))
                            : LegacyComponentSerializer.legacyAmpersand()
                                    .deserialize(Messages.get("session.preparation.pick-team", p));
                    p.showTitle(Title.title(titleComp, subtitleComp, times));
                });
            }
        });

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
                            .deserialize("&ePreparation: &a" + secs + "s &eremaining");
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
        Set<Player> members = new HashSet<>(session.getLobbyMembers());
        for (Player p : members) { removePicker(p); removeVoteItem(p); }
        session.clearTeams();
        session.clearMapVotes();
        session.setState(SessionState.LOBBY);
    }

    @Override
    public void finishPreparation(Session session) {
        sessionManager.cancelCountdownTask(session.getId());
        if (session.getMiniGame() != null) {
            final MiniGame _mg = session.getMiniGame();
            Debug.logMiniGame(_mg.getId(), "preparation-end", () -> "session=" + session.getId());
        }
        Set<Player> members = new HashSet<>(session.getLobbyMembers());
        for (Player p : members) { removePicker(p); removeVoteItem(p); }

        // Close any open SkGame GUI (team picker, map vote) on all session members before
        // game starts. Synchronous closeInventory() is safe here because the only click-context
        // trigger (AdminPanelGuiService force-start) is a server admin who is NOT a member of
        // the target session — we never close the triggering player's inventory inside their own
        // InventoryClickEvent. NOTE: if a host-side in-session force-start (member triggers from
        // a click) is ever added, this block must move to a 1-tick Bukkit.getScheduler().runTask
        // defer to avoid client desync, mirroring the enterPreparation close from ad5c250.
        for (Player p : session.getMembers()) {
            if (p.isOnline()
                    && p.getOpenInventory().getTopInventory().getHolder() instanceof GuiHolder) {
                p.closeInventory();
            }
        }

        // Resolve map vote before auto-fill and start
        if (session.isMapVoting()) {
            GameMap winner = tallyMapVotes(session);
            session.setGameMap(winner);
            session.setMapVoting(false);
            session.clearMapVotes();
            if (winner != null) {
                for (Player p : session.getLobbyMembers()) {
                    Messages.send(p, "session.preparation.map-vote-result", winner.getId());
                }
            }
        }

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
        if (picker.isPicker(existing)) return;
        if (existing != null && !existing.getType().isAir()) {
            pickerSlotBackup.put(player.getUniqueId(), existing);
        }
        player.getInventory().setItem(PICKER_SLOT, picker.create());
    }

    private void removePicker(Player player) {
        TeamPickerItem picker = TeamPickerItem.getInstance();
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            if (picker.isPicker(player.getInventory().getItem(i))) {
                player.getInventory().setItem(i, null);
            }
        }
        // Restore slot 4 only if something was displaced; otherwise the strip already cleared it
        ItemStack backup = pickerSlotBackup.remove(player.getUniqueId());
        if (backup != null) player.getInventory().setItem(PICKER_SLOT, backup);
    }

    private void giveVoteItem(Player player) {
        MapVoteItem item = MapVoteItem.getInstance();
        ItemStack existing = player.getInventory().getItem(VOTE_SLOT);
        if (item.isVoteItem(existing)) return;
        if (existing != null && !existing.getType().isAir()) {
            voteSlotBackup.put(player.getUniqueId(), existing);
        }
        player.getInventory().setItem(VOTE_SLOT, item.create());
    }

    private void removeVoteItem(Player player) {
        MapVoteItem item = MapVoteItem.getInstance();
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            if (item.isVoteItem(player.getInventory().getItem(i))) {
                player.getInventory().setItem(i, null);
            }
        }
        // Restore slot 5 only if something was displaced; otherwise the strip already cleared it
        ItemStack backup = voteSlotBackup.remove(player.getUniqueId());
        if (backup != null) player.getInventory().setItem(VOTE_SLOT, backup);
    }

    // Returns maps supporting the minigame that are available for voting.
    // Non-slotted claimed maps are excluded (setGameMap silently no-ops for them).
    // Slotted maps are always included — claimSlot creates overflow if needed.
    private List<GameMap> getCandidateMaps(Session session) {
        MiniGame mg = session.getMiniGame();
        if (mg == null) return List.of();
        GameMapManager gmm = GameMapManager.getInstance();
        return Arrays.stream(gmm.getGameMaps())
                .filter(m -> m.supportsMiniGame(mg))
                .filter(m -> m.hasArenaSlots() || !gmm.isMapClaimed(m.getId()))
                .collect(Collectors.toList());
    }

    // Tally votes; no votes → random; ties → random among tied winners.
    // Known edge case: a non-slotted winner could be claimed by another session between
    // vote-start and tally (rare race); setGameMap silently no-ops in that case.
    private GameMap tallyMapVotes(Session session) {
        List<GameMap> candidates = getCandidateMaps(session);
        if (candidates.isEmpty()) return null;
        Map<String, Integer> tally = new HashMap<>();
        candidates.forEach(m -> tally.put(m.getId(), 0));
        session.getMapVotes().values()
                .forEach(votedId -> tally.computeIfPresent(votedId, (k, v) -> v + 1));
        int max = Collections.max(tally.values());
        List<GameMap> topMaps = candidates.stream()
                .filter(m -> tally.getOrDefault(m.getId(), 0) == max)
                .collect(Collectors.toList());
        GameMap winner = topMaps.get(new Random().nextInt(topMaps.size()));
        if (session.getMiniGame() != null && winner != null) {
            final MiniGame _mg = session.getMiniGame();
            final GameMap _winner = winner;
            Debug.logMiniGame(_mg.getId(), "map-vote", () -> {
                StringBuilder sb = new StringBuilder("tally:");
                tally.forEach((id, v) -> sb.append(' ').append(id).append('=').append(v));
                sb.append(" winner=").append(_winner.getId());
                return sb.toString();
            });
        }
        return winner;
    }

    @Override
    public void endGame(Session session, String reason) {
        sessionManager.cancelCountdownTask(session.getId());

        // Re-entry guard: ENDED means window is active. Task already cancelled above; run deferred cleanup only.
        if (session.getState() == SessionState.ENDED) {
            runDeferredBlock(session, reason, null);
            return;
        }

        Region arena = session.getArenaRegion();

        // Release arena slot
        if (session.getClaimedSlot() != null && session.getGameMap() != null) {
            session.getGameMap().releaseSlot(session.getId());
            session.setClaimedSlot(null);
            session.setArenaRegion(null);
        }

        session.setState(SessionState.ENDED);

        // Fire GameStopEvent while players are still in PLAYER role, state=ENDED (semantically correct)
        MiniGame miniGame = session.getMiniGame();
        if (miniGame != null) {
            Bukkit.getPluginManager().callEvent(new GameStopEvent(miniGame, session, reason));
        }

        if (reason.startsWith("admin:")) {
            String customMsg = reason.substring(6);
            if (customMsg.isEmpty()) {
                for (Player member : session.getMembers())
                    Messages.send(member, "admin.force-end.broadcast");
            } else {
                for (Player member : session.getMembers())
                    Messages.send(member, "admin.force-end.broadcast-reason", customMsg);
            }
        } else if ("admin".equals(reason)) {
            // Legacy plain "admin" reason — keep existing behaviour
            for (Player member : session.getMembers())
                Messages.send(member, "admin.force-end.broadcast");
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
            Map<UUID, Integer> scores = new HashMap<>();
            for (Player p : session.getPlayers()) {
                GamePlayer gp = playerManager.getPlayer(p);
                if (gp != null) {
                    Object v = gp.getValue(GamePlayerKeys.SCORE, true);
                    scores.put(p.getUniqueId(), v instanceof Number n ? n.intValue() : 0);
                }
            }
            GameResultsRepository.getInstance().recordAsync(
                    plugin, sessionId, minigameId, gamemapId,
                    startTime, endTime, reason, playerUuids, winnerUuids, scores);
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
        if (miniGame != null) {
            final java.util.List<String> _wnames = sessionWinners.stream().map(Player::getName).collect(Collectors.toList());
            Debug.logMiniGame(miniGame.getId(), "game-stop", () ->
                "session=" + session.getId() + " reason=" + reason + " winners=[" + String.join(",", _wnames) + "]");
        }
        long windowTicks = plugin.getConfig().getLong("session.post-game.window-seconds", 0L) * 20L;
        if (windowTicks > 0 && !isAbortReason(reason)) {
            Region capturedArena = arena;
            String sessionId = session.getId();
            BukkitTask windowTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (sessionManager.getSessionById(sessionId) != null)
                    runDeferredBlock(session, reason, capturedArena);
            }, windowTicks);
            sessionManager.setCountdownTask(session.getId(), windowTask);
        } else {
            runDeferredBlock(session, reason, arena);
        }
    }

    private void runDeferredBlock(Session session, String reason, @Nullable Region arena) {
        // Apply admin-queued config changes before clearing game state
        Map<String, Object> pending = session.drainPendingAdminChanges();
        if (pending.containsKey("map-mode"))
            session.setMapSelectionMode((MapSelectionMode) pending.get("map-mode"));
        if (pending.containsKey("map-id")) {
            String mapId = (String) pending.get("map-id");
            session.setGameMap(mapId != null ? GameMapManager.getInstance().getGameMapById(mapId) : null);
        }

        // Moved from sync part — winners/teams data must persist through the window for scripters.
        session.clearWinners();
        session.clearTeams();
        session.clearMapVotes();

        // Dispose session scoreboard and restore all players' captured boards
        ScoreboardService.getInstance().disposeIfPresent(session);

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

        session.setState(SessionState.LOBBY);
        partyManager.registerActivity(session);

        int cur = session.getCurrentRound();
        int total = session.getTotalRounds();
        long windowTicks = plugin.getConfig().getLong("session.post-game.window-seconds", 0L) * 20L;

        if (!plugin.isMaintenanceMode() && cur > 0 && cur < total) {
            session.setCurrentRound(cur + 1);
            // Window was the inter-round pause; if disabled, keep the legacy 40-tick buffer.
            long delay = windowTicks > 0 ? 1L : 40L;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (sessionManager.getSessionById(session.getId()) != null) {
                    startGame(session, GameStartReason.AUTO_NEXT_ROUND, null);
                }
            }, delay);
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
        if (session.getMiniGame() != null) {
            final MiniGame _mg = session.getMiniGame();
            Debug.logMiniGame(_mg.getId(), "disband", () -> "session=" + session.getId() + " reason=" + reason);
        }
        // Teardown running game first so scripts clean up, players are reset and teleported
        SessionState stateAtDisband = session.getState();
        if (stateAtDisband == SessionState.STARTED || stateAtDisband == SessionState.STARTING
                || stateAtDisband == SessionState.ENDED) {
            endGame(session, "disband");
        } else if (stateAtDisband == SessionState.PREPARATION) {
            cancelPreparation(session);
        }

        String disbandKey = switch (reason) {
            case EXPLICIT_DISBAND -> "session.disband.explicit";
            case EMPTY_PARTY      -> "session.disband.empty-party";
            case HOST_LEAVE       -> "session.disband.host-leave";
            case IDLE_TIMEOUT     -> "session.disband.idle-timeout";
            case SHUTDOWN         -> "session.disband.shutdown";
        };
        for (Player member : session.getMembers()) {
            Messages.send(member, disbandKey);
        }
        partyManager.onSessionDisbanded(session.getId());
        // Delete before firing event so listeners (e.g. admin panel) see the session already gone
        sessionManager.deleteSession(session.getId());
        if (session.isEventSession() && session.equals(sessionManager.getEventSession())) {
            sessionManager.clearEventSession();
        }
        Bukkit.getPluginManager().callEvent(new SessionDisbandEvent(session, reason));
    }

    /** Kick a member from their session. All validation included — safe to call from command and GUI. */
    public void kickMember(Player host, Player target) {
        Session session = sessionManager.getSession(host);
        if (session == null || !host.equals(session.getHost())) {
            Messages.send(host, "gui.session.error.not-host");
            return;
        }
        if (target.equals(host)) {
            Messages.send(host, "session.kick.error.self");
            return;
        }
        if (session.getRole(target) == null) {
            Messages.send(host, "session.kick.error.not-member", target.getName());
            return;
        }
        Messages.send(target, "session.kick.kicked");
        leaveSession(target);
        Messages.send(host, "session.kick.confirmed", target.getName());
    }

    /** Ban and remove a member from their session. All validation included. */
    public void banMember(Player host, Player target) {
        Session session = sessionManager.getSession(host);
        if (session == null || !host.equals(session.getHost())) {
            Messages.send(host, "gui.session.error.not-host");
            return;
        }
        if (target.equals(host)) {
            Messages.send(host, "session.kick.error.self");
            return;
        }
        if (session.getRole(target) == null) {
            Messages.send(host, "session.kick.error.not-member", target.getName());
            return;
        }
        session.addBan(target.getUniqueId(), target.getName()); // ban before removal — no rejoin window
        Messages.send(target, "session.ban.banned");
        leaveSession(target);
        Messages.send(host, "session.ban.confirmed", target.getName());
    }

    public int getRejoinSnapshotCount() { return rejoinSnapshots.size(); }
    public int getIdleTimerCount() { return partyManager.getIdleTimerCount(); }

    /** Called from SkGame.onDisable — ends running games then disbands all sessions. */
    public void shutdown() {
        for (Session session : sessionManager.getAllSessions()) {
            SessionState state = session.getState();
            if (state == SessionState.STARTED || state == SessionState.STARTING
                    || state == SessionState.PREPARATION || state == SessionState.ENDED) {
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
