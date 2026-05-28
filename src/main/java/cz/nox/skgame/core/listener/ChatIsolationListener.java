package cz.nox.skgame.core.listener;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.SessionRole;
import cz.nox.skgame.core.game.SessionManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Room-based chat isolation when session.chat.isolation is enabled.
 * Room = session ID for session members, null for lobby. Players only hear their own room.
 * Spectator-isolation further separates spectators from active players within a session.
 * Non-destructive viewer filtering only — no cancel, safe for async context.
 */
public class ChatIsolationListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncChat(AsyncChatEvent event) {
        SkGame plugin = SkGame.getInstance();
        if (!plugin.getConfig().getBoolean("session.chat.isolation", false)) return;

        Player sender = event.getPlayer();
        Session senderSession = SessionManager.getInstance().getSession(sender);
        String senderRoom = senderSession != null ? senderSession.getId() : null;
        boolean spectatorIsolation = plugin.getConfig().getBoolean("session.chat.spectator-isolation", false);
        SessionRole senderRole = senderSession != null ? senderSession.getRole(sender) : null;

        Set<Audience> toRemove = new HashSet<>();
        for (Audience audience : event.viewers()) {
            if (!(audience instanceof Player recipient)) continue;
            Session recipientSession = SessionManager.getInstance().getSession(recipient);
            String recipientRoom = recipientSession != null ? recipientSession.getId() : null;
            if (!Objects.equals(senderRoom, recipientRoom)) {
                toRemove.add(audience);
                continue;
            }
            if (spectatorIsolation && senderRole != null) {
                SessionRole recipientRole = senderSession.getRole(recipient);
                boolean senderIsSpec = senderRole == SessionRole.SPECTATOR;
                boolean recipientIsSpec = recipientRole == SessionRole.SPECTATOR;
                if (senderIsSpec != recipientIsSpec) toRemove.add(audience);
            }
        }
        event.viewers().removeAll(toRemove);
    }
}
