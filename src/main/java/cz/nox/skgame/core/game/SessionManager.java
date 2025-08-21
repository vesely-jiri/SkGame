package cz.nox.skgame.core.game;

import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.GameMode;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.SessionReadOnly;
import cz.nox.skgame.api.game.model.type.SessionState;
import org.bukkit.entity.Player;

import java.util.*;

public class SessionManager {

    private static SessionManager manager;
    private final Map<String,Session> sessions = new HashMap<>();
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
        }
    }

    public void setSessionId(String oldId, String newId) {
        Session session = sessions.remove(oldId);
        if (session != null) {
            session.setId(newId);
            sessions.put(newId, session);
        }
    }
    public SessionReadOnly getSessionById(String id) {
        return sessions.get(id);
    }
    public void setSessionName(String id, String name) {
        Session session = sessions.get(id);
        if (session != null) session.setName(name);
    }
    public void setSessionHost(String id, Player host) {
        Session session = sessions.get(id);
        if (session != null) session.setHost(host);
    }

    public void setSessionPlayers(String id, Player[] players) {
        Session session = sessions.get(id);
        session.getPlayers().clear();
        session.getPlayers().addAll(Set.of(players));
    }
    public void addSessionPlayers(String id, Player[] players) {
        Session session = sessions.get(id);
        session.getPlayers().addAll(Set.of(players));
    }
    public void removeSessionPlayers(String id, Player[] players) {
        Session session = sessions.get(id);
        session.getPlayers().removeAll(Set.of(players));
    }
    public void clearSessionPlayers(String id) {
        Session session = sessions.get(id);
        session.getPlayers().clear();
    }

    public void setSessionSpectators(String id, Player[] players) {
        Session session = sessions.get(id);
        session.getSpectators().clear();
        session.getSpectators().addAll(Set.of(players));
    }
    public void addSessionSpectators(String id, Player[] spectator) {
        Session session = sessions.get(id);
        session.getSpectators().addAll(Set.of(spectator));
    }
    public void removeSessionSpectators(String id, Player[] spectator) {
        Session session = sessions.get(id);
        session.getSpectators().removeAll(Set.of(spectator));
    }
    public void clearSessionSpectators(String id) {
        Session session = sessions.get(id);
        session.getSpectators().clear();
    }

    public void setSessionGameMode(String id, GameMode gameMode) {
        Session session = sessions.get(id);
        if (session != null) session.setGameMode(gameMode);
    }
    public void setSessionState(String id, SessionState state) {
        Session session = sessions.get(id);
        if (session != null) session.setState(state);
    }
    public void setSessionMap(String id, GameMap map) {
        Session session = sessions.get(id);
        if (session != null) session.setGameMap(map);
    }
    public void setLastCreatedSession(Session session) {
        this.lastCreatedSession = session;
    }
    public Session getLastCreatedSession() {
        return this.lastCreatedSession;
    }
}
