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
import java.util.logging.Logger;

/**
 * Room-based chat isolation when session.chat.isolation is enabled.
 * Room = session ID for session members, null for lobby. Players only hear their own room.
 * Spectator-isolation further separates spectators from active players within a session.
 * Non-destructive viewer filtering only — no cancel, safe for async context.
 */
public class ChatIsolationListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncChat(AsyncChatEvent event) {
        // TEMP DEBUG — revert after diagnosis
        SkGame plugin = SkGame.getInstance();
        Logger log = plugin.getLogger();

        boolean isolationEnabled = plugin.getConfig().getBoolean("session.chat.isolation", false);
        log.warning("[DEBUG-ISOLATION] handler fired | isolation-enabled=" + isolationEnabled
                + " | sender=" + event.getPlayer().getName());
        if (!isolationEnabled) return;

        Player sender = event.getPlayer();
        Session senderSession = SessionManager.getInstance().getSession(sender);
        String senderRoom = senderSession != null ? senderSession.getId() : null;
        boolean spectatorIsolation = plugin.getConfig().getBoolean("session.chat.spectator-isolation", false);
        SessionRole senderRole = senderSession != null ? senderSession.getRole(sender) : null;

        log.warning("[DEBUG-ISOLATION] sender=" + sender.getName()
                + " senderRoom=" + senderRoom
                + " spectatorIsolation=" + spectatorIsolation
                + " senderRole=" + senderRole);

        int viewersBefore = event.viewers().size();
        StringBuilder viewerDetail = new StringBuilder();
        for (Audience audience : event.viewers()) {
            if (audience instanceof Player p) {
                Session ps = SessionManager.getInstance().getSession(p);
                String pr = ps != null ? ps.getId() : null;
                viewerDetail.append(p.getName()).append("(room=").append(pr).append(") ");
            } else {
                viewerDetail.append("[non-player:").append(audience.getClass().getSimpleName()).append("] ");
            }
        }
        log.warning("[DEBUG-ISOLATION] viewers before=" + viewersBefore + " | " + viewerDetail.toString().trim());

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

        log.warning("[DEBUG-ISOLATION] toRemove.size=" + toRemove.size());
        event.viewers().removeAll(toRemove);
        log.warning("[DEBUG-ISOLATION] viewers after=" + event.viewers().size());
        // END TEMP DEBUG
    }
}
