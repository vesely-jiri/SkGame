package cz.nox.skgame.api.game.lifecycle;

import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.DisbandReason;
import cz.nox.skgame.api.game.model.type.GameStartReason;
import cz.nox.skgame.api.game.model.type.SessionRole;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public interface SessionLifecycleManager {

    /** Create session with host as first LOBBY member. Fires SessionCreateEvent + LobbyEnterEvent. */
    @Nullable Session createSession(Player host);

    /** Add player to LOBBY role. Fires GamePlayerSessionJoin + LobbyEnterEvent (cancellable). */
    boolean joinSession(Player player, Session session);

    /** Add non-member as SPECTATOR. Fires SpectatorJoinEvent (cancellable) + GamePlayerSessionJoin. */
    boolean joinAsSpectator(Player player, Session session);

    /** Remove player from their current role. Auto-promotes host, auto-disbands if empty. */
    void leaveSession(Player player);

    /** Transition LOBBY → STARTED (with optional countdown). All LOBBY members → PLAYER. */
    boolean startGame(Session session, GameStartReason reason, @Nullable Long countdownTicks);

    /** Transition STARTED → LOBBY. All PLAYER/SPECTATOR → LOBBY. Fires GameStopEvent + LobbyEnterEvents. */
    void endGame(Session session, String reason);

    /** Tear down session completely. Fires SessionDisbandEvent with reason. */
    void disbandSession(Session session, DisbandReason reason);

    /** End the PREPARATION window early: auto-fill unpicked players and start immediately. */
    void finishPreparation(Session session);

    @Nullable SessionRole getRole(Player player);

    default boolean startGame(Session session, GameStartReason reason) {
        return startGame(session, reason, null);
    }
}
