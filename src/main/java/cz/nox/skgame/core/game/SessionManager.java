package cz.nox.skgame.core.game;

import cz.nox.skgame.api.game.event.SessionCreateEvent;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SessionManager {

    private static SessionManager manager;
    private final Map<String,Session> sessions = new HashMap<>();
    private final Map<UUID, String> playerToSession = new HashMap<>();
    private Session lastCreatedSession;

    public static synchronized SessionManager getInstance() {
        if (manager == null) {
            manager = new SessionManager();
        }
        return manager;
    }

    public void createSession(String id) {
        if (!sessions.containsKey(id)) {
            Session session = new Session(id);
            sessions.put(id,session);
            setLastCreatedSession(session);

            Event e = new SessionCreateEvent(session);
            Bukkit.getPluginManager().callEvent(e);
        }
    }
    public void deleteSession(String id) {
        if (!sessions.containsKey(id)) return;
        sessions.remove(id);
    }
    @Nullable
    public Session getSession(Player player) {
        String id = playerToSession.get(player.getUniqueId());
        return getSessionById(id);
    }
    public Session[] getAllSessions() {
        return sessions.values().toArray(new Session[0]);
    }

    public void setSessionId(String oldId, String newId) {
        Session session = sessions.remove(oldId);
        if (session != null) {
            session.setId(newId);
            sessions.put(newId, session);
        }
    }
    @Nullable
    public Session getSessionById(String id) {
        return sessions.get(id);
    }

    public void setLastCreatedSession(Session session) {
        this.lastCreatedSession = session;
    }
    @Nullable
    public Session getLastCreatedSession() {
        return this.lastCreatedSession;
    }
}
