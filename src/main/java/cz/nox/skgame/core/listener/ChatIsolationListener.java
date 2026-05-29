package cz.nox.skgame.core.listener;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.SessionRole;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.gui.services.MainGuiService;
import cz.nox.skgame.util.Debug;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Room-based chat isolation on AsyncPlayerChatEvent (CMI delivery path).
 * AsyncChatEvent left unhandled — Paper delivers to console for logging.
 * Room = session ID for session members, null for lobby.
 */
@SuppressWarnings("deprecation")
public class ChatIsolationListener implements Listener {

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onLegacyChat(AsyncPlayerChatEvent event) {
        SkGame plugin = SkGame.getInstance();
        if (!plugin.getConfig().getBoolean("session.chat.isolation", false)) return;

        boolean isolateLobby = plugin.getConfig().getBoolean("session.chat.isolate-lobby", false);
        boolean spectatorIsolation = plugin.getConfig().getBoolean("session.chat.spectator-isolation", false);

        Player sender = event.getPlayer();
        if (MainGuiService.getInstance().isAwaitingChatInput(sender)) return;
        Session senderSession = SessionManager.getInstance().getSession(sender);
        String senderRoom = roomOf(senderSession, isolateLobby);
        SessionRole senderRole = senderSession != null ? senderSession.getRole(sender) : null;

        Debug.log("chat-isolation", () -> "sender=" + sender.getName()
                + " room=" + senderRoom + " role=" + senderRole
                + " recipients-before=" + event.getRecipients().size());

        Set<Player> allowed = new HashSet<>();
        for (Player recipient : event.getRecipients()) {
            Session recipientSession = SessionManager.getInstance().getSession(recipient);
            String recipientRoom = roomOf(recipientSession, isolateLobby);
            if (!Objects.equals(senderRoom, recipientRoom)) continue;
            if (spectatorIsolation && senderRoom != null && senderRole != null) {
                SessionRole recipientRole = senderSession.getRole(recipient);
                boolean senderIsSpec = senderRole == SessionRole.SPECTATOR;
                boolean recipientIsSpec = recipientRole == SessionRole.SPECTATOR;
                if (senderIsSpec != recipientIsSpec) continue;
            }
            allowed.add(recipient);
        }

        Debug.log("chat-isolation", () -> "allowed=" + allowed.size()
                + " [" + allowed.stream().map(Player::getName).collect(Collectors.joining(", ")) + "]");

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

    private static String roomOf(Session session, boolean isolateLobby) {
        if (session == null) return null;
        if (session.getState() == SessionState.STARTED) return session.getId();
        return isolateLobby ? session.getId() : null;
    }
}
