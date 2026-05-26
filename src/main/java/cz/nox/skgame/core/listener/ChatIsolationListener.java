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
import java.util.Set;

/**
 * Restricts chat delivery to session members when session.chat.isolation is enabled.
 * Runs on async chat event; reads session state without writing — safe for async context.
 */
public class ChatIsolationListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncChat(AsyncChatEvent event) {
        SkGame plugin = SkGame.getInstance();
        if (!plugin.getConfig().getBoolean("session.chat.isolation", false)) return;

        Player sender = event.getPlayer();
        Session session = SessionManager.getInstance().getSession(sender);
        if (session == null) return;

        boolean spectatorIsolation = plugin.getConfig().getBoolean("session.chat.spectator-isolation", false);
        SessionRole senderRole = session.getRole(sender);

        Set<Audience> toRemove = new HashSet<>();
        for (Audience audience : event.viewers()) {
            if (!(audience instanceof Player recipient)) continue;
            Session recipientSession = SessionManager.getInstance().getSession(recipient);
            if (recipientSession == null || !recipientSession.getId().equals(session.getId())) {
                toRemove.add(audience);
            } else if (spectatorIsolation && senderRole != null) {
                SessionRole recipientRole = session.getRole(recipient);
                boolean senderIsSpec = senderRole == SessionRole.SPECTATOR;
                boolean recipientIsSpec = recipientRole == SessionRole.SPECTATOR;
                if (senderIsSpec != recipientIsSpec) {
                    toRemove.add(audience);
                }
            }
        }
        event.viewers().removeAll(toRemove);
    }
}
