package cz.nox.skgame.core.game;

import cz.nox.skgame.api.game.event.GamePlayerSessionJoin;
import cz.nox.skgame.api.game.event.GamePlayerSessionLeave;
import cz.nox.skgame.api.game.event.SessionCreateEvent;
import cz.nox.skgame.api.game.event.SessionDisbandEvent;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SessionManager implements Listener {

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

    public Session createSession(String id) {
        Session session;
        if (!sessions.containsKey(id)) {
            session = new Session(id);
            Event e = new SessionCreateEvent(session);
            Bukkit.getPluginManager().callEvent(e);
            sessions.put(id,session);
            setLastCreatedSession(session);
        } else {
            session = sessions.get(id);
        }
        return session;
    }
    public void deleteSession(String id) {
        Session session = sessions.get(id);
        if (session == null) return;
        sessions.remove(id);
        Event e = new SessionDisbandEvent(session);
        Bukkit.getPluginManager().callEvent(e);
    }
    @Nullable
    public Session getSession(Player player) {
        String id = playerToSession.get(player.getUniqueId());
        return getSessionById(id);
    }
    public Session[] getAllSessions() {
        return sessions.values().toArray(new Session[0]);
    }

    @Nullable
    public Session getSessionById(String id) {
        if (id == null) return null;
        return sessions.get(id.toLowerCase());
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
        playerToSession.put(e.getPlayer().getUniqueId(),e.getSession().getId());
    }

    @EventHandler
    public void onPlayerSessionLeave(GamePlayerSessionLeave e) {
        playerToSession.remove(e.getPlayer().getUniqueId());
    }
}
