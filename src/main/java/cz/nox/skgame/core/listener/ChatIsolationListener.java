package cz.nox.skgame.core.listener;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.SessionRole;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Room-based chat isolation on AsyncPlayerChatEvent (CMI delivery path).
 * AsyncChatEvent left unhandled — Paper delivers to console for logging.
 * Room = session ID for session members, null for lobby.
 */
@SuppressWarnings("deprecation")
public class ChatIsolationListener implements Listener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onLegacyChat(AsyncPlayerChatEvent event) {
        // TEMP DEBUG — revert after diagnosis
        SkGame plugin = SkGame.getInstance();
        Logger log = plugin.getLogger();

        boolean isolationEnabled = plugin.getConfig().getBoolean("session.chat.isolation", false);
        log.warning("[DEBUG-ISOLATION] legacy handler fired | isolation-enabled=" + isolationEnabled
                + " | sender=" + event.getPlayer().getName()
                + " | event.isCancelled()=" + event.isCancelled());

        if (!isolationEnabled) return;

        Player sender = event.getPlayer();
        Session senderSession = SessionManager.getInstance().getSession(sender);
        String senderRoom = senderSession != null ? senderSession.getId() : null;
        boolean spectatorIsolation = plugin.getConfig().getBoolean("session.chat.spectator-isolation", false);
        SessionRole senderRole = senderSession != null ? senderSession.getRole(sender) : null;

        log.warning("[DEBUG-ISOLATION] senderRoom=" + senderRoom
                + " | senderRole=" + senderRole
                + " | getRecipients().size()=" + event.getRecipients().size());

        Set<Player> allowed = new HashSet<>();
        for (Player recipient : event.getRecipients()) {
            Session recipientSession = SessionManager.getInstance().getSession(recipient);
            String recipientRoom = recipientSession != null ? recipientSession.getId() : null;
            if (!Objects.equals(senderRoom, recipientRoom)) continue;
            if (spectatorIsolation && senderRoom != null && senderRole != null) {
                SessionRole recipientRole = senderSession.getRole(recipient);
                boolean senderIsSpec = senderRole == SessionRole.SPECTATOR;
                boolean recipientIsSpec = recipientRole == SessionRole.SPECTATOR;
                if (senderIsSpec != recipientIsSpec) continue;
            }
            allowed.add(recipient);
        }

        StringBuilder allowedNames = new StringBuilder();
        for (Player p : allowed) {
            if (allowedNames.length() > 0) allowedNames.append(",");
            allowedNames.append(p.getName());
        }
        log.warning("[DEBUG-ISOLATION] allowed.size=" + allowed.size() + " | allowed=" + allowedNames);
        // END TEMP DEBUG

        event.setCancelled(true);
        String formatted = formatMessage(event);
        for (Player recipient : allowed) {
            recipient.sendMessage(formatted);
        }
    }

    private String formatMessage(AsyncPlayerChatEvent event) {
        try {
            return String.format(event.getFormat(), event.getPlayer().getDisplayName(), event.getMessage());
        } catch (Exception e) {
            return "<" + event.getPlayer().getDisplayName() + "> " + event.getMessage();
        }
    }
}
