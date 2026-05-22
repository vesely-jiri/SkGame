package cz.nox.skgame.core.game.lifecycle;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.DisbandReason;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Internal helper owned by SessionLifecycleManagerImpl.
 * Owns: host promotion policy, auto-disband detection, idle-disband timer, shutdown.
 */
class PartyManager {

    private final SessionLifecycleManagerImpl lifecycle;
    private final SessionManager sessionManager;
    private final SkGame plugin;
    private final boolean autoPromoteHost;
    private final int idleTimeoutSeconds;
    private final Map<String, BukkitTask> idleTimers = new HashMap<>();

    PartyManager(SessionLifecycleManagerImpl lifecycle, SessionManager sessionManager, SkGame plugin) {
        this.lifecycle = lifecycle;
        this.sessionManager = sessionManager;
        this.plugin = plugin;
        this.autoPromoteHost = plugin.getConfig().getBoolean("session.auto-promote-host", true);
        this.idleTimeoutSeconds = plugin.getConfig().getInt("session.idle-disband-timeout", 600);
    }

    /**
     * Reset idle-disband timer on any party activity.
     * Only arms the timer when session is in LOBBY state and timeout is configured.
     */
    void registerActivity(Session session) {
        cancelIdleTimer(session.getId());
        if (idleTimeoutSeconds <= 0) return;
        if (session.getState() != SessionState.LOBBY) return;
        String sessionId = session.getId();
        BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Session s = sessionManager.getSessionById(sessionId);
            if (s == null || s.getState() != SessionState.LOBBY) return;
            lifecycle.disbandSession(s, DisbandReason.IDLE_TIMEOUT);
        }, (long) idleTimeoutSeconds * 20L);
        idleTimers.put(sessionId, task);
    }

    /**
     * Handle host-leave logic. Call after the leaving player has been removed from their role set.
     *
     * @param explicitLeave true = player chose to leave (GUI/command); false = server disconnect.
     *                      Matters in STARTED state: explicit leave promotes a new host immediately;
     *                      disconnect leaves host = null (Phase 9 design for mid-game disconnect).
     * @return true  — no disband needed (player wasn't host, host promoted, or disconnect path)
     * @return false — disband needed; use {@link #disbandReasonForHostLeave()} for the reason
     */
    boolean tryPromoteHost(Session session, Player leaving, boolean explicitLeave) {
        if (!leaving.equals(session.getHost())) return true;
        if (session.getState() != SessionState.LOBBY) {
            if (explicitLeave && autoPromoteHost) {
                Set<Player> players = session.getPlayers();
                if (!players.isEmpty()) {
                    session.setHost(players.iterator().next());
                    return true;
                }
                return false; // explicit leave, no remaining players — disband
            }
            // Disconnect or auto-promote disabled: clear host, game continues without one
            session.setHost(null);
            return true;
        }
        if (autoPromoteHost) {
            Set<Player> lobby = session.getLobbyMembers();
            if (!lobby.isEmpty()) {
                session.setHost(lobby.iterator().next());
                return true; // promoted
            }
        }
        return false; // host left lobby with no promotion path
    }

    boolean shouldAutoDisband(Session session) {
        return session.getMembers().isEmpty();
    }

    /**
     * Reason to use when {@link #tryPromoteHost} returns false.
     * HOST_LEAVE when auto-promote is disabled (members exist but host refused promotion).
     * EMPTY_PARTY otherwise (auto-promote enabled but no lobby members remain).
     */
    DisbandReason disbandReasonForHostLeave() {
        return autoPromoteHost ? DisbandReason.EMPTY_PARTY : DisbandReason.HOST_LEAVE;
    }

    void onSessionDisbanded(String sessionId) {
        cancelIdleTimer(sessionId);
    }

    /** Cancel all idle timers and disband every live session with SHUTDOWN reason. */
    void shutdown() {
        idleTimers.values().forEach(BukkitTask::cancel);
        idleTimers.clear();
        for (Session session : sessionManager.getAllSessions()) {
            lifecycle.disbandSession(session, DisbandReason.SHUTDOWN);
        }
    }

    private void cancelIdleTimer(String sessionId) {
        BukkitTask task = idleTimers.remove(sessionId);
        if (task != null) task.cancel();
    }
}
