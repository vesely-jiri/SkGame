package cz.nox.skgame.core.game;

import cz.nox.skgame.api.game.event.GamePlayerSessionJoin;
import cz.nox.skgame.api.game.event.GamePlayerSessionLeave;
import cz.nox.skgame.api.game.event.SessionCreateEvent;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SessionManager implements Listener {

    private static SessionManager manager;
    private static final GameMapManager mapManager = GameMapManager.getInstance();
    private final Map<String, Session> sessions = new HashMap<>();
    private final Map<UUID, LinkedList<String>> playerToSession = new HashMap<>();
    private final Map<String, BukkitTask> countdownTasks = new HashMap<>();
    private Session lastCreatedSession;

    public static synchronized SessionManager getInstance() {
        if (manager == null) {
            manager = new SessionManager();
        }
        return manager;
    }

    /**
     * Register a new session in the map without firing SessionCreateEvent.
     * Callers that need the event fire it themselves after full initialization.
     * If a session with this id already exists, returns the existing session.
     */
    public Session registerSession(String id) {
        if (!sessions.containsKey(id)) {
            Session session = new Session(id);
            sessions.put(id, session);
            setLastCreatedSession(session);
            return session;
        }
        return sessions.get(id);
    }

    /** @deprecated Use registerSession(id) + fire SessionCreateEvent after initialization. */
    @Deprecated
    public Session createSession(String id) {
        boolean isNew = !sessions.containsKey(id);
        Session session = registerSession(id);
        if (isNew) {
            Bukkit.getPluginManager().callEvent(new SessionCreateEvent(session));
        }
        return session;
    }
    public int getCountdownTaskCount() { return countdownTasks.size(); }

    public void deleteSession(String id) {
        Session session = sessions.get(id);
        if (session == null) return;
        cancelCountdownTask(id);
        if (session.getClaimedSlot() != null && session.getGameMap() != null) {
            session.getGameMap().releaseSlot(session.getId());
            session.setClaimedSlot(null);
            session.setArenaRegion(null);
        }
        mapManager.removeMapFromClaimed(session.getGameMap());
        sessions.remove(id);
    }

    public void setCountdownTask(String sessionId, BukkitTask task) {
        countdownTasks.put(sessionId, task);
    }

    public void cancelCountdownTask(String sessionId) {
        BukkitTask task = countdownTasks.remove(sessionId);
        if (task != null) task.cancel();
    }
    @Nullable
    public Session getSession(Player player) {
        LinkedList<String> ids = playerToSession.get(player.getUniqueId());
        if (ids == null || ids.isEmpty()) return null;
        String first = ids.iterator().next();
        return getSessionById(first);
    }
    public Session[] getSessions(Player player) {
        LinkedList<String> ids = playerToSession.get(player.getUniqueId());
        if (ids == null || ids.isEmpty()) return null;
        return ids.stream()
                .map(this::getSessionById)
                .filter(Objects::nonNull)
                .toArray(Session[]::new);
    }
    public Session[] getAllSessions() {
        return sessions.values().toArray(new Session[0]);
    }

    @Nullable
    public Session getSessionById(String id) {
        if (id == null) return null;
        return sessions.get(id.toLowerCase());
    }

    @Nullable
    public Session getSessionByCode(String code) {
        if (code == null) return null;
        for (Session session : sessions.values()) {
            if (code.equalsIgnoreCase(session.getJoinCode())) return session;
        }
        return null;
    }

    public void setLastCreatedSession(Session session) {
        this.lastCreatedSession = session;
    }
    @Nullable
    public Session getLastCreatedSession() {
        return this.lastCreatedSession;
    }

    @EventHandler
    public void onPlayerSessionJoin(GamePlayerSessionJoin e) {
        UUID uuid = e.getPlayer().getUniqueId();
        playerToSession
                .computeIfAbsent(uuid, k -> new LinkedList<>())
                .addFirst(e.getSession().getId());
    }

    @EventHandler
    public void onPlayerSessionLeave(GamePlayerSessionLeave e) {
        UUID uuid = e.getPlayer().getUniqueId();
        playerToSession.computeIfPresent(uuid, (k, sessions) -> {
           sessions.remove(e.getSession().getId());
           return sessions.isEmpty() ? null : sessions;
        });
    }
}
