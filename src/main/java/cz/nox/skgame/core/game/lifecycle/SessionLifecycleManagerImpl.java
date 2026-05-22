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
import cz.nox.skgame.api.game.model.type.GameStartReason;
import cz.nox.skgame.api.game.model.type.SessionRole;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.api.region.Region;
import cz.nox.skgame.core.game.PlayerManager;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.region.ArenaSlot;
import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.util.PlayerResetter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public class SessionLifecycleManagerImpl implements SessionLifecycleManager {

    private static SessionLifecycleManagerImpl instance;

    private final SessionManager sessionManager;
    private final PlayerManager playerManager;
    private final SkGame plugin;
    private final PartyManager partyManager;

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
        Session session = sessionManager.getSession(player);
        if (session == null) return;

        SessionRole role = session.getRole(player);
        if (role == null) return;

        switch (role) {
            case LOBBY -> session.removeLobbyMember(player);  // fires GamePlayerSessionLeave
            case PLAYER -> session.removePlayers(player);      // fires GamePlayerSessionLeave
            case SPECTATOR -> {
                // removeSpectators does not fire leave event — fire explicitly
                session.removeSpectators(player);
                Bukkit.getPluginManager().callEvent(new GamePlayerSessionLeave(player, session));
            }
        }

        if (role == SessionRole.PLAYER || role == SessionRole.SPECTATOR) {
            PlayerResetter.reset(player, plugin.getDefaultGameMode());
            Location lobbySpawn = plugin.getLobbySpawn();
            if (lobbySpawn != null) player.teleport(lobbySpawn);
        }

        playerManager.getPlayer(player).removeValues(true);

        Messages.send(player, "session.leave.notification");

        if (!partyManager.tryPromoteHost(session, player)) {
            disbandSession(session, partyManager.disbandReasonForHostLeave());
            return;
        }
        if (partyManager.shouldAutoDisband(session)) {
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
        // Snapshot before mutation
        Set<Player> lobbySnapshot = session.getLobbyMembers();

        // Transition LOBBY → PLAYER (fires PlayerRoleChangeEvent per member)
        for (Player p : lobbySnapshot) {
            session.setRole(p, SessionRole.PLAYER);
        }

        session.setState(SessionState.STARTED);

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

        // Clear temp values after handlers have run
        for (Player p : session.getPlayers()) {
            playerManager.getPlayer(p).removeValues(true);
        }
        session.removeValues(true);

        // Transition PLAYER → LOBBY (snapshot to avoid ConcurrentModification)
        for (Player p : session.getPlayers()) {
            session.setRole(p, SessionRole.LOBBY); // fires PlayerRoleChangeEvent
            LobbyEnterEvent e = new LobbyEnterEvent(p, session);
            Bukkit.getPluginManager().callEvent(e); // not cancellable on game end — ignore result
        }

        // Transition SPECTATOR → LOBBY
        for (Player p : session.getSpectators()) {
            session.setRole(p, SessionRole.LOBBY); // fires PlayerRoleChangeEvent
            LobbyEnterEvent e = new LobbyEnterEvent(p, session);
            Bukkit.getPluginManager().callEvent(e);
        }

        partyManager.registerActivity(session);

        int cur = session.getCurrentRound();
        int total = session.getTotalRounds();
        if (cur > 0 && cur < total) {
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

    @Override
    public void disbandSession(Session session, DisbandReason reason) {
        String reasonName = reason.name().toLowerCase().replace('_', ' ');
        for (Player member : session.getMembers()) {
            Messages.send(member, "session.disband.notification", reasonName);
        }
        partyManager.onSessionDisbanded(session.getId());
        Bukkit.getPluginManager().callEvent(new SessionDisbandEvent(session, reason));
        sessionManager.deleteSession(session.getId());
    }

    /** Called from SkGame.onDisable — disbands all live sessions with SHUTDOWN reason. */
    public void shutdown() {
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
